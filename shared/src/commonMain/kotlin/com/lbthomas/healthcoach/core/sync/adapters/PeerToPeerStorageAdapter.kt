package com.lbthomas.healthcoach.core.sync.adapters

import com.lbthomas.healthcoach.core.logging.LoggingConfig
import com.lbthomas.healthcoach.core.sync.*
import com.lbthomas.healthcoach.core.sync.auth.AuthRequirement
import com.lbthomas.healthcoach.core.sync.auth.AuthState
import com.lbthomas.healthcoach.core.sync.auth.ProviderCredentials
import com.lbthomas.healthcoach.core.sync.auth.RequiresAuth
import com.lbthomas.healthcoach.core.sync.p2p.PairRequest
import com.lbthomas.healthcoach.core.sync.p2p.PairResponse
import com.lbthomas.healthcoach.core.sync.p2p.PeerStatusResponse
import com.lbthomas.healthcoach.core.sync.p2p.ServerSyncMetadata
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
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * Storage adapter targeting a local network HealthCoach peer synchronization server.
 *
 * Implements [RemoteStorageAdapter] and [RequiresAuth] using Ktor HTTP Client.
 * All database operations are transferred over raw binary streams with optimistic
 * concurrency checks via SHA-256 validation.
 */
class PeerToPeerStorageAdapter(
    val serverHost: String = "",
    val serverPort: Int = SyncConfig.DEFAULT_P2P_PORT,
    val serverToken: String = "",
    val serverName: String = "",
    val clientInstanceId: String = "",
    val clientDeviceName: String = "",
    private val httpClient: HttpClient? = null
) : RemoteStorageAdapter, RequiresAuth {

    override val providerType: SyncProviderType = SyncProviderType.PEER_TO_PEER

    private var activeToken: String = serverToken

    private val _authState = MutableStateFlow<AuthState>(
        if (serverToken.isNotBlank()) {
            AuthState.Authenticated(serverToken)
        } else {
            AuthState.Unauthenticated
        }
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val requirement: AuthRequirement = AuthRequirement.SessionToken

    override val securityNotice: String =
        "HealthCoach connects directly to a peer server on your local Wi-Fi network. Database transfers are streamed point-to-point without cloud intermediaries."

    private val client: HttpClient by lazy {
        httpClient ?: HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    prettyPrint = false
                })
            }
        }
    }

    private val baseUrl: String
        get() = "http://${serverHost.trim()}:$serverPort"

    override suspend fun authenticate(credentials: ProviderCredentials): Result<String> {
        if (serverHost.isBlank() || serverPort <= 0) {
            val errorMsg = "Peer server host and port must be configured"
            _authState.value = AuthState.Error(errorMsg)
            return Result.failure(IllegalArgumentException(errorMsg))
        }

        val pinToUse = credentials.password.ifBlank { credentials.token }
        LoggingConfig.clientLogger.i("Initiating pairing handshake with peer server at $baseUrl...")

        return try {
            val request = PairRequest(
                clientInstanceId = clientInstanceId,
                clientName = clientDeviceName.ifBlank { "HealthCoach Client" },
                pin = pinToUse
            )

            val response = client.post("$baseUrl/api/v1/auth/pair") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val pairResponse = response.body<PairResponse>()
                    val token = pairResponse.token
                    activeToken = token
                    _authState.value = AuthState.Authenticated(token)
                    LoggingConfig.clientLogger.i("Pairing successful with peer server '${pairResponse.serverName}' (ID: ${pairResponse.serverInstanceId})")
                    Result.success(token)
                }
                HttpStatusCode.Unauthorized -> {
                    val msg = "Invalid PIN for server at $serverHost:$serverPort"
                    LoggingConfig.clientLogger.w(msg)
                    _authState.value = AuthState.Error("Invalid PIN")
                    Result.failure(IllegalStateException(msg))
                }
                else -> {
                    val msg = "Pairing failed with status: ${response.status}"
                    LoggingConfig.clientLogger.e(msg)
                    _authState.value = AuthState.Error(msg)
                    Result.failure(IllegalStateException(msg))
                }
            }
        } catch (e: Exception) {
            LoggingConfig.clientLogger.e("Error during peer authentication: ${e.message}", e)
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        activeToken = ""
        _authState.value = AuthState.Unauthenticated
        LoggingConfig.clientLogger.i("Disconnected from peer server at $baseUrl")
        return Result.success(Unit)
    }

    override suspend fun testConnection(): Result<Unit> {
        if (serverHost.isBlank() || serverPort <= 0) {
            return Result.failure(IllegalArgumentException("Peer server host and port must be specified"))
        }

        return try {
            val token = activeToken
            val response = client.get("$baseUrl/api/v1/status") {
                if (token.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            if (response.status == HttpStatusCode.OK) {
                val statusResponse = response.body<PeerStatusResponse>()
                if (token.isNotBlank() && !statusResponse.isAccessGranted) {
                    Result.failure(IllegalStateException("Pairing required: server authorization token is invalid or expired"))
                } else {
                    Result.success(Unit)
                }
            } else {
                Result.failure(IllegalStateException("Server responded with HTTP ${response.status}"))
            }
        } catch (e: Exception) {
            LoggingConfig.clientLogger.w("Connection test failed for $baseUrl: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getFileMetadata(fileName: String): FileMetadata? {
        val token = activeToken
        if (token.isBlank()) {
            LoggingConfig.clientLogger.w("Cannot retrieve file metadata: client is unauthenticated")
            return null
        }

        return try {
            val response = client.get("$baseUrl/api/v1/sync/metadata") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                val metadata = response.body<ServerSyncMetadata>()
                if (!metadata.exists) {
                    null
                } else {
                    FileMetadata(
                        name = fileName,
                        size = metadata.sizeBytes,
                        lastModified = metadata.lastModified,
                        sha256Hash = metadata.sha256Hash,
                        exists = true
                    )
                }
            } else {
                LoggingConfig.clientLogger.w("Metadata request failed with status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            LoggingConfig.clientLogger.e("Error retrieving peer metadata: ${e.message}", e)
            null
        }
    }

    override suspend fun downloadFile(fileName: String, destinationPath: String): Boolean {
        val token = activeToken
        if (token.isBlank()) {
            LoggingConfig.clientLogger.w("Cannot download database: client is unauthenticated")
            return false
        }

        val destFile = File(destinationPath)
        val destDir = destFile.parentFile ?: File(".")
        destDir.mkdirs()
        val tempFile = File(destDir, "${destFile.name}.download_${generateUuid()}")

        return try {
            val response = client.get("$baseUrl/api/v1/sync/db/download") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (response.status != HttpStatusCode.OK) {
                LoggingConfig.clientLogger.e("Download failed with server status: ${response.status}")
                return false
            }

            val channel: ByteReadChannel = response.bodyAsChannel()
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
                LoggingConfig.clientLogger.e("Downloaded file is empty")
                return false
            }

            val moveSuccess = tempFile.renameTo(destFile) || run {
                FileUtils.copyFile(tempFile.absolutePath, destFile.absolutePath).also {
                    tempFile.delete()
                }
            }

            if (moveSuccess && LoggingConfig.p2pClientLogging) {
                LoggingConfig.clientLogger.i("Database downloaded successfully from peer server (${destFile.length()} bytes)")
            }
            moveSuccess
        } catch (e: Exception) {
            tempFile.delete()
            LoggingConfig.clientLogger.e("Error downloading database from peer server: ${e.message}", e)
            false
        }
    }

    override suspend fun uploadFile(
        sourcePath: String,
        fileName: String,
        expectedHash: String?
    ): Boolean {
        val token = activeToken
        if (token.isBlank()) {
            LoggingConfig.clientLogger.w("Cannot upload database: client is unauthenticated")
            return false
        }

        val srcFile = File(sourcePath)
        if (!srcFile.exists()) {
            LoggingConfig.clientLogger.e("Upload source file does not exist: $sourcePath")
            return false
        }

        return try {
            val fileBytes = srcFile.readBytes()
            val response = client.post("$baseUrl/api/v1/sync/db/upload") {
                header(HttpHeaders.Authorization, "Bearer $token")
                if (!expectedHash.isNullOrBlank()) {
                    header("X-Expected-Hash", expectedHash)
                }
                contentType(ContentType.Application.OctetStream)
                setBody(fileBytes)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    if (LoggingConfig.p2pClientLogging) {
                        LoggingConfig.clientLogger.i("Database uploaded successfully to peer server (${fileBytes.size} bytes)")
                    }
                    true
                }
                HttpStatusCode.Conflict -> {
                    LoggingConfig.clientLogger.w("Upload rejected by peer server due to concurrent modification (HTTP 409 Conflict)")
                    false
                }
                else -> {
                    LoggingConfig.clientLogger.e("Upload failed with server status: ${response.status}")
                    false
                }
            }
        } catch (e: Exception) {
            LoggingConfig.clientLogger.e("Error uploading database to peer server: ${e.message}", e)
            false
        }
    }
}
