package com.lbthomas.healthcoach.features.foodjournal.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.core.utils.currentEpochMillis
import com.lbthomas.healthcoach.core.utils.generateUuid
import com.lbthomas.healthcoach.foodjournal.data.FoodItem
import com.lbthomas.healthcoach.foodjournal.data.FoodUnit
import com.lbthomas.healthcoach.foodjournal.data.MealEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FoodJournalRepository(private val database: Database) {

    /**
     * Ensures all default measurement units and seed food items exist.
     * Can be invoked anytime to restore missing default records.
     */
    fun ensureDefaultFoods() {
        DefaultFoodData.ensureDefaultFoodData(database)
    }

    // ==========================================
    // Food Units
    // ==========================================

    fun observeAllFoodUnits(): Flow<List<FoodUnitData>> = database
        .foodUnitQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { units -> units.map(FoodUnit::toData) }

    fun getFoodUnitById(id: String): FoodUnitData? =
        database.foodUnitQueries.selectById(id).executeAsOneOrNull()?.toData()

    fun addFoodUnit(
        name: String,
        abbreviation: String? = null,
        isDefault: Boolean = false,
        id: String = generateUuid(),
        updatedAt: Long = currentEpochMillis()
    ): String {
        database.foodUnitQueries.insert(
            id = id,
            name = name.trim(),
            abbreviation = abbreviation?.trim(),
            isDefault = if (isDefault) 1L else 0L,
            updated_at = updatedAt
        )
        return id
    }

    fun updateFoodUnit(
        unit: FoodUnitData,
        updatedAt: Long = currentEpochMillis()
    ) {
        database.foodUnitQueries.update(
            name = unit.name.trim(),
            abbreviation = unit.abbreviation?.trim(),
            isDefault = if (unit.isDefault) 1L else 0L,
            updated_at = updatedAt,
            id = unit.id
        )
    }

    fun deleteFoodUnit(id: String) {
        database.foodUnitQueries.delete(id)
    }

    // ==========================================
    // Master Food Items
    // ==========================================

    fun observeAllFoodItems(): Flow<List<FoodItemData>> = database
        .foodItemQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { items -> items.map(FoodItem::toData) }

    fun getFoodItemById(id: String): FoodItemData? =
        database.foodItemQueries.selectById(id).executeAsOneOrNull()?.toData()

    fun addFoodItem(
        name: String,
        unitName: String,
        unitQuantity: Double,
        caloriesPerUnit: Double,
        brand: String? = null,
        upc: String? = null,
        description: String? = null,
        id: String = generateUuid(),
        updatedAt: Long = currentEpochMillis()
    ): String {
        database.foodItemQueries.insert(
            id = id,
            name = name.trim(),
            brand = brand?.trim()?.takeIf { it.isNotEmpty() },
            upc = upc?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            unitName = unitName.trim(),
            unitQuantity = unitQuantity,
            caloriesPerUnit = caloriesPerUnit,
            updated_at = updatedAt
        )
        return id
    }

    fun updateFoodItem(
        item: FoodItemData,
        updatedAt: Long = currentEpochMillis()
    ) {
        database.foodItemQueries.update(
            name = item.name.trim(),
            brand = item.brand?.trim()?.takeIf { it.isNotEmpty() },
            upc = item.upc?.trim()?.takeIf { it.isNotEmpty() },
            description = item.description?.trim()?.takeIf { it.isNotEmpty() },
            unitName = item.unitName.trim(),
            unitQuantity = item.unitQuantity,
            caloriesPerUnit = item.caloriesPerUnit,
            updated_at = updatedAt,
            id = item.id
        )
    }

    fun deleteFoodItem(id: String) {
        database.foodItemQueries.delete(id)
    }

    // ==========================================
    // Usage Statistics (MRU / MFU) & Search
    // ==========================================

    fun observeFoodUsageStats(): Flow<Map<String, FoodUsageStats>> = database
        .mealEntryQueries
        .selectFoodUsageStats()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { statsList ->
            statsList.associate { stat ->
                stat.foodId to FoodUsageStats(
                    foodId = stat.foodId,
                    usageCount = stat.usageCount,
                    lastUsedAt = stat.lastUsedAt ?: 0L
                )
            }
        }

    fun observeRecentFoodItems(limit: Long = 10): Flow<List<FoodItemData>> = database
        .mealEntryQueries
        .selectRecentFoodItems(limit)
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { items -> items.map(FoodItem::toData) }

    fun observeFrequentFoodItems(limit: Long = 10): Flow<List<FoodItemData>> = database
        .mealEntryQueries
        .selectFrequentFoodItems(limit)
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { items -> items.map(FoodItem::toData) }

    fun searchFoodItems(query: String): Flow<List<FoodItemData>> =
        searchFoodItemsWithRelevance(query).map { results -> results.map { it.foodItem } }

    fun searchFoodItemsWithRelevance(
        query: String,
        nowMillis: Long = currentEpochMillis()
    ): Flow<List<FoodSearchResult>> {
        return combine(observeAllFoodItems(), observeFoodUsageStats()) { items, statsMap ->
            rankFoodItems(items, statsMap, query, nowMillis)
        }
    }

    // ==========================================
    // Meal Entries
    // ==========================================

    fun observeAllMealEntries(): Flow<List<MealEntryData>> = database
        .mealEntryQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entries -> entries.map(MealEntry::toData) }

    fun observeMealEntriesByDate(date: LocalDate): Flow<List<MealEntryData>> = database
        .mealEntryQueries
        .selectByDate(date.toString())
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entries -> entries.map(MealEntry::toData) }

    fun observeMealEntriesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MealEntryData>> = database
        .mealEntryQueries
        .selectByDateRange(startDate.toString(), endDate.toString())
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entries -> entries.map(MealEntry::toData) }

    fun observeDailyCalorieTotals(): Flow<Map<LocalDate, Double>> = database
        .mealEntryQueries
        .selectDailyCalorieTotals()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { rows ->
            rows.associate { row ->
                LocalDate.parse(row.date) to (row.totalCalories ?: 0.0)
            }
        }

    fun observeDailyCalorieTotalByDate(date: LocalDate): Flow<Double> = database
        .mealEntryQueries
        .selectDailyCalorieTotalByDate(date.toString())
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { it?.totalCalories ?: 0.0 }

    fun observeDailyMealSummary(date: LocalDate): Flow<DailyMealSummaryData> =
        observeMealEntriesByDate(date).map { entries ->
            val groups = MealTime.entries.sortedBy { it.order }.map { mealTime ->
                val mealEntries = entries.filter { it.mealTime == mealTime }
                MealTimeGroupData(
                    mealTime = mealTime,
                    entries = mealEntries,
                    subtotalCalories = mealEntries.sumOf { it.totalCalories }
                )
            }
            DailyMealSummaryData(
                date = date,
                totalCalories = entries.sumOf { it.totalCalories },
                mealGroups = groups
            )
        }

    fun addMealEntry(
        date: LocalDate,
        mealTime: MealTime,
        foodName: String,
        unitName: String,
        unitQuantity: Double,
        caloriesPerUnit: Double,
        portionMultiplier: Double,
        totalCalories: Double = portionMultiplier * caloriesPerUnit,
        foodId: String? = null,
        foodDescription: String? = null,
        brand: String? = null,
        id: String = generateUuid(),
        updatedAt: Long = currentEpochMillis()
    ): String {
        database.mealEntryQueries.insert(
            id = id,
            date = date.toString(),
            mealTime = mealTime.name,
            foodId = foodId,
            foodName = foodName.trim(),
            foodDescription = foodDescription?.trim()?.takeIf { it.isNotEmpty() },
            brand = brand?.trim()?.takeIf { it.isNotEmpty() },
            unitName = unitName.trim(),
            unitQuantity = unitQuantity,
            caloriesPerUnit = caloriesPerUnit,
            portionMultiplier = portionMultiplier,
            totalCalories = totalCalories,
            updated_at = updatedAt
        )
        return id
    }

    fun updateMealEntry(
        entry: MealEntryData,
        updatedAt: Long = currentEpochMillis()
    ) {
        database.mealEntryQueries.update(
            date = entry.date.toString(),
            mealTime = entry.mealTime.name,
            foodId = entry.foodId,
            foodName = entry.foodName.trim(),
            foodDescription = entry.foodDescription?.trim()?.takeIf { it.isNotEmpty() },
            brand = entry.brand?.trim()?.takeIf { it.isNotEmpty() },
            unitName = entry.unitName.trim(),
            unitQuantity = entry.unitQuantity,
            caloriesPerUnit = entry.caloriesPerUnit,
            portionMultiplier = entry.portionMultiplier,
            totalCalories = entry.totalCalories,
            updated_at = updatedAt,
            id = entry.id
        )
    }

    fun deleteMealEntry(id: String) {
        database.mealEntryQueries.delete(id)
    }

    companion object {
        fun rankFoodItems(
            items: List<FoodItemData>,
            usageStats: Map<String, FoodUsageStats>,
            query: String,
            nowMillis: Long = currentEpochMillis()
        ): List<FoodSearchResult> {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                return items.map { item ->
                    val stats = usageStats[item.id]
                    val count = stats?.usageCount ?: 0L
                    val lastUsed = stats?.lastUsedAt ?: 0L
                    val recencyBonus = if (lastUsed > 0L) {
                        val daysAgo = (nowMillis - lastUsed).toDouble() / (1000 * 60 * 60 * 24)
                        when {
                            daysAgo <= 7 -> 25.0
                            daysAgo <= 30 -> 15.0
                            daysAgo <= 90 -> 5.0
                            else -> 1.0
                        }
                    } else 0.0
                    val frequencyBonus = (count * 5.0).coerceAtMost(50.0)
                    FoodSearchResult(
                        foodItem = item,
                        relevanceScore = recencyBonus + frequencyBonus,
                        usageStats = stats
                    )
                }.sortedWith(
                    compareByDescending<FoodSearchResult> { it.relevanceScore }
                        .thenBy { it.foodItem.name.lowercase() }
                )
            }

            val tokens = trimmed.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

            return items.mapNotNull { item ->
                if (!item.matchesQuery(query)) return@mapNotNull null

                val nameLower = item.name.lowercase()
                val brandLower = item.brand?.lowercase() ?: ""
                val descLower = item.description?.lowercase() ?: ""

                var score = 0.0

                // Exact / Prefix match bonuses
                if (nameLower == trimmed.lowercase()) {
                    score += 100.0
                } else if (nameLower.startsWith(trimmed.lowercase())) {
                    score += 60.0
                } else if (nameLower.contains(trimmed.lowercase())) {
                    score += 30.0
                }

                if (brandLower.isNotEmpty() && brandLower == trimmed.lowercase()) {
                    score += 80.0
                } else if (brandLower.isNotEmpty() && brandLower.startsWith(trimmed.lowercase())) {
                    score += 40.0
                }

                // Per-token bonuses
                for (token in tokens) {
                    val nameWords = nameLower.split(Regex("[^a-zA-Z0-9]+"))
                    if (nameWords.any { it.startsWith(token) }) {
                        score += 20.0
                    } else if (nameLower.contains(token)) {
                        score += 10.0
                    }

                    val brandWords = brandLower.split(Regex("[^a-zA-Z0-9]+"))
                    if (brandWords.any { it.startsWith(token) }) {
                        score += 15.0
                    } else if (brandLower.contains(token)) {
                        score += 8.0
                    }

                    if (descLower.contains(token)) {
                        score += 4.0
                    }
                }

                // Usage bonuses (MFU / MRU)
                val stats = usageStats[item.id]
                if (stats != null) {
                    val frequencyBonus = (stats.usageCount * 5.0).coerceAtMost(50.0)
                    val daysAgo = (nowMillis - stats.lastUsedAt).toDouble() / (1000 * 60 * 60 * 24)
                    val recencyBonus = when {
                        daysAgo <= 7 -> 25.0
                        daysAgo <= 30 -> 15.0
                        daysAgo <= 90 -> 5.0
                        else -> 1.0
                    }
                    score += (frequencyBonus + recencyBonus)
                }

                FoodSearchResult(
                    foodItem = item,
                    relevanceScore = score,
                    usageStats = stats
                )
            }.sortedWith(
                compareByDescending<FoodSearchResult> { it.relevanceScore }
                    .thenBy { it.foodItem.name.lowercase() }
            )
        }
    }
}

// ==========================================
// Extension Mappers
// ==========================================

private fun FoodUnit.toData(): FoodUnitData =
    FoodUnitData(
        id = id,
        name = name,
        abbreviation = abbreviation,
        isDefault = isDefault != 0L,
        updatedAt = updated_at
    )

private fun FoodItem.toData(): FoodItemData =
    FoodItemData(
        id = id,
        name = name,
        brand = brand,
        upc = upc,
        description = description,
        unitName = unitName,
        unitQuantity = unitQuantity,
        caloriesPerUnit = caloriesPerUnit,
        updatedAt = updated_at
    )

private fun MealEntry.toData(): MealEntryData =
    MealEntryData(
        id = id,
        date = LocalDate.parse(date),
        mealTime = MealTime.fromName(mealTime),
        foodId = foodId,
        foodName = foodName,
        foodDescription = foodDescription,
        brand = brand,
        unitName = unitName,
        unitQuantity = unitQuantity,
        caloriesPerUnit = caloriesPerUnit,
        portionMultiplier = portionMultiplier,
        totalCalories = totalCalories,
        updatedAt = updated_at
    )
