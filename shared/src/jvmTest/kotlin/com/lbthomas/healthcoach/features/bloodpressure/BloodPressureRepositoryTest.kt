package com.lbthomas.healthcoach.features.bloodpressure

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BloodPressureRepositoryTest {

    private lateinit var database: Database
    private lateinit var repository: BloodPressureRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        database = Database(driver)
        repository = BloodPressureRepository(database)
    }

    @Test
    fun testAddAndObserveBloodPressureEntries() = runBlocking {
        repository.addEntry(
            dateTime = "2026-09-23T15:00:00Z",
            systolic = 120,
            diastolic = 80,
            pulse = 72
        )

        repository.addEntry(
            dateTime = "2026-09-24",
            systolic = 118,
            diastolic = 78,
            pulse = null
        )

        val entries = repository.observeAllEntries().first()
        assertEquals(2, entries.size)

        // SelectAll orders by dateTime DESC
        val entry1 = entries[0]
        assertEquals("2026-09-24", entry1.dateTime)
        assertEquals(118, entry1.systolic)
        assertEquals(78, entry1.diastolic)
        assertNull(entry1.pulse)

        val entry2 = entries[1]
        assertEquals("2026-09-23T15:00:00Z", entry2.dateTime)
        assertEquals(120, entry2.systolic)
        assertEquals(80, entry2.diastolic)
        assertEquals(72, entry2.pulse)
    }

    @Test
    fun testUpdateBloodPressureEntry() = runBlocking {
        val id = repository.addEntry(
            dateTime = "2026-09-23T15:00:00Z",
            systolic = 120,
            diastolic = 80,
            pulse = 72
        )

        val initialEntries = repository.observeAllEntries().first()
        val entryToUpdate = initialEntries.first { it.id == id }

        repository.updateEntry(
            entryToUpdate.copy(
                systolic = 125,
                diastolic = 82,
                pulse = 75
            )
        )

        val updatedEntries = repository.observeAllEntries().first()
        val updated = updatedEntries.first { it.id == id }

        assertEquals(125, updated.systolic)
        assertEquals(82, updated.diastolic)
        assertEquals(75, updated.pulse)
    }

    @Test
    fun testDeleteBloodPressureEntry() = runBlocking {
        val id = repository.addEntry(
            dateTime = "2026-09-23",
            systolic = 120,
            diastolic = 80,
            pulse = null
        )

        val entriesBefore = repository.observeAllEntries().first()
        assertEquals(1, entriesBefore.size)

        repository.deleteEntry(id)

        val entriesAfter = repository.observeAllEntries().first()
        assertEquals(0, entriesAfter.size)
    }
}
