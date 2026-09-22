package com.lbthomas.healthcoach.features.weight.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.QueryResult
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.weight.data.WeightEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class WeightRepository(private val database: Database) {
    fun observeAllEntries() = database
        .weightEntryQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entries -> entries.map(WeightEntry::toData) }

    fun observeEntriesByDateRange(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<WeightEntryData>> {
        TODO("Not yet implemented")
    }

    fun observeEntry(id: Long): Flow<WeightEntryData?> {
        TODO("Not yet implemented")
    }

    fun addEntry(date: LocalDate, weight: Double): Long {
        return database.weightEntryQueries.insert(date.toString(), weight).executeAsOne()
    }

    fun updateEntry(entry: WeightEntryData): QueryResult<Long> {
        return database.weightEntryQueries.update(entry.date.toString(), entry.weight, entry.id)
    }

    fun deleteEntry(id: Long): QueryResult<Long> {
        return database.weightEntryQueries.delete(id)
    }
}

private fun WeightEntry.toData(): WeightEntryData =
    WeightEntryData(
        id = id,
        date = LocalDate.parse(date),
        weight = weight
    )