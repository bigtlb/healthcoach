package com.lbthomas.healthcoach.core.sync.adapters

import co.touchlab.kermit.Logger
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
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

@Serializable
private data class GoogleDriveFileListResponse(
    val files: List<GoogleDriveFileItem> = emptyList()
)

@Serializable
private data class GoogleDriveFileItem(
    val id: String = "",
    val name: String = "",
    val size: String? = null,
    val modifiedTime: String? = null,
    val md5Checksum: String? = null,
    val appProperties: Map<String, String>? = null
)

@Serializable
private data class GoogleDriveCreateFileRequest(
    val name: String,
    val parents: List<String> = listOf("appDataFolder"),
    val appProperties: Map<String, String>? = null
)

@Serializable
private data class GoogleDriveUpdateMetadataRequest(
    val appProperties: Map<String, String>? = null
)

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
    val scopes: List<String> = listOf(SCOPE_DRIVE_APPDATA),
    private val httpClient: HttpClient? = null
) : RemoteStorageAdapter, RequiresAuth {

    override val providerType: SyncProviderType = SyncProviderType.GOOGLE_DRIVE

    private var activeAccessToken: String = accessToken

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

    private val client: HttpClient by lazy {
        httpClient ?: HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    prettyPrint = false
                    isLenient = true
                })
            }
        }
    }

    /**
     * The resolved target directory where Google Drive application data files are stored / staged when using local fallback.
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
            activeAccessToken = token
            _authState.value = AuthState.Authenticated(token)
            if (!customBasePath.isNullOrBlank()) {
                FileUtils.ensureDirectoryExists(targetDirectory)
            }
            Result.success(token)
        } catch (e: Exception) {
            val msg = e.message ?: "Google Drive authentication failed"
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        activeAccessToken = ""
        _authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    private suspend fun executeWithAuthRetry(block: suspend (token: String) -> HttpResponse): HttpResponse {
        var token = activeAccessToken
        var response = block(token)
        if (response.status == HttpStatusCode.Unauthorized && (refreshToken.isNotBlank() || clientId.isNotBlank())) {
            Logger.i("GoogleDriveStorageAdapter: Received HTTP 401, attempting token refresh...")
            val refreshRes = GoogleOAuthManager.refreshAccessToken(clientId, refreshToken)
            if (refreshRes.isSuccess) {
                val session = refreshRes.getOrThrow()
                activeAccessToken = session.accessToken
                token = session.accessToken
                response = block(token)
            }
        }
        return response
    }

    override suspend fun testConnection(): Result<Unit> {
        if (!isAuthenticated()) {
            return Result.failure(IllegalArgumentException("Google Drive account is not authenticated"))
        }

        if (!customBasePath.isNullOrBlank()) {
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

        return try {
            Logger.i("GoogleDriveStorageAdapter: Testing connection against Google Drive AppData space...")
            val response = executeWithAuthRetry { token ->
                client.get(API_BASE_URL) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("spaces", "appDataFolder")
                    parameter("pageSize", "1")
                }
            }

            if (response.status == HttpStatusCode.OK) {
                Logger.i("GoogleDriveStorageAdapter: Connection test successful (HTTP 200 OK)")
                Result.success(Unit)
            } else {
                val errorBody = try { response.bodyAsText() } catch (_: Exception) { "" }
                val errorMsg = "Google Drive API responded with HTTP ${response.status}: $errorBody"
                Logger.w("GoogleDriveStorageAdapter: $errorMsg")
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: Exception) {
            Logger.e("GoogleDriveStorageAdapter: testConnection failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getFileMetadata(fileName: String): FileMetadata? {
        if (!isAuthenticated()) return null

        if (!customBasePath.isNullOrBlank()) {
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

        return try {
            val encodedQuery = "name = '$fileName' and trashed = false"
            val response = executeWithAuthRetry { token ->
                client.get(API_BASE_URL) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("spaces", "appDataFolder")
                    parameter("q", encodedQuery)
                    parameter("fields", "files(id,name,size,modifiedTime,md5Checksum,appProperties)")
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val list = response.body<GoogleDriveFileListResponse>()
                val file = list.files.firstOrNull() ?: return null
                val sizeBytes = file.size?.toLongOrNull() ?: 0L
                val sha256 = file.appProperties?.get("sha256") ?: file.md5Checksum ?: ""
                val lastModified = try {
                    file.modifiedTime?.let { Instant.parse(it).toEpochMilliseconds() } ?: 0L
                } catch (_: Exception) {
                    0L
                }
                FileMetadata(
                    name = fileName,
                    size = sizeBytes,
                    lastModified = lastModified,
                    sha256Hash = sha256,
                    exists = true
                )
            } else {
                Logger.w("GoogleDriveStorageAdapter: getFileMetadata failed with status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Logger.e("GoogleDriveStorageAdapter: getFileMetadata error: ${e.message}", e)
            null
        }
    }

    override suspend fun downloadFile(fileName: String, destinationPath: String): Boolean {
        if (!isAuthenticated()) return false

        if (!customBasePath.isNullOrBlank()) {
            val sourcePath = FileUtils.joinPath(targetDirectory, fileName)
            if (!FileUtils.fileExists(sourcePath)) return false
            return FileUtils.copyFile(sourcePath, destinationPath)
        }

        return try {
            val encodedQuery = "name = '$fileName' and trashed = false"
            val metaResponse = executeWithAuthRetry { token ->
                client.get(API_BASE_URL) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("spaces", "appDataFolder")
                    parameter("q", encodedQuery)
                    parameter("fields", "files(id)")
                }
            }

            if (metaResponse.status != HttpStatusCode.OK) {
                Logger.e("GoogleDriveStorageAdapter: File search failed with status: ${metaResponse.status}")
                return false
            }

            val list = metaResponse.body<GoogleDriveFileListResponse>()
            val fileId = list.files.firstOrNull()?.id
            if (fileId.isNullOrBlank()) {
                Logger.w("GoogleDriveStorageAdapter: Remote file '$fileName' not found in appDataFolder")
                return false
            }

            val destFile = File(destinationPath)
            val destDir = destFile.parentFile ?: File(".")
            destDir.mkdirs()
            val tempFile = File(destDir, "${destFile.name}.download_${generateUuid()}")

            val downloadResponse = executeWithAuthRetry { token ->
                client.get("$API_BASE_URL/$fileId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("alt", "media")
                }
            }

            if (downloadResponse.status != HttpStatusCode.OK) {
                Logger.e("GoogleDriveStorageAdapter: Download media failed with status: ${downloadResponse.status}")
                return false
            }

            val channel: ByteReadChannel = downloadResponse.bodyAsChannel()
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead <= 0) break
                    output.write(buffer, 0, bytesRead)
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                Logger.e("GoogleDriveStorageAdapter: Downloaded file is empty")
                return false
            }

            val moveSuccess = tempFile.renameTo(destFile) || run {
                FileUtils.copyFile(tempFile.absolutePath, destFile.absolutePath).also {
                    tempFile.delete()
                }
            }

            if (moveSuccess) {
                Logger.i("GoogleDriveStorageAdapter: Database downloaded successfully from Google Drive (${destFile.length()} bytes)")
            }
            moveSuccess
        } catch (e: Exception) {
            Logger.e("GoogleDriveStorageAdapter: Error downloading file: ${e.message}", e)
            false
        }
    }

    override suspend fun uploadFile(
        sourcePath: String,
        fileName: String,
        expectedHash: String?
    ): Boolean {
        if (!isAuthenticated()) return false

        if (!customBasePath.isNullOrBlank()) {
            if (!FileUtils.fileExists(sourcePath)) return false
            if (!FileUtils.ensureDirectoryExists(targetDirectory)) return false

            val targetPath = FileUtils.joinPath(targetDirectory, fileName)
            if (expectedHash != null && FileUtils.fileExists(targetPath)) {
                val currentRemoteHash = FileUtils.calculateFileSha256(targetPath)
                if (currentRemoteHash != null && currentRemoteHash != expectedHash) {
                    return false
                }
            }

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

        val srcFile = File(sourcePath)
        if (!srcFile.exists()) {
            Logger.e("GoogleDriveStorageAdapter: Upload source file does not exist: $sourcePath")
            return false
        }

        val fileBytes = srcFile.readBytes()
        val newHash = FileUtils.calculateFileSha256(sourcePath) ?: ""

        return try {
            val encodedQuery = "name = '$fileName' and trashed = false"
            val searchResponse = executeWithAuthRetry { token ->
                client.get(API_BASE_URL) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("spaces", "appDataFolder")
                    parameter("q", encodedQuery)
                    parameter("fields", "files(id,appProperties,md5Checksum)")
                }
            }

            if (searchResponse.status != HttpStatusCode.OK) {
                Logger.e("GoogleDriveStorageAdapter: Upload pre-check failed with status: ${searchResponse.status}")
                return false
            }

            val list = searchResponse.body<GoogleDriveFileListResponse>()
            val existingFile = list.files.firstOrNull()

            if (existingFile != null) {
                // Optimistic concurrency check
                if (!expectedHash.isNullOrBlank()) {
                    val currentRemoteHash = existingFile.appProperties?.get("sha256") ?: existingFile.md5Checksum
                    if (currentRemoteHash != null && currentRemoteHash != expectedHash) {
                        Logger.w("GoogleDriveStorageAdapter: Concurrent modification in Google Drive (expected: $expectedHash, remote: $currentRemoteHash)")
                        return false
                    }
                }

                val fileId = existingFile.id
                // Update metadata
                executeWithAuthRetry { token ->
                    client.patch("$API_BASE_URL/$fileId") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(GoogleDriveUpdateMetadataRequest(appProperties = mapOf("sha256" to newHash)))
                    }
                }

                // Upload media content
                val uploadResponse = executeWithAuthRetry { token ->
                    client.patch("$UPLOAD_BASE_URL/$fileId") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        parameter("uploadType", "media")
                        contentType(ContentType.Application.OctetStream)
                        setBody(fileBytes)
                    }
                }

                if (uploadResponse.status == HttpStatusCode.OK) {
                    Logger.i("GoogleDriveStorageAdapter: Updated '$fileName' in Google Drive appDataFolder (${fileBytes.size} bytes)")
                    true
                } else {
                    Logger.e("GoogleDriveStorageAdapter: Upload media content failed with status: ${uploadResponse.status}")
                    false
                }
            } else {
                // Create file metadata in appDataFolder
                val createResponse = executeWithAuthRetry { token ->
                    client.post(API_BASE_URL) {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(
                            GoogleDriveCreateFileRequest(
                                name = fileName,
                                parents = listOf("appDataFolder"),
                                appProperties = mapOf("sha256" to newHash)
                            )
                        )
                    }
                }

                if (createResponse.status != HttpStatusCode.OK) {
                    Logger.e("GoogleDriveStorageAdapter: Create file metadata failed with status: ${createResponse.status}")
                    return false
                }

                val createdItem = createResponse.body<GoogleDriveFileItem>()
                val fileId = createdItem.id

                // Upload initial content
                val uploadResponse = executeWithAuthRetry { token ->
                    client.patch("$UPLOAD_BASE_URL/$fileId") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        parameter("uploadType", "media")
                        contentType(ContentType.Application.OctetStream)
                        setBody(fileBytes)
                    }
                }

                if (uploadResponse.status == HttpStatusCode.OK) {
                    Logger.i("GoogleDriveStorageAdapter: Uploaded initial '$fileName' to Google Drive appDataFolder (${fileBytes.size} bytes)")
                    true
                } else {
                    Logger.e("GoogleDriveStorageAdapter: Upload initial content failed with status: ${uploadResponse.status}")
                    false
                }
            }
        } catch (e: Exception) {
            Logger.e("GoogleDriveStorageAdapter: Error uploading to Google Drive: ${e.message}", e)
            false
        }
    }

    private fun isAuthenticated(): Boolean {
        return activeAccessToken.isNotBlank() ||
                accessToken.isNotBlank() ||
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
            } else if (userHome.isNotBlank() && userHome != "/") {
                File(userHome, ".healthcoach/google_drive")
            } else {
                File(System.getProperty("java.io.tmpdir") ?: "/tmp", "healthcoach/google_drive")
            }
            val appDir = File(baseRoot, cleanSub)
            return appDir.absolutePath
        }
    }
}
