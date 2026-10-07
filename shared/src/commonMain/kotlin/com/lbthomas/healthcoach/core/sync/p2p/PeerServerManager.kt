package com.lbthomas.healthcoach.core.sync.p2p

import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.logging.LoggingConfig
import com.lbthomas.healthcoach.core.sync.FileUtils
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.utils.currentEpochMillis
import com.lbthomas.healthcoach.core.utils.generateUuid
import com.lbthomas.healthcoach.features.foodjournal.data.DefaultFoodData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import com.lbthomas.healthcoach.features.sync.SyncNotificationManager
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * Session metadata for an authenticated peer client.
 */
data class PeerClientSession(
    val clientInstanceId: String,
    val clientName: String,
    val ipAddress: String,
    val pairedAtTimestamp: Long,
    val authToken: String = ""
)

/**
 * Embedded Ktor HTTP server managing local network peer-to-peer sync operations.
 *
 * Provides REST endpoints for:
 * - Server health/status (`GET /api/v1/status`)
 * - PIN-based client pairing & session token generation (`POST /api/v1/auth/pair`)
 * - Remote DB metadata retrieval (`GET /api/v1/sync/metadata`)
 * - Binary SQLite DB download streaming (`GET /api/v1/sync/db/download`)
 * - Binary SQLite DB upload with optimistic concurrency (`POST /api/v1/sync/db/upload`)
 */
class PeerServerManager(
    private val driverFactory: DriverFactory,
    private val settingsStore: SettingsStore,
    private val serverScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val serverMutex = Mutex()
    private var embeddedServerInstance: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    private val _serverStatus = MutableStateFlow(PeerServerStatus())
    val serverStatus: StateFlow<PeerServerStatus> = _serverStatus.asStateFlow()

    // Map of active session tokens to authenticated client sessions
    private val activeClientSessions = mutableMapOf<String, PeerClientSession>()

    /**
     * Optional callback invoked after a new database snapshot is uploaded and swapped on disk.
     */
    var onDatabaseReset: (suspend () -> Unit)? = null

    /**
     * SQLite header magic bytes for verifying uploaded database file integrity.
     */
    companion object {
        private val SQLITE_MAGIC_HEADER = "SQLite format 3\u0000".encodeToByteArray()
        private const val MAX_PORT_RETRY_ATTEMPTS = 10
    }

    /**
     * Starts the embedded Ktor server on the configured or fallback port.
     */
    suspend fun start(preferredPort: Int? = null): Result<Int> = serverMutex.withLock {
        if (_serverStatus.value.isRunning && embeddedServerInstance != null) {
            return Result.success(_serverStatus.value.port)
        }

        _serverStatus.value = _serverStatus.value.copy(
            isStarting = true,
            isStopping = false,
            errorMessage = null
        )

        val initialPort = preferredPort
            ?: settingsStore.settings.value.peerSync.localServerPort.takeIf { it > 0 }
            ?: SyncConfig.DEFAULT_P2P_PORT

        var currentPort = initialPort
        var boundPort: Int? = null
        var lastException: Throwable? = null

        val activePin = settingsStore.settings.value.peerSync.localServerPin.ifBlank {
            // Generate a random 4-digit PIN if none is configured
            (1000..9999).random().toString().also { generated ->
                settingsStore.setPeerServerPin(generated)
            }
        }

        for (attempt in 0..MAX_PORT_RETRY_ATTEMPTS) {
            try {
                LoggingConfig.serverLogger.i("Attempting to start P2P server on port $currentPort (attempt ${attempt + 1})...")

                val server = embeddedServer(
                    factory = CIO,
                    port = currentPort,
                    host = "0.0.0.0"
                ) {
                    configureServerPlugins()
                    configureServerRoutes()
                }

                server.start(wait = false)

                val actualPort = server.engine.resolvedConnectors().firstOrNull()?.port ?: currentPort
                embeddedServerInstance = server
                boundPort = actualPort

                _serverStatus.value = PeerServerStatus(
                    isRunning = true,
                    isStarting = false,
                    isStopping = false,
                    host = "0.0.0.0",
                    port = actualPort,
                    activePin = activePin,
                    errorMessage = null
                )

                // Update transient runtime port in settings
                settingsStore.updateSettings { current ->
                    current.copy(peerSync = current.peerSync.copy(serverPort = actualPort))
                }

                // Pre-populate active sessions from persisted history
                settingsStore.settings.value.peerSync.localServerHistory.forEach { record ->
                    if (record.authToken.isNotBlank()) {
                        activeClientSessions[record.authToken] = PeerClientSession(
                            clientInstanceId = record.clientInstanceId,
                            clientName = record.clientName,
                            ipAddress = record.ipAddress,
                            pairedAtTimestamp = record.lastAccessTimestamp,
                            authToken = record.authToken
                        )
                    }
                }

                LoggingConfig.serverLogger.i("P2P server successfully listening on port $actualPort (PIN: $activePin)")
                break
            } catch (e: Throwable) {
                LoggingConfig.serverLogger.w("Port $currentPort unavailable: ${e.message}")
                lastException = e
                currentPort++
            }
        }

        return if (boundPort != null) {
            Result.success(boundPort)
        } else {
            val errorMsg = "Failed to bind P2P server after $MAX_PORT_RETRY_ATTEMPTS attempts: ${lastException?.message}"
            LoggingConfig.serverLogger.e(errorMsg, lastException)
            _serverStatus.value = PeerServerStatus(
                isRunning = false,
                isStarting = false,
                isStopping = false,
                port = initialPort,
                activePin = activePin,
                errorMessage = errorMsg
            )
            Result.failure(lastException ?: IllegalStateException(errorMsg))
        }
    }

    /**
     * Stops the embedded Ktor server.
     */
    suspend fun stop(): Unit = serverMutex.withLock {
        if (!_serverStatus.value.isRunning && embeddedServerInstance == null && !_serverStatus.value.isStarting) {
            _serverStatus.value = _serverStatus.value.copy(isStopping = false, isStarting = false)
            return
        }
        _serverStatus.value = _serverStatus.value.copy(
            isStopping = true,
            isStarting = false
        )
        try {
            if (embeddedServerInstance != null) {
                SyncNotificationManager.postNotification("Shutting down Peer-to-Peer server...")
            }
            embeddedServerInstance?.stop(gracePeriodMillis = 100, timeoutMillis = 500)
            embeddedServerInstance = null
            _serverStatus.value = _serverStatus.value.copy(
                isRunning = false,
                isStarting = false,
                isStopping = false,
                errorMessage = null
            )
            LoggingConfig.serverLogger.i("P2P server stopped")
        } catch (e: Throwable) {
            LoggingConfig.serverLogger.e("Error stopping P2P server: ${e.message}", e)
            _serverStatus.value = _serverStatus.value.copy(
                isRunning = false,
                isStarting = false,
                isStopping = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * Restarts the server with the current configuration.
     */
    suspend fun restart(port: Int? = null): Result<Int> {
        stop()
        return start(port)
    }

    /**
     * Configures Ktor middleware: ContentNegotiation, CORS, and StatusPages.
     */
    private fun io.ktor.server.application.Application.configureServerPlugins() {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                prettyPrint = false
            })
        }

        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader("X-Expected-Hash")
            allowHeader("X-Database-Hash")
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Options)
        }

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                LoggingConfig.serverLogger.e("Unhandled error processing request: ${cause.message}", cause)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (cause.message ?: "Internal Server Error"))
                )
            }
        }
    }

    /**
     * Configures routing endpoints for P2P synchronization.
     */
    private fun io.ktor.server.application.Application.configureServerRoutes() {
        routing {
            // Health & Status check
            get("/api/v1/status") {
                val settings = settingsStore.settings.value
                val status = _serverStatus.value

                val authHeader = call.request.headers[HttpHeaders.Authorization]
                val isAuthorized = if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
                    val token = authHeader.substring(7).trim()
                    findSessionByToken(token) != null
                } else {
                    false
                }

                val authStatus = if (isAuthorized) PeerAuthStatus.ACCESS_GRANTED else PeerAuthStatus.PAIRING_REQUIRED

                call.respond(
                    HttpStatusCode.OK,
                    PeerStatusResponse(
                        status = "OK",
                        instanceId = settings.peerSync.instanceId,
                        deviceName = settings.peerSync.deviceName,
                        port = status.port.toString(),
                        interfaceVersion = "1.0",
                        isRunning = status.isRunning.toString(),
                        authStatus = authStatus
                    )
                )
            }

            // On-demand PIN generation and notification request
            post("/api/v1/auth/request-pin") {
                val initRequest = try {
                    call.receive<PairInitRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid JSON payload: ${e.message}"))
                    return@post
                }

                val clientIp = call.request.origin.remoteAddress
                val randomPin = kotlin.random.Random.nextInt(1000, 9999).toString()
                _serverStatus.value = _serverStatus.value.copy(activePin = randomPin)

                // Show persistent pairing prompt dialog and post notification on server
                SyncNotificationManager.showPairingPrompt(initRequest.clientName, randomPin)
                serverScope.launch {
                    SyncNotificationManager.postNotification("Peer '${initRequest.clientName}' requested pairing")
                }

                if (LoggingConfig.p2pServerLogging) {
                    LoggingConfig.serverLogger.i("Generated pairing PIN $randomPin for '${initRequest.clientName}' ($clientIp)")
                }

                call.respond(
                    HttpStatusCode.OK,
                    mapOf("status" to "PIN_GENERATED")
                )
            }

            // PIN-based client pairing
            post("/api/v1/auth/pair") {
                val pairRequest = try {
                    call.receive<PairRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid JSON payload: ${e.message}"))
                    return@post
                }

                val currentPin = _serverStatus.value.activePin.ifBlank {
                    settingsStore.settings.value.peerSync.localServerPin
                }

                val clientIp = call.request.origin.remoteAddress

                if (currentPin.isNotBlank() && pairRequest.pin.trim() != currentPin.trim()) {
                    LoggingConfig.serverLogger.w("Rejected pairing attempt from ${pairRequest.clientName} ($clientIp): incorrect PIN")
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid PIN"))
                    return@post
                }

                // Clear one-time PIN and dismiss prompt upon successful pairing
                _serverStatus.value = _serverStatus.value.copy(activePin = "")
                SyncNotificationManager.dismissPairingPrompt()

                val token = generateUuid()
                val session = PeerClientSession(
                    clientInstanceId = pairRequest.clientInstanceId,
                    clientName = pairRequest.clientName,
                    ipAddress = clientIp,
                    pairedAtTimestamp = currentEpochMillis(),
                    authToken = token
                )
                activeClientSessions[token] = session

                // Record in persistent history
                settingsStore.addServerHistoryRecord(
                    PeerClientRecord(
                        authToken = token,
                        clientInstanceId = pairRequest.clientInstanceId,
                        clientName = pairRequest.clientName,
                        ipAddress = clientIp,
                        lastAccessTimestamp = currentEpochMillis(),
                        lastAction = "PAIRED"
                    )
                )

                serverScope.launch {
                    SyncNotificationManager.postNotification("Peer '${pairRequest.clientName}' paired successfully")
                }

                val serverSettings = settingsStore.settings.value
                call.respond(
                    HttpStatusCode.OK,
                    PairResponse(
                        token = token,
                        serverInstanceId = serverSettings.peerSync.instanceId,
                        serverName = serverSettings.peerSync.deviceName
                    )
                )
            }

            // Remote database metadata
            get("/api/v1/sync/metadata") {
                val session = authenticate(call) ?: return@get
                val dbPath = driverFactory.getDatabaseFilePath()

                if (!FileUtils.fileExists(dbPath)) {
                    call.respond(HttpStatusCode.OK, ServerSyncMetadata(exists = false))
                    return@get
                }

                val hash = FileUtils.calculateFileSha256(dbPath) ?: ""
                val size = FileUtils.getFileSize(dbPath)
                val lastModified = FileUtils.getFileLastModified(dbPath)

                if (LoggingConfig.p2pServerLogging) {
                    LoggingConfig.serverLogger.d("Sync metadata served to '${session.clientName}' (hash: $hash, size: $size)")
                }

                call.respond(
                    HttpStatusCode.OK,
                    ServerSyncMetadata(
                        exists = true,
                        sha256Hash = hash,
                        sizeBytes = size,
                        lastModified = lastModified
                    )
                )
            }

            // Binary SQLite DB download stream
            get("/api/v1/sync/db/download") {
                val session = authenticate(call) ?: return@get
                val dbPath = driverFactory.getDatabaseFilePath()

                if (!FileUtils.fileExists(dbPath)) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Database file does not exist on server"))
                    return@get
                }

                val dbFile = File(dbPath)
                val hash = FileUtils.calculateFileSha256(dbPath) ?: ""
                val fileBytes = dbFile.readBytes()

                // Record client history and notification
                settingsStore.addServerHistoryRecord(
                    PeerClientRecord(
                        authToken = session.authToken,
                        clientInstanceId = session.clientInstanceId,
                        clientName = session.clientName,
                        ipAddress = session.ipAddress,
                        lastAccessTimestamp = currentEpochMillis(),
                        lastAction = "DOWNLOAD"
                    )
                )

                serverScope.launch {
                    SyncNotificationManager.postNotification("Peer '${session.clientName}' downloaded database")
                }

                if (LoggingConfig.p2pServerLogging) {
                    LoggingConfig.serverLogger.i("Database downloaded by '${session.clientName}' (${fileBytes.size} bytes, hash: $hash)")
                }

                call.response.header(HttpHeaders.ContentLength, fileBytes.size.toString())
                call.response.header("X-Database-Hash", hash)
                call.respondBytes(
                    bytes = fileBytes,
                    contentType = ContentType.Application.OctetStream,
                    status = HttpStatusCode.OK
                )
            }

            // Binary SQLite DB upload stream with optimistic concurrency check
            post("/api/v1/sync/db/upload") {
                val session = authenticate(call) ?: return@post
                val dbPath = driverFactory.getDatabaseFilePath()
                val expectedHash = call.request.header("X-Expected-Hash")

                // Optimistic concurrency check: if server DB exists, verify expected hash
                if (expectedHash != null && FileUtils.fileExists(dbPath)) {
                    val currentServerHash = FileUtils.calculateFileSha256(dbPath)
                    if (currentServerHash != null && currentServerHash != expectedHash) {
                        LoggingConfig.serverLogger.w("Upload conflict from '${session.clientName}': expected $expectedHash, server has $currentServerHash")
                        call.respond(
                            HttpStatusCode.Conflict,
                            mapOf(
                                "error" to "Database modified on server",
                                "currentHash" to currentServerHash,
                                "expectedHash" to expectedHash
                            )
                        )
                        return@post
                    }
                }

                val stagingFileName = "${File(dbPath).name}.upload_staging_${generateUuid()}"
                val stagingDir = File(dbPath).parentFile ?: File(".")
                val stagingFile = File(stagingDir, stagingFileName)

                try {
                    stagingDir.mkdirs()

                    // Stream request body into staging file
                    val channel: ByteReadChannel = call.receiveChannel()
                    FileOutputStream(stagingFile).use { output ->
                        val buffer = ByteArray(8192)
                        while (!channel.isClosedForRead) {
                            val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                            if (bytesRead <= 0) break
                            output.write(buffer, 0, bytesRead)
                        }
                    }

                    // Verify SQLite header integrity
                    if (!isValidSqliteDatabase(stagingFile)) {
                        stagingFile.delete()
                        LoggingConfig.serverLogger.e("Uploaded file from '${session.clientName}' is not a valid SQLite database")
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Uploaded file is not a valid SQLite database"))
                        return@post
                    }

                    // Atomic swap of the database file
                    val targetFile = File(dbPath)
                    val backupFile = File(stagingDir, "${targetFile.name}.bak_${generateUuid()}")

                    if (targetFile.exists()) {
                        targetFile.renameTo(backupFile)
                    }

                    val moveSuccess = stagingFile.renameTo(targetFile) || run {
                        FileUtils.copyFile(stagingFile.absolutePath, targetFile.absolutePath).also {
                            stagingFile.delete()
                        }
                    }

                    if (!moveSuccess) {
                        backupFile.renameTo(targetFile)
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to atomically replace database file"))
                        return@post
                    }

                    backupFile.delete()

                    // Invoke live database reset hook
                    onDatabaseReset?.invoke()

                    val newHash = FileUtils.calculateFileSha256(dbPath) ?: ""
                    val newSize = FileUtils.getFileSize(dbPath)

                    // Record client history and notification
                    settingsStore.addServerHistoryRecord(
                        PeerClientRecord(
                            authToken = session.authToken,
                            clientInstanceId = session.clientInstanceId,
                            clientName = session.clientName,
                            ipAddress = session.ipAddress,
                            lastAccessTimestamp = currentEpochMillis(),
                            lastAction = "UPLOAD"
                        )
                    )

                    // Clear any sync errors and record success from client update
                    settingsStore.updateSettings {
                        it.copy(
                            sync = it.sync.copy(
                                lastSyncStatus = "Success (Updated by ${session.clientName})",
                                lastSyncTime = currentEpochMillis(),
                                lastSyncHash = newHash,
                                lastSyncError = null,
                                lastSyncFailed = false
                            )
                        )
                    }

                    serverScope.launch {
                        SyncNotificationManager.postNotification("Peer '${session.clientName}' uploaded new database snapshot")
                    }

                    if (LoggingConfig.p2pServerLogging) {
                        LoggingConfig.serverLogger.i("Database updated by '${session.clientName}' (${newSize} bytes, hash: $newHash)")
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ServerSyncMetadata(
                            exists = true,
                            sha256Hash = newHash,
                            sizeBytes = newSize,
                            lastModified = currentEpochMillis()
                        )
                    )
                } catch (e: Exception) {
                    stagingFile.delete()
                    LoggingConfig.serverLogger.e("Error processing database upload from '${session.clientName}': ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to process upload: ${e.message}"))
                }
            }
        }
    }

    /**
     * Resets the local server database by clearing all metric tables and restoring default food items.
     */
    suspend fun resetDatabase(): Result<Unit> = serverMutex.withLock {
        try {
            val driver = driverFactory.createDriver()
            try {
                driver.execute(null, "DELETE FROM weightEntry", 0, null)
                driver.execute(null, "DELETE FROM bloodPressureEntry", 0, null)
                driver.execute(null, "DELETE FROM mealEntry", 0, null)
                driver.execute(null, "DELETE FROM foodItem", 0, null)
                driver.execute(null, "DELETE FROM foodUnit", 0, null)
                driver.execute(null, "DELETE FROM profileSetting", 0, null)

                val db = createDatabaseForDriver(driver)
                DefaultFoodData.ensureDefaultFoodData(db)
                driver.notifyListeners("weightEntry", "bloodPressureEntry", "foodUnit", "foodItem", "mealEntry", "profileSetting")
            } finally {
                try { driver.close() } catch (_: Exception) {}
            }

            onDatabaseReset?.invoke()
            LoggingConfig.serverLogger.i("Server database has been reset to defaults.")
            Result.success(Unit)
        } catch (e: Exception) {
            LoggingConfig.serverLogger.e("Failed to reset server database: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Validates that the file starts with SQLite magic header bytes.
     */
    private fun isValidSqliteDatabase(file: File): Boolean {
        if (!file.exists() || file.length() < SQLITE_MAGIC_HEADER.size) return false
        val headerBytes = ByteArray(SQLITE_MAGIC_HEADER.size)
        file.inputStream().use { input ->
            val read = input.read(headerBytes)
            if (read != SQLITE_MAGIC_HEADER.size) return false
        }
        return headerBytes.contentEquals(SQLITE_MAGIC_HEADER)
    }

    /**
     * Resolves an authenticated client session by token from active memory or persistent settings.
     */
    private fun findSessionByToken(token: String): PeerClientSession? {
        if (token.isBlank()) return null
        activeClientSessions[token]?.let { return it }

        val persisted = settingsStore.settings.value.peerSync.localServerHistory.find {
            it.authToken.isNotBlank() && it.authToken == token
        }
        if (persisted != null) {
            val restored = PeerClientSession(
                clientInstanceId = persisted.clientInstanceId,
                clientName = persisted.clientName,
                ipAddress = persisted.ipAddress,
                pairedAtTimestamp = persisted.lastAccessTimestamp,
                authToken = persisted.authToken
            )
            activeClientSessions[token] = restored
            return restored
        }
        return null
    }

    /**
     * Authenticates an incoming call via Bearer token.
     */
    private suspend fun authenticate(call: ApplicationCall): PeerClientSession? {
        val authHeader = call.request.headers[HttpHeaders.Authorization]
        if (authHeader == null || !authHeader.startsWith("Bearer ", ignoreCase = true)) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing or invalid Authorization header"))
            return null
        }

        val token = authHeader.substring(7).trim()
        val session = findSessionByToken(token)

        if (session == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired session token"))
            return null
        }

        return session
    }

    /**
     * Registers a pre-authenticated session token (useful for testing or cached credentials).
     */
    fun registerSessionToken(token: String, session: PeerClientSession) {
        val updatedSession = if (session.authToken.isBlank()) session.copy(authToken = token) else session
        activeClientSessions[token] = updatedSession
    }

    /**
     * Revokes an active session token.
     */
    fun revokeSessionToken(token: String) {
        activeClientSessions.remove(token)
    }

    /**
     * Clears all active session tokens.
     */
    fun clearAllSessions() {
        activeClientSessions.clear()
    }
}
