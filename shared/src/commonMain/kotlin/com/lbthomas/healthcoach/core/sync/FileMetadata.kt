package com.lbthomas.healthcoach.core.sync

/**
 * Metadata for a file in local or remote storage.
 */
data class FileMetadata(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val sha256Hash: String,
    val exists: Boolean = true
)
