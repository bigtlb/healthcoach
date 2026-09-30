package com.lbthomas.healthcoach.core.di


import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.features.bloodpressure.BloodPressureViewModel
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import com.lbthomas.healthcoach.features.foodjournal.FoodJournalViewModel
import com.lbthomas.healthcoach.features.foodjournal.data.DailyMealSummaryData
import com.lbthomas.healthcoach.features.foodjournal.data.FoodItemData
import com.lbthomas.healthcoach.features.foodjournal.data.FoodUnitData
import com.lbthomas.healthcoach.features.foodjournal.data.MealTimeGroupData
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import org.koin.dsl.module


fun previewAppModuleWith(settings: SettingsData = SettingsData()) = module {
    factory {
        SettingsViewModel(
            settings = MutableStateFlow(settings)
        )
    }

    factory { com.lbthomas.healthcoach.features.sync.SyncViewModel() }

    factory {
        val sampleUnits = listOf(
            FoodUnitData("1", "Cup", "cup", true, 0L),
            FoodUnitData("2", "oz", "oz", false, 0L),
            FoodUnitData("3", "grams", "g", false, 0L),
            FoodUnitData("4", "Tablespoon", "tbsp", false, 0L),
            FoodUnitData("5", "Teaspoon", "tsp", false, 0L),
            FoodUnitData("6", "Each", "ea", false, 0L),
            FoodUnitData("7", "Piece", "pc", false, 0L),
            FoodUnitData("8", "Slice", "slice", false, 0L)
        )
        val sampleItems = listOf(
            FoodItemData("1", "Rolled Oats", "Quaker", null, "Old fashioned whole grain oats", "Cup", 0.5, 150.0, 0L),
            FoodItemData("2", "Greek Yogurt Plain", "Kirkland", null, "Non-fat plain Greek yogurt", "Cup", 0.75, 100.0, 0L),
            FoodItemData("3", "Banana", null, null, "Medium fresh banana", "serving", 1.0, 105.0, 0L),
            FoodItemData("4", "Almond Butter", "Justin's", null, "Classic creamy almond butter", "Tablespoon", 2.0, 190.0, 0L)
        )
        val sampleSummary = DailyMealSummaryData(
            date = LocalDate(2023, 1, 5),
            totalCalories = 430.0,
            mealGroups = listOf(
                MealTimeGroupData(
                    mealTime = MealTime.BREAKFAST,
                    entries = listOf(
                        com.lbthomas.healthcoach.features.foodjournal.data.MealEntryData(
                            id = "1",
                            date = LocalDate(2023, 1, 5),
                            mealTime = MealTime.BREAKFAST,
                            foodId = "1",
                            foodName = "Rolled Oats",
                            foodDescription = "Old fashioned whole grain oats",
                            brand = "Quaker",
                            unitName = "Cup",
                            unitQuantity = 0.5,
                            caloriesPerUnit = 150.0,
                            portionMultiplier = 1.5,
                            totalCalories = 225.0,
                            updatedAt = 0L
                        ),
                        com.lbthomas.healthcoach.features.foodjournal.data.MealEntryData(
                            id = "2",
                            date = LocalDate(2023, 1, 5),
                            mealTime = MealTime.BREAKFAST,
                            foodId = "3",
                            foodName = "Banana",
                            foodDescription = "Medium fresh banana",
                            brand = null,
                            unitName = "serving",
                            unitQuantity = 1.0,
                            caloriesPerUnit = 105.0,
                            portionMultiplier = 1.0,
                            totalCalories = 105.0,
                            updatedAt = 0L
                        )
                    ),
                    subtotalCalories = 330.0
                ),
                MealTimeGroupData(
                    mealTime = MealTime.LUNCH,
                    entries = listOf(
                        com.lbthomas.healthcoach.features.foodjournal.data.MealEntryData(
                            id = "3",
                            date = LocalDate(2023, 1, 5),
                            mealTime = MealTime.LUNCH,
                            foodId = "2",
                            foodName = "Greek Yogurt Plain",
                            foodDescription = "Non-fat plain Greek yogurt",
                            brand = "Kirkland",
                            unitName = "Cup",
                            unitQuantity = 0.75,
                            caloriesPerUnit = 100.0,
                            portionMultiplier = 1.0,
                            totalCalories = 100.0,
                            updatedAt = 0L
                        )
                    ),
                    subtotalCalories = 100.0
                )
            )
        )
        val sampleDailyCalorieTotals = mapOf(
            LocalDate(2022, 12, 1) to 2100.0,
            LocalDate(2022, 12, 2) to 1950.0,
            LocalDate(2023, 1, 3) to 2200.0,
            LocalDate(2023, 1, 4) to 1850.0,
            LocalDate(2023, 1, 5) to 2050.0,
            LocalDate(2023, 2, 6) to 1900.0,
            LocalDate(2023, 2, 7) to 2150.0
        )
        FoodJournalViewModel(
            previewDate = LocalDate(2023, 1, 5),
            previewSummary = sampleSummary,
            previewUnits = sampleUnits,
            previewItems = sampleItems,
            previewDailyCalorieTotals = sampleDailyCalorieTotals
        )
    }

    factory {
        WeightViewModel(
            previewEntries = MutableStateFlow(
                listOf(
                    WeightEntryData("1", LocalDate(2022, 12, 1), 140.0),
                    WeightEntryData("2", LocalDate(2022, 12, 2), 145.0),
                    WeightEntryData("3", LocalDate(2023, 1, 3), 140.0),
                    WeightEntryData("4", LocalDate(2023, 1, 4), 142.0),
                    WeightEntryData("5", LocalDate(2023, 1, 5), 130.0),
                    WeightEntryData("6", LocalDate(2023, 2, 6), 125.0),
                    WeightEntryData("7", LocalDate(2023, 2, 7), 120.0)
                )
            )
        )
    }

    factory {
        BloodPressureViewModel(
            previewEntries = MutableStateFlow(
                listOf(
                    BloodPressureEntryData("1", "2023-01-03T08:30:00Z", 118, 76, 68),
                    BloodPressureEntryData("2", "2023-01-04T12:15:00Z", 124, 78, 72),
                    BloodPressureEntryData("3", "2023-01-05T19:45:00Z", 134, 84, 75),
                    BloodPressureEntryData("4", "2023-02-06", 142, 92, 80),
                    BloodPressureEntryData("5", "2023-02-07T09:00:00Z", 115, 75, 65)
                )
            )
        )
    }
}

val previewAppModule = previewAppModuleWith()

