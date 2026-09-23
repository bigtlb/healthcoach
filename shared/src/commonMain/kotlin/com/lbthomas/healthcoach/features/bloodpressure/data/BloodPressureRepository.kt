package com.lbthomas.healthcoach.features.bloodpressure.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.QueryResult
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.bloodpressure.data.BloodPressureEntry
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

    fun addEntry(dateTime: String, systolic: Int, diastolic: Int, pulse: Int?): Long {
        return database.bloodPressureEntryQueries.insert(
            dateTime = dateTime,
            systolic = systolic.toLong(),
            diastolic = diastolic.toLong(),
            pulse = pulse?.toLong()
        ).executeAsOne()
    }

    fun updateEntry(entry: BloodPressureEntryData): QueryResult<Long> {
        return database.bloodPressureEntryQueries.update(
            dateTime = entry.dateTime,
            systolic = entry.systolic.toLong(),
            diastolic = entry.diastolic.toLong(),
            pulse = entry.pulse?.toLong(),
            id = entry.id
        )
    }

    fun deleteEntry(id: Long): QueryResult<Long> {
        return database.bloodPressureEntryQueries.delete(id)
    }
}

private fun BloodPressureEntry.toData(): BloodPressureEntryData =
    BloodPressureEntryData(
        id = id,
        dateTime = dateTime,
        systolic = systolic.toInt(),
        diastolic = diastolic.toInt(),
        pulse = pulse?.toInt()
    )
