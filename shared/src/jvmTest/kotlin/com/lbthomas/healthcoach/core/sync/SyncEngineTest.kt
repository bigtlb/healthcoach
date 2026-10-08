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
    fun testAutoVacuumAndIncrementalVacuumCompaction() {
        val testDbFile = File(tempDir, "autovacuum_test.db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${testDbFile.absolutePath}")
        val db = createDatabaseForDriver(driver)

        // Verify incremental auto-vacuum is enabled
        val autoVacuumMode = com.lbthomas.healthcoach.core.database.getAutoVacuum(driver)
        assertEquals(2L, autoVacuumMode, "Expected auto_vacuum = INCREMENTAL (2)")

        // Insert records to grow the database
        for (i in 1..200) {
            db.mealEntryQueries.insert(
                id = "meal-$i",
                date = "2026-10-01",
                mealTime = "12:00",
                foodId = "food-$i",
                foodName = "Large Food Name Text Entry $i ".repeat(10),
                foodDescription = "Description text $i ".repeat(10),
                brand = "Brand $i",
                unitName = "serving",
                unitQuantity = 1.0,
                caloriesPerUnit = 500.0,
                portionMultiplier = 1.0,
                totalCalories = 500.0,
                updated_at = 1000L
            )
        }

        val sizeBeforeDelete = testDbFile.length()
        assertTrue(sizeBeforeDelete > 10000)

        // Delete all inserted records
        for (i in 1..200) {
            db.mealEntryQueries.delete("meal-$i")
        }

        // Run incremental vacuum
        com.lbthomas.healthcoach.core.database.incrementalVacuum(driver)

        // Staging a copy and vacuuming reclaims free pages
        val compactedDbFile = File(tempDir, "compacted_test.db")
        FileUtils.copyFile(testDbFile.absolutePath, compactedDbFile.absolutePath)
        val compactDriver = JdbcSqliteDriver("jdbc:sqlite:${compactedDbFile.absolutePath}")
        com.lbthomas.healthcoach.core.database.incrementalVacuum(compactDriver)

        val sizeAfterVacuum = compactedDbFile.length()
        assertTrue(sizeAfterVacuum <= sizeBeforeDelete)
        driver.close()
        compactDriver.close()
    }

    @Test
    fun testSyncWithGoogleDriveAdapterCompressedPayloadRoundTrip() = runBlocking {
        val engineA = createEngineForDirectory(localDbDir, localDatabase)

        // Insert local data on Device A
        localDatabase.weightEntryQueries.insert("w-gdrive-1", "2026-09-30", 72.0, 1000L)
        localDatabase.bloodPressureEntryQueries.insert("bp-gdrive-1", "2026-09-30T09:00:00Z", 118, 78, 62, 1000L)

        val googleConfig = SyncConfig(
            providerType = SyncProviderType.GOOGLE_DRIVE,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db",
            googleAccessToken = "gdt_test_token"
        )

        // Device A syncs to Google Drive (customBasePath)
        val resultA = engineA.sync(googleConfig)
        assertTrue(resultA is SyncResult.Success, "Expected successful initial upload to Google Drive")

        // Verify remote file is compressed .db.gz
        val remoteGdriveDir = File(remoteStorageDir, "appDataFolder")
        val remoteGzFile = File(remoteGdriveDir, "healthcoach.db.gz")
        assertTrue(remoteGzFile.exists(), "Remote .db.gz file should exist in appDataFolder")
        assertTrue(FileUtils.isGzipFile(remoteGzFile.absolutePath), "Remote file should have valid Gzip magic header")

        // Device B on a separate folder bootstraps from Google Drive
        val deviceBDir = File(tempDir, "device_b_data").apply { mkdirs() }
        val deviceBDbFile = File(deviceBDir, "healthcoach.db")
        val driverB = JdbcSqliteDriver("jdbc:sqlite:${deviceBDbFile.absolutePath}")
        val dbB = createDatabaseForDriver(driverB)

        val engineB = createEngineForDirectory(deviceBDir, dbB)
        val resultB = engineB.sync(googleConfig)
        assertTrue(resultB is SyncResult.Success, "Expected successful bootstrap from Google Drive on Device B")

        val bWeights = dbB.weightEntryQueries.selectAll().executeAsList()
        assertEquals(1, bWeights.size)
        assertEquals("w-gdrive-1", bWeights.first().id)

        val bBp = dbB.bloodPressureEntryQueries.selectAll().executeAsList()
        assertEquals(1, bBp.size)
        assertEquals("bp-gdrive-1", bBp.first().id)
        driverB.close()
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

    @Test
    fun testSeparateCompressedCachePerSyncMethodAndSelectiveReset() = runBlocking {
        val engine = createEngineForDirectory(localDbDir, localDatabase)

        // Local DB has 1 entry
        localDatabase.weightEntryQueries.insert("entry-1", "2026-10-01", 75.0, 1000L)

        val localConfig = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )
        val googleConfig = SyncConfig(
            providerType = SyncProviderType.GOOGLE_DRIVE,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db",
            googleAccessToken = "mock_token"
        )

        // 1. Sync to Local Folder
        val resLocal = engine.sync(localConfig)
        assertTrue(resLocal is SyncResult.Success)

        // 2. Sync to Google Drive
        val resGoogle = engine.sync(googleConfig)
        assertTrue(resGoogle is SyncResult.Success)

        // 3. Verify both compressed caches exist on disk
        val localCacheFile = File(engine.getSyncedCachePath(SyncProviderType.LOCAL_FOLDER))
        val googleCacheFile = File(engine.getSyncedCachePath(SyncProviderType.GOOGLE_DRIVE))
        assertTrue(localCacheFile.exists(), "Local folder compressed cache should exist")
        assertTrue(googleCacheFile.exists(), "Google drive compressed cache should exist")
        assertTrue(FileUtils.isGzipFile(localCacheFile.absolutePath), "Local cache should be gzipped")
        assertTrue(FileUtils.isGzipFile(googleCacheFile.absolutePath), "Google cache should be gzipped")

        // 4. Verify working uncompressed cache files are cleaned up
        val localWorkingCacheFile = File(engine.getSyncedWorkingCachePath(SyncProviderType.LOCAL_FOLDER))
        val googleWorkingCacheFile = File(engine.getSyncedWorkingCachePath(SyncProviderType.GOOGLE_DRIVE))
        assertFalse(localWorkingCacheFile.exists(), "Working uncompressed cache should be cleaned up")
        assertFalse(googleWorkingCacheFile.exists(), "Working uncompressed cache should be cleaned up")

        // 5. Reset only Google Drive destination
        val googleAdapter = StorageAdapterFactory.createAdapter(googleConfig)
        val resetResult = engine.resetRemoteDestination(googleAdapter)
        assertTrue(resetResult.isSuccess)

        // 6. Verify Google Drive cache is deleted, but Local Folder cache remains
        assertFalse(googleCacheFile.exists(), "Google cache should be cleared on reset")
        assertTrue(localCacheFile.exists(), "Local folder cache should remain intact after Google Drive reset")
    }

    @Test
    fun testConfigurationChangeInvalidatesProviderCache() = runBlocking {
        val engine = createEngineForDirectory(localDbDir, localDatabase)

        localDatabase.weightEntryQueries.insert("entry-config-1", "2026-10-01", 70.0, 1000L)

        val folder1 = File(tempDir, "remote_folder_1").apply { mkdirs() }
        val folder2 = File(tempDir, "remote_folder_2").apply { mkdirs() }

        val config1 = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = folder1.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        // Sync with folder1
        val res1 = engine.sync(config1)
        assertTrue(res1 is SyncResult.Success)

        val cacheFile = File(engine.getSyncedCachePath(SyncProviderType.LOCAL_FOLDER))
        val metaFile = File(engine.getSyncedCacheMetaPath(SyncProviderType.LOCAL_FOLDER))
        assertTrue(cacheFile.exists())
        assertTrue(metaFile.exists())
        assertEquals(engine.getTargetConfigKey(config1), FileUtils.readUtf8String(metaFile.absolutePath))

        // Change config to folder2
        val config2 = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = folder2.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        val res2 = engine.sync(config2)
        assertTrue(res2 is SyncResult.Success)

        // Meta file should now reflect config2
        assertEquals(engine.getTargetConfigKey(config2), FileUtils.readUtf8String(metaFile.absolutePath))
    }

    @Test
    fun testMultiProviderSyncPreventsUnintentionalDeletions() = runBlocking {
        val engine = createEngineForDirectory(localDbDir, localDatabase)

        val localDestDir = File(tempDir, "multi_sync_local").apply { mkdirs() }
        val googleDestDir = File(tempDir, "multi_sync_gdrive").apply { mkdirs() }

        val localConfig = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = localDestDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )
        val googleConfig = SyncConfig(
            providerType = SyncProviderType.GOOGLE_DRIVE,
            localFolderPath = googleDestDir.absolutePath,
            remoteFileName = "healthcoach.db",
            googleAccessToken = "token_abc"
        )

        // 1. Initial entries
        localDatabase.weightEntryQueries.insert("item-1", "2026-10-01", 70.0, 1000L)
        localDatabase.weightEntryQueries.insert("item-2", "2026-10-02", 71.0, 1000L)

        // Sync to both targets
        assertTrue(engine.sync(localConfig) is SyncResult.Success)
        assertTrue(engine.sync(googleConfig) is SyncResult.Success)

        // 2. Add item-3 locally
        localDatabase.weightEntryQueries.insert("item-3", "2026-10-03", 72.0, 2000L)

        // Sync to Local Folder
        val resLocal = engine.sync(localConfig)
        assertTrue(resLocal is SyncResult.Success)

        // 3. Now sync to Google Drive
        // If cache was shared, Google Drive sync would see differences and potentially corrupt/delete records!
        // With isolated caches, Google Drive sync properly merges item-3
        val resGoogle = engine.sync(googleConfig)
        assertTrue(resGoogle is SyncResult.Success)

        val allLocalItems = localDatabase.weightEntryQueries.selectAll().executeAsList().map { it.id }.toSet()
        assertEquals(setOf("item-1", "item-2", "item-3"), allLocalItems, "All 3 items must be preserved locally")
    }

    @Test
    fun testLegacyCacheMigrationTransformsAndDeletesDanglingCache() = runBlocking {
        val engine = createEngineForDirectory(localDbDir, localDatabase)

        // Seed initial local & remote data
        localDatabase.weightEntryQueries.insert("legacy-1", "2026-10-01", 70.0, 1000L)

        // Place legacy uncompressed synced_cache.db into the sync directory
        val syncDir = File(engine.getSyncDirectory()).apply { mkdirs() }
        val legacyCacheFile = File(syncDir, "synced_cache.db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${legacyCacheFile.absolutePath}")
        val legacyDb = createDatabaseForDriver(driver)
        legacyDb.weightEntryQueries.insert("legacy-1", "2026-10-01", 70.0, 1000L)
        driver.close()

        val localConfig = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = remoteStorageDir.absolutePath,
            remoteFileName = "healthcoach.db"
        )

        // Remote database matches legacy cache
        val remoteAppDir = File(remoteStorageDir, SyncConfig.LOCAL_APP_SUBFOLDER).apply { mkdirs() }
        val remoteDbFile = File(remoteAppDir, "healthcoach.db")
        val remoteDriver = JdbcSqliteDriver("jdbc:sqlite:${remoteDbFile.absolutePath}")
        val remoteDb = createDatabaseForDriver(remoteDriver)
        remoteDb.weightEntryQueries.insert("legacy-1", "2026-10-01", 70.0, 1000L)
        remoteDriver.close()

        // Add a new entry locally
        localDatabase.weightEntryQueries.insert("legacy-2", "2026-10-02", 71.0, 2000L)

        assertTrue(legacyCacheFile.exists(), "Legacy cache should exist before sync")

        // Perform sync with local provider
        val res = engine.sync(localConfig)
        if (res is SyncResult.Error) {
            println("Sync failed with: ${res.message}, cause: ${res.error.message}")
            res.error.printStackTrace()
        }
        assertTrue(res is SyncResult.Success, "Expected success but got: $res")

        // Verify legacy cache was transformed and not left dangling
        assertFalse(legacyCacheFile.exists(), "Legacy uncompressed cache file should be deleted")
        val migratedGzCache = File(engine.getSyncedCachePath(SyncProviderType.LOCAL_FOLDER))
        assertTrue(migratedGzCache.exists(), "Migrated gzip cache should exist")
        assertTrue(FileUtils.isGzipFile(migratedGzCache.absolutePath))

        val metaFile = File(engine.getSyncedCacheMetaPath(SyncProviderType.LOCAL_FOLDER))
        assertTrue(metaFile.exists(), "Metadata file should exist")
        assertEquals(engine.getTargetConfigKey(localConfig), FileUtils.readUtf8String(metaFile.absolutePath))

        // Remote DB should have both items
        val verifyDriver = JdbcSqliteDriver("jdbc:sqlite:${remoteDbFile.absolutePath}")
        val verifyDb = Database(verifyDriver)
        val remoteList = verifyDb.weightEntryQueries.selectAll().executeAsList().map { it.id }
        assertEquals(listOf("legacy-1", "legacy-2"), remoteList)
    }
}

private class CustomTestDriverFactory(private val baseDir: File) : DriverFactory() {
    override fun createDriver() = JdbcSqliteDriver("jdbc:sqlite:${baseDir.absolutePath}/healthcoach.db")
    override fun createDriverForPath(dbFilePath: String) = JdbcSqliteDriver("jdbc:sqlite:$dbFilePath")
    override fun getDatabaseDirectory() = baseDir.absolutePath
    override fun getDatabaseFilePath() = "${baseDir.absolutePath}/healthcoach.db"
}
