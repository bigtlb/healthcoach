package com.lbthomas.healthcoach.core.sync.handlers

import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.sync.GenericTableSyncHandler
import com.lbthomas.healthcoach.foodjournal.data.FoodItem

/**
 * Synchronization handler for the `foodItem` table.
 */
object FoodItemTableSyncHandler : GenericTableSyncHandler<FoodItem>("foodItem") {
    override fun selectAll(database: Database): List<FoodItem> =
        database.foodItemQueries.selectAll().executeAsList()

    override fun getId(entity: FoodItem): String = entity.id
    override fun getUpdatedAt(entity: FoodItem): Long = entity.updated_at

    override fun insert(database: Database, entity: FoodItem) {
        database.foodItemQueries.insert(
            entity.id,
            entity.name,
            entity.brand,
            entity.upc,
            entity.description,
            entity.unitName,
            entity.unitQuantity,
            entity.caloriesPerUnit,
            entity.updated_at
        )
    }

    override fun update(database: Database, entity: FoodItem) {
        database.foodItemQueries.update(
            entity.name,
            entity.brand,
            entity.upc,
            entity.description,
            entity.unitName,
            entity.unitQuantity,
            entity.caloriesPerUnit,
            entity.updated_at,
            entity.id
        )
    }

    override fun delete(database: Database, id: String) {
        database.foodItemQueries.delete(id)
    }
}
