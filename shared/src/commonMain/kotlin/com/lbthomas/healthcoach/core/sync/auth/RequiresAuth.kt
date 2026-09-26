package com.lbthomas.healthcoach.core.sync.auth

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Represents the current authentication status of a storage provider.
 */
sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val sessionToken: String = "") : AuthState
    data object Expired : AuthState
    data class Error(val message: String) : AuthState
}

/**
 * Represents the specific credential requirements for a storage provider.
 */
sealed interface AuthRequirement {
    data object None : AuthRequirement
    data class UsernamePassword(
        val domainRequired: Boolean = false,
        val twoFactorRequired: Boolean = false
    ) : AuthRequirement
    data object SessionToken : AuthRequirement
    data object LocalPath : AuthRequirement
    data class OAuthWebFlow(val authUrl: String, val redirectUri: String) : AuthRequirement
    data class OAuthClient(val clientId: String, val scopes: List<String> = emptyList()) : AuthRequirement
}

/**
 * Holder for credential data provided by the user.
 */
@Serializable
data class ProviderCredentials(
    val username: String = "",
    val password: String = "",
    val twoFactorCode: String = "",
    val token: String = "",
    val domain: String = "",
    val path: String = ""
)

/**
 * Interface for storage adapters that require authentication and session management.
 */
interface RequiresAuth {
    /**
     * Active authentication state flow.
     */
    val authState: StateFlow<AuthState>

    /**
     * The type of credentials or setup required by this adapter.
     */
    val requirement: AuthRequirement

    /**
     * Optional security or compliance notice to be displayed to the user when entering credentials.
     */
    val securityNotice: String?
        get() = null

    /**
     * Authenticate using the provided credentials, returning a persistent session token on success.
     */
    suspend fun authenticate(credentials: ProviderCredentials): Result<String>

    /**
     * Disconnect / forget the active session and credentials.
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * Refresh active access token if supported.
     */
    suspend fun refreshToken(): Result<Unit> {
        return Result.success(Unit)
    }
}
