package com.lbthomas.healthcoach.core.sync

import com.lbthomas.healthcoach.Database

/**
 * Interface defining synchronization operations for an individual database table.
 */
interface TableSyncHandler {
    val tableName: String
    fun hasRecords(database: Database): Boolean
    fun selectRecordCount(database: Database): Int
    fun bootstrap(sourceDb: Database, targetDb: Database): SyncStats
    fun unionMerge(localDb: Database, remoteDb: Database): SyncStats
    fun threeWayMerge(baseDb: Database, localDb: Database, remoteDb: Database): SyncStats
}

/**
 * Generic base handler that implements standard differential 3-way merge, union merge,
 * and bootstrap copying for any table adhering to the standard UUID identifier and
 * modification timestamp (`updated_at`) contract.
 */
abstract class GenericTableSyncHandler<T : Any>(
    override val tableName: String
) : TableSyncHandler {

    abstract fun selectAll(database: Database): List<T>
    abstract fun getId(entity: T): String
    abstract fun getUpdatedAt(entity: T): Long
    abstract fun insert(database: Database, entity: T)
    abstract fun update(database: Database, entity: T)
    abstract fun delete(database: Database, id: String)

    override fun hasRecords(database: Database): Boolean = selectAll(database).any { getUpdatedAt(it) > 0L }

    override fun selectRecordCount(database: Database): Int = selectAll(database).count { getUpdatedAt(it) > 0L }

    override fun bootstrap(sourceDb: Database, targetDb: Database): SyncStats {
        val records = selectAll(sourceDb)
        var downloaded = 0
        targetDb.transaction {
            val existingTarget = selectAll(targetDb).associateBy { getId(it) }
            records.forEach { record ->
                val id = getId(record)
                val existing = existingTarget[id]
                if (existing == null) {
                    insert(targetDb, record)
                    if (getUpdatedAt(record) > 0L) {
                        downloaded++
                    }
                } else if (existing != record && getUpdatedAt(record) > getUpdatedAt(existing)) {
                    update(targetDb, record)
                    if (getUpdatedAt(record) > 0L) {
                        downloaded++
                    }
                }
            }
        }
        return SyncStats(uploaded = 0, downloaded = downloaded)
    }

    override fun unionMerge(localDb: Database, remoteDb: Database): SyncStats {
        val localRecords = selectAll(localDb).associateBy { getId(it) }
        val remoteRecords = selectAll(remoteDb).associateBy { getId(it) }
        val allIds = localRecords.keys + remoteRecords.keys
        var uploaded = 0
        var downloaded = 0

        localDb.transaction {
            remoteDb.transaction {
                allIds.forEach { id ->
                    val l = localRecords[id]
                    val r = remoteRecords[id]
                    val winning = when {
                        l != null && r != null -> if (getUpdatedAt(l) >= getUpdatedAt(r)) l else r
                        l != null -> l
                        else -> r!!
                    }
                    if (l == null) {
                        insert(localDb, winning)
                        if (getUpdatedAt(winning) > 0L) downloaded++
                    } else if (l != winning) {
                        update(localDb, winning)
                        if (getUpdatedAt(winning) > 0L) downloaded++
                    }

                    if (r == null) {
                        insert(remoteDb, winning)
                        if (getUpdatedAt(winning) > 0L) uploaded++
                    } else if (r != winning) {
                        update(remoteDb, winning)
                        if (getUpdatedAt(winning) > 0L) uploaded++
                    }
                }
            }
        }
        return SyncStats(uploaded = uploaded, downloaded = downloaded)
    }

    override fun threeWayMerge(baseDb: Database, localDb: Database, remoteDb: Database): SyncStats {
        val baseRecords = selectAll(baseDb).associateBy { getId(it) }
        val localRecords = selectAll(localDb).associateBy { getId(it) }
        val remoteRecords = selectAll(remoteDb).associateBy { getId(it) }
        val allIds = baseRecords.keys + localRecords.keys + remoteRecords.keys
        var uploaded = 0
        var downloaded = 0

        localDb.transaction {
            remoteDb.transaction {
                allIds.forEach { id ->
                    val b = baseRecords[id]
                    val l = localRecords[id]
                    val r = remoteRecords[id]

                    when {
                        // Unchanged anywhere
                        l == r -> { /* In sync */ }

                        // New row added on Local only
                        b == null && l != null && r == null -> {
                            insert(remoteDb, l)
                            uploaded++
                        }

                        // New row added on Remote only
                        b == null && l == null && r != null -> {
                            insert(localDb, r)
                            downloaded++
                        }

                        // Both sides added row independently with same ID -> LWW
                        b == null && l != null && r != null -> {
                            if (getUpdatedAt(l) >= getUpdatedAt(r)) {
                                update(remoteDb, l)
                                uploaded++
                            } else {
                                update(localDb, r)
                                downloaded++
                            }
                        }

                        // Row existed in base snapshot
                        b != null -> {
                            when {
                                // Both kept and modified -> LWW arbitration
                                l != null && r != null -> {
                                    if (l == b && r != b) {
                                        // Remote modified
                                        update(localDb, r)
                                        downloaded++
                                    } else if (r == b && l != b) {
                                        // Local modified
                                        update(remoteDb, l)
                                        uploaded++
                                    } else if (l != b && r != b) {
                                        // Conflict: both modified
                                        if (getUpdatedAt(l) >= getUpdatedAt(r)) {
                                            update(remoteDb, l)
                                            uploaded++
                                        } else {
                                            update(localDb, r)
                                            downloaded++
                                        }
                                    }
                                }

                                // Local deleted, Remote kept
                                l == null && r != null -> {
                                    if (r == b) {
                                        // Remote didn't touch it -> propagate deletion to Remote
                                        delete(remoteDb, id)
                                        uploaded++
                                    } else {
                                        // Remote edited concurrently
                                        if (getUpdatedAt(r) > getUpdatedAt(b)) {
                                            // Remote edit is newer than base snapshot -> restore on Local
                                            insert(localDb, r)
                                            downloaded++
                                        } else {
                                            // Local deletion wins -> delete on Remote
                                            delete(remoteDb, id)
                                            uploaded++
                                        }
                                    }
                                }

                                // Remote deleted, Local kept
                                l != null && r == null -> {
                                    if (l == b) {
                                        // Local didn't touch it -> propagate deletion to Local
                                        delete(localDb, id)
                                        downloaded++
                                    } else {
                                        // Local edited concurrently
                                        if (getUpdatedAt(l) > getUpdatedAt(b)) {
                                            // Local edit is newer than base snapshot -> restore on Remote
                                            insert(remoteDb, l)
                                            uploaded++
                                        } else {
                                            // Remote deletion wins -> delete on Local
                                            delete(localDb, id)
                                            downloaded++
                                        }
                                    }
                                }

                                // Both deleted
                                l == null && r == null -> { /* Deleted on both */ }
                            }
                        }
                    }
                }
            }
        }
        return SyncStats(uploaded = uploaded, downloaded = downloaded)
    }
}
