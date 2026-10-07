package com.lbthomas.healthcoach.core.sync.p2p

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.sync.FileUtils
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.PeerSyncSettings
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import com.lbthomas.healthcoach.features.settings.data.SyncSettings
import com.lbthomas.healthcoach.features.sync.SyncNotificationManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class PeerServerManagerTest {

    private lateinit var tempDir: File
    private lateinit var dbDir: File
    private lateinit var settingsFile: File
    private lateinit var settingsStore: SettingsStore
    private lateinit var testDriverFactory: DriverFactory
    private lateinit var serverManager: PeerServerManager
    private lateinit var httpClient: HttpClient

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("peer_server_test").toFile()
        dbDir = File(tempDir, "db").apply { mkdirs() }
        settingsFile = File(tempDir, "settings.json")
        settingsStore = SettingsStore(settingsFile)

        testDriverFactory = object : DriverFactory() {
            override fun createDriver() = JdbcSqliteDriver("jdbc:sqlite:${dbDir.absolutePath}/healthcoach.db")
            override fun createDriverForPath(dbFilePath: String) = JdbcSqliteDriver("jdbc:sqlite:$dbFilePath")
            override fun getDatabaseDirectory() = dbDir.absolutePath
            override fun getDatabaseFilePath() = "${dbDir.absolutePath}/healthcoach.db"
        }

        // Initialize SQLite database
        val driver = testDriverFactory.createDriver()
        val db = createDatabaseForDriver(driver)
        db.weightEntryQueries.insert(
            id = "entry_1",
            date = "2026-09-27",
            weight = 175.5,
            updated_at = 1000L
        )

        serverManager = PeerServerManager(
            driverFactory = testDriverFactory,
            settingsStore = settingsStore
        )

        httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
        }
    }

    @AfterTest
    fun tearDown() {
        runBlocking {
            httpClient.close()
            serverManager.stop()
        }
        tempDir.deleteRecursively()
    }

    @Test
    fun testServerLifecycle_StartStatusAndStop() {
        runBlocking {
            settingsStore.setPeerServerPin("1234")
            val startResult = serverManager.start(preferredPort = 0)
            assertTrue(startResult.isSuccess, "Server should start successfully")

            val port = startResult.getOrThrow()
            assertTrue(port > 0, "Bound port must be greater than 0")
            assertTrue(serverManager.serverStatus.value.isRunning, "Server status isRunning should be true")

            // Health / Status Check without token -> 200 OK with PAIRING_REQUIRED
            val response = httpClient.get("http://127.0.0.1:$port/api/v1/status")
            assertEquals(HttpStatusCode.OK, response.status)
            val unauthStatus = response.body<PeerStatusResponse>()
            assertEquals("OK", unauthStatus.status)
            assertEquals(PeerAuthStatus.PAIRING_REQUIRED, unauthStatus.authStatus)
            assertTrue(unauthStatus.isPairingRequired)

            // Health / Status Check with bad token -> 200 OK with PAIRING_REQUIRED
            val badAuthResponse = httpClient.get("http://127.0.0.1:$port/api/v1/status") {
                header(HttpHeaders.Authorization, "Bearer invalid-token-12345")
            }
            assertEquals(HttpStatusCode.OK, badAuthResponse.status)
            val badAuthStatus = badAuthResponse.body<PeerStatusResponse>()
            assertEquals(PeerAuthStatus.PAIRING_REQUIRED, badAuthStatus.authStatus)

            // Register a session token
            val validToken = "test-token-valid-abc"
            serverManager.registerSessionToken(
                validToken,
                PeerClientSession("client-1", "Client One", "127.0.0.1", System.currentTimeMillis())
            )

            // Health / Status Check with valid token -> 200 OK with ACCESS_GRANTED
            val validAuthResponse = httpClient.get("http://127.0.0.1:$port/api/v1/status") {
                header(HttpHeaders.Authorization, "Bearer $validToken")
            }
            assertEquals(HttpStatusCode.OK, validAuthResponse.status)
            val validAuthStatus = validAuthResponse.body<PeerStatusResponse>()
            assertEquals(PeerAuthStatus.ACCESS_GRANTED, validAuthStatus.authStatus)
            assertTrue(validAuthStatus.isAccessGranted)

            serverManager.stop()
            assertFalse(serverManager.serverStatus.value.isRunning, "Server status isRunning should be false after stop")
        }
    }

    @Test
    fun testPairEndpoint_PinValidationAndSessionCreation() {
        runBlocking {
            settingsStore.setPeerServerPin("4321")
            val port = serverManager.start(preferredPort = 0).getOrThrow()

            // 1. Attempt pairing with invalid PIN -> 401
            val invalidPairReq = PairRequest(
                clientInstanceId = "client-uuid-1",
                clientName = "Client Device A",
                pin = "9999"
            )
            val invalidResponse = httpClient.post("http://127.0.0.1:$port/api/v1/auth/pair") {
                contentType(ContentType.Application.Json)
                setBody(invalidPairReq)
            }
            assertEquals(HttpStatusCode.Unauthorized, invalidResponse.status)

            // 2. Pair with valid PIN -> 200 OK
            val validPairReq = PairRequest(
                clientInstanceId = "client-uuid-1",
                clientName = "Client Device A",
                pin = "4321"
            )
            val validResponse = httpClient.post("http://127.0.0.1:$port/api/v1/auth/pair") {
                contentType(ContentType.Application.Json)
                setBody(validPairReq)
            }
            assertEquals(HttpStatusCode.OK, validResponse.status)
            val pairResponse = validResponse.body<PairResponse>()
            assertTrue(pairResponse.token.isNotBlank(), "PairResponse should contain a non-blank token")
            assertEquals(settingsStore.settings.value.peerSync.instanceId, pairResponse.serverInstanceId)

            // Verify history was recorded
            val history = settingsStore.settings.value.peerSync.localServerHistory
            assertTrue(history.any { it.clientInstanceId == "client-uuid-1" && it.lastAction == "PAIRED" })
        }
    }

    @Test
    fun testMetadataAndDownloadEndpoints_BinaryStreaming() {
        runBlocking {
            settingsStore.setPeerServerPin("5555")
            val port = serverManager.start(preferredPort = 0).getOrThrow()

            // Pair to obtain bearer token
            val pairRes = httpClient.post("http://127.0.0.1:$port/api/v1/auth/pair") {
                contentType(ContentType.Application.Json)
                setBody(PairRequest("client-2", "Client B", "5555"))
            }.body<PairResponse>()

            val token = pairRes.token

            // 1. Metadata check without token -> 401
            val unauthMetadata = httpClient.get("http://127.0.0.1:$port/api/v1/sync/metadata")
            assertEquals(HttpStatusCode.Unauthorized, unauthMetadata.status)

            // 2. Metadata check with valid token -> 200 OK
            val metaResponse = httpClient.get("http://127.0.0.1:$port/api/v1/sync/metadata") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(HttpStatusCode.OK, metaResponse.status)
            val metadata = metaResponse.body<ServerSyncMetadata>()
            assertTrue(metadata.exists)
            assertTrue(metadata.sha256Hash.isNotBlank())
            assertTrue(metadata.sizeBytes > 0)

            // 3. Download database binary stream -> 200 OK
            val downloadResponse = httpClient.get("http://127.0.0.1:$port/api/v1/sync/db/download") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(HttpStatusCode.OK, downloadResponse.status)
            val downloadedBytes = downloadResponse.bodyAsBytes()
            val localDbFile = File(testDriverFactory.getDatabaseFilePath())
            val localBytes = localDbFile.readBytes()
            assertContentEquals(localBytes, downloadedBytes, "Downloaded bytes must match server DB bytes exactly")

            // Verify history was updated with DOWNLOAD
            val history = settingsStore.settings.value.peerSync.localServerHistory
            assertTrue(history.any { it.clientInstanceId == "client-2" && it.lastAction == "DOWNLOAD" })
        }
    }

    @Test
    fun testUploadEndpoint_OptimisticConcurrencyAndLiveReset() {
        runBlocking {
            settingsStore.setPeerServerPin("7777")
            val port = serverManager.start(preferredPort = 0).getOrThrow()

            var resetCallbackInvoked = false
            serverManager.onDatabaseReset = {
                resetCallbackInvoked = true
            }

            // Pair client
            val pairRes = httpClient.post("http://127.0.0.1:$port/api/v1/auth/pair") {
                contentType(ContentType.Application.Json)
                setBody(PairRequest("client-3", "Client C", "7777"))
            }.body<PairResponse>()
            val token = pairRes.token

            // Seed prior sync error on server
            settingsStore.updateSettings {
                it.copy(
                    sync = it.sync.copy(
                        lastSyncFailed = true,
                        lastSyncError = "Prior sync failure"
                    )
                )
            }

            val currentServerHash = FileUtils.calculateFileSha256(testDriverFactory.getDatabaseFilePath())!!

            // Create an updated client database
            val clientDbFile = File(tempDir, "client_updated.db")
            val clientDriver = testDriverFactory.createDriverForPath(clientDbFile.absolutePath)
            val clientDb = createDatabaseForDriver(clientDriver)
            clientDb.weightEntryQueries.insert(
                id = "entry_client_uploaded",
                date = "2026-09-27",
                weight = 180.0,
                updated_at = 2000L
            )

            // 1. Conflict test: Upload with mismatched expected hash -> 409 Conflict
            val conflictUpload = httpClient.post("http://127.0.0.1:$port/api/v1/sync/db/upload") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("X-Expected-Hash", "wrong_mismatched_hash_value")
                contentType(ContentType.Application.OctetStream)
                setBody(clientDbFile.readBytes())
            }
            assertEquals(HttpStatusCode.Conflict, conflictUpload.status)

            // 2. Invalid format test: Upload corrupt data (not SQLite) -> 400 Bad Request
            val corruptUpload = httpClient.post("http://127.0.0.1:$port/api/v1/sync/db/upload") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("X-Expected-Hash", currentServerHash)
                contentType(ContentType.Application.OctetStream)
                setBody("This is not a SQLite database file".encodeToByteArray())
            }
            assertEquals(HttpStatusCode.BadRequest, corruptUpload.status)

            // 3. Successful upload with matching expected hash -> 200 OK
            val successUpload = httpClient.post("http://127.0.0.1:$port/api/v1/sync/db/upload") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("X-Expected-Hash", currentServerHash)
                contentType(ContentType.Application.OctetStream)
                setBody(clientDbFile.readBytes())
            }
            assertEquals(HttpStatusCode.OK, successUpload.status)
            val newMeta = successUpload.body<ServerSyncMetadata>()
            assertTrue(newMeta.exists)
            assertTrue(resetCallbackInvoked, "onDatabaseReset callback should have been invoked")

            // Verify that server's database now contains the uploaded record
            val reloadedDriver = testDriverFactory.createDriver()
            val reloadedDb = Database(reloadedDriver)
            val allEntries = reloadedDb.weightEntryQueries.selectAll().executeAsList()
            assertTrue(allEntries.any { it.id == "entry_client_uploaded" }, "Server DB must contain newly uploaded record")

            // Verify history updated with UPLOAD
            val history = settingsStore.settings.value.peerSync.localServerHistory
            assertTrue(history.any { it.clientInstanceId == "client-3" && it.lastAction == "UPLOAD" })

            // Verify prior sync error on server is cleared and success recorded
            val syncSettings = settingsStore.settings.value.sync
            assertFalse(syncSettings.lastSyncFailed, "lastSyncFailed must be cleared to false")
            assertNull(syncSettings.lastSyncError, "lastSyncError must be cleared to null")
            assertTrue(syncSettings.lastSyncStatus.contains("Success (Updated by Client C)"))
        }
    }

    @Test
    fun testServerPortFallback_WhenPortOccupied() {
        runBlocking {
            // Start first server on an auto-allocated port
            val port1 = serverManager.start(preferredPort = 0).getOrThrow()
            assertTrue(port1 > 0)

            // Create a second server manager instance attempting to start on the exact same port
            val serverManager2 = PeerServerManager(
                driverFactory = testDriverFactory,
                settingsStore = settingsStore
            )

            try {
                val start2Result = serverManager2.start(preferredPort = port1)
                assertTrue(start2Result.isSuccess, "Second server should automatically fallback to another port")
                val port2 = start2Result.getOrThrow()
                assertNotEquals(port1, port2, "Second server must bind to a different fallback port")
                assertTrue(serverManager2.serverStatus.value.isRunning)
            } finally {
                serverManager2.stop()
            }
        }
    }

    @Test
    fun testRequestPinEndpoint_GeneratesPinAndPrompts() {
        runBlocking {
            val port = serverManager.start(preferredPort = 0).getOrThrow()

            // 1. Request on-demand PIN
            val initReq = PairInitRequest(
                clientInstanceId = "client-req-1",
                clientName = "Client Tablet"
            )
            val requestPinResponse = httpClient.post("http://127.0.0.1:$port/api/v1/auth/request-pin") {
                contentType(ContentType.Application.Json)
                setBody(initReq)
            }
            assertEquals(HttpStatusCode.OK, requestPinResponse.status)

            // Verify active PIN generated on server and prompt displayed
            val activePin = serverManager.serverStatus.value.activePin
            assertTrue(activePin.isNotBlank(), "Active PIN should be generated")
            assertEquals(4, activePin.length, "PIN should be 4 digits")

            val prompt = SyncNotificationManager.pairingPinPrompt.value
            assertNotNull(prompt)
            assertEquals("Client Tablet", prompt.clientName)
            assertEquals(activePin, prompt.pin)

            // 2. Pair using the generated PIN
            val pairReq = PairRequest(
                clientInstanceId = "client-req-1",
                clientName = "Client Tablet",
                pin = activePin
            )
            val pairResponse = httpClient.post("http://127.0.0.1:$port/api/v1/auth/pair") {
                contentType(ContentType.Application.Json)
                setBody(pairReq)
            }
            assertEquals(HttpStatusCode.OK, pairResponse.status)
            val pairBody = pairResponse.body<PairResponse>()
            assertTrue(pairBody.token.isNotBlank())

            // Verify PIN and prompt cleared after successful pairing
            assertEquals("", serverManager.serverStatus.value.activePin)
            assertNull(SyncNotificationManager.pairingPinPrompt.value)
        }
    }

    @Test
    fun testClientTokenPersistenceAndReloadAcrossServerRestart() {
        runBlocking {
            settingsStore.setPeerServerPin("8888")
            val port1 = serverManager.start(preferredPort = 0).getOrThrow()

            // 1. Pair client and obtain token
            val pairReq = PairRequest(
                clientInstanceId = "client-persist-1",
                clientName = "Client Persist Device",
                pin = "8888"
            )
            val pairResponse = httpClient.post("http://127.0.0.1:$port1/api/v1/auth/pair") {
                contentType(ContentType.Application.Json)
                setBody(pairReq)
            }
            assertEquals(HttpStatusCode.OK, pairResponse.status)
            val token = pairResponse.body<PairResponse>().token
            assertTrue(token.isNotBlank())

            // 2. Verify token is persisted in SettingsStore localServerHistory
            val history = settingsStore.settings.value.peerSync.localServerHistory
            val clientRecord = history.find { it.clientInstanceId == "client-persist-1" }
            assertNotNull(clientRecord)
            assertEquals(token, clientRecord.authToken)

            // 3. Stop server completely
            serverManager.stop()

            // 4. Instantiate a new PeerServerManager instance with the same SettingsStore
            val newServerManager = PeerServerManager(
                driverFactory = testDriverFactory,
                settingsStore = settingsStore
            )

            try {
                val port2 = newServerManager.start(preferredPort = 0).getOrThrow()

                // 5. Test status endpoint using the previously issued token without re-pairing
                val statusResponse = httpClient.get("http://127.0.0.1:$port2/api/v1/status") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                assertEquals(HttpStatusCode.OK, statusResponse.status)
                val statusBody = statusResponse.body<PeerStatusResponse>()
                assertEquals(PeerAuthStatus.ACCESS_GRANTED, statusBody.authStatus)
                assertTrue(statusBody.isAccessGranted)

                // 6. Test sync metadata endpoint using the persisted token
                val metadataResponse = httpClient.get("http://127.0.0.1:$port2/api/v1/sync/metadata") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                assertEquals(HttpStatusCode.OK, metadataResponse.status)
                val meta = metadataResponse.body<ServerSyncMetadata>()
                assertTrue(meta.exists)
            } finally {
                newServerManager.stop()
            }
        }
    }

    @Test
    fun testSettingsViewModel_ServerLifecycleWithServerMode() {
        runBlocking {
            settingsStore.updateSettings {
                it.copy(
                    sync = it.sync.copy(syncEnabled = true, syncProvider = SyncProviderType.PEER_TO_PEER),
                    peerSync = it.peerSync.copy(isServerMode = false, localServerEnabled = true, localServerPort = 0)
                )
            }
            val viewModel = SettingsViewModel(
                persistence = settingsStore,
                peerServerManager = serverManager
            )

            // 1. Initial state: isServerMode is false, server is not running even if localServerEnabled is true
            assertFalse(serverManager.serverStatus.value.isRunning)

            // 2. Switch to server mode: both isServerMode and localServerEnabled are true, server starts
            viewModel.setPeerIsServerMode(true)
            delay(200.milliseconds)
            assertTrue(serverManager.serverStatus.value.isRunning)
            assertTrue(viewModel.settings.value.peerSync.isServerMode)
            assertTrue(viewModel.settings.value.peerSync.localServerEnabled)

            // 3. Momentarily toggle localServerEnabled to false while in server mode: server stops
            viewModel.setPeerServerEnabled(false)
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning)
            assertTrue(viewModel.settings.value.peerSync.isServerMode)
            assertFalse(viewModel.settings.value.peerSync.localServerEnabled)

            // 4. Toggle localServerEnabled back to true: server restarts
            viewModel.setPeerServerEnabled(true)
            delay(200.milliseconds)
            assertTrue(serverManager.serverStatus.value.isRunning)

            // 5. Switch back to client mode: server stops
            viewModel.setPeerIsServerMode(false)
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning)
            assertFalse(viewModel.settings.value.peerSync.isServerMode)
        }
    }

    @Test
    fun testSettingsViewModel_SwitchAwayFromPeerToPeerStopsServerAndRelaunchChecks() {
        runBlocking {
            settingsStore.updateSettings {
                it.copy(
                    sync = it.sync.copy(syncEnabled = true, syncProvider = SyncProviderType.PEER_TO_PEER),
                    peerSync = it.peerSync.copy(isServerMode = true, localServerEnabled = true, localServerPort = 0)
                )
            }
            val viewModel = SettingsViewModel(
                persistence = settingsStore,
                peerServerManager = serverManager
            )

            // Server initializes when peer to peer, isServerMode, and localServerEnabled are active
            viewModel.initializeServerIfEnabled()
            delay(200.milliseconds)
            assertTrue(serverManager.serverStatus.value.isRunning, "Server should run when all peer conditions are met")

            // Switching away to Local Folder must immediately stop the server
            viewModel.setSyncProvider(SyncProviderType.LOCAL_FOLDER)
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning, "Server should stop when switching away to LOCAL_FOLDER")

            // Simulate relaunch: server should remain stopped because provider is not PEER_TO_PEER
            viewModel.initializeServerIfEnabled()
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning, "Server should remain stopped on relaunch when not PEER_TO_PEER")

            // Switch to Google Drive: server remains stopped
            viewModel.setSyncProvider(SyncProviderType.GOOGLE_DRIVE)
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning, "Server should remain stopped on GOOGLE_DRIVE")

            // Simulate relaunch: server should remain stopped
            viewModel.initializeServerIfEnabled()
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning, "Server should remain stopped on relaunch when GOOGLE_DRIVE")

            // Switch back to Peer-to-Peer: server starts
            viewModel.setSyncProvider(SyncProviderType.PEER_TO_PEER)
            delay(200.milliseconds)
            assertTrue(serverManager.serverStatus.value.isRunning, "Server should start when switching back to PEER_TO_PEER")

            // Simulate relaunch when isServerMode is false
            viewModel.setPeerIsServerMode(false)
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning)
            viewModel.initializeServerIfEnabled()
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning, "Server should remain stopped on relaunch when isServerMode is false")

            // Restore isServerMode true but set localServerEnabled false
            viewModel.setPeerIsServerMode(true)
            viewModel.setPeerServerEnabled(false)
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning)
            viewModel.initializeServerIfEnabled()
            delay(200.milliseconds)
            assertFalse(serverManager.serverStatus.value.isRunning, "Server should remain stopped on relaunch when localServerEnabled is false")
        }
    }
}
