package com.lbthomas.healthcoach.features.graphs

import com.lbthomas.healthcoach.features.graphs.ui.TimeFrameChartRangeProvider
import com.lbthomas.healthcoach.features.graphs.ui.components.calculateOptimalYStep
import com.lbthomas.healthcoach.features.graphs.ui.components.createBottomAxisValueFormatter
import com.patrykandpatrick.vico.compose.common.data.MutableExtraStore
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ChartRangeProviderTest {
    @Test
    fun testWeightRangeSnapsToMultiplesOfFive() {
        val rangeProvider = TimeFrameChartRangeProvider(
            forcedMinX = null,
            forcedMaxX = null,
            minPadding = 3.0,
            maxPadding = 3.0,
            yStepMultiple = 5.0
        )
        val dummyStore = MutableExtraStore()

        // With diff = 17.0, padding = 1.0, rawMin = 325.4 -> should snap down to 325.0 (multiple of 5)
        val minY = rangeProvider.getMinY(326.4, 343.4, dummyStore)
        assertEquals(325.0, minY)

        // Max weight 343.4, rawMax = 344.4 -> should snap up to 345.0 (multiple of 5)
        val maxY = rangeProvider.getMaxY(326.4, 343.4, dummyStore)
        assertEquals(345.0, maxY)
    }

    @Test
    fun testBpRangeSnapsToMultiplesOfFive() {
        val rangeProvider = TimeFrameChartRangeProvider(
            forcedMinX = null,
            forcedMaxX = null,
            minPadding = 5.0,
            maxPadding = 5.0,
            yPaddingFraction = 0.15,
            yStepMultiple = 5.0
        )
        val dummyStore = MutableExtraStore()

        // If min BP was ~61 mmHg, with diff ~ 70, padding = 70 * 0.15 = 10.5 -> rawMin = 50.5 -> snapped to 50.0
        val minY = rangeProvider.getMinY(61.0, 131.0, dummyStore)
        assertEquals(50.0, minY)

        // Max BP 131.0, rawMax = 131 + 10.5 = 141.5 -> snapped to 145.0
        val maxY = rangeProvider.getMaxY(61.0, 131.0, dummyStore)
        assertEquals(145.0, maxY)
    }

    @Test
    fun testCalculateOptimalYStepAdaptiveMultiples() {
        // Small range (e.g. 15 lbs / mmHg) -> 5.0
        assertEquals(5.0, calculateOptimalYStep(140.0, 155.0, defaultStep = 5.0))

        // Range of 35 -> 5.0 (7 intervals)
        assertEquals(5.0, calculateOptimalYStep(140.0, 175.0, defaultStep = 5.0))

        // Range of 60 -> 10.0 (6 intervals of 10)
        assertEquals(10.0, calculateOptimalYStep(100.0, 160.0, defaultStep = 5.0))

        // Range of 90 -> 15.0 (6 intervals of 15)
        assertEquals(15.0, calculateOptimalYStep(50.0, 140.0, defaultStep = 5.0))

        // Range of 130 -> 20.0
        assertEquals(20.0, calculateOptimalYStep(50.0, 180.0, defaultStep = 5.0))

        // Range of 200 -> 30.0 (or 50.0)
        assertEquals(30.0, calculateOptimalYStep(150.0, 350.0, defaultStep = 5.0))

        // Calories range of 2500 -> 500.0
        assertEquals(500.0, calculateOptimalYStep(0.0, 2500.0, defaultStep = 50.0))
    }

    @Test
    fun testBottomAxisValueFormatterOutputsYYMMForLongTimeFrame() {
        val dateJan26 = LocalDate(2026, 1, 15)
        val formattedJan26 = com.lbthomas.healthcoach.features.graphs.ui.components.formatBottomAxisDate(dateJan26, daySpan = 1000)
        assertEquals("26/01", formattedJan26)

        val dateNov24 = LocalDate(2024, 11, 2)
        val formattedNov24 = com.lbthomas.healthcoach.features.graphs.ui.components.formatBottomAxisDate(dateNov24, daySpan = 1000)
        assertEquals("24/11", formattedNov24)
    }
}
