package com.lbthomas.healthcoach.features.foodjournal.data

import com.lbthomas.healthcoach.core.enums.MealTime
import kotlinx.datetime.LocalDate

data class FoodUnitData(
    val id: String,
    val name: String,
    val abbreviation: String?,
    val isDefault: Boolean,
    val updatedAt: Long
)

data class FoodItemData(
    val id: String,
    val name: String,
    val brand: String? = null,
    val upc: String? = null,
    val description: String? = null,
    val unitName: String,
    val unitQuantity: Double,
    val caloriesPerUnit: Double,
    val updatedAt: Long
) {
    fun matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val nameLower = name.lowercase()
        val brandLower = brand?.lowercase() ?: ""
        val descLower = description?.lowercase() ?: ""
        return tokens.all { token ->
            nameLower.contains(token) || brandLower.contains(token) || descLower.contains(token)
        }
    }
}

data class MealEntryData(
    val id: String,
    val date: LocalDate,
    val mealTime: MealTime,
    val foodId: String? = null,
    val foodName: String,
    val foodDescription: String? = null,
    val brand: String? = null,
    val unitName: String,
    val unitQuantity: Double,
    val caloriesPerUnit: Double,
    val portionMultiplier: Double,
    val totalCalories: Double,
    val updatedAt: Long
)

data class FoodUsageStats(
    val foodId: String,
    val usageCount: Long,
    val lastUsedAt: Long
)

data class FoodSearchResult(
    val foodItem: FoodItemData,
    val relevanceScore: Double,
    val usageStats: FoodUsageStats? = null
)

data class MealTimeGroupData(
    val mealTime: MealTime,
    val entries: List<MealEntryData>,
    val subtotalCalories: Double
)

data class DailyMealSummaryData(
    val date: LocalDate,
    val totalCalories: Double,
    val mealGroups: List<MealTimeGroupData>
)
