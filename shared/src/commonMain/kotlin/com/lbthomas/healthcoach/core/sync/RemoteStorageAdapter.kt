package com.lbthomas.healthcoach.core.sync

/**
 * Common abstraction for remote/external storage backends.
 */
interface RemoteStorageAdapter {
    /**
     * The type of storage provider implemented by this adapter.
     */
    val providerType: SyncProviderType

    /**
     * Test connectivity, authentication, and directory write permissions.
     * Returns Result.success(Unit) on success or Result.failure on error.
     */
    suspend fun testConnection(): Result<Unit>

    /**
     * Retrieve metadata for the specified remote file, or null if the file does not exist.
     */
    suspend fun getFileMetadata(fileName: String = SyncConfig.DEFAULT_REMOTE_DB_NAME): FileMetadata?

    /**
     * Download the remote file to the local destination file path.
     * Returns true on success, false otherwise.
     */
    suspend fun downloadFile(
        fileName: String = SyncConfig.DEFAULT_REMOTE_DB_NAME,
        destinationPath: String
    ): Boolean

    /**
     * Atomically upload a local file to the remote storage.
     * If [expectedHash] is provided, the upload will only proceed if the current remote
     * file hash matches [expectedHash] (optimistic concurrency control).
     * Returns true on success, false if a conflict occurs or upload fails.
     */
    suspend fun uploadFile(
        sourcePath: String,
        fileName: String = SyncConfig.DEFAULT_REMOTE_DB_NAME,
        expectedHash: String? = null
    ): Boolean

    /**
     * Check if a remote file exists.
     */
    suspend fun fileExists(fileName: String = SyncConfig.DEFAULT_REMOTE_DB_NAME): Boolean {
        return getFileMetadata(fileName)?.exists == true
    }
}
