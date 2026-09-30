package com.lbthomas.healthcoach.core.sync.handlers

import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.sync.GenericTableSyncHandler
import com.lbthomas.healthcoach.foodjournal.data.MealEntry

/**
 * Synchronization handler for the `mealEntry` table.
 */
object MealEntryTableSyncHandler : GenericTableSyncHandler<MealEntry>("mealEntry") {
    override fun selectAll(database: Database): List<MealEntry> =
        database.mealEntryQueries.selectAll().executeAsList()

    override fun getId(entity: MealEntry): String = entity.id
    override fun getUpdatedAt(entity: MealEntry): Long = entity.updated_at

    override fun insert(database: Database, entity: MealEntry) {
        database.mealEntryQueries.insert(
            entity.id,
            entity.date,
            entity.mealTime,
            entity.foodId,
            entity.foodName,
            entity.foodDescription,
            entity.brand,
            entity.unitName,
            entity.unitQuantity,
            entity.caloriesPerUnit,
            entity.portionMultiplier,
            entity.totalCalories,
            entity.updated_at
        )
    }

    override fun update(database: Database, entity: MealEntry) {
        database.mealEntryQueries.update(
            entity.date,
            entity.mealTime,
            entity.foodId,
            entity.foodName,
            entity.foodDescription,
            entity.brand,
            entity.unitName,
            entity.unitQuantity,
            entity.caloriesPerUnit,
            entity.portionMultiplier,
            entity.totalCalories,
            entity.updated_at,
            entity.id
        )
    }

    override fun delete(database: Database, id: String) {
        database.mealEntryQueries.delete(id)
    }
}
