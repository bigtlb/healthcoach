package com.lbthomas.healthcoach.core.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.database.setDbVersion
import com.lbthomas.healthcoach.core.sync.adapters.GoogleDriveStorageAdapter
import com.lbthomas.healthcoach.core.sync.auth.AuthState
import com.lbthomas.healthcoach.core.sync.auth.ProviderCredentials
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class SyncEngineTest {

    private lateinit var tempDir: File
    private lateinit var localDbDir: File
    private lateinit var remoteStorageDir: File
    private lateinit var localDriver: JdbcSqliteDriver
    private lateinit var localDatabase: Database
    private lateinit var testDriverFactory: DriverFactory
    private lateinit var syncEngine: SyncEngine

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("sync_engine_test").toFile()
        localDbDir = File(tempDir, "local_app_data").apply { mkdirs() }
        remoteStorageDir = File(tempDir, "remote_storage").apply { mkdirs() }

        val localDbFile = File(localDbDir, "healthcoach.db")
        localDriver = JdbcSqliteDriver("jdbc:sqlite:${localDbFile.absolutePath}")
        localDatabase = createDatabaseForDriver(localDriver)

        testDriverFactory = object : DriverFactory() {
            // JVM DriverFactory mock via subclassing or reflection
        }
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createEngineForDirectory(baseDir: File, liveDb: Database): SyncEngine {
        val customDriverFactory = CustomTestDriverFactory(baseDir)
        return SyncEngine(localDatabase = liveDb, driverFactory = customDriverFactory)
    }

    @Test
    fun testInitialSyncRemoteAbsent() = runBlocking {
        val engine = createEngineForDirectory(localDbDir, localDatabase)

        // Add an entry to local DB
        localDatabase.weightEntryQueries.insert("uuid-1", "2026-09-25", 75.5, 1000L)
        localDatabase.bloodPressureEntryQueries.insert("bp-1", "2026-09-25T10:00:00Z", 120, 80, 70, 1000L)

        val config = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        val result = engine.sync(config)
        assertTrue(result is SyncResult.Success, "Expected initial sync success")
        assertEquals(2, result.stats.uploaded)
        assertEquals(0, result.stats.downloaded)
        assertEquals("Sync successful: 2 uploaded, 0 downloaded", result.message)

        // Verify remote DB file exists
        val remoteAppDir = File(remoteStorageDir, SyncConfig.LOCAL_APP_SUBFOLDER)
        val remoteDbFile = File(remoteAppDir, "healthcoach.db")
        assertTrue(remoteDbFile.exists(), "Remote DB file should be created")

        // Verify base cache file exists
        val baseCacheFile = File(engine.getSyncedCachePath())
        assertTrue(baseCacheFile.exists(), "Base cache should be created")

        // Verify remote database has the entry
        val remoteDriver = JdbcSqliteDriver("jdbc:sqlite:${remoteDbFile.absolutePath}")
        val remoteDb = Database(remoteDriver)
        assertEquals(1, remoteDb.weightEntryQueries.selectAll().executeAsList().size)
        assertEquals("uuid-1", remoteDb.weightEntryQueries.selectAll().executeAsList().first().id)
        assertEquals(1, remoteDb.bloodPressureEntryQueries.selectAll().executeAsList().size)
    }

    @Test
    fun testFreshDeviceBootstrapFromRemote() = runBlocking {
        // Prepare remote database with entries
        val remoteAppDir = File(remoteStorageDir, SyncConfig.LOCAL_APP_SUBFOLDER).apply { mkdirs() }
        val remoteDbFile = File(remoteAppDir, "healthcoach.db")
        val remoteDriver = JdbcSqliteDriver("jdbc:sqlite:${remoteDbFile.absolutePath}")
        val remoteDb = createDatabaseForDriver(remoteDriver)
        remoteDb.weightEntryQueries.insert("remote-uuid-1", "2026-09-24", 80.0, 500L)
        remoteDb.bloodPressureEntryQueries.insert("remote-bp-1", "2026-09-24T08:00:00Z", 125, 82, 65, 500L)

        // Local DB is empty
        val engine = createEngineForDirectory(localDbDir, localDatabase)
        assertTrue(localDatabase.weightEntryQueries.selectAll().executeAsList().isEmpty())

        val config = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        val result = engine.sync(config)
        assertTrue(result is SyncResult.Success, "Expected sync success")
        assertEquals(0, result.stats.uploaded)
        assertEquals(2, result.stats.downloaded)
        assertEquals("Sync successful: 0 uploaded, 2 downloaded", result.message)

        // Local DB should now contain remote entries
        val localWeights = localDatabase.weightEntryQueries.selectAll().executeAsList()
        assertEquals(1, localWeights.size)
        assertEquals("remote-uuid-1", localWeights.first().id)
        assertEquals(80.0, localWeights.first().weight)

        val localBps = localDatabase.bloodPressureEntryQueries.selectAll().executeAsList()
        assertEquals(1, localBps.size)
        assertEquals("remote-bp-1", localBps.first().id)

        // Base cache should also exist
        assertTrue(File(engine.getSyncedCachePath()).exists())
    }

    @Test
    fun testInitialSyncUnionMerge() = runBlocking {
        // Remote DB has row 1
        val remoteAppDir = File(remoteStorageDir, SyncConfig.LOCAL_APP_SUBFOLDER).apply { mkdirs() }
        val remoteDbFile = File(remoteAppDir, "healthcoach.db")
        val remoteDriver = JdbcSqliteDriver("jdbc:sqlite:${remoteDbFile.absolutePath}")
        val remoteDb = createDatabaseForDriver(remoteDriver)
        remoteDb.weightEntryQueries.insert("remote-1", "2026-09-20", 72.0, 500L)

        // Local DB has row 2 (offline created)
        localDatabase.weightEntryQueries.insert("local-1", "2026-09-21", 73.0, 600L)

        val engine = createEngineForDirectory(localDbDir, localDatabase)
        val config = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        val result = engine.sync(config)
        assertTrue(result is SyncResult.Success)

        // Both local and remote should have 2 entries
        val localRows = localDatabase.weightEntryQueries.selectAll().executeAsList()
        assertEquals(2, localRows.size)

        val updatedRemoteDb = Database(remoteDriver)
        val remoteRows = updatedRemoteDb.weightEntryQueries.selectAll().executeAsList()
        assertEquals(2, remoteRows.size)
    }

    @Test
    fun test3WayDifferentialMerge_IndependentEdits() = runBlocking {
        val engine = createEngineForDirectory(localDbDir, localDatabase)
        val config = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        // 1. Initial sync to establish base cache with 2 rows
        localDatabase.weightEntryQueries.insert("w-1", "2026-09-01", 70.0, 100L)
        localDatabase.weightEntryQueries.insert("w-2", "2026-09-02", 71.0, 100L)
        engine.sync(config)

        // 2. Local modifies w-1
        localDatabase.weightEntryQueries.update("2026-09-01", 69.5, 200L, "w-1")

        // 3. Remote modifies w-2
        val remoteAppDir = File(remoteStorageDir, SyncConfig.LOCAL_APP_SUBFOLDER)
        val remoteDbFile = File(remoteAppDir, "healthcoach.db")
        val remoteDriver = JdbcSqliteDriver("jdbc:sqlite:${remoteDbFile.absolutePath}")
        val remoteDb = Database(remoteDriver)
        remoteDb.weightEntryQueries.update("2026-09-02", 72.5, 250L, "w-2")

        // 4. Perform 3-way merge
        val result = engine.sync(config)
        assertTrue(result is SyncResult.Success)

        // Both Local and Remote should have w-1 (69.5) and w-2 (72.5)
        val finalLocal = localDatabase.weightEntryQueries.selectAll().executeAsList().associateBy { it.id }
        assertEquals(69.5, finalLocal["w-1"]?.weight)
        assertEquals(72.5, finalLocal["w-2"]?.weight)

        val finalRemote = Database(remoteDriver).weightEntryQueries.selectAll().executeAsList().associateBy { it.id }
        assertEquals(69.5, finalRemote["w-1"]?.weight)
        assertEquals(72.5, finalRemote["w-2"]?.weight)
    }

    @Test
    fun test3WayDifferentialMerge_LWWConflictResolution() = runBlocking {
        val engine = createEngineForDirectory(localDbDir, localDatabase)
        val config = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        // Establish base cache with row w-1 at timestamp 100
        localDatabase.weightEntryQueries.insert("w-1", "2026-09-01", 70.0, 100L)
        engine.sync(config)

        // Local edits w-1 with timestamp 200 (older)
        localDatabase.weightEntryQueries.update("2026-09-01", 68.0, 200L, "w-1")

        // Remote edits w-1 with timestamp 300 (newer)
        val remoteAppDir = File(remoteStorageDir, SyncConfig.LOCAL_APP_SUBFOLDER)
        val remoteDbFile = File(remoteAppDir, "healthcoach.db")
        val remoteDriver = JdbcSqliteDriver("jdbc:sqlite:${remoteDbFile.absolutePath}")
        val remoteDb = Database(remoteDriver)
        remoteDb.weightEntryQueries.update("2026-09-01", 74.0, 300L, "w-1")

        // Sync: Remote should win (LWW)
        val result = engine.sync(config)
        assertTrue(result is SyncResult.Success)

        val localWeight = localDatabase.weightEntryQueries.selectById("w-1").executeAsOne()
        assertEquals(74.0, localWeight.weight, "Remote should win conflict via Last-Write-Wins")
        assertEquals(300L, localWeight.updated_at)
    }

    @Test
    fun test3WayDifferentialMerge_DeletionPropagation() = runBlocking {
        val engine = createEngineForDirectory(localDbDir, localDatabase)
        val config = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        // Establish base cache with 2 rows
        localDatabase.weightEntryQueries.insert("w-keep", "2026-09-01", 70.0, 100L)
        localDatabase.weightEntryQueries.insert("w-delete", "2026-09-02", 71.0, 100L)
        engine.sync(config)

        // Local deletes w-delete
        localDatabase.weightEntryQueries.delete("w-delete")

        // Sync
        val result = engine.sync(config)
        assertTrue(result is SyncResult.Success)

        // Verify remote also had w-delete deleted
        val remoteAppDir = File(remoteStorageDir, SyncConfig.LOCAL_APP_SUBFOLDER)
        val remoteDbFile = File(remoteAppDir, "healthcoach.db")
        val remoteDb = Database(JdbcSqliteDriver("jdbc:sqlite:${remoteDbFile.absolutePath}"))
        assertEquals(1, remoteDb.weightEntryQueries.selectAll().executeAsList().size)
        assertEquals("w-keep", remoteDb.weightEntryQueries.selectAll().executeAsList().first().id)
    }

    @Test
    fun testSchemaCompatibility_RejectsNewerRemoteSchema() = runBlocking {
        // Create remote DB with user_version newer than local schema version
        val remoteAppDir = File(remoteStorageDir, SyncConfig.LOCAL_APP_SUBFOLDER).apply { mkdirs() }
        val remoteDbFile = File(remoteAppDir, "healthcoach.db")
        val remoteDriver = JdbcSqliteDriver("jdbc:sqlite:${remoteDbFile.absolutePath}")
        createDatabaseForDriver(remoteDriver)
        setDbVersion(remoteDriver, Database.Companion.Schema.version + 1)
        remoteDriver.close()

        val engine = createEngineForDirectory(localDbDir, localDatabase)
        val config = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        val result = engine.sync(config)
        assertTrue(result is SyncResult.Error, "Should reject newer schema version")
        assertTrue(result.error is IncompatibleSchemaException)
    }

    @Test
    fun testGoogleDriveRequiresAuthContract() = runBlocking {
        val adapter = GoogleDriveStorageAdapter(
            accountEmail = "user@gmail.com",
            accessToken = "mock_access_token"
        )

        assertEquals(SyncProviderType.GOOGLE_DRIVE, adapter.providerType)
        assertNotNull(adapter.securityNotice)
        assertTrue(adapter.securityNotice.contains("appDataFolder"))

        // Disconnect
        val disconnectRes = adapter.disconnect()
        assertTrue(disconnectRes.isSuccess)
        assertEquals(AuthState.Unauthenticated, adapter.authState.value)

        // Authenticate
        val authRes = adapter.authenticate(
            ProviderCredentials(
                token = "mock_token_123",
                username = "user@gmail.com"
            )
        )
        assertTrue(authRes.isSuccess)
        assertTrue(adapter.authState.value is AuthState.Authenticated)
    }
}

private class CustomTestDriverFactory(private val baseDir: File) : DriverFactory() {
    override fun createDriver() = JdbcSqliteDriver("jdbc:sqlite:${baseDir.absolutePath}/healthcoach.db")
    override fun createDriverForPath(dbFilePath: String) = JdbcSqliteDriver("jdbc:sqlite:$dbFilePath")
    override fun getDatabaseDirectory() = baseDir.absolutePath
    override fun getDatabaseFilePath() = "${baseDir.absolutePath}/healthcoach.db"
}
