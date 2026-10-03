package com.lbthomas.healthcoach.core.sync.auth

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import co.touchlab.kermit.Logger
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transparent activity handling Google Identity Services AuthorizationClient interactive consent flows.
 */
class GoogleAuthActivity : ComponentActivity() {

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            handleAuthorizationIntentResult(result.data)
        } else {
            val ex = Exception("Google authorization consent was cancelled or denied (result code: ${result.resultCode})")
            Logger.w("GoogleAuthActivity: Authorization cancelled or returned non-OK: ${result.resultCode}")
            GoogleOAuthManager.completeAuth(Result.failure(ex))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestedScopes = listOf(
            Scope("https://www.googleapis.com/auth/drive.appdata"),
            Scope("https://www.googleapis.com/auth/userinfo.email"),
            Scope("openid")
        )

        val authRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        val authorizationClient = Identity.getAuthorizationClient(this)

        authorizationClient.authorize(authRequest)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    if (pendingIntent != null) {
                        val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        authLauncher.launch(intentSenderRequest)
                    } else {
                        val ex = IllegalStateException("Authorization resolution required but pendingIntent is null")
                        Logger.e("GoogleAuthActivity: $ex")
                        GoogleOAuthManager.completeAuth(Result.failure(ex))
                        finish()
                    }
                } else {
                    processAuthorizationResult(authorizationResult)
                }
            }
            .addOnFailureListener { e ->
                Logger.e("GoogleAuthActivity: AuthorizationClient.authorize failed: ${e.message}", e)
                GoogleOAuthManager.completeAuth(Result.failure(e))
                finish()
            }
    }

    private fun handleAuthorizationIntentResult(data: android.content.Intent?) {
        try {
            val authorizationResult = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data)
            processAuthorizationResult(authorizationResult)
        } catch (e: ApiException) {
            Logger.e("GoogleAuthActivity: Authorization result API exception (status code ${e.statusCode}): ${e.message}", e)
            GoogleOAuthManager.completeAuth(Result.failure(e))
            finish()
        } catch (e: Exception) {
            Logger.e("GoogleAuthActivity: Failed to process authorization result", e)
            GoogleOAuthManager.completeAuth(Result.failure(e))
            finish()
        }
    }

    private fun processAuthorizationResult(authorizationResult: AuthorizationResult) {
        val accessToken = authorizationResult.accessToken
        if (accessToken.isNullOrBlank()) {
            val ex = IllegalStateException("Authorization succeeded but access token is blank")
            Logger.e("GoogleAuthActivity: $ex")
            GoogleOAuthManager.completeAuth(Result.failure(ex))
            finish()
            return
        }

        Logger.i("GoogleAuthActivity: Successfully acquired OAuth access token via Identity Services")
        CoroutineScope(Dispatchers.IO).launch {
            val validation = GoogleOAuthManager.validateToken(accessToken, "", "")
            val email = validation.getOrNull()?.email ?: ""
            val expiresIn = validation.getOrNull()?.expiresInSeconds ?: 3600L

            withContext(Dispatchers.Main) {
                GoogleOAuthManager.completeAuth(
                    Result.success(
                        GoogleAuthSession(
                            accessToken = accessToken,
                            refreshToken = "",
                            email = email,
                            expiresInSeconds = expiresIn
                        )
                    )
                )
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            GoogleOAuthManager.completeAuthIfPending(
                Result.failure(Exception("Google authorization flow finished without completing authorization"))
            )
        }
    }
}
