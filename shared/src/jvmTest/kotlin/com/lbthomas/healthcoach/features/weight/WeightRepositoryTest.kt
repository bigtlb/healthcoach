package com.lbthomas.healthcoach.features.weight

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.features.weight.data.WeightRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeightRepositoryTest {

    private lateinit var database: Database
    private lateinit var repository: WeightRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        database = Database(driver)
        repository = WeightRepository(database)
    }

    @Test
    fun testAddAndObserveWeightEntries() = runBlocking {
        val id1 = repository.addEntry(
            date = LocalDate(2026, 9, 23),
            weight = 75.5
        )

        val id2 = repository.addEntry(
            date = LocalDate(2026, 9, 24),
            weight = 75.0
        )

        assertTrue(id1.isNotEmpty())
        assertTrue(id2.isNotEmpty())

        val entries = repository.observeAllEntries().first()
        assertEquals(2, entries.size)

        val entry1 = entries.first { it.id == id1 }
        assertEquals(LocalDate(2026, 9, 23), entry1.date)
        assertEquals(75.5, entry1.weight)
        assertTrue(entry1.updatedAt > 0L)

        val entry2 = entries.first { it.id == id2 }
        assertEquals(LocalDate(2026, 9, 24), entry2.date)
        assertEquals(75.0, entry2.weight)
        assertTrue(entry2.updatedAt > 0L)
    }

    @Test
    fun testUpdateWeightEntry() = runBlocking {
        val id = repository.addEntry(
            date = LocalDate(2026, 9, 23),
            weight = 75.5
        )

        val initialEntries = repository.observeAllEntries().first()
        val entryToUpdate = initialEntries.first { it.id == id }

        val newUpdatedAt = entryToUpdate.updatedAt + 1000L
        repository.updateEntry(
            entryToUpdate.copy(weight = 76.0),
            updatedAt = newUpdatedAt
        )

        val updatedEntries = repository.observeAllEntries().first()
        val updated = updatedEntries.first { it.id == id }

        assertEquals(76.0, updated.weight)
        assertEquals(newUpdatedAt, updated.updatedAt)
    }

    @Test
    fun testDeleteWeightEntry() = runBlocking {
        val id = repository.addEntry(
            date = LocalDate(2026, 9, 23),
            weight = 75.5
        )

        val entriesBefore = repository.observeAllEntries().first()
        assertEquals(1, entriesBefore.size)

        repository.deleteEntry(id)

        val entriesAfter = repository.observeAllEntries().first()
        assertEquals(0, entriesAfter.size)
    }
}
