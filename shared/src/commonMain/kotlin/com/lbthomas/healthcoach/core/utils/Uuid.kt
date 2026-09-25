package com.lbthomas.healthcoach.core.utils

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Generates a standard RFC 9562 UUIDv7 (time-ordered UUID) using Kotlin standard library [Uuid].
 *
 * UUIDv7 provides optimal locality of reference and insert performance in SQLite B-tree indexes
 * compared to random UUIDv4.
 */
@OptIn(ExperimentalUuidApi::class)
fun generateUuid(): String {
    return Uuid.generateV7().toString()
}
