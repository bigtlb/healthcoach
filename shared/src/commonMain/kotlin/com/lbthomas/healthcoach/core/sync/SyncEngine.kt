package com.lbthomas.healthcoach.core.sync

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.*
import com.lbthomas.healthcoach.core.sync.handlers.*
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
    val tableHandlers: List<TableSyncHandler> = listOf(
        WeightTableSyncHandler,
        BloodPressureTableSyncHandler,
        FoodUnitTableSyncHandler,
        FoodItemTableSyncHandler,
        MealEntryTableSyncHandler,
        ProfileTableSyncHandler
    )
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

    fun getSyncedCachePath(providerType: SyncProviderType = SyncProviderType.LOCAL_FOLDER): String {
        val providerKey = providerType.name.lowercase()
        return FileUtils.joinPath(getSyncDirectory(), "synced_cache_$providerKey.db.gz")
    }

    fun getSyncedWorkingCachePath(providerType: SyncProviderType = SyncProviderType.LOCAL_FOLDER): String {
        val providerKey = providerType.name.lowercase()
        return FileUtils.joinPath(getSyncDirectory(), "synced_cache_$providerKey.db")
    }

    fun getSyncedCacheMetaPath(providerType: SyncProviderType = SyncProviderType.LOCAL_FOLDER): String {
        val providerKey = providerType.name.lowercase()
        return FileUtils.joinPath(getSyncDirectory(), "synced_cache_$providerKey.meta")
    }

    fun getTargetConfigKey(config: SyncConfig): String {
        val remoteFileName = config.remoteFileName.ifBlank { SyncConfig.DEFAULT_REMOTE_DB_NAME }.trim()
        return when (config.providerType) {
            SyncProviderType.LOCAL_FOLDER -> "LOCAL_FOLDER:${config.localFolderPath.trim()}:$remoteFileName"
            SyncProviderType.GOOGLE_DRIVE -> "GOOGLE_DRIVE:${config.googleAccountEmail.trim().lowercase()}:$remoteFileName"
            SyncProviderType.PEER_TO_PEER -> "PEER_TO_PEER:${config.peerServerHost.trim()}:${config.peerServerPort}:${config.peerServerToken.trim()}:$remoteFileName"
        }
    }

    fun hasSyncedCache(providerType: SyncProviderType): Boolean {
        val gzPath = getSyncedCachePath(providerType)
        val dbPath = getSyncedWorkingCachePath(providerType)
        return FileUtils.fileExists(gzPath) || FileUtils.fileExists(dbPath)
    }

    fun migrateLegacyCacheIfNeeded(providerType: SyncProviderType, targetKey: String) {
        val syncDir = getSyncDirectory()
        val legacyDbPath = FileUtils.joinPath(syncDir, "synced_cache.db")
        val legacyGzPath = FileUtils.joinPath(syncDir, "synced_cache.db.gz")
        val compressedCachePath = getSyncedCachePath(providerType)
        val metaPath = getSyncedCacheMetaPath(providerType)

        if (!hasSyncedCache(providerType)) {
            if (FileUtils.fileExists(legacyDbPath)) {
                Logger.i("Migrating legacy uncompressed synced_cache.db to $compressedCachePath")
                if (FileUtils.compressGzip(legacyDbPath, compressedCachePath)) {
                    FileUtils.writeUtf8String(metaPath, targetKey)
                    FileUtils.deleteFile(legacyDbPath)
                }
            } else if (FileUtils.fileExists(legacyGzPath)) {
                Logger.i("Migrating legacy synced_cache.db.gz to $compressedCachePath")
                if (FileUtils.copyFile(legacyGzPath, compressedCachePath)) {
                    FileUtils.writeUtf8String(metaPath, targetKey)
                    FileUtils.deleteFile(legacyGzPath)
                }
            }
        } else {
            if (FileUtils.fileExists(legacyDbPath)) {
                FileUtils.deleteFile(legacyDbPath)
            }
            if (FileUtils.fileExists(legacyGzPath)) {
                FileUtils.deleteFile(legacyGzPath)
            }
        }
    }

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
                    sync = it.sync.copy(
                        lastSyncStatus = metadata.lastSyncStatus,
                        lastSyncTime = metadata.lastSyncedTimestamp,
                        lastSyncHash = metadata.lastSyncedHash,
                        lastSyncError = metadata.lastSyncError,
                        lastSyncFailed = metadata.lastSyncStatus.startsWith("Error")
                    )
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
        val providerType = config.providerType
        val compressedCachePath = getSyncedCachePath(providerType)
        val workingCachePath = getSyncedWorkingCachePath(providerType)
        val metaPath = getSyncedCacheMetaPath(providerType)
        val stagingPath = getRemoteStagingPath()
        val liveDbPath = driverFactory.getDatabaseFilePath()
        val currentTargetKey = getTargetConfigKey(config)

        try {
            currentCoroutineContext().ensureActive()

            // 0. Invalidate cache if the target configuration for this provider has changed
            migrateLegacyCacheIfNeeded(providerType, currentTargetKey)
            if (hasSyncedCache(providerType)) {
                val savedTargetKey = FileUtils.readUtf8String(metaPath)
                if (savedTargetKey != null && savedTargetKey != currentTargetKey) {
                    Logger.i("Configuration for provider $providerType changed from '$savedTargetKey' to '$currentTargetKey'. Clearing cached base snapshot.")
                    clearLocalSyncCache(providerType)
                }
            }

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
                val initStagingDriver = driverFactory.createDriverForPath(stagingPath)
                try {
                    incrementalVacuum(initStagingDriver)
                } catch (e: Exception) {
                    Logger.w("Failed to vacuum staging database prior to initial upload: ${e.message}")
                }
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

                // Compress staging database to persistent base cache
                FileUtils.compressGzip(stagingPath, compressedCachePath)
                FileUtils.writeUtf8String(metaPath, currentTargetKey)
                val finalHash = FileUtils.calculateFileSha256(stagingPath)
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

            // Ensure staging file is uncompressed SQLite database
            if (FileUtils.isGzipFile(stagingPath)) {
                val tempDecompressed = "$stagingPath.decompressed_${currentEpochMillis()}"
                if (FileUtils.decompressGzip(stagingPath, tempDecompressed)) {
                    FileUtils.moveFile(tempDecompressed, stagingPath)
                } else {
                    FileUtils.deleteFile(tempDecompressed)
                }
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

            // 3. Prepare base cache for merge
            val stagingDb = createDatabaseForPath(driverFactory, stagingPath)
            var hasBaseCache = false
            if (FileUtils.fileExists(compressedCachePath)) {
                FileUtils.deleteFile(workingCachePath)
                if (FileUtils.decompressGzip(compressedCachePath, workingCachePath)) {
                    hasBaseCache = true
                }
            } else if (FileUtils.fileExists(workingCachePath)) {
                hasBaseCache = true
            }

            var syncStats = SyncStats()

            if (!hasBaseCache) {
                // Fresh device / initial sync bootstrap without cache
                Logger.i("Base cache absent for $providerType. Performing fresh sync / bootstrap.")
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
                Logger.i("Base cache exists for $providerType. Performing 3-way differential merge across all registered tables.")
                val baseDb = createDatabaseForPath(driverFactory, workingCachePath)
                tableHandlers.forEach { handler ->
                    syncStats += handler.threeWayMerge(baseDb = baseDb, localDb = localDatabase, remoteDb = stagingDb)
                }
            }

            currentCoroutineContext().ensureActive()

            // 4. Compact and atomic upload with optimistic concurrency check
            val stagingCompactDriver = driverFactory.createDriverForPath(stagingPath)
            try {
                incrementalVacuum(stagingCompactDriver)
            } catch (e: Exception) {
                Logger.w("Failed to vacuum staging database prior to upload: ${e.message}")
            }

            val uploadSuccess = adapter.uploadFile(stagingPath, remoteFileName, expectedHash)
            if (!uploadSuccess) {
                Logger.w("Remote file was modified concurrently during sync merge.")
                return SyncResult.ConflictResolved("Concurrent update detected; retrying merge", loadSyncMetadata())
            }

            // 5. Update persistent base cache (compressed) and configuration metadata
            FileUtils.compressGzip(stagingPath, compressedCachePath)
            FileUtils.writeUtf8String(metaPath, currentTargetKey)
            val finalHash = FileUtils.calculateFileSha256(stagingPath)
            val msg = syncStats.toSummaryMessage()
            val metadata = SyncMetadata(
                lastSyncedHash = finalHash,
                lastSyncedTimestamp = currentEpochMillis(),
                lastSyncStatus = "Success (${syncStats.uploaded} uploaded, ${syncStats.downloaded} downloaded)",
                lastSyncError = null,
                localSchemaVersion = Database.Companion.Schema.version
            )
            saveSyncMetadata(metadata)
            Logger.i("Sync completed successfully for $providerType: $msg. Final hash: $finalHash")
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
        } finally {
            // Clean up temporary uncompressed working database to save space
            FileUtils.deleteFile(workingCachePath)
        }
    }

    /**
     * Clears local sync staging and cache files and resets sync metadata in settings.
     * If [providerType] is specified, clears only that provider's cache and metadata key.
     * If [providerType] is null, clears all provider caches, staging, and metadata.
     */
    fun clearLocalSyncCache(providerType: SyncProviderType? = null) {
        try {
            if (providerType != null) {
                FileUtils.deleteFile(getSyncedCachePath(providerType))
                FileUtils.deleteFile(getSyncedWorkingCachePath(providerType))
                FileUtils.deleteFile(getSyncedCacheMetaPath(providerType))
                Logger.i("Cleared local sync cache for provider $providerType")
            } else {
                for (type in SyncProviderType.entries) {
                    FileUtils.deleteFile(getSyncedCachePath(type))
                    FileUtils.deleteFile(getSyncedWorkingCachePath(type))
                    FileUtils.deleteFile(getSyncedCacheMetaPath(type))
                }
                FileUtils.deleteFile(FileUtils.joinPath(getSyncDirectory(), "synced_cache.db"))
                FileUtils.deleteFile(FileUtils.joinPath(getSyncDirectory(), "synced_cache.db.gz"))
                FileUtils.deleteFile(getRemoteStagingPath())
                FileUtils.deleteFile(getMetadataPath())
                saveSyncMetadata(
                    SyncMetadata(
                        lastSyncedHash = null,
                        lastSyncedTimestamp = 0L,
                        lastSyncStatus = "Never Synced",
                        lastSyncError = null
                    )
                )
                Logger.i("Cleared all local sync caches and metadata")
            }
        } catch (e: Exception) {
            Logger.w("Failed to clear local sync cache: ${e.message}")
        }
    }

    /**
     * Resets the remote sync destination file and clears the local base cache for that provider.
     */
    suspend fun resetRemoteDestination(
        adapter: RemoteStorageAdapter,
        remoteFileName: String = SyncConfig.DEFAULT_REMOTE_DB_NAME
    ): Result<Unit> {
        return try {
            val deleteSuccess = adapter.deleteFile(remoteFileName)
            clearLocalSyncCache(adapter.providerType)
            if (deleteSuccess) {
                Logger.i("Successfully reset remote destination for provider ${adapter.providerType.name}")
                Result.success(Unit)
            } else {
                val err = "Failed to delete remote file for provider ${adapter.providerType.name}"
                Logger.w(err)
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Logger.e("Error resetting remote destination: ${e.message}", e)
            Result.failure(e)
        }
    }
}
