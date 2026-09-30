package com.lbthomas.healthcoach.core.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.sync.handlers.*
import kotlin.test.*

class FoodJournalSyncTest {

    private lateinit var driverA: JdbcSqliteDriver
    private lateinit var dbA: Database
    private lateinit var driverB: JdbcSqliteDriver
    private lateinit var dbB: Database
    private lateinit var driverBase: JdbcSqliteDriver
    private lateinit var dbBase: Database

    @BeforeTest
    fun setUp() {
        driverA = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        dbA = createDatabaseForDriver(driverA)

        driverB = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        dbB = createDatabaseForDriver(driverB)

        driverBase = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        dbBase = createDatabaseForDriver(driverBase)
    }

    @Test
    fun testFoodUnitSyncHandlersBootstrapAndMerge() {
        // Insert custom food unit on dbA
        dbA.foodUnitQueries.insert("unit-custom", "CustomUnit", "cu", 0L, 1000L)

        // Bootstrap from dbA to dbB
        val bootstrapStats = FoodUnitTableSyncHandler.bootstrap(sourceDb = dbA, targetDb = dbB)
        assertEquals(0, bootstrapStats.uploaded)
        assertEquals(1, bootstrapStats.downloaded)

        val unitsOnB = dbB.foodUnitQueries.selectAll().executeAsList()
        assertTrue(unitsOnB.any { it.id == "unit-custom" && it.name == "CustomUnit" })

        // 3-way merge with update on dbA and keep on dbB
        dbBase.foodUnitQueries.insert("unit-custom", "CustomUnit", "cu", 0L, 1000L)
        dbA.foodUnitQueries.update("CustomUnitUpdated", "cu", 0L, 2000L, "unit-custom")

        val mergeStats = FoodUnitTableSyncHandler.threeWayMerge(baseDb = dbBase, localDb = dbA, remoteDb = dbB)
        assertEquals(1, mergeStats.uploaded) // remote updated
        assertEquals(0, mergeStats.downloaded)

        val updatedUnitOnB = dbB.foodUnitQueries.selectById("unit-custom").executeAsOneOrNull()
        assertNotNull(updatedUnitOnB)
        assertEquals("CustomUnitUpdated", updatedUnitOnB.name)
    }

    @Test
    fun testFoodItemSyncHandlers() {
        // Insert master food on dbA
        dbA.foodItemQueries.insert(
            id = "food-1",
            name = "Oatmeal",
            brand = "Quaker",
            upc = "0123456789",
            description = "Rolled oats",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 150.0,
            updated_at = 1000L
        )

        // Union merge
        val stats = FoodItemTableSyncHandler.unionMerge(localDb = dbA, remoteDb = dbB)
        assertEquals(1, stats.uploaded)
        assertEquals(0, stats.downloaded)

        val foodOnB = dbB.foodItemQueries.selectById("food-1").executeAsOneOrNull()
        assertNotNull(foodOnB)
        assertEquals("Oatmeal", foodOnB.name)
        assertEquals(150.0, foodOnB.caloriesPerUnit)

        // Delete on dbA in 3-way merge
        dbBase.foodItemQueries.insert(
            id = "food-1",
            name = "Oatmeal",
            brand = "Quaker",
            upc = "0123456789",
            description = "Rolled oats",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 150.0,
            updated_at = 1000L
        )
        FoodItemTableSyncHandler.delete(dbA, "food-1")

        val threeWayStats = FoodItemTableSyncHandler.threeWayMerge(baseDb = dbBase, localDb = dbA, remoteDb = dbB)
        assertEquals(1, threeWayStats.uploaded)
        assertNull(dbB.foodItemQueries.selectById("food-1").executeAsOneOrNull())
    }

    @Test
    fun testMealEntrySyncHandlers() {
        // Insert meal entry on dbA
        dbA.mealEntryQueries.insert(
            id = "meal-1",
            date = "2026-09-30",
            mealTime = "BREAKFAST",
            foodId = "food-1",
            foodName = "Oatmeal",
            foodDescription = "Rolled oats",
            brand = "Quaker",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 150.0,
            portionMultiplier = 1.5,
            totalCalories = 225.0,
            updated_at = 1000L
        )

        val stats = MealEntryTableSyncHandler.unionMerge(localDb = dbA, remoteDb = dbB)
        assertEquals(1, stats.uploaded)
        assertEquals(0, stats.downloaded)

        val mealOnB = dbB.mealEntryQueries.selectById("meal-1").executeAsOneOrNull()
        assertNotNull(mealOnB)
        assertEquals("2026-09-30", mealOnB.date)
        assertEquals("BREAKFAST", mealOnB.mealTime)
        assertEquals(225.0, mealOnB.totalCalories)
    }
}
