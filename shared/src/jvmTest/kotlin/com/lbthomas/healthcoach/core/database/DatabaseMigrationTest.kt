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
    fun testSchemaVersionIsFourOnCreation() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        assertEquals(4L, Database.Schema.version)

        Database.Schema.create(driver)
        setDbVersion(driver, Database.Schema.version)
        assertEquals(4L, getDbVersion(driver))
    }
}
