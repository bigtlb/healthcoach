package com.lbthomas.healthcoach.core.sync.auth

import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

actual object GoogleOAuthManager {
    private var appContext: Context? = null
    private var pendingDeferred: CompletableDeferred<Result<GoogleAuthSession>>? = null
    private val authMutex = Mutex()

    private val driveScopes = listOf(
        Scope("https://www.googleapis.com/auth/drive.appdata"),
        Scope("https://www.googleapis.com/auth/userinfo.email"),
        Scope("openid")
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    internal fun completeAuth(result: Result<GoogleAuthSession>) {
        pendingDeferred?.complete(result)
        pendingDeferred = null
    }

    internal fun completeAuthIfPending(result: Result<GoogleAuthSession>) {
        if (pendingDeferred?.isActive == true) {
            pendingDeferred?.complete(result)
            pendingDeferred = null
        }
    }

    actual fun getResolvedClientId(): String {
        val sysProp = System.getProperty("google.clientId.android")?.trim()
        if (!sysProp.isNullOrBlank()) return sysProp
        return GoogleAuthConfig.ANDROID_CLIENT_ID.ifBlank { "" }
    }

    actual fun getResolvedClientSecret(): String = ""

    actual suspend fun authorize(clientId: String): Result<GoogleAuthSession> = withContext(Dispatchers.IO) {
        val context = appContext
            ?: return@withContext Result.failure(IllegalStateException("Android Application Context not initialized in GoogleOAuthManager"))

        // Attempt silent authorization via Google Identity Services
        try {
            val authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(driveScopes)
                .build()

            val authorizationClient = Identity.getAuthorizationClient(context)
            val authResultTask = authorizationClient.authorize(authRequest)
            val authResult = Tasks.await(authResultTask, 3, TimeUnit.SECONDS)

            if (!authResult.hasResolution() && !authResult.accessToken.isNullOrBlank()) {
                val token = authResult.accessToken!!
                Logger.i("GoogleOAuthManager: Silent authorization succeeded via Identity Services")
                val validation = validateToken(token, "", "")
                val email = validation.getOrNull()?.email ?: ""
                val expiresIn = validation.getOrNull()?.expiresInSeconds ?: 3600L
                return@withContext Result.success(
                    GoogleAuthSession(
                        accessToken = token,
                        refreshToken = "",
                        email = email,
                        expiresInSeconds = expiresIn
                    )
                )
            }
        } catch (e: Exception) {
            Logger.d("GoogleOAuthManager: Silent authorization not available, opening interactive consent: ${e.message}")
        }

        val deferred = CompletableDeferred<Result<GoogleAuthSession>>()
        authMutex.withLock {
            pendingDeferred?.cancel()
            pendingDeferred = deferred

            val intent = Intent(context, GoogleAuthActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        deferred.await()
    }

    actual suspend fun validateToken(
        accessToken: String,
        refreshToken: String,
        clientId: String
    ): Result<GoogleTokenValidationResult> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            Logger.d("GoogleOAuthManager: Validation requested with empty access token")
            return@withContext Result.success(
                GoogleTokenValidationResult(
                    isValid = false,
                    errorMessage = "No access token present. Click Authorize to connect."
                )
            )
        }

        try {
            Logger.i("GoogleOAuthManager: Validating token at https://oauth2.googleapis.com/tokeninfo...")
            val url = URL("https://oauth2.googleapis.com/tokeninfo?access_token=" + URLEncoder.encode(accessToken, "UTF-8"))
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }
            val statusCode = conn.responseCode
            val responseBody = if (statusCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            if (statusCode == 200) {
                val bodyJson = json.parseToJsonElement(responseBody).jsonObject
                val email = bodyJson["email"]?.jsonPrimitive?.content
                val expiresIn = bodyJson["expires_in"]?.jsonPrimitive?.longOrNull
                val scopeStr = bodyJson["scope"]?.jsonPrimitive?.content ?: ""
                val scopes = scopeStr.split(" ").filter { it.isNotBlank() }
                Logger.i("GoogleOAuthManager: Token is valid. Email: $email, expiresIn: ${expiresIn}s, scopes: $scopes")
                Result.success(
                    GoogleTokenValidationResult(
                        isValid = true,
                        email = email,
                        expiresInSeconds = expiresIn,
                        scopes = scopes
                    )
                )
            } else {
                Logger.w("GoogleOAuthManager: Tokeninfo reported invalid/expired token (HTTP $statusCode): $responseBody")
                // Attempt refresh via Google Identity Services
                val refreshResult = refreshAccessToken(clientId, accessToken)
                if (refreshResult.isSuccess) {
                    val session = refreshResult.getOrThrow()
                    Logger.i("GoogleOAuthManager: Token refreshed successfully during validation")
                    Result.success(
                        GoogleTokenValidationResult(
                            isValid = true,
                            email = session.email,
                            expiresInSeconds = session.expiresInSeconds,
                            newAccessToken = session.accessToken
                        )
                    )
                } else {
                    Result.success(
                        GoogleTokenValidationResult(
                            isValid = false,
                            errorMessage = "Access token is invalid or expired."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.e("GoogleOAuthManager: Exception during token validation: ${e.message}", e)
            Result.failure(e)
        }
    }

    actual suspend fun refreshAccessToken(
        clientId: String,
        refreshToken: String
    ): Result<GoogleAuthSession> = withContext(Dispatchers.IO) {
        val context = appContext
            ?: return@withContext Result.failure(IllegalStateException("Android Application Context not initialized"))

        try {
            val authRequest = AuthorizationRequest.builder()
                .setRequestedScopes(driveScopes)
                .build()

            val authorizationClient = Identity.getAuthorizationClient(context)
            val authResult = Tasks.await(authorizationClient.authorize(authRequest), 5, TimeUnit.SECONDS)

            if (!authResult.hasResolution() && !authResult.accessToken.isNullOrBlank()) {
                val token = authResult.accessToken!!
                Logger.i("GoogleOAuthManager: Successfully refreshed Google OAuth token via Identity Services")
                val validation = validateToken(token, "", "")
                val email = validation.getOrNull()?.email ?: ""
                val expiresIn = validation.getOrNull()?.expiresInSeconds ?: 3600L
                Result.success(
                    GoogleAuthSession(
                        accessToken = token,
                        refreshToken = "",
                        email = email,
                        expiresInSeconds = expiresIn
                    )
                )
            } else {
                Result.failure(IllegalStateException("Token refresh requires user interaction"))
            }
        } catch (e: Exception) {
            Logger.e("GoogleOAuthManager: Failed to refresh token via Identity Services: ${e.message}", e)
            Result.failure(e)
        }
    }

    actual suspend fun revokeToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        val context = appContext
            ?: return@withContext Result.failure(IllegalStateException("Android Application Context not initialized"))

        try {
            val signInClient = Identity.getSignInClient(context)
            try {
                Tasks.await(signInClient.signOut(), 5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Logger.w("GoogleOAuthManager: signInClient.signOut failed or timed out: ${e.message}")
            }
            Logger.i("GoogleOAuthManager: Successfully signed out Google account via Identity Services")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.w("GoogleOAuthManager: Error during revoke/sign-out: ${e.message}")
            Result.success(Unit)
        }
    }
}
