package com.lbthomas.healthcoach.features.bloodpressure.data

import com.lbthomas.healthcoach.core.enums.BloodPressureCategory
import com.lbthomas.healthcoach.core.utils.parseBpDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class BloodPressureEntryData(
    val id: Long,
    val dateTime: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int? = null
) {
    val date: LocalDate
        get() = parsedDateTime.first

    val time: LocalTime?
        get() = parsedDateTime.second

    val hasTime: Boolean
        get() = time != null

    val category: BloodPressureCategory
        get() = BloodPressureCategory.fromReadings(systolic, diastolic)

    private val parsedDateTime: Pair<LocalDate, LocalTime?>
        get() = parseBpDateTime(dateTime)
}
