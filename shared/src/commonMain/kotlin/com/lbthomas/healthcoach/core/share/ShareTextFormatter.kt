package com.lbthomas.healthcoach.core.share

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate

/**
 * Formats weight progress data into a shareable text string with an optional date range line.
 */
object ShareTextFormatter {

    /**
     * Build a formatted share text string from weight entries.
     *
     * With weights: "My 30-day weight progress: 185.2 → 178.6 lbs"
     * Without weights: "My 30-day weight progress: -6.6 lbs"
     *
     * If the start of the selected time frame has no data point, the starting weight is
     * extrapolated from the prior reading using linear interpolation, matching the chart
     * renderer's edge-point behavior. This ensures the delta reflects the change over the
     * actual time frame period, not just the last two readings.
     *
     * Returns a pair: (progress text, date range line)
     */
    fun format(
        entries: List<WeightEntryData>,
        allEntries: List<WeightEntryData>,
        timeFrame: GraphTimeFrame,
        weightUnit: WeightUnit,
        showAbsoluteWeight: Boolean
    ): Pair<String, String> {
        // Filter out interpolated edge entries (id is empty) — those are synthetic chart points.
        val allActualEntries = allEntries.filter { it.id.isNotEmpty() }.sortedBy { it.date }

        if (allActualEntries.size < 2) {
            return Pair("No weight data for this period.", "")
        }

        val timeLabel = timeFrame.label
        val unitLabel = if (weightUnit == WeightUnit.METRIC) "kg" else "lbs"

        // Compute the start date of the selected time frame, matching GraphDataTransformers logic.
        val latestDate = allActualEntries.last().date
        val latestEpochDay = latestDate.toEpochDays()
        val (minEpochDay: Long?, entriesInRange: List<WeightEntryData>) = if (timeFrame == GraphTimeFrame.ALL) {
            null to allActualEntries
        } else if (timeFrame == GraphTimeFrame.YEAR_TO_DATE) {
            val ytdStart = LocalDate(latestDate.year, 1, 1).toEpochDays()
            ytdStart to allActualEntries.filter { it.date.toEpochDays() >= ytdStart }
        } else {
            val days = timeFrame.days ?: return Pair("No weight data for this period.", "")
            val minDay = latestEpochDay - days
            minDay to allActualEntries.filter { it.date.toEpochDays() >= minDay }
        }

        // Determine the starting weight: use actual first entry in range or extrapolate from prior data.
        val firstWeightInRange = entriesInRange.firstOrNull()
        val lastWeightInRange = entriesInRange.lastOrNull() ?: allActualEntries.last()

        val (startWeightKg: Double, endWeightKg: Double) = when {
            // If the range has 2+ entries, use the first and last directly.
            entriesInRange.size >= 2 -> {
                firstWeightInRange!!.weight to lastWeightInRange.weight
            }
            // If the range has exactly 1 entry, extrapolate the start from prior data if possible.
            entriesInRange.size == 1 && minEpochDay != null -> {
                val entryInRange = firstWeightInRange!!
                // Find the entry just before the time frame start.
                val priorEntry = allActualEntries.lastOrNull { it.date.toEpochDays() < minEpochDay }
                if (priorEntry != null && entryInRange.date.toEpochDays() > minEpochDay) {
                    // Linear interpolation: project weight at the time frame boundary.
                    val priorDay = priorEntry.date.toEpochDays()
                    val rangeDay = entryInRange.date.toEpochDays()
                    val daySpan = (rangeDay - priorDay).toDouble()
                    if (daySpan > 0) {
                        val progress = (minEpochDay - priorDay).toDouble() / daySpan
                        val interpolatedWeight = priorEntry.weight + (entryInRange.weight - priorEntry.weight) * progress
                        interpolatedWeight to entryInRange.weight
                    } else {
                        entryInRange.weight to entryInRange.weight
                    }
                } else {
                    // No prior data to extrapolate from; use the last two overall readings.
                    val prev = allActualEntries[allActualEntries.size - 2].weight
                    val last = allActualEntries.last().weight
                    prev to last
                }
            }
            // No entries in range at all — use the last two overall readings.
            else -> {
                val prev = allActualEntries[allActualEntries.size - 2].weight
                val last = allActualEntries.last().weight
                prev to last
            }
        }

        val firstWeight = startWeightKg.getWeightInCurrentUnits(weightUnit)
        val lastWeight = endWeightKg.getWeightInCurrentUnits(weightUnit)
        val change = lastWeight - firstWeight
        val changeFormatted = formatChange(change, unitLabel)

        // Build the date range line
        val dateRangeLine = formatDateRange(entriesInRange, timeFrame, allActualEntries)

        val progressText = if (showAbsoluteWeight) {
            val firstFormatted = formatWeight(firstWeight)
            val lastFormatted = formatWeight(lastWeight)
            "My $timeLabel weight progress: $firstFormatted → $lastFormatted $unitLabel ($changeFormatted)"
        } else {
            "My $timeLabel weight progress: $changeFormatted"
        }

        return Pair(progressText, dateRangeLine)
    }

    /**
     * Format a date range line for the share output.
     * For a single date: "Nov 21, 2026"
     * For a range: "Oct 21, 2026 - Nov 21, 2026"
     */
    private fun formatDateRange(
        entriesInRange: List<WeightEntryData>,
        timeFrame: GraphTimeFrame,
        allActualEntries: List<WeightEntryData>
    ): String {
        if (entriesInRange.isEmpty()) {
            return ""
        }

        val latestDate = allActualEntries.last().date

        // Determine the start date of the range
        val startDate = if (timeFrame == GraphTimeFrame.ALL) {
            entriesInRange.first().date
        } else if (timeFrame == GraphTimeFrame.YEAR_TO_DATE) {
            LocalDate(latestDate.year, 1, 1)
        } else {
            val days = timeFrame.days ?: return ""
            val latestEpochDay = latestDate.toEpochDays()
            val minDay = latestEpochDay - days
            // Convert epoch day back to LocalDate
            val startYear = if (minDay >= 0) {
                // Approximate year from epoch days (epoch is 1970-01-01)
                val approxYear = 1970 + (minDay / 365).toInt()
                // Find the exact year by checking Jan 1 of that year
                var year = approxYear
                while (LocalDate(year, 1, 1).toEpochDays() > minDay) year--
                while (LocalDate(year + 1, 1, 1).toEpochDays() <= minDay) year++
                year
            } else {
                1970
            }
            // Find exact month/day from epoch day
            var y = startYear
            var remaining = (minDay - LocalDate(y, 1, 1).toEpochDays()).toInt()
            var month = 1
            val daysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            // Check for leap year
            if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) {
                daysInMonth[1] = 29
            }
            while (month <= 12 && remaining >= daysInMonth[month - 1]) {
                remaining -= daysInMonth[month - 1]
                month++
            }
            val day = (remaining + 1).coerceIn(1, daysInMonth[month - 1])
            LocalDate(y, month, day)
        }

        val endDate = latestDate

        if (startDate == endDate) {
            return formatDate(startDate)
        } else {
            return "${formatDate(startDate)} - ${formatDate(endDate)}"
        }
    }

    private fun formatDate(date: LocalDate): String {
        val monthNames = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        return "${monthNames[date.month.ordinal]} ${date.day}, ${date.year}"
    }

    /** Helper: convert kg to the target unit. */
    private fun Double.getWeightInCurrentUnits(weightUnit: WeightUnit): Double {
        return if (weightUnit == WeightUnit.METRIC) this else this * 2.20462
    }

    private fun formatWeight(weight: Double): String {
        return "%.1f".format(weight)
    }

    private fun formatChange(change: Double, unitLabel: String): String {
        val sign = if (change >= 0) "+" else ""
        return "$sign%.1f $unitLabel".format(change)
    }
}
