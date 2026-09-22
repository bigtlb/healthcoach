package com.lbthomas.healthcoach.features.graphs

import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
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
