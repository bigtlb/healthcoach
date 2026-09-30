package com.lbthomas.healthcoach.core.sync.handlers

import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.bloodpressure.data.BloodPressureEntry
import com.lbthomas.healthcoach.core.sync.GenericTableSyncHandler

/**
 * Synchronization handler for the `bloodPressureEntry` table.
 */
object BloodPressureTableSyncHandler : GenericTableSyncHandler<BloodPressureEntry>("bloodPressureEntry") {
    override fun selectAll(database: Database): List<BloodPressureEntry> =
        database.bloodPressureEntryQueries.selectAll().executeAsList()

    override fun getId(entity: BloodPressureEntry): String = entity.id
    override fun getUpdatedAt(entity: BloodPressureEntry): Long = entity.updated_at

    override fun insert(database: Database, entity: BloodPressureEntry) {
        database.bloodPressureEntryQueries.insert(
            entity.id,
            entity.dateTime,
            entity.systolic,
            entity.diastolic,
            entity.pulse,
            entity.updated_at
        )
    }

    override fun update(database: Database, entity: BloodPressureEntry) {
        database.bloodPressureEntryQueries.update(
            entity.dateTime,
            entity.systolic,
            entity.diastolic,
            entity.pulse,
            entity.updated_at,
            entity.id
        )
    }

    override fun delete(database: Database, id: String) {
        database.bloodPressureEntryQueries.delete(id)
    }
}
