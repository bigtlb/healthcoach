package com.lbthomas.healthcoach.core.sync.handlers

import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.sync.GenericTableSyncHandler
import com.lbthomas.healthcoach.weight.data.WeightEntry

/**
 * Synchronization handler for the `weightEntry` table.
 */
object WeightTableSyncHandler : GenericTableSyncHandler<WeightEntry>("weightEntry") {
    override fun selectAll(database: Database): List<WeightEntry> =
        database.weightEntryQueries.selectAll().executeAsList()

    override fun getId(entity: WeightEntry): String = entity.id
    override fun getUpdatedAt(entity: WeightEntry): Long = entity.updated_at

    override fun insert(database: Database, entity: WeightEntry) {
        database.weightEntryQueries.insert(entity.id, entity.date, entity.weight, entity.updated_at)
    }

    override fun update(database: Database, entity: WeightEntry) {
        database.weightEntryQueries.update(entity.date, entity.weight, entity.updated_at, entity.id)
    }

    override fun delete(database: Database, id: String) {
        database.weightEntryQueries.delete(id)
    }
}
