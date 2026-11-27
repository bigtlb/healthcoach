package com.lbthomas.healthcoach.core.share

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ShareTextFormatterTest {

    private val entries = listOf(
        WeightEntryData("1", LocalDate(2024, 1, 1), 80.0),
        WeightEntryData("2", LocalDate(2024, 1, 31), 79.0)
    )

    @Test
    fun formatsAbsoluteProgressValueOnNewLine() {
        val (progressText, _) = ShareTextFormatter.format(
            entries = entries,
            allEntries = entries,
            timeFrame = GraphTimeFrame.ONE_MONTH,
            weightUnit = WeightUnit.METRIC,
            showAbsoluteWeight = true
        )

        assertEquals("My 1 month weight progress:\n80.0 → 79.0 kg (-1.0 kg)", progressText)
    }

    @Test
    fun formatsChangeOnlyOnNewLine() {
        val (progressText, _) = ShareTextFormatter.format(
            entries = entries,
            allEntries = entries,
            timeFrame = GraphTimeFrame.ONE_MONTH,
            weightUnit = WeightUnit.METRIC,
            showAbsoluteWeight = false
        )

        assertEquals("My 1 month weight progress:\n-1.0 kg", progressText)
    }
}