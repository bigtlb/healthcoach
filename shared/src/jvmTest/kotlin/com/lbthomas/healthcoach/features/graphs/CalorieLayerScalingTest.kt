package com.lbthomas.healthcoach.features.graphs

import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.graphs.data.BpGraphPoint
import com.lbthomas.healthcoach.features.graphs.data.CalorieGraphPoint
import com.lbthomas.healthcoach.features.graphs.ui.TimeFrameChartRangeProvider
import com.lbthomas.healthcoach.features.graphs.ui.components.*
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.data.MutableCartesianChartRanges
import com.patrykandpatrick.vico.compose.common.data.MutableExtraStore
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CalorieLayerScalingTest {
    @Test
    fun testCalorieYRangeConsistencyAcrossCombinations() {
        val weightEntries = listOf(
            WeightEntryData("1", LocalDate(2026, 1, 1), 262.0),
            WeightEntryData("2", LocalDate(2026, 3, 1), 253.0)
        )
        val bpPoints = listOf(
            BpGraphPoint("1", LocalDate(2026, 1, 1).toEpochDays().toDouble(), LocalDate(2026, 1, 1), 120.0, 80.0, 70),
            BpGraphPoint("2", LocalDate(2026, 3, 1).toEpochDays().toDouble(), LocalDate(2026, 3, 1), 115.0, 75.0, 65)
        )
        val caloriePoints = listOf(
            CalorieGraphPoint(LocalDate(2026, 1, 1), LocalDate(2026, 1, 1).toEpochDays().toDouble(), 1850.0),
            CalorieGraphPoint(LocalDate(2026, 3, 1), LocalDate(2026, 3, 1).toEpochDays().toDouble(), 2250.0)
        )

        val globalMinX = LocalDate(2026, 1, 1).toEpochDays().toDouble()
        val globalMaxX = LocalDate(2026, 3, 1).toEpochDays().toDouble()

        val weightRangeProvider = TimeFrameChartRangeProvider(
            forcedMinX = globalMinX,
            forcedMaxX = globalMaxX,
            minPadding = 3.0,
            maxPadding = 3.0,
            yStepMultiple = 5.0
        )
        val bpRangeProvider = TimeFrameChartRangeProvider(
            forcedMinX = globalMinX,
            forcedMaxX = globalMaxX,
            minPadding = 5.0,
            maxPadding = 5.0,
            yPaddingFraction = 0.15,
            yStepMultiple = 5.0
        )
        val calorieRangeProvider = TimeFrameChartRangeProvider(
            forcedMinX = globalMinX,
            forcedMaxX = globalMaxX,
            forcedMinY = 0.0,
            minPadding = 50.0,
            maxPadding = 100.0,
            yStepMultiple = 50.0
        )

        val weightModel = buildWeightLayerModel(weightEntries, WeightUnit.US)!!
        val bpModel = buildBpLayerModel(bpPoints, true, bpPoints)!!
        val calorieModel = buildCalorieLayerModel(caloriePoints)!!
        val metabolicModel = buildMetabolicLinesLayerModel(
            globalMinX = globalMinX,
            globalMaxX = globalMaxX,
            maintenanceCalories = 2390.0,
            targetCalories = 1996.0
        )!!

        // Function to simulate Vico range calculation across layers and axes
        fun computeYRange(
            weightPos: Axis.Position.Vertical?,
            bpPos: Axis.Position.Vertical?,
            caloriePos: Axis.Position.Vertical?
        ): MutableCartesianChartRanges {
            val ranges = MutableCartesianChartRanges()
            val extraStore = MutableExtraStore()
            if (caloriePos != null || weightPos != null || bpPos != null) {
                if (caloriePos != null || (weightPos != null && bpPos != null)) {
                    val minY = calorieRangeProvider.getMinY(0.0, 2390.0, extraStore)
                    val maxY = calorieRangeProvider.getMaxY(0.0, 2390.0, extraStore)
                    ranges.tryUpdate(globalMinX, globalMaxX, minY, maxY, caloriePos)
                }
                if (weightPos != null) {
                    val minY = weightRangeProvider.getMinY(253.0, 262.0, extraStore)
                    val maxY = weightRangeProvider.getMaxY(253.0, 262.0, extraStore)
                    ranges.tryUpdate(globalMinX, globalMaxX, minY, maxY, weightPos)
                }
                if (bpPos != null) {
                    val minY = bpRangeProvider.getMinY(65.0, 120.0, extraStore)
                    val maxY = bpRangeProvider.getMaxY(65.0, 120.0, extraStore)
                    ranges.tryUpdate(globalMinX, globalMaxX, minY, maxY, bpPos)
                }
            }
            return ranges
        }

        // Case 1: Calories only (on Start)
        val r1 = computeYRange(weightPos = null, bpPos = null, caloriePos = Axis.Position.Vertical.Start)
        val calY1 = r1.getYRange(Axis.Position.Vertical.Start)

        // Case 2: Weight + Calories (Weight on Start, Calories on End)
        val r2 = computeYRange(weightPos = Axis.Position.Vertical.Start, bpPos = null, caloriePos = Axis.Position.Vertical.End)
        val calY2 = r2.getYRange(Axis.Position.Vertical.End)
        val weightY2 = r2.getYRange(Axis.Position.Vertical.Start)

        // Case 3: BP + Calories (Calories on Start, BP on End)
        val r3 = computeYRange(weightPos = null, bpPos = Axis.Position.Vertical.End, caloriePos = Axis.Position.Vertical.Start)
        val calY3 = r3.getYRange(Axis.Position.Vertical.Start)
        val bpY3 = r3.getYRange(Axis.Position.Vertical.End)

        // Case 4: Weight + BP + Calories (Weight on Start, BP on End, Calories on null)
        val r4 = computeYRange(weightPos = Axis.Position.Vertical.Start, bpPos = Axis.Position.Vertical.End, caloriePos = null)
        val calY4 = r4.getYRange(null)
        val weightY4 = r4.getYRange(Axis.Position.Vertical.Start)
        val bpY4 = r4.getYRange(Axis.Position.Vertical.End)

        assertEquals(0.0, calY1.minY)
        assertEquals(0.0, calY2.minY)
        assertEquals(0.0, calY3.minY)
        assertEquals(0.0, calY4.minY)

        assertEquals(calY1.maxY, calY2.maxY)
        assertEquals(calY1.maxY, calY3.maxY)
        assertEquals(calY1.maxY, calY4.maxY)

        assertEquals(weightY2.minY, weightY4.minY)
        assertEquals(weightY2.maxY, weightY4.maxY)

        assertEquals(bpY3.minY, bpY4.minY)
        assertEquals(bpY3.maxY, bpY4.maxY)
    }
}
