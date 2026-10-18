package com.lbthomas.healthcoach.features.settings

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.enums.BloodPressureCategory
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.features.foodjournal.data.DefaultFoodData
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsDatabaseResetTest {

    private lateinit var tempDir: File
    private lateinit var testDriverFactory: DriverFactory
    private lateinit var settingsStore: SettingsStore

    private class TestDriverFactory(private val baseDir: File) : DriverFactory() {
        override fun createDriver() = JdbcSqliteDriver("jdbc:sqlite:${baseDir.absolutePath}/healthcoach.db")
        override fun createDriverForPath(dbFilePath: String) = JdbcSqliteDriver("jdbc:sqlite:$dbFilePath")
        override fun getDatabaseDirectory() = baseDir.absolutePath
        override fun getDatabaseFilePath() = "${baseDir.absolutePath}/healthcoach.db"
    }

    @BeforeTest
    fun setUp() {
        tempDir = File.createTempFile("hc_test_reset_db", "").apply {
            delete()
            mkdirs()
        }
        testDriverFactory = TestDriverFactory(tempDir)
        val settingsFile = File(tempDir, "settings.json")
        settingsStore = SettingsStore(settingsFile)

        val driver = testDriverFactory.createDriver()
        createDatabaseForDriver(driver)
        driver.close()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun seedUserDataAndCustomFoods() {
        val driver = testDriverFactory.createDriver()
        val db = createDatabaseForDriver(driver)

        // Seed metric entries
        db.weightEntryQueries.insert(
            id = "weight-1",
            date = "2026-10-18",
            weight = 175.5,
            updated_at = 1000L
        )

        db.bloodPressureEntryQueries.insert(
            id = "bp-1",
            dateTime = "2026-10-18T10:00:00",
            systolic = 120L,
            diastolic = 80L,
            pulse = 70L,
            updated_at = 1000L
        )

        db.mealEntryQueries.insert(
            id = "meal-1",
            date = "2026-10-18",
            mealTime = MealTime.LUNCH.name,
            foodId = "food-custom-1",
            foodName = "Custom Protein Bar",
            foodDescription = "High protein bar",
            brand = "CustomBrand",
            unitName = "CustomScoop",
            unitQuantity = 1.0,
            caloriesPerUnit = 250.0,
            portionMultiplier = 1.0,
            totalCalories = 250.0,
            updated_at = 1000L
        )

        // Seed custom food unit and custom food item
        db.foodUnitQueries.insert(
            id = "unit-custom-1",
            name = "CustomScoop",
            abbreviation = "cs",
            isDefault = 0L,
            updated_at = 1000L
        )

        db.foodItemQueries.insert(
            id = "food-custom-1",
            name = "Custom Protein Bar",
            brand = "CustomBrand",
            upc = "123456789012",
            description = "High protein bar",
            unitName = "CustomScoop",
            unitQuantity = 1.0,
            caloriesPerUnit = 250.0,
            updated_at = 1000L
        )

        driver.close()
    }

    @Test
    fun testResetDatabasePreservesFoodDefinitionsWhenFalse() = runBlocking {
        seedUserDataAndCustomFoods()

        val viewModel = SettingsViewModel(
            persistence = settingsStore,
            driverFactory = testDriverFactory
        )

        // Reset with resetFoodDefinitions = false (default)
        val result = viewModel.resetDatabaseAndSettings(resetFoodDefinitions = false)
        assertTrue(result.isSuccess)

        val driver = testDriverFactory.createDriver()
        val db = createDatabaseForDriver(driver)

        // Metrics and logs cleared
        assertEquals(0, db.weightEntryQueries.selectAll().executeAsList().size)
        assertEquals(0, db.bloodPressureEntryQueries.selectAll().executeAsList().size)
        assertEquals(0, db.mealEntryQueries.selectAll().executeAsList().size)

        // Custom food definitions preserved
        assertNotNull(db.foodItemQueries.selectById("food-custom-1").executeAsOneOrNull())
        assertNotNull(db.foodUnitQueries.selectById("unit-custom-1").executeAsOneOrNull())

        // Default foods still present
        assertNotNull(db.foodItemQueries.selectById("food-usda-0001").executeAsOneOrNull())

        driver.close()
    }

    @Test
    fun testResetDatabasePurgesFoodDefinitionsWhenTrue() = runBlocking {
        seedUserDataAndCustomFoods()

        val viewModel = SettingsViewModel(
            persistence = settingsStore,
            driverFactory = testDriverFactory
        )

        // Reset with resetFoodDefinitions = true
        val result = viewModel.resetDatabaseAndSettings(resetFoodDefinitions = true)
        assertTrue(result.isSuccess)

        val driver = testDriverFactory.createDriver()
        val db = createDatabaseForDriver(driver)

        // Metrics and logs cleared
        assertEquals(0, db.weightEntryQueries.selectAll().executeAsList().size)
        assertEquals(0, db.bloodPressureEntryQueries.selectAll().executeAsList().size)
        assertEquals(0, db.mealEntryQueries.selectAll().executeAsList().size)

        // Custom food definitions purged
        assertNull(db.foodItemQueries.selectById("food-custom-1").executeAsOneOrNull())
        assertNull(db.foodUnitQueries.selectById("unit-custom-1").executeAsOneOrNull())

        // Default foods restored
        assertNotNull(db.foodItemQueries.selectById("food-usda-0001").executeAsOneOrNull())

        driver.close()
    }

    @Test
    fun testDefaultParameterPreservesFoodDefinitions() = runBlocking {
        seedUserDataAndCustomFoods()

        val viewModel = SettingsViewModel(
            persistence = settingsStore,
            driverFactory = testDriverFactory
        )

        // Calling resetDatabaseAndSettings() without parameters defaults to resetFoodDefinitions = false
        val result = viewModel.resetDatabaseAndSettings()
        assertTrue(result.isSuccess)

        val driver = testDriverFactory.createDriver()
        val db = createDatabaseForDriver(driver)

        assertNotNull(db.foodItemQueries.selectById("food-custom-1").executeAsOneOrNull())
        assertNotNull(db.foodUnitQueries.selectById("unit-custom-1").executeAsOneOrNull())

        driver.close()
    }
}
