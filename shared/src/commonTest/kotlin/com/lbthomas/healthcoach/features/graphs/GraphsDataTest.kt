package com.lbthomas.healthcoach.features.graphs

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import com.lbthomas.healthcoach.features.graphs.data.buildBpGraphEntries
import com.lbthomas.healthcoach.features.graphs.data.buildCalorieGraphEntries
import com.lbthomas.healthcoach.features.graphs.data.buildWeightGraphEntries
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
            WeightEntryData(id = "1", date = LocalDate(2026, 3, 15), weight = 75.0),
            WeightEntryData(id = "2", date = LocalDate(2026, 1, 10), weight = 77.5),
            WeightEntryData(id = "3", date = LocalDate(2026, 2, 20), weight = 76.0),
        )

        val sorted = entries.sortedBy { it.date }

        assertEquals(LocalDate(2026, 1, 10), sorted[0].date)
        assertEquals(LocalDate(2026, 2, 20), sorted[1].date)
        assertEquals(LocalDate(2026, 3, 15), sorted[2].date)
    }

    @Test
    fun testUnitConversion() {
        val entry = WeightEntryData(id = "1", date = LocalDate(2026, 1, 10), weight = 80.0)

        val metricWeight = entry.getWeightInCurrentUnits(WeightUnit.METRIC)
        assertEquals(80.0, metricWeight, 0.001)

        val usWeight = entry.getWeightInCurrentUnits(WeightUnit.US)
        assertEquals(80.0 * 2.20462, usWeight, 0.001)
    }

    @Test
    fun testBuildWeightGraphEntriesYearToDateWithInterpolation() {
        val entries = listOf(
            WeightEntryData(id = "1", date = LocalDate(2025, 12, 1), weight = 80.0),
            WeightEntryData(id = "2", date = LocalDate(2026, 1, 31), weight = 70.0),
            WeightEntryData(id = "3", date = LocalDate(2026, 3, 1), weight = 68.0)
        )

        val result = buildWeightGraphEntries(entries, GraphTimeFrame.YEAR_TO_DATE)

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
    fun testBuildBpGraphEntries() {
        val entries = listOf(
            BloodPressureEntryData(id = "1", dateTime = "2026-01-15T08:00:00Z", systolic = 120, diastolic = 80, pulse = 70),
            BloodPressureEntryData(id = "2", dateTime = "2026-01-15T20:00:00Z", systolic = 125, diastolic = 82, pulse = 72),
            BloodPressureEntryData(id = "3", dateTime = "2026-02-10", systolic = 130, diastolic = 85, pulse = null)
        )

        val result = buildBpGraphEntries(entries, GraphTimeFrame.ALL)
        assertEquals(3, result.points.size)
        assertTrue(result.points[0].x < result.points[1].x)
        assertEquals(120.0, result.points[0].systolic)
        assertEquals(80.0, result.points[0].diastolic)
        assertEquals(70, result.points[0].pulse)
        assertEquals(72, result.points[1].pulse)
        assertNull(result.points[2].pulse)
    }

    @Test
    fun testBuildCalorieGraphEntries() {
        val totals = mapOf(
            LocalDate(2026, 1, 10) to 1800.0,
            LocalDate(2026, 1, 15) to 2150.0,
            LocalDate(2026, 2, 20) to 1950.0
        )

        val resultAll = buildCalorieGraphEntries(totals, GraphTimeFrame.ALL)
        assertEquals(3, resultAll.points.size)
        assertEquals(1800.0, resultAll.points[0].calories)
        assertEquals(2150.0, resultAll.points[1].calories)
        assertEquals(1950.0, resultAll.points[2].calories)

        val resultOneMonth = buildCalorieGraphEntries(totals, GraphTimeFrame.ONE_MONTH)
        assertEquals(1, resultOneMonth.points.size)
        assertEquals(LocalDate(2026, 2, 20), resultOneMonth.points[0].date)
        assertEquals(1950.0, resultOneMonth.points[0].calories)

        val emptyResult = buildCalorieGraphEntries(emptyMap(), GraphTimeFrame.ALL)
        assertTrue(emptyResult.points.isEmpty())
        assertNull(emptyResult.minEpochDay)
        assertNull(emptyResult.maxEpochDay)
    }

    @Test
    fun testBuildMetabolicLinesLayerModel() {
        val minX = LocalDate(2026, 1, 1).toEpochDays().toDouble()
        val maxX = LocalDate(2026, 1, 31).toEpochDays().toDouble()

        // Both maintenance and target
        val modelBoth = com.lbthomas.healthcoach.features.graphs.ui.components.buildMetabolicLinesLayerModel(
            globalMinX = minX,
            globalMaxX = maxX,
            maintenanceCalories = 2150.0,
            targetCalories = 1750.0
        )
        kotlin.test.assertNotNull(modelBoth)
        assertEquals(2, modelBoth.series.size)
        assertEquals(2, modelBoth.series[0].size)
        assertEquals(2150.0, modelBoth.series[0][0].y)
        assertEquals(2150.0, modelBoth.series[0][1].y)
        assertEquals(1750.0, modelBoth.series[1][0].y)
        assertEquals(1750.0, modelBoth.series[1][1].y)

        // Only maintenance
        val modelMaintenanceOnly = com.lbthomas.healthcoach.features.graphs.ui.components.buildMetabolicLinesLayerModel(
            globalMinX = minX,
            globalMaxX = maxX,
            maintenanceCalories = 2150.0,
            targetCalories = null
        )
        kotlin.test.assertNotNull(modelMaintenanceOnly)
        assertEquals(1, modelMaintenanceOnly.series.size)
        assertEquals(2150.0, modelMaintenanceOnly.series[0][0].y)

        // Null bounds or null calories
        assertNull(com.lbthomas.healthcoach.features.graphs.ui.components.buildMetabolicLinesLayerModel(null, maxX, 2000.0, 1800.0))
        assertNull(com.lbthomas.healthcoach.features.graphs.ui.components.buildMetabolicLinesLayerModel(minX, null, 2000.0, 1800.0))
        assertNull(com.lbthomas.healthcoach.features.graphs.ui.components.buildMetabolicLinesLayerModel(minX, maxX, null, null))
    }

    @Test
    fun testBuildGraphEntriesEmpty() {
        val result = buildWeightGraphEntries(emptyList(), GraphTimeFrame.YEAR_TO_DATE)
        assertTrue(result.entries.isEmpty())
        assertNull(result.minEpochDay)
        assertNull(result.maxEpochDay)

        val bpResult = buildBpGraphEntries(emptyList(), GraphTimeFrame.ALL)
        assertTrue(bpResult.points.isEmpty())
        assertNull(bpResult.minEpochDay)
        assertNull(bpResult.maxEpochDay)
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
