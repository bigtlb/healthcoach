package com.lbthomas.healthcoach.core.sync.p2p

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.sync.FileUtils
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.sync.adapters.PeerToPeerStorageAdapter
import com.lbthomas.healthcoach.core.sync.auth.AuthState
import com.lbthomas.healthcoach.core.sync.auth.ProviderCredentials
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class PeerToPeerStorageAdapterTest {

    private lateinit var tempDir: File
    private lateinit var dbDir: File
    private lateinit var settingsFile: File
    private lateinit var settingsStore: SettingsStore
    private lateinit var testDriverFactory: DriverFactory
    private lateinit var serverManager: PeerServerManager
    private var serverPort: Int = 0

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("p2p_adapter_test").toFile()
        dbDir = File(tempDir, "db").apply { mkdirs() }
        settingsFile = File(tempDir, "settings.json")
        settingsStore = SettingsStore(settingsFile)

        testDriverFactory = object : DriverFactory() {
            override fun createDriver() = JdbcSqliteDriver("jdbc:sqlite:${dbDir.absolutePath}/healthcoach.db")
            override fun createDriverForPath(dbFilePath: String) = JdbcSqliteDriver("jdbc:sqlite:$dbFilePath")
            override fun getDatabaseDirectory() = dbDir.absolutePath
            override fun getDatabaseFilePath() = "${dbDir.absolutePath}/healthcoach.db"
        }

        // Initialize server SQLite DB
        val serverDriver = testDriverFactory.createDriver()
        val serverDb = createDatabaseForDriver(serverDriver)
        serverDb.weightEntryQueries.insert(
            id = "server_entry_1",
            date = "2026-09-27",
            weight = 168.0,
            updated_at = 1500L
        )

        settingsStore.setPeerServerPin("4321")
        serverManager = PeerServerManager(
            driverFactory = testDriverFactory,
            settingsStore = settingsStore
        )

        runBlocking {
            serverPort = serverManager.start(preferredPort = 0).getOrThrow()
        }
    }

    @AfterTest
    fun tearDown() {
        runBlocking {
            serverManager.stop()
        }
        tempDir.deleteRecursively()
    }

    @Test
    fun testProviderTypeAndInitialState() {
        val adapter = PeerToPeerStorageAdapter(
            serverHost = "127.0.0.1",
            serverPort = serverPort,
            clientInstanceId = "client-inst-1",
            clientDeviceName = "Client Device"
        )

        assertEquals(SyncProviderType.PEER_TO_PEER, adapter.providerType)
        assertTrue(adapter.authState.value is AuthState.Unauthenticated)
        assertTrue(adapter.securityNotice.isNotBlank())
    }

    @Test
    fun testTestConnection_OnlineAndOffline() {
        runBlocking {
            val onlineAdapter = PeerToPeerStorageAdapter(
                serverHost = "127.0.0.1",
                serverPort = serverPort
            )
            val onlineResult = onlineAdapter.testConnection()
            assertTrue(onlineResult.isSuccess, "Connection test should succeed against online server")

            val offlineAdapter = PeerToPeerStorageAdapter(
                serverHost = "127.0.0.1",
                serverPort = 59999 // unused port
            )
            val offlineResult = offlineAdapter.testConnection()
            assertTrue(offlineResult.isFailure, "Connection test should fail against offline port")
        }
    }

    @Test
    fun testAuthentication_ValidAndInvalidPin() {
        runBlocking {
            val adapter = PeerToPeerStorageAdapter(
                serverHost = "127.0.0.1",
                serverPort = serverPort,
                clientInstanceId = "client-test-auth",
                clientDeviceName = "Client Auth Device"
            )

            // Invalid PIN
            val failAuth = adapter.authenticate(ProviderCredentials(password = "0000"))
            assertTrue(failAuth.isFailure)
            assertTrue(adapter.authState.value is AuthState.Error)

            // Valid PIN
            val successAuth = adapter.authenticate(ProviderCredentials(password = "4321"))
            assertTrue(successAuth.isSuccess)
            val token = successAuth.getOrThrow()
            assertTrue(token.isNotBlank())
            assertEquals(AuthState.Authenticated(token), adapter.authState.value)

            // Disconnect
            val disconnectRes = adapter.disconnect()
            assertTrue(disconnectRes.isSuccess)
            assertEquals(AuthState.Unauthenticated, adapter.authState.value)
        }
    }

    @Test
    fun testGetFileMetadata_AuthenticatedAndUnauthenticated() {
        runBlocking {
            val adapter = PeerToPeerStorageAdapter(
                serverHost = "127.0.0.1",
                serverPort = serverPort,
                clientInstanceId = "client-test-meta",
                clientDeviceName = "Client Meta Device"
            )

            // Unauthenticated -> null
            val unauthMeta = adapter.getFileMetadata("healthcoach.db")
            assertNull(unauthMeta)

            // Authenticate
            adapter.authenticate(ProviderCredentials(password = "4321"))
            val meta = adapter.getFileMetadata("healthcoach.db")
            assertNotNull(meta)
            assertTrue(meta.exists)
            assertTrue(meta.sha256Hash.isNotBlank())
            assertTrue(meta.size > 0)
        }
    }

    @Test
    fun testDownloadFile_BinaryStreaming() {
        runBlocking {
            val adapter = PeerToPeerStorageAdapter(
                serverHost = "127.0.0.1",
                serverPort = serverPort,
                clientInstanceId = "client-test-download",
                clientDeviceName = "Client Download Device"
            )
            adapter.authenticate(ProviderCredentials(password = "4321"))

            val downloadedFile = File(tempDir, "downloaded.db")
            val downloadSuccess = adapter.downloadFile("healthcoach.db", downloadedFile.absolutePath)
            assertTrue(downloadSuccess, "Download should succeed")
            assertTrue(downloadedFile.exists())

            val serverFile = File(testDriverFactory.getDatabaseFilePath())
            assertContentEquals(serverFile.readBytes(), downloadedFile.readBytes())
        }
    }

    @Test
    fun testUploadFile_OptimisticConcurrencyAndLiveReset() {
        runBlocking {
            var resetCallbackInvoked = false
            serverManager.onDatabaseReset = {
                resetCallbackInvoked = true
            }

            val adapter = PeerToPeerStorageAdapter(
                serverHost = "127.0.0.1",
                serverPort = serverPort,
                clientInstanceId = "client-test-upload",
                clientDeviceName = "Client Upload Device"
            )
            adapter.authenticate(ProviderCredentials(password = "4321"))

            val serverHash = FileUtils.calculateFileSha256(testDriverFactory.getDatabaseFilePath())!!

            // Create client modified DB
            val clientDbFile = File(tempDir, "client_mod.db")
            val clientDriver = testDriverFactory.createDriverForPath(clientDbFile.absolutePath)
            val clientDb = createDatabaseForDriver(clientDriver)
            clientDb.weightEntryQueries.insert(
                id = "client_uploaded_entry_99",
                date = "2026-09-27",
                weight = 172.5,
                updated_at = 3000L
            )

            // 1. Conflict upload with mismatched hash
            val conflictSuccess = adapter.uploadFile(
                sourcePath = clientDbFile.absolutePath,
                fileName = "healthcoach.db",
                expectedHash = "invalid_expected_hash"
            )
            assertFalse(conflictSuccess, "Upload should fail on expected hash mismatch (conflict)")

            // 2. Successful upload with matching hash
            val uploadSuccess = adapter.uploadFile(
                sourcePath = clientDbFile.absolutePath,
                fileName = "healthcoach.db",
                expectedHash = serverHash
            )
            assertTrue(uploadSuccess, "Upload should succeed with matching expected hash")
            assertTrue(resetCallbackInvoked, "Live DB reset hook should have been called on server")

            // Verify server DB now contains new entry
            val reloadedDriver = testDriverFactory.createDriver()
            val reloadedDb = Database(reloadedDriver)
            val entries = reloadedDb.weightEntryQueries.selectAll().executeAsList()
            assertTrue(entries.any { it.id == "client_uploaded_entry_99" })
        }
    }
}
