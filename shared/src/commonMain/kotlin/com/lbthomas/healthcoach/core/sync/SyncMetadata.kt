package com.lbthomas.healthcoach.core.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SyncMetadata(
    val lastSyncedHash: String? = null,
    val lastSyncedTimestamp: Long? = null,
    val lastSyncStatus: String = "Never",
    val lastSyncError: String? = null,
    val localSchemaVersion: Long = 3L
) {
    companion object {
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromJson(jsonString: String): SyncMetadata {
            return runCatching {
                json.decodeFromString<SyncMetadata>(jsonString)
            }.getOrDefault(SyncMetadata())
        }

        fun toJson(metadata: SyncMetadata): String {
            return json.encodeToString(metadata)
        }
    }
}
