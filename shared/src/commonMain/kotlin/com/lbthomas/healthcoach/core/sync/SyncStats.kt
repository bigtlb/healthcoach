package com.lbthomas.healthcoach.core.sync

/**
 * Statistics representing bidirectional record synchronization counters.
 *
 * @property uploaded Number of records uploaded (sent from local to remote).
 * @property downloaded Number of records downloaded (received from remote to local).
 */
data class SyncStats(
    val uploaded: Int = 0,
    val downloaded: Int = 0
) {
    val total: Int get() = uploaded + downloaded

    operator fun plus(other: SyncStats): SyncStats =
        SyncStats(
            uploaded = this.uploaded + other.uploaded,
            downloaded = this.downloaded + other.downloaded
        )

    fun toSummaryMessage(): String =
        "Sync successful: $uploaded uploaded, $downloaded downloaded"
}
