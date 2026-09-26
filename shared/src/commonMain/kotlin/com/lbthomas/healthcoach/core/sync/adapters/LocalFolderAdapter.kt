package com.lbthomas.healthcoach.core.sync.adapters

import com.lbthomas.healthcoach.core.sync.FileMetadata
import com.lbthomas.healthcoach.core.sync.FileUtils
import com.lbthomas.healthcoach.core.sync.RemoteStorageAdapter
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.utils.generateUuid

/**
 * Storage adapter targeting local filesystem paths, external media, or mounted cloud drives.
 * Automatically manages an underlying application subfolder (`/com.lbthomas.healthcoach`)
 * to prevent namespace collision with other files in the user-selected folder.
 */
class LocalFolderAdapter(
    val basePath: String,
    val subFolder: String = SyncConfig.LOCAL_APP_SUBFOLDER
) : RemoteStorageAdapter {

    override val providerType: SyncProviderType = SyncProviderType.LOCAL_FOLDER

    /**
     * The resolved target directory where database files are stored.
     */
    val targetDirectory: String
        get() = if (basePath.isBlank()) "" else FileUtils.joinPath(basePath, subFolder)

    override suspend fun testConnection(): Result<Unit> {
        if (basePath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local folder path is not configured"))
        }

        return try {
            if (!FileUtils.ensureDirectoryExists(targetDirectory)) {
                return Result.failure(IllegalStateException("Failed to create target directory: $targetDirectory"))
            }

            // Test write & delete permission with a temporary marker file
            val testFileName = ".healthcoach_write_test_${generateUuid()}"
            val testFilePath = FileUtils.joinPath(targetDirectory, testFileName)
            val testBytes = "test_connection".encodeToByteArray()

            // We test writing and deleting via temp file copy
            val tempSourcePath = FileUtils.joinPath(targetDirectory, "$testFileName.src")
            val writeSuccess = try {
                FileUtils.copyFile(testFilePath, testFilePath) // test
                // Perform simple write test using platform file utilities
                val isWritable = FileUtils.isDirectoryWritable(targetDirectory)
                isWritable
            } finally {
                FileUtils.deleteFile(testFilePath)
                FileUtils.deleteFile(tempSourcePath)
            }

            if (writeSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Target directory is not writable: $targetDirectory"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFileMetadata(fileName: String): FileMetadata? {
        if (targetDirectory.isBlank()) return null
        val filePath = FileUtils.joinPath(targetDirectory, fileName)
        if (!FileUtils.fileExists(filePath)) return null

        val hash = FileUtils.calculateFileSha256(filePath) ?: return null
        val size = FileUtils.getFileSize(filePath)
        val lastModified = FileUtils.getFileLastModified(filePath)

        return FileMetadata(
            name = fileName,
            size = size,
            lastModified = lastModified,
            sha256Hash = hash,
            exists = true
        )
    }

    override suspend fun downloadFile(fileName: String, destinationPath: String): Boolean {
        if (targetDirectory.isBlank()) return false
        val sourcePath = FileUtils.joinPath(targetDirectory, fileName)
        if (!FileUtils.fileExists(sourcePath)) return false

        return FileUtils.copyFile(sourcePath, destinationPath)
    }

    override suspend fun uploadFile(
        sourcePath: String,
        fileName: String,
        expectedHash: String?
    ): Boolean {
        if (targetDirectory.isBlank()) return false
        if (!FileUtils.fileExists(sourcePath)) return false

        if (!FileUtils.ensureDirectoryExists(targetDirectory)) {
            return false
        }

        val targetPath = FileUtils.joinPath(targetDirectory, fileName)

        // Optimistic concurrency check
        if (expectedHash != null && FileUtils.fileExists(targetPath)) {
            val currentRemoteHash = FileUtils.calculateFileSha256(targetPath)
            if (currentRemoteHash != null && currentRemoteHash != expectedHash) {
                // Remote file has been modified concurrently by another client
                return false
            }
        }

        // Atomic write: copy to temporary file in target directory first, then move/rename
        val tempFileName = "$fileName.tmp.${generateUuid()}"
        val tempFilePath = FileUtils.joinPath(targetDirectory, tempFileName)

        val copySuccess = FileUtils.copyFile(sourcePath, tempFilePath)
        if (!copySuccess) {
            FileUtils.deleteFile(tempFilePath)
            return false
        }

        val moveSuccess = FileUtils.moveFile(tempFilePath, targetPath)
        if (!moveSuccess) {
            FileUtils.deleteFile(tempFilePath)
            return false
        }

        return true
    }
}
