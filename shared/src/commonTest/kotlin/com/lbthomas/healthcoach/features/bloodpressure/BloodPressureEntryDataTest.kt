package com.lbthomas.healthcoach.features.bloodpressure

import com.lbthomas.healthcoach.core.enums.BloodPressureCategory
import com.lbthomas.healthcoach.core.utils.formatBpStorageString
import com.lbthomas.healthcoach.core.utils.parseBpDateTime
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class BloodPressureEntryDataTest {

    @Test
    fun testDateOnlyParsing() {
        val entry = BloodPressureEntryData(
            id = 1,
            dateTime = "2026-09-23",
            systolic = 118,
            diastolic = 76,
            pulse = 70
        )

        assertEquals(LocalDate(2026, 9, 23), entry.date)
        assertNull(entry.time)
        assertFalse(entry.hasTime)
        assertEquals(BloodPressureCategory.NORMAL, entry.category)
    }

    @Test
    fun testRfc3339DateTimeParsing() {
        val entry = BloodPressureEntryData(
            id = 2,
            dateTime = "2026-09-23T15:30:00Z",
            systolic = 145,
            diastolic = 95,
            pulse = 80
        )

        val (parsedDate, parsedTime) = parseBpDateTime("2026-09-23T15:30:00Z", TimeZone.UTC)
        assertEquals(LocalDate(2026, 9, 23), parsedDate)
        assertEquals(LocalTime(15, 30, 0), parsedTime)
        assertEquals(BloodPressureCategory.STAGE_2_HYPERTENSION, entry.category)
    }

    @Test
    fun testStorageFormattingWithoutTime() {
        val formatted = formatBpStorageString(LocalDate(2026, 9, 23), null)
        assertEquals("2026-09-23", formatted)
    }

    @Test
    fun testStorageFormattingWithTime() {
        val formatted = formatBpStorageString(LocalDate(2026, 9, 23), LocalTime(15, 30, 0), TimeZone.UTC)
        assertEquals("2026-09-23T15:30:00Z", formatted)
    }
}
