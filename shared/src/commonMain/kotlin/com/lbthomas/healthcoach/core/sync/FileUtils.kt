package com.lbthomas.healthcoach.core.sync

/**
 * Cross-platform file and crypto utilities for sync operations.
 */
expect object FileUtils {
    fun calculateFileSha256(filePath: String): String?
    fun calculateSha256(data: ByteArray): String
    fun copyFile(sourcePath: String, destinationPath: String): Boolean
    fun moveFile(sourcePath: String, destinationPath: String): Boolean
    fun deleteFile(filePath: String): Boolean
    fun fileExists(filePath: String): Boolean
    fun ensureDirectoryExists(directoryPath: String): Boolean
    fun isDirectoryWritable(directoryPath: String): Boolean
    fun getFileSize(filePath: String): Long
    fun getFileLastModified(filePath: String): Long
    fun joinPath(base: String, vararg parts: String): String
}
