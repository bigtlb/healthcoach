package com.lbthomas.healthcoach.core.enums

import androidx.compose.ui.graphics.Color

enum class BloodPressureCategory(
    val title: String,
    val description: String,
    val color: Color
) {
    NORMAL(
        title = "Normal",
        description = "Normal: Systolic less than 120 and Diastolic less than 80 mm Hg",
        color = Color(0xFF7CB342) // Green
    ),
    ELEVATED(
        title = "Elevated",
        description = "Elevated: Systolic 120–129 and Diastolic less than 80 mm Hg",
        color = Color(0xFFFBC02D) // Yellow
    ),
    STAGE_1_HYPERTENSION(
        title = "Stage 1 Hypertension",
        description = "Stage 1 Hypertension: Systolic 130–139 or Diastolic 80–89 mm Hg",
        color = Color(0xFFF57C00) // Orange
    ),
    STAGE_2_HYPERTENSION(
        title = "Stage 2 Hypertension",
        description = "Stage 2 Hypertension: Systolic 140 or higher or Diastolic 90 or higher mm Hg",
        color = Color(0xFFD32F2F) // Red
    ),
    HYPERTENSIVE_CRISIS(
        title = "Hypertensive Crisis",
        description = "Hypertensive Crisis: Systolic higher than 180 and/or Diastolic higher than 120 mm Hg",
        color = Color(0xFF880E4F) // Dark Burgundy / Purple
    );

    companion object {
        fun fromReadings(systolic: Int, diastolic: Int): BloodPressureCategory {
            return when {
                systolic > 180 || diastolic > 120 -> HYPERTENSIVE_CRISIS
                systolic >= 140 || diastolic >= 90 -> STAGE_2_HYPERTENSION
                systolic >= 130 || diastolic >= 80 -> STAGE_1_HYPERTENSION
                systolic >= 120 -> ELEVATED
                else -> NORMAL
            }
        }
    }
}
