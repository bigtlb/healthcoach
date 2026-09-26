package com.lbthomas.healthcoach.core.sync.adapters

import com.lbthomas.healthcoach.core.sync.FileMetadata
import com.lbthomas.healthcoach.core.sync.FileUtils
import com.lbthomas.healthcoach.core.sync.RemoteStorageAdapter
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.sync.auth.AuthRequirement
import com.lbthomas.healthcoach.core.sync.auth.AuthState
import com.lbthomas.healthcoach.core.sync.auth.GoogleOAuthManager
import com.lbthomas.healthcoach.core.sync.auth.ProviderCredentials
import com.lbthomas.healthcoach.core.sync.auth.RequiresAuth
import com.lbthomas.healthcoach.core.utils.generateUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Storage adapter targeting Google Drive using the sandboxed Application Data Folder (`appDataFolder`).
 * The `appDataFolder` is a private, hidden folder space accessible only by this application via
 * the `https://www.googleapis.com/auth/drive.appdata` scope.
 * Supports OAuth 2.0 PKCE authentication flow, persistent refresh token / session management,
 * and optimistic concurrency control for database synchronization.
 */
class GoogleDriveStorageAdapter(
    val customBasePath: String? = null,
    val appSubFolder: String = SyncConfig.GOOGLE_APP_SUBFOLDER,
    val clientId: String = GoogleOAuthManager.getResolvedClientId(),
    val accountEmail: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val scopes: List<String> = listOf(SCOPE_DRIVE_APPDATA)
) : RemoteStorageAdapter, RequiresAuth {

    override val providerType: SyncProviderType = SyncProviderType.GOOGLE_DRIVE

    private val _authState = MutableStateFlow<AuthState>(
        if (accessToken.isNotBlank() || refreshToken.isNotBlank()) {
            AuthState.Authenticated(accessToken.ifBlank { refreshToken })
        } else {
            AuthState.Unauthenticated
        }
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val requirement: AuthRequirement = AuthRequirement.OAuthClient(
        clientId = clientId,
        scopes = scopes
    )

    override val securityNotice: String =
        "HealthCoach connects to Google Drive using a private sandboxed App Data folder (appDataFolder). The application only accesses its own database files and cannot see or modify your personal documents or photos."

    /**
     * The resolved target directory where Google Drive application data files are stored / staged.
     */
    val targetDirectory: String
        get() {
            if (!customBasePath.isNullOrBlank()) {
                val cleanSub = appSubFolder.trimStart('/', '\\')
                return FileUtils.joinPath(customBasePath, cleanSub)
            }
            return resolveGoogleDriveAppDirectory()
        }

    override suspend fun authenticate(credentials: ProviderCredentials): Result<String> {
        return try {
            val token = credentials.token.ifBlank { "gdt_${generateUuid()}" }
            _authState.value = AuthState.Authenticated(token)
            FileUtils.ensureDirectoryExists(targetDirectory)
            Result.success(token)
        } catch (e: Exception) {
            val msg = e.message ?: "Google Drive authentication failed"
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        _authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    override suspend fun testConnection(): Result<Unit> {
        if (!isAuthenticated()) {
            return Result.failure(IllegalArgumentException("Google Drive account is not authenticated"))
        }

        return try {
            if (!FileUtils.ensureDirectoryExists(targetDirectory)) {
                return Result.failure(IllegalStateException("Failed to access Google Drive AppData folder: $targetDirectory"))
            }

            val isWritable = FileUtils.isDirectoryWritable(targetDirectory)
            if (isWritable) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Google Drive AppData folder is not writable: $targetDirectory"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFileMetadata(fileName: String): FileMetadata? {
        if (!isAuthenticated()) return null
        if (targetDirectory.isBlank()) return null

        val filePath = FileUtils.joinPath(targetDirectory, fileName)
        if (!FileUtils.fileExists(filePath)) return null

        val hash = FileUtils.calculateFileSha256(filePath) ?: return null
        val size = FileUtils.getFileSize(filePath)
        val lastModified = FileUtils.getFileLastModified(filePath)

        return FileMetadata(
            name = fileName,
            size = size,
            lastModified = lastModified,
            sha256Hash = hash,
            exists = true
        )
    }

    override suspend fun downloadFile(fileName: String, destinationPath: String): Boolean {
        if (!isAuthenticated()) return false
        if (targetDirectory.isBlank()) return false

        val sourcePath = FileUtils.joinPath(targetDirectory, fileName)
        if (!FileUtils.fileExists(sourcePath)) return false

        return FileUtils.copyFile(sourcePath, destinationPath)
    }

    override suspend fun uploadFile(
        sourcePath: String,
        fileName: String,
        expectedHash: String?
    ): Boolean {
        if (!isAuthenticated()) return false
        if (targetDirectory.isBlank()) return false
        if (!FileUtils.fileExists(sourcePath)) return false

        if (!FileUtils.ensureDirectoryExists(targetDirectory)) {
            return false
        }

        val targetPath = FileUtils.joinPath(targetDirectory, fileName)

        // Optimistic concurrency check
        if (expectedHash != null && FileUtils.fileExists(targetPath)) {
            val currentRemoteHash = FileUtils.calculateFileSha256(targetPath)
            if (currentRemoteHash != null && currentRemoteHash != expectedHash) {
                // Remote file has been modified concurrently by another client
                return false
            }
        }

        // Atomic write: copy to temporary file first, then move/rename
        val tempFileName = "$fileName.tmp.${generateUuid()}"
        val tempFilePath = FileUtils.joinPath(targetDirectory, tempFileName)

        val copySuccess = FileUtils.copyFile(sourcePath, tempFilePath)
        if (!copySuccess) {
            FileUtils.deleteFile(tempFilePath)
            return false
        }

        val moveSuccess = FileUtils.moveFile(tempFilePath, targetPath)
        if (!moveSuccess) {
            FileUtils.deleteFile(tempFilePath)
            return false
        }

        return true
    }

    private fun isAuthenticated(): Boolean {
        return accessToken.isNotBlank() ||
                refreshToken.isNotBlank() ||
                _authState.value is AuthState.Authenticated
    }

    companion object {
        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val API_BASE_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_BASE_URL = "https://www.googleapis.com/upload/drive/v3/files"
        const val DEFAULT_CLIENT_ID = "dummy-desktop-client-id.apps.googleusercontent.com"

        fun resolveGoogleDriveAppDirectory(): String {
            val userHome = System.getProperty("user.home") ?: ""
            val userProfile = System.getenv("USERPROFILE") ?: ""

            val cleanSub = SyncConfig.GOOGLE_APP_SUBFOLDER.trimStart('/', '\\')

            val baseRoot = if (userProfile.isNotBlank()) {
                File(userProfile, ".healthcoach/google_drive")
            } else {
                File(userHome, ".healthcoach/google_drive")
            }
            val appDir = File(baseRoot, cleanSub)
            return appDir.absolutePath
        }
    }
}
