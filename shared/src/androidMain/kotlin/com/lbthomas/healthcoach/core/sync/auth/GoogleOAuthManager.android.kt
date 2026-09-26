package com.lbthomas.healthcoach.core.sync.auth

actual object GoogleOAuthManager {
    actual fun getResolvedClientId(): String {
        val sysProp = System.getProperty("google.clientId.android")?.trim()
        if (!sysProp.isNullOrBlank()) return sysProp
        return GoogleAuthConfig.ANDROID_CLIENT_ID.ifBlank { "dummy-android-client-id.apps.googleusercontent.com" }
    }

    actual fun getResolvedClientSecret(): String = ""

    actual suspend fun authorize(clientId: String): Result<GoogleAuthSession> {
        return Result.failure(UnsupportedOperationException("Google OAuth on Android is handled by Google Sign-In SDK"))
    }

    actual suspend fun validateToken(
        accessToken: String,
        refreshToken: String,
        clientId: String
    ): Result<GoogleTokenValidationResult> {
        return if (accessToken.isNotBlank()) {
            Result.success(GoogleTokenValidationResult(isValid = true))
        } else {
            Result.success(GoogleTokenValidationResult(isValid = false, errorMessage = "No access token"))
        }
    }

    actual suspend fun refreshAccessToken(
        clientId: String,
        refreshToken: String
    ): Result<GoogleAuthSession> {
        return Result.failure(UnsupportedOperationException("Token refresh on Android is handled by Google Play Services"))
    }

    actual suspend fun revokeToken(token: String): Result<Unit> {
        return Result.success(Unit)
    }
}
