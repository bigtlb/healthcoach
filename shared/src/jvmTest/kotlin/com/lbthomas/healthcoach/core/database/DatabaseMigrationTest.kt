package com.lbthomas.healthcoach.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureRepository
import com.lbthomas.healthcoach.features.weight.data.WeightRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseMigrationTest {

    @Test
    fun testMigrationFromV3ToV4PreservesDataAndGeneratesUuids() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Initialize schema at version 3 (weightEntry + legacy bloodPressureEntry from 2.sqm)
        driver.execute(
            null,
            """
            CREATE TABLE weightEntry (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                weight REAL NOT NULL
            );
            """.trimIndent(),
            0
        )

        driver.execute(
            null,
            """
            CREATE TABLE bloodPressureEntry (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dateTime TEXT NOT NULL,
                systolic INTEGER NOT NULL,
                diastolic INTEGER NOT NULL,
                pulse INTEGER
            );
            """.trimIndent(),
            0
        )

        setDbVersion(driver, 3L)

        // Insert legacy data with auto-increment IDs
        driver.execute(
            null,
            "INSERT INTO weightEntry (date, weight) VALUES ('2026-01-10', 78.5);",
            0
        )
        driver.execute(
            null,
            "INSERT INTO weightEntry (date, weight) VALUES ('2026-01-11', 78.2);",
            0
        )
        driver.execute(
            null,
            "INSERT INTO bloodPressureEntry (dateTime, systolic, diastolic, pulse) VALUES ('2026-01-10T08:00:00Z', 120, 80, 72);",
            0
        )

        // Execute migration from 3 to 4 (runs 3.sqm)
        Database.Schema.migrate(driver, 3L, 4L)
        setDbVersion(driver, 4L)

        assertEquals(4L, getDbVersion(driver))

        val database = Database(driver)
        val weightRepo = WeightRepository(database)
        val bpRepo = BloodPressureRepository(database)

        val weightEntries = weightRepo.observeAllEntries().first()
        assertEquals(2, weightEntries.size)
        assertTrue(weightEntries.all { it.id.isNotEmpty() && it.id.length == 36 })
        assertTrue(weightEntries.all { it.updatedAt > 0L })

        val bpEntries = bpRepo.observeAllEntries().first()
        assertEquals(1, bpEntries.size)
        assertTrue(bpEntries.all { it.id.isNotEmpty() && it.id.length == 36 })
        assertEquals(120, bpEntries[0].systolic)
        assertEquals(80, bpEntries[0].diastolic)
        assertEquals(72, bpEntries[0].pulse)
        assertTrue(bpEntries[0].updatedAt > 0L)
    }

    @Test
    fun testMigrationFromV2ToV4PreservesData() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Initialize schema at version 2 (only weightEntry)
        driver.execute(
            null,
            """
            CREATE TABLE weightEntry (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                weight REAL NOT NULL
            );
            """.trimIndent(),
            0
        )
        setDbVersion(driver, 2L)

        driver.execute(
            null,
            "INSERT INTO weightEntry (date, weight) VALUES ('2026-01-10', 75.0);",
            0
        )

        // Execute migration from 2 to 4 (runs 2.sqm then 3.sqm)
        Database.Schema.migrate(driver, 2L, 4L)
        setDbVersion(driver, 4L)

        val database = Database(driver)
        val weightRepo = WeightRepository(database)
        val bpRepo = BloodPressureRepository(database)

        val weightEntries = weightRepo.observeAllEntries().first()
        assertEquals(1, weightEntries.size)
        assertEquals("2026-01-10", weightEntries[0].date.toString())
        assertEquals(75.0, weightEntries[0].weight)
        assertTrue(weightEntries[0].id.isNotEmpty() && weightEntries[0].id.length == 36)

        // Verify bloodPressureEntry table was created by 2.sqm and migrated by 3.sqm
        val bpEntries = bpRepo.observeAllEntries().first()
        assertEquals(0, bpEntries.size)
    }

    @Test
    fun testSchemaVersionIsSixOnCreation() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        assertEquals(6L, Database.Schema.version)

        Database.Schema.create(driver)
        setDbVersion(driver, Database.Schema.version)
        assertEquals(6L, getDbVersion(driver))
    }

    @Test
    fun testMigrationFromV4ToV5CreatesFoodJournalTablesAndSeedsUnits() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Initialize schema at version 4 (UUID weightEntry and bloodPressureEntry)
        driver.execute(
            null,
            """
            CREATE TABLE weightEntry (
                id TEXT PRIMARY KEY NOT NULL,
                date TEXT NOT NULL,
                weight REAL NOT NULL,
                updated_at INTEGER NOT NULL
            );
            """.trimIndent(),
            0
        )

        driver.execute(
            null,
            """
            CREATE TABLE bloodPressureEntry (
                id TEXT PRIMARY KEY NOT NULL,
                dateTime TEXT NOT NULL,
                systolic INTEGER NOT NULL,
                diastolic INTEGER NOT NULL,
                pulse INTEGER,
                updated_at INTEGER NOT NULL
            );
            """.trimIndent(),
            0
        )

        setDbVersion(driver, 4L)

        // Migrate 4 -> 5 (runs 4.sqm)
        Database.Schema.migrate(driver, 4L, 5L)
        setDbVersion(driver, 5L)

        assertEquals(5L, getDbVersion(driver))

        val database = Database(driver)
        val units = database.foodUnitQueries.selectAll().executeAsList()
        assertEquals(15, units.size)
        assertTrue(units.any { it.name == "Each" })
        assertTrue(units.any { it.name == "Piece" })
        assertTrue(units.any { it.name == "Slice" })
        assertTrue(units.any { it.name == "Cup" })
        assertTrue(units.any { it.name == "oz" })
        assertTrue(units.any { it.name == "grams" })
        assertTrue(units.any { it.name == "Tablespoon" })
        assertTrue(units.any { it.name == "Teaspoon" })
        assertTrue(units.any { it.name == "Lbs" })
        assertTrue(units.any { it.name == "Package" })

        // Check foodItem and mealEntry can be queried
        val foodItems = database.foodItemQueries.selectAll().executeAsList()
        assertEquals(0, foodItems.size)

        val mealEntries = database.mealEntryQueries.selectAll().executeAsList()
        assertEquals(0, mealEntries.size)
    }

    @Test
    fun testMigrationFromV5ToV6CreatesProfileSettingTable() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Initialize schema at version 4 (UUID weightEntry and bloodPressureEntry)
        driver.execute(
            null,
            """
            CREATE TABLE weightEntry (
                id TEXT PRIMARY KEY NOT NULL,
                date TEXT NOT NULL,
                weight REAL NOT NULL,
                updated_at INTEGER NOT NULL
            );
            """.trimIndent(),
            0
        )

        driver.execute(
            null,
            """
            CREATE TABLE bloodPressureEntry (
                id TEXT PRIMARY KEY NOT NULL,
                dateTime TEXT NOT NULL,
                systolic INTEGER NOT NULL,
                diastolic INTEGER NOT NULL,
                pulse INTEGER,
                updated_at INTEGER NOT NULL
            );
            """.trimIndent(),
            0
        )

        setDbVersion(driver, 4L)

        // Migrate 4 -> 5 (runs 4.sqm)
        Database.Schema.migrate(driver, 4L, 5L)
        setDbVersion(driver, 5L)

        // Migrate 5 -> 6 (runs 5.sqm)
        Database.Schema.migrate(driver, 5L, 6L)
        setDbVersion(driver, 6L)

        assertEquals(6L, getDbVersion(driver))

        val database = Database(driver)
        val initialSettings = database.profileSettingQueries.selectAll().executeAsList()
        assertEquals(0, initialSettings.size)

        database.profileSettingQueries.insertOrUpdate("user_name", "Alice", 1000L)
        val alice = database.profileSettingQueries.selectByKey("user_name").executeAsOneOrNull()
        assertEquals("user_name", alice?.key)
        assertEquals("Alice", alice?.value_)
        assertEquals(1000L, alice?.updated_at)
    }
}
