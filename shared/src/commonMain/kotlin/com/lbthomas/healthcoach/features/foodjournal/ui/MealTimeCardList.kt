package com.lbthomas.healthcoach.features.foodjournal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.features.foodjournal.data.DailyMealSummaryData
import com.lbthomas.healthcoach.features.foodjournal.data.MealEntryData
import com.lbthomas.healthcoach.features.foodjournal.data.MealTimeGroupData
import kotlinx.datetime.LocalDate

@Composable
fun MealTimeCardList(
    summary: DailyMealSummaryData,
    onAddFoodClick: (MealTime) -> Unit,
    onEditEntryClick: (MealEntryData) -> Unit,
    onDeleteEntryClick: (MealEntryData) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupMap = summary.mealGroups.associateBy { it.mealTime }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(MealTime.entries.sortedBy { it.order }, key = { it.name }) { mealTime ->
            val group = groupMap[mealTime]
            val entries = group?.entries ?: emptyList()
            val subtotal = group?.subtotalCalories ?: 0.0

            MealTimeSectionCard(
                mealTime = mealTime,
                entries = entries,
                subtotalCalories = subtotal,
                onAddFoodClick = onAddFoodClick,
                onEditEntryClick = onEditEntryClick,
                onDeleteEntryClick = onDeleteEntryClick
            )
        }
    }
}

@Preview(name = "Meal Time Card List - Day View")
@Composable
fun MealTimeCardListPreview() {
    val sampleSummary = DailyMealSummaryData(
        date = LocalDate(2023, 1, 5),
        totalCalories = 880.0,
        mealGroups = listOf(
            MealTimeGroupData(
                mealTime = MealTime.BREAKFAST,
                entries = listOf(
                    MealEntryData(
                        id = "1",
                        date = LocalDate(2023, 1, 5),
                        mealTime = MealTime.BREAKFAST,
                        foodId = "101",
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
                ),
                subtotalCalories = 225.0
            ),
            MealTimeGroupData(
                mealTime = MealTime.LUNCH,
                entries = listOf(
                    MealEntryData(
                        id = "2",
                        date = LocalDate(2023, 1, 5),
                        mealTime = MealTime.LUNCH,
                        foodId = "102",
                        foodName = "Chicken Salad Bowl",
                        foodDescription = "Grilled chicken with mixed greens",
                        brand = "Sweetgreen",
                        unitName = "Bowl",
                        unitQuantity = 1.0,
                        caloriesPerUnit = 655.0,
                        portionMultiplier = 1.0,
                        totalCalories = 655.0,
                        updatedAt = 0L
                    )
                ),
                subtotalCalories = 655.0
            )
        )
    )

    MaterialTheme {
        MealTimeCardList(
            summary = sampleSummary,
            onAddFoodClick = {},
            onEditEntryClick = {},
            onDeleteEntryClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
