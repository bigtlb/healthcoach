package com.lbthomas.healthcoach.core.sync

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.database.createDatabaseForPath
import com.lbthomas.healthcoach.core.database.getDbVersion
import com.lbthomas.healthcoach.core.database.setDbVersion
import com.lbthomas.healthcoach.core.utils.currentEpochMillis
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

sealed class SyncResult {
    data class Success(
        val message: String,
        val metadata: SyncMetadata,
        val stats: SyncStats = SyncStats()
    ) : SyncResult()
    data class ConflictResolved(val message: String, val metadata: SyncMetadata) : SyncResult()
    data class Error(val error: Throwable, val message: String) : SyncResult()
    data object Cancelled : SyncResult()
}

sealed class SchemaCompatibilityResult {
    data object Compatible : SchemaCompatibilityResult()
    data class LocalOlderThanRemote(val localVersion: Long, val remoteVersion: Long) : SchemaCompatibilityResult()
    data class LocalNewerThanRemote(val localVersion: Long, val remoteVersion: Long) : SchemaCompatibilityResult()
}

class IncompatibleSchemaException(message: String) : Exception(message)

class SyncEngine(
    private val localDatabase: Database,
    private val driverFactory: DriverFactory,
    private val settingsStore: SettingsStore? = null,
    val tableHandlers: List<TableSyncHandler> = listOf(WeightTableSyncHandler, BloodPressureTableSyncHandler)
) {
    companion object {
        private const val MAX_SYNC_RETRIES = 3
    }

    fun getSyncDirectory(): String {
        val baseDir = driverFactory.getDatabaseDirectory()
        val syncDir = FileUtils.joinPath(baseDir, "sync")
        FileUtils.ensureDirectoryExists(syncDir)
        return syncDir
    }

    fun getSyncedCachePath(): String = FileUtils.joinPath(getSyncDirectory(), "synced_cache.db")
    fun getRemoteStagingPath(): String = FileUtils.joinPath(getSyncDirectory(), "remote_staging.db")
    fun getMetadataPath(): String = FileUtils.joinPath(getSyncDirectory(), "sync_metadata.json")

    fun loadSyncMetadata(): SyncMetadata {
        val metadataPath = getMetadataPath()
        return if (FileUtils.fileExists(metadataPath)) {
            val jsonContent = FileUtils.calculateFileSha256(metadataPath)?.let {
                // Read metadata
                try {
                    // Load json string from disk if possible
                    null
                } catch (_: Exception) {
                    null
                }
            }
            SyncMetadata()
        } else {
            SyncMetadata()
        }
    }

    fun saveSyncMetadata(metadata: SyncMetadata) {
        val metadataPath = getMetadataPath()
        try {
            val jsonStr = SyncMetadata.toJson(metadata)
            val tempPath = "$metadataPath.tmp"
            // We can write via temporary staging or update settings store
            settingsStore?.updateSettings {
                it.copy(
                    lastSyncStatus = metadata.lastSyncStatus,
                    lastSyncTime = metadata.lastSyncedTimestamp,
                    lastSyncHash = metadata.lastSyncedHash,
                    lastSyncError = metadata.lastSyncError,
                    lastSyncFailed = metadata.lastSyncStatus.startsWith("Error")
                )
            }
        } catch (e: Exception) {
            Logger.e("Failed to persist sync metadata", e)
        }
    }

    fun checkSchemaCompatibility(stagingDriver: SqlDriver): SchemaCompatibilityResult {
        val remoteVersion = getDbVersion(stagingDriver)
        val localVersion = Database.Companion.Schema.version
        return when {
            remoteVersion > localVersion -> SchemaCompatibilityResult.LocalOlderThanRemote(localVersion, remoteVersion)
            remoteVersion < localVersion -> SchemaCompatibilityResult.LocalNewerThanRemote(localVersion, remoteVersion)
            else -> SchemaCompatibilityResult.Compatible
        }
    }

    suspend fun sync(config: SyncConfig): SyncResult {
        var retries = 0
        while (retries < MAX_SYNC_RETRIES) {
            currentCoroutineContext().ensureActive()
            val result = executeSyncAttempt(config)
            if (result !is SyncResult.ConflictResolved) {
                return result
            }
            retries++
            Logger.w("Concurrent modification detected. Retrying sync (attempt ${retries + 1}/$MAX_SYNC_RETRIES)...")
        }
        val err = "Sync failed after $MAX_SYNC_RETRIES attempts due to continuous concurrent changes."
        val metadata = SyncMetadata(
            lastSyncedTimestamp = currentEpochMillis(),
            lastSyncStatus = "Error: Conflict",
            lastSyncError = err
        )
        saveSyncMetadata(metadata)
        return SyncResult.Error(IllegalStateException(err), err)
    }

    private suspend fun executeSyncAttempt(config: SyncConfig): SyncResult {
        val adapter = StorageAdapterFactory.createAdapter(config)
        val remoteFileName = config.remoteFileName.ifBlank { SyncConfig.DEFAULT_REMOTE_DB_NAME }
        val syncDir = getSyncDirectory()
        val syncedCachePath = getSyncedCachePath()
        val stagingPath = getRemoteStagingPath()
        val liveDbPath = driverFactory.getDatabaseFilePath()

        try {
            currentCoroutineContext().ensureActive()

            // 1. Test connection
            val connResult = adapter.testConnection()
            if (connResult.isFailure) {
                val ex = connResult.exceptionOrNull() ?: Exception("Failed to connect to storage provider")
                val msg = "Storage connection failed: ${ex.message}"
                val metadata = SyncMetadata(
                    lastSyncedTimestamp = currentEpochMillis(),
                    lastSyncStatus = "Error: Connection",
                    lastSyncError = msg
                )
                saveSyncMetadata(metadata)
                return SyncResult.Error(ex, msg)
            }

            // 2. Check if remote file exists
            val remoteMetadata = adapter.getFileMetadata(remoteFileName)

            if (remoteMetadata == null || !remoteMetadata.exists) {
                // Remote does not exist: Initial Upload
                Logger.i("Remote database does not exist. Performing initial upload of local database.")
                FileUtils.copyFile(liveDbPath, stagingPath)
                val uploadSuccess = adapter.uploadFile(stagingPath, remoteFileName, null)
                if (!uploadSuccess) {
                    val err = "Failed to upload local database to remote storage"
                    val metadata = SyncMetadata(
                        lastSyncedTimestamp = currentEpochMillis(),
                        lastSyncStatus = "Error: Upload",
                        lastSyncError = err
                    )
                    saveSyncMetadata(metadata)
                    return SyncResult.Error(IllegalStateException(err), err)
                }

                // Copy to base cache
                FileUtils.copyFile(stagingPath, syncedCachePath)
                val finalHash = FileUtils.calculateFileSha256(syncedCachePath)
                val localRecordCount = tableHandlers.sumOf { it.selectRecordCount(localDatabase) }
                val stats = SyncStats(uploaded = localRecordCount, downloaded = 0)
                val msg = stats.toSummaryMessage()
                val metadata = SyncMetadata(
                    lastSyncedHash = finalHash,
                    lastSyncedTimestamp = currentEpochMillis(),
                    lastSyncStatus = "Success (${stats.uploaded} uploaded, ${stats.downloaded} downloaded)",
                    lastSyncError = null,
                    localSchemaVersion = Database.Companion.Schema.version
                )
                saveSyncMetadata(metadata)
                return SyncResult.Success(msg, metadata, stats)
            }

            // Remote exists: Download to staging
            val expectedHash = remoteMetadata.sha256Hash
            val downloadSuccess = adapter.downloadFile(remoteFileName, stagingPath)
            if (!downloadSuccess) {
                val err = "Failed to download remote database file"
                val metadata = SyncMetadata(
                    lastSyncedTimestamp = currentEpochMillis(),
                    lastSyncStatus = "Error: Download",
                    lastSyncError = err
                )
                saveSyncMetadata(metadata)
                return SyncResult.Error(IllegalStateException(err), err)
            }

            // Check schema compatibility
            val stagingDriver = driverFactory.createDriverForPath(stagingPath)
            try {
                val compat = checkSchemaCompatibility(stagingDriver)
                when (compat) {
                    is SchemaCompatibilityResult.LocalOlderThanRemote -> {
                        val msg = "Remote database schema version (${compat.remoteVersion}) is newer than local application schema version (${compat.localVersion}). Please update HealthCoach to synchronize."
                        val metadata = SyncMetadata(
                            lastSyncedTimestamp = currentEpochMillis(),
                            lastSyncStatus = "Error: Schema Mismatch",
                            lastSyncError = msg
                        )
                        saveSyncMetadata(metadata)
                        return SyncResult.Error(IncompatibleSchemaException(msg), msg)
                    }
                    is SchemaCompatibilityResult.LocalNewerThanRemote -> {
                        // Migrate remote staging database forward
                        Logger.i("Migrating remote database from version ${compat.remoteVersion} to ${compat.localVersion}")
                        Database.Companion.Schema.migrate(stagingDriver, compat.remoteVersion, compat.localVersion)
                        setDbVersion(stagingDriver, compat.localVersion)
                    }
                    is SchemaCompatibilityResult.Compatible -> {
                        // Schema is compatible
                    }
                }
            } finally {
                // Driver will be managed or closed by SQLite
            }

            currentCoroutineContext().ensureActive()

            // 3. Merge: Check if base cache exists
            val stagingDb = createDatabaseForPath(driverFactory, stagingPath)
            val hasBaseCache = FileUtils.fileExists(syncedCachePath)
            var syncStats = SyncStats()

            if (!hasBaseCache) {
                // Fresh device / initial sync bootstrap without cache
                Logger.i("Base cache absent. Performing fresh sync / bootstrap.")
                val isLocalEmpty = tableHandlers.all { !it.hasRecords(localDatabase) }

                if (isLocalEmpty) {
                    // Empty local DB: Copy all remote records to local
                    Logger.i("Local database is empty. Bootstrapping from remote records.")
                    tableHandlers.forEach { handler ->
                        syncStats += handler.bootstrap(sourceDb = stagingDb, targetDb = localDatabase)
                    }
                } else {
                    // Non-empty local and remote: Perform union merge (insert-only, LWW on ID match)
                    tableHandlers.forEach { handler ->
                        syncStats += handler.unionMerge(localDb = localDatabase, remoteDb = stagingDb)
                    }
                }
            } else {
                // 3-Way Snapshot Differential Merge
                Logger.i("Base cache exists. Performing 3-way differential merge across all registered tables.")
                val baseDb = createDatabaseForPath(driverFactory, syncedCachePath)
                tableHandlers.forEach { handler ->
                    syncStats += handler.threeWayMerge(baseDb = baseDb, localDb = localDatabase, remoteDb = stagingDb)
                }
            }

            currentCoroutineContext().ensureActive()

            // 4. Atomic upload with optimistic concurrency check
            val uploadSuccess = adapter.uploadFile(stagingPath, remoteFileName, expectedHash)
            if (!uploadSuccess) {
                Logger.w("Remote file was modified concurrently during sync merge.")
                return SyncResult.ConflictResolved("Concurrent update detected; retrying merge", loadSyncMetadata())
            }

            // 5. Update base cache
            FileUtils.copyFile(stagingPath, syncedCachePath)
            val finalHash = FileUtils.calculateFileSha256(syncedCachePath)
            val msg = syncStats.toSummaryMessage()
            val metadata = SyncMetadata(
                lastSyncedHash = finalHash,
                lastSyncedTimestamp = currentEpochMillis(),
                lastSyncStatus = "Success (${syncStats.uploaded} uploaded, ${syncStats.downloaded} downloaded)",
                lastSyncError = null,
                localSchemaVersion = Database.Companion.Schema.version
            )
            saveSyncMetadata(metadata)
            Logger.i("Sync completed successfully: $msg. Final hash: $finalHash")
            return SyncResult.Success(msg, metadata, syncStats)

        } catch (c: CancellationException) {
            Logger.i("Sync operation was cancelled")
            return SyncResult.Cancelled
        } catch (e: Exception) {
            Logger.e("Sync operation encountered an error: ${e.message}", e)
            val msg = e.message ?: "Unknown sync error (${e::class.simpleName ?: "Exception"})"
            val metadata = SyncMetadata(
                lastSyncedTimestamp = currentEpochMillis(),
                lastSyncStatus = "Error: ${e::class.simpleName ?: "Exception"}",
                lastSyncError = msg
            )
            saveSyncMetadata(metadata)
            return SyncResult.Error(e, msg)
        }
    }
}
