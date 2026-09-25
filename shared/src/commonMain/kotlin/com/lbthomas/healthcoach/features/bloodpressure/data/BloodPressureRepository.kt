package com.lbthomas.healthcoach.features.bloodpressure.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.QueryResult
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.bloodpressure.data.BloodPressureEntry
import com.lbthomas.healthcoach.core.utils.currentEpochMillis
import com.lbthomas.healthcoach.core.utils.generateUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BloodPressureRepository(private val database: Database) {
    fun observeAllEntries(): Flow<List<BloodPressureEntryData>> = database
        .bloodPressureEntryQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entries -> entries.map(BloodPressureEntry::toData) }

    fun addEntry(
        dateTime: String,
        systolic: Int,
        diastolic: Int,
        pulse: Int?,
        id: String = generateUuid(),
        updatedAt: Long = currentEpochMillis()
    ): String {
        database.bloodPressureEntryQueries.insert(
            id = id,
            dateTime = dateTime,
            systolic = systolic.toLong(),
            diastolic = diastolic.toLong(),
            pulse = pulse?.toLong(),
            updated_at = updatedAt
        )
        return id
    }

    fun updateEntry(
        entry: BloodPressureEntryData,
        updatedAt: Long = currentEpochMillis()
    ): QueryResult<Long> {
        return database.bloodPressureEntryQueries.update(
            dateTime = entry.dateTime,
            systolic = entry.systolic.toLong(),
            diastolic = entry.diastolic.toLong(),
            pulse = entry.pulse?.toLong(),
            updated_at = updatedAt,
            id = entry.id
        )
    }

    fun deleteEntry(id: String): QueryResult<Long> {
        return database.bloodPressureEntryQueries.delete(id)
    }
}

private fun BloodPressureEntry.toData(): BloodPressureEntryData =
    BloodPressureEntryData(
        id = id,
        dateTime = dateTime,
        systolic = systolic.toInt(),
        diastolic = diastolic.toInt(),
        pulse = pulse?.toInt(),
        updatedAt = updated_at
    )
