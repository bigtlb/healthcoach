package com.lbthomas.healthcoach.core.sync.p2p

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.sync.FileUtils
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import com.lbthomas.healthcoach.features.sync.SyncNotificationManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import kotlin.test.*

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

            // Health / Status Check
            val response = httpClient.get("http://127.0.0.1:$port/api/v1/status")
            assertEquals(HttpStatusCode.OK, response.status)
            val responseText = response.bodyAsText()
            assertTrue(responseText.contains("\"status\":\"OK\""))

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
}
