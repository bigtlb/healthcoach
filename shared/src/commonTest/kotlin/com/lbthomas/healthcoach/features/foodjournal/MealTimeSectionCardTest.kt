package com.lbthomas.healthcoach.features.foodjournal

import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.features.foodjournal.data.MealEntryData
import com.lbthomas.healthcoach.features.foodjournal.ui.formatPortionCalculation
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MealTimeSectionCardTest {

    @Test
    fun testFormatPortionCalculationMultiplierOne() {
        val entry = MealEntryData(
            id = "1",
            date = LocalDate(2026, 10, 5),
            mealTime = MealTime.BREAKFAST,
            foodId = "101",
            foodName = "Greek Yogurt",
            foodDescription = null,
            brand = null,
            unitName = "Cup",
            unitQuantity = 0.25,
            caloriesPerUnit = 110.0,
            portionMultiplier = 1.0,
            totalCalories = 110.0,
            updatedAt = 0L
        )

        assertEquals("0.25 Cup @ 110 cal", formatPortionCalculation(entry))
    }

    @Test
    fun testFormatPortionCalculationMultiplierNotOne() {
        val entry = MealEntryData(
            id = "1",
            date = LocalDate(2026, 10, 5),
            mealTime = MealTime.BREAKFAST,
            foodId = "101",
            foodName = "Greek Yogurt",
            foodDescription = null,
            brand = null,
            unitName = "Cup",
            unitQuantity = 0.25,
            caloriesPerUnit = 110.0,
            portionMultiplier = 2.0,
            totalCalories = 220.0,
            updatedAt = 0L
        )

        assertEquals("2 × (0.25 Cup @ 110 cal)", formatPortionCalculation(entry))
    }

    @Test
    fun testFormatPortionCalculationFractionalMultiplier() {
        val entry = MealEntryData(
            id = "1",
            date = LocalDate(2026, 10, 5),
            mealTime = MealTime.LUNCH,
            foodId = "102",
            foodName = "Rolled Oats",
            foodDescription = null,
            brand = "Quaker",
            unitName = "Cup",
            unitQuantity = 0.5,
            caloriesPerUnit = 150.0,
            portionMultiplier = 1.5,
            totalCalories = 225.0,
            updatedAt = 0L
        )

        assertEquals("1.5 × (0.5 Cup @ 150 cal)", formatPortionCalculation(entry))
    }
}
