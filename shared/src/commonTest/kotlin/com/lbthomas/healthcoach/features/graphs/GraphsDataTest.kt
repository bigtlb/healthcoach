package com.lbthomas.healthcoach.features.graphs

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphsDataTest {

    @Test
    fun testChronologicalSorting() {
        val entries = listOf(
            WeightEntryData(id = 1, date = LocalDate(2026, 3, 15), weight = 75.0),
            WeightEntryData(id = 2, date = LocalDate(2026, 1, 10), weight = 77.5),
            WeightEntryData(id = 3, date = LocalDate(2026, 2, 20), weight = 76.0),
        )

        val sorted = entries.sortedBy { it.date }

        assertEquals(LocalDate(2026, 1, 10), sorted[0].date)
        assertEquals(LocalDate(2026, 2, 20), sorted[1].date)
        assertEquals(LocalDate(2026, 3, 15), sorted[2].date)
    }

    @Test
    fun testUnitConversion() {
        val entry = WeightEntryData(id = 1, date = LocalDate(2026, 1, 10), weight = 80.0)

        val metricWeight = entry.getWeightInCurrentUnits(WeightUnit.METRIC)
        assertEquals(80.0, metricWeight, 0.001)

        val usWeight = entry.getWeightInCurrentUnits(WeightUnit.US)
        assertEquals(80.0 * 2.20462, usWeight, 0.001)
    }

    @Test
    fun testBuildGraphEntriesYearToDateWithInterpolation() {
        val entries = listOf(
            WeightEntryData(id = 1, date = LocalDate(2025, 12, 1), weight = 80.0),
            WeightEntryData(id = 2, date = LocalDate(2026, 1, 31), weight = 70.0),
            WeightEntryData(id = 3, date = LocalDate(2026, 3, 1), weight = 68.0)
        )

        val result = buildGraphEntries(entries, GraphTimeFrame.YEAR_TO_DATE)

        val expectedMinEpochDay = LocalDate(2026, 1, 1).toEpochDays().toDouble()
        val expectedMaxEpochDay = LocalDate(2026, 3, 1).toEpochDays().toDouble()

        assertEquals(expectedMinEpochDay, result.minEpochDay)
        assertEquals(expectedMaxEpochDay, result.maxEpochDay)
        assertEquals(3, result.entries.size)
        // First entry should be interpolated at 2026-01-01
        assertEquals(LocalDate(2026, 1, 1), result.entries[0].date)
        assertEquals(LocalDate(2026, 1, 31), result.entries[1].date)
        assertEquals(LocalDate(2026, 3, 1), result.entries[2].date)
    }

    @Test
    fun testBuildGraphEntriesYearToDateStartingOnJanFirst() {
        val entries = listOf(
            WeightEntryData(id = 1, date = LocalDate(2026, 1, 1), weight = 80.0),
            WeightEntryData(id = 2, date = LocalDate(2026, 2, 1), weight = 78.0)
        )

        val result = buildGraphEntries(entries, GraphTimeFrame.YEAR_TO_DATE)

        val expectedMinEpochDay = LocalDate(2026, 1, 1).toEpochDays().toDouble()
        val expectedMaxEpochDay = LocalDate(2026, 2, 1).toEpochDays().toDouble()

        assertEquals(expectedMinEpochDay, result.minEpochDay)
        assertEquals(expectedMaxEpochDay, result.maxEpochDay)
        assertEquals(2, result.entries.size)
        assertEquals(LocalDate(2026, 1, 1), result.entries[0].date)
        assertEquals(LocalDate(2026, 2, 1), result.entries[1].date)
    }

    @Test
    fun testBuildGraphEntriesEmpty() {
        val result = buildGraphEntries(emptyList(), GraphTimeFrame.YEAR_TO_DATE)
        assertTrue(result.entries.isEmpty())
        assertNull(result.minEpochDay)
        assertNull(result.maxEpochDay)
    }

    @Test
    fun testBuildGraphEntriesAll() {
        val entries = listOf(
            WeightEntryData(id = 1, date = LocalDate(2024, 5, 1), weight = 85.0),
            WeightEntryData(id = 2, date = LocalDate(2026, 2, 1), weight = 75.0)
        )
        val result = buildGraphEntries(entries, GraphTimeFrame.ALL)
        assertEquals(2, result.entries.size)
        assertNull(result.minEpochDay)
        assertNull(result.maxEpochDay)
    }

    @Test
    fun testRangeProviderPadding() {
        // Test single value / zero diff
        val singleMin = 80.0
        val singleMax = 80.0
        val singleDiff = singleMax - singleMin
        val singlePadding = if (singleDiff <= 0.0) 5.0 else max(1.0, singleDiff * 0.15)
        val calculatedMin = (singleMin - singlePadding).coerceAtLeast(0.0)
        val calculatedMax = singleMax + singlePadding

        assertEquals(75.0, calculatedMin)
        assertEquals(85.0, calculatedMax)

        // Test normal range
        val rangeMin = 70.0
        val rangeMax = 80.0
        val rangeDiff = rangeMax - rangeMin
        val rangePadding = if (rangeDiff <= 0.0) 5.0 else max(1.0, rangeDiff * 0.15)
        val calculatedRangeMin = (rangeMin - rangePadding).coerceAtLeast(0.0)
        val calculatedRangeMax = rangeMax + rangePadding

        assertEquals(68.5, calculatedRangeMin)
        assertEquals(81.5, calculatedRangeMax)
        assertTrue(calculatedRangeMin < rangeMin)
        assertTrue(calculatedRangeMax > rangeMax)
    }
}
