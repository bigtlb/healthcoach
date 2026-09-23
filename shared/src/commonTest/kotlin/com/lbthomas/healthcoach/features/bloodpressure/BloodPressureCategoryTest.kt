package com.lbthomas.healthcoach.features.bloodpressure

import com.lbthomas.healthcoach.core.enums.BloodPressureCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class BloodPressureCategoryTest {

    @Test
    fun testNormalCategory() {
        // Normal: Systolic < 120 AND Diastolic < 80
        assertEquals(BloodPressureCategory.NORMAL, BloodPressureCategory.fromReadings(118, 76))
        assertEquals(BloodPressureCategory.NORMAL, BloodPressureCategory.fromReadings(100, 65))
        assertEquals(BloodPressureCategory.NORMAL, BloodPressureCategory.fromReadings(119, 79))
    }

    @Test
    fun testElevatedCategory() {
        // Elevated: Systolic 120-129 AND Diastolic < 80
        assertEquals(BloodPressureCategory.ELEVATED, BloodPressureCategory.fromReadings(120, 75))
        assertEquals(BloodPressureCategory.ELEVATED, BloodPressureCategory.fromReadings(125, 78))
        assertEquals(BloodPressureCategory.ELEVATED, BloodPressureCategory.fromReadings(129, 79))
    }

    @Test
    fun testStage1HypertensionCategory() {
        // Stage 1: Systolic 130-139 OR Diastolic 80-89
        assertEquals(BloodPressureCategory.STAGE_1_HYPERTENSION, BloodPressureCategory.fromReadings(130, 75))
        assertEquals(BloodPressureCategory.STAGE_1_HYPERTENSION, BloodPressureCategory.fromReadings(135, 85))
        assertEquals(BloodPressureCategory.STAGE_1_HYPERTENSION, BloodPressureCategory.fromReadings(118, 80))
        assertEquals(BloodPressureCategory.STAGE_1_HYPERTENSION, BloodPressureCategory.fromReadings(122, 85))
        assertEquals(BloodPressureCategory.STAGE_1_HYPERTENSION, BloodPressureCategory.fromReadings(139, 89))
    }

    @Test
    fun testStage2HypertensionCategory() {
        // Stage 2: Systolic >= 140 OR Diastolic >= 90
        assertEquals(BloodPressureCategory.STAGE_2_HYPERTENSION, BloodPressureCategory.fromReadings(140, 75))
        assertEquals(BloodPressureCategory.STAGE_2_HYPERTENSION, BloodPressureCategory.fromReadings(115, 90))
        assertEquals(BloodPressureCategory.STAGE_2_HYPERTENSION, BloodPressureCategory.fromReadings(150, 95))
        assertEquals(BloodPressureCategory.STAGE_2_HYPERTENSION, BloodPressureCategory.fromReadings(175, 115))
    }

    @Test
    fun testHypertensiveCrisisCategory() {
        // Crisis: Systolic > 180 and/or Diastolic > 120
        assertEquals(BloodPressureCategory.HYPERTENSIVE_CRISIS, BloodPressureCategory.fromReadings(181, 85))
        assertEquals(BloodPressureCategory.HYPERTENSIVE_CRISIS, BloodPressureCategory.fromReadings(130, 121))
        assertEquals(BloodPressureCategory.HYPERTENSIVE_CRISIS, BloodPressureCategory.fromReadings(190, 130))
    }
}
