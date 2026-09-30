package com.lbthomas.healthcoach.core.sync.handlers

import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.sync.GenericTableSyncHandler
import com.lbthomas.healthcoach.foodjournal.data.FoodUnit

/**
 * Synchronization handler for the `foodUnit` table.
 */
object FoodUnitTableSyncHandler : GenericTableSyncHandler<FoodUnit>("foodUnit") {
    override fun selectAll(database: Database): List<FoodUnit> =
        database.foodUnitQueries.selectAll().executeAsList()

    override fun getId(entity: FoodUnit): String = entity.id
    override fun getUpdatedAt(entity: FoodUnit): Long = entity.updated_at

    override fun insert(database: Database, entity: FoodUnit) {
        database.foodUnitQueries.insert(
            entity.id,
            entity.name,
            entity.abbreviation,
            entity.isDefault,
            entity.updated_at
        )
    }

    override fun update(database: Database, entity: FoodUnit) {
        database.foodUnitQueries.update(
            entity.name,
            entity.abbreviation,
            entity.isDefault,
            entity.updated_at,
            entity.id
        )
    }

    override fun delete(database: Database, id: String) {
        database.foodUnitQueries.delete(id)
    }
}
