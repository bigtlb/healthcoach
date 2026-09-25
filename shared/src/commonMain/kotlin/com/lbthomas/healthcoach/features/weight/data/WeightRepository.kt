package com.lbthomas.healthcoach.features.weight.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.QueryResult
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.utils.currentEpochMillis
import com.lbthomas.healthcoach.core.utils.generateUuid
import com.lbthomas.healthcoach.weight.data.WeightEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class WeightRepository(private val database: Database) {
    fun observeAllEntries(): Flow<List<WeightEntryData>> = database
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

    fun observeEntry(id: String): Flow<WeightEntryData?> {
        TODO("Not yet implemented")
    }

    fun addEntry(
        date: LocalDate,
        weight: Double,
        id: String = generateUuid(),
        updatedAt: Long = currentEpochMillis()
    ): String {
        database.weightEntryQueries.insert(
            id = id,
            date = date.toString(),
            weight = weight,
            updated_at = updatedAt
        )
        return id
    }

    fun updateEntry(
        entry: WeightEntryData,
        updatedAt: Long = currentEpochMillis()
    ): QueryResult<Long> {
        return database.weightEntryQueries.update(
            date = entry.date.toString(),
            weight = entry.weight,
            updated_at = updatedAt,
            id = entry.id
        )
    }

    fun deleteEntry(id: String): QueryResult<Long> {
        return database.weightEntryQueries.delete(id)
    }
}

private fun WeightEntry.toData(): WeightEntryData =
    WeightEntryData(
        id = id,
        date = LocalDate.parse(date),
        weight = weight,
        updatedAt = updated_at
    )
