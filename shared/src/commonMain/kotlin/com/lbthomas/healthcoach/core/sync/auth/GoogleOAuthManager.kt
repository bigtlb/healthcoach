package com.lbthomas.healthcoach.core.sync.auth

import kotlinx.serialization.Serializable

@Serializable
data class GoogleTokenValidationResult(
    val isValid: Boolean,
    val email: String? = null,
    val expiresInSeconds: Long? = null,
    val scopes: List<String> = emptyList(),
    val errorMessage: String? = null,
    val newAccessToken: String? = null
)

@Serializable
data class GoogleAuthSession(
    val accessToken: String,
    val refreshToken: String = "",
    val email: String = "",
    val expiresInSeconds: Long? = null
)

expect object GoogleOAuthManager {
    fun getResolvedClientId(): String
    fun getResolvedClientSecret(): String
    suspend fun authorize(clientId: String = ""): Result<GoogleAuthSession>
    suspend fun validateToken(
        accessToken: String,
        refreshToken: String = "",
        clientId: String = ""
    ): Result<GoogleTokenValidationResult>
    suspend fun refreshAccessToken(
        clientId: String,
        refreshToken: String
    ): Result<GoogleAuthSession>
    suspend fun revokeToken(token: String): Result<Unit>
}
