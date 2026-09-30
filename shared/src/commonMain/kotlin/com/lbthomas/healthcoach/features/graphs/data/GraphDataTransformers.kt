package com.lbthomas.healthcoach.features.graphs.data

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.utils.formatTime
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate
import kotlin.math.round

internal data class WeightGraphEntries(
    val entries: List<WeightEntryData>,
    val minEpochDay: Double?,
    val maxEpochDay: Double?
)

internal data class BpGraphPoint(
    val id: String,
    val x: Double,
    val date: LocalDate,
    val systolic: Double,
    val diastolic: Double,
    val pulse: Int? = null,
    val timeFormatted: String? = null
)

internal data class BpGraphEntries(
    val points: List<BpGraphPoint>,
    val minEpochDay: Double?,
    val maxEpochDay: Double?
)

internal data class CalorieGraphPoint(
    val date: LocalDate,
    val x: Double,
    val calories: Double
)

internal data class CalorieGraphEntries(
    val points: List<CalorieGraphPoint>,
    val minEpochDay: Double?,
    val maxEpochDay: Double?
)

internal fun buildWeightGraphEntries(
    rawEntries: List<WeightEntryData>,
    timeFrame: GraphTimeFrame
): WeightGraphEntries {
    val sorted = rawEntries.sortedBy { it.date }

    if (sorted.isEmpty()) {
        return WeightGraphEntries(
            entries = emptyList(),
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    if (timeFrame == GraphTimeFrame.ALL) {
        return WeightGraphEntries(
            entries = sorted,
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    val latestDate = sorted.last().date
    val latestEpochDay = latestDate.toEpochDays()
    val minEpochDay = if (timeFrame == GraphTimeFrame.YEAR_TO_DATE) {
        LocalDate(latestDate.year, 1, 1).toEpochDays()
    } else {
        val days = timeFrame.days ?: return WeightGraphEntries(
            entries = sorted,
            minEpochDay = null,
            maxEpochDay = null
        )
        latestEpochDay - days
    }
    val maxEpochDay = latestEpochDay.toDouble()

    val entriesInRange = sorted.filter { it.date.toEpochDays() >= minEpochDay }

    val previousEntry = sorted.lastOrNull { it.date.toEpochDays() < minEpochDay }
    val firstEntryInRange = entriesInRange.firstOrNull()

    fun interpolateEdgeEntry(): WeightEntryData? = if (previousEntry != null && firstEntryInRange != null && firstEntryInRange.date.toEpochDays() > minEpochDay) {
        val previousX = previousEntry.date.toEpochDays()
        val firstX = firstEntryInRange.date.toEpochDays()
        val rangeWidth = firstX - previousX

        if (rangeWidth > 0) {
            val progress = (minEpochDay - previousX).toDouble() / rangeWidth.toDouble()
            val interpolatedWeight =
                previousEntry.weight + ((firstEntryInRange.weight - previousEntry.weight) * progress)

            WeightEntryData(
                id = "",
                date = LocalDate.fromEpochDays(minEpochDay),
                weight = interpolatedWeight
            )
        } else {
            null
        }
    } else {
        null
    }

    return WeightGraphEntries(
        entries = listOfNotNull(interpolateEdgeEntry()) + entriesInRange,
        minEpochDay = minEpochDay.toDouble(),
        maxEpochDay = maxEpochDay
    )
}

internal fun buildBpGraphEntries(
    rawEntries: List<BloodPressureEntryData>,
    timeFrame: GraphTimeFrame
): BpGraphEntries {
    val sorted = rawEntries.sortedWith(
        compareBy<BloodPressureEntryData> { it.date }
            .thenBy { it.time?.toString() ?: "" }
    )

    if (sorted.isEmpty()) {
        return BpGraphEntries(
            points = emptyList(),
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    val points = sorted.map { entry ->
        val fractionOfDay = if (entry.hasTime && entry.time != null) {
            round(((entry.time!!.hour * 3600 + entry.time!!.minute * 60 + entry.time!!.second) / 86400.0) * 10000.0) / 10000.0
        } else {
            0.0
        }
        val rawX = entry.date.toEpochDays().toDouble() + fractionOfDay
        BpGraphPoint(
            id = entry.id,
            x = round(rawX * 10000.0) / 10000.0,
            date = entry.date,
            systolic = entry.systolic.toDouble(),
            diastolic = entry.diastolic.toDouble(),
            pulse = entry.pulse,
            timeFormatted = entry.time?.formatTime()
        )
    }

    if (timeFrame == GraphTimeFrame.ALL) {
        return BpGraphEntries(
            points = points,
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    val latestDate = sorted.last().date
    val latestEpochDay = latestDate.toEpochDays()
    val minEpochDay = if (timeFrame == GraphTimeFrame.YEAR_TO_DATE) {
        LocalDate(latestDate.year, 1, 1).toEpochDays()
    } else {
        val days = timeFrame.days ?: return BpGraphEntries(
            points = points,
            minEpochDay = null,
            maxEpochDay = null
        )
        latestEpochDay - days
    }
    val maxEpochDay = latestEpochDay.toDouble()

    val filteredPoints = points.filter { it.x >= minEpochDay.toDouble() }

    val previousPoint = points.lastOrNull { it.x < minEpochDay.toDouble() }
    val firstPointInRange = filteredPoints.firstOrNull()

    fun interpolateEdgePoint(): BpGraphPoint? = if (previousPoint != null && firstPointInRange != null && firstPointInRange.x > minEpochDay.toDouble()) {
        val previousX = previousPoint.x
        val firstX = firstPointInRange.x
        val rangeWidth = firstX - previousX

        if (rangeWidth > 0.0) {
            val progress = (minEpochDay.toDouble() - previousX) / rangeWidth
            val interpolatedSystolic =
                previousPoint.systolic + ((firstPointInRange.systolic - previousPoint.systolic) * progress)
            val interpolatedDiastolic =
                previousPoint.diastolic + ((firstPointInRange.diastolic - previousPoint.diastolic) * progress)
            val interpolatedPulse = if (previousPoint.pulse != null && firstPointInRange.pulse != null) {
                (previousPoint.pulse + ((firstPointInRange.pulse - previousPoint.pulse) * progress)).toInt()
            } else {
                null
            }

            BpGraphPoint(
                id = "",
                x = minEpochDay.toDouble(),
                date = LocalDate.fromEpochDays(minEpochDay),
                systolic = interpolatedSystolic,
                diastolic = interpolatedDiastolic,
                pulse = interpolatedPulse,
                timeFormatted = null
            )
        } else {
            null
        }
    } else {
        null
    }

    return BpGraphEntries(
        points = listOfNotNull(interpolateEdgePoint()) + filteredPoints,
        minEpochDay = minEpochDay.toDouble(),
        maxEpochDay = maxEpochDay
    )
}

internal fun buildCalorieGraphEntries(
    rawTotals: Map<LocalDate, Double>,
    timeFrame: GraphTimeFrame
): CalorieGraphEntries {
    val sorted = rawTotals.entries.sortedBy { it.key }

    if (sorted.isEmpty()) {
        return CalorieGraphEntries(
            points = emptyList(),
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    val points = sorted.map { (date, calories) ->
        CalorieGraphPoint(
            date = date,
            x = date.toEpochDays().toDouble(),
            calories = calories
        )
    }

    if (timeFrame == GraphTimeFrame.ALL) {
        return CalorieGraphEntries(
            points = points,
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    val latestDate = sorted.last().key
    val latestEpochDay = latestDate.toEpochDays()
    val minEpochDay = if (timeFrame == GraphTimeFrame.YEAR_TO_DATE) {
        LocalDate(latestDate.year, 1, 1).toEpochDays()
    } else {
        val days = timeFrame.days ?: return CalorieGraphEntries(
            points = points,
            minEpochDay = null,
            maxEpochDay = null
        )
        latestEpochDay - days
    }
    val maxEpochDay = latestEpochDay.toDouble()

    val filteredPoints = points.filter { it.x >= minEpochDay.toDouble() }

    return CalorieGraphEntries(
        points = filteredPoints,
        minEpochDay = minEpochDay.toDouble(),
        maxEpochDay = maxEpochDay
    )
}
