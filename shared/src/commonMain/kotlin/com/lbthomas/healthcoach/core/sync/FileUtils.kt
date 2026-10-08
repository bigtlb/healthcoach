package com.lbthomas.healthcoach.core.sync

/**
 * Cross-platform file and crypto utilities for sync operations.
 */
expect object FileUtils {
    fun calculateFileSha256(filePath: String): String?
    fun calculateSha256(data: ByteArray): String
    fun calculateUncompressedSha256(filePath: String): String?
    fun copyFile(sourcePath: String, destinationPath: String): Boolean
    fun moveFile(sourcePath: String, destinationPath: String): Boolean
    fun deleteFile(filePath: String): Boolean
    fun fileExists(filePath: String): Boolean
    fun ensureDirectoryExists(directoryPath: String): Boolean
    fun isDirectoryWritable(directoryPath: String): Boolean
    fun getFileSize(filePath: String): Long
    fun getFileLastModified(filePath: String): Long
    fun joinPath(base: String, vararg parts: String): String
    fun isGzipFile(filePath: String): Boolean
    fun compressGzip(sourcePath: String, destinationPath: String): Boolean
    fun decompressGzip(sourcePath: String, destinationPath: String): Boolean
    fun decompressGzipIfNeeded(sourcePath: String, destinationPath: String): Boolean
    fun readUtf8String(filePath: String): String?
    fun writeUtf8String(filePath: String, content: String): Boolean
}
