package com.lbthomas.healthcoach.features.foodjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.foodjournal.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

open class FoodJournalViewModel : ViewModel {
    val repository: FoodJournalRepository?

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val foodUnits: StateFlow<List<FoodUnitData>>
    val foodItems: StateFlow<List<FoodItemData>>
    val searchResults: StateFlow<List<FoodSearchResult>>
    val recentFoodItems: StateFlow<List<FoodItemData>>
    val frequentFoodItems: StateFlow<List<FoodItemData>>
    val dailyMealSummary: StateFlow<DailyMealSummaryData>
    val dailyCalorieTotals: StateFlow<Map<LocalDate, Double>>

    @OptIn(ExperimentalCoroutinesApi::class)
    constructor(repository: FoodJournalRepository) : super() {
        this.repository = repository

        foodUnits = repository
            .observeAllFoodUnits()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        foodItems = repository
            .observeAllFoodItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        recentFoodItems = repository
            .observeRecentFoodItems(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        frequentFoodItems = repository
            .observeFrequentFoodItems(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        searchResults = _searchQuery
            .flatMapLatest { query ->
                repository.searchFoodItemsWithRelevance(query)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        dailyMealSummary = _selectedDate
            .flatMapLatest { date ->
                repository.observeDailyMealSummary(date)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                DailyMealSummaryData(today, 0.0, emptyList())
            )

        dailyCalorieTotals = repository
            .observeDailyCalorieTotals()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    }

    // Preview constructor
    constructor(
        previewDate: LocalDate = today,
        previewSummary: DailyMealSummaryData = DailyMealSummaryData(previewDate, 0.0, emptyList()),
        previewUnits: List<FoodUnitData> = emptyList(),
        previewItems: List<FoodItemData> = emptyList(),
        previewRecentItems: List<FoodItemData> = previewItems,
        previewFrequentItems: List<FoodItemData> = previewItems,
        previewSearchQuery: String = "",
        previewDailyCalorieTotals: Map<LocalDate, Double> = emptyMap()
    ) : super() {
        this.repository = null
        _selectedDate.value = previewDate
        _searchQuery.value = previewSearchQuery
        dailyMealSummary = MutableStateFlow(previewSummary).asStateFlow()
        foodUnits = MutableStateFlow(previewUnits).asStateFlow()
        foodItems = MutableStateFlow(previewItems).asStateFlow()
        recentFoodItems = MutableStateFlow(previewRecentItems).asStateFlow()
        frequentFoodItems = MutableStateFlow(previewFrequentItems).asStateFlow()
        searchResults = MutableStateFlow(previewItems.map { FoodSearchResult(it, 100.0) }).asStateFlow()
        dailyCalorieTotals = MutableStateFlow(previewDailyCalorieTotals).asStateFlow()
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectPreviousDay() {
        _selectedDate.value = _selectedDate.value.minus(DatePeriod(days = 1))
    }

    fun selectNextDay() {
        _selectedDate.value = _selectedDate.value.plus(DatePeriod(days = 1))
    }

    fun selectToday() {
        _selectedDate.value = today
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    // Food Unit Operations
    fun addFoodUnit(name: String, abbreviation: String? = null, isDefault: Boolean = false): String? {
        return repository?.addFoodUnit(name, abbreviation, isDefault)
    }

    fun updateFoodUnit(unit: FoodUnitData) {
        repository?.updateFoodUnit(unit)
    }

    fun deleteFoodUnit(id: String) {
        repository?.deleteFoodUnit(id)
    }

    // Master Food Item Operations
    fun addFoodItem(
        name: String,
        unitName: String,
        unitQuantity: Double,
        caloriesPerUnit: Double,
        brand: String? = null,
        upc: String? = null,
        description: String? = null
    ): String? {
        return repository?.addFoodItem(
            name = name,
            unitName = unitName,
            unitQuantity = unitQuantity,
            caloriesPerUnit = caloriesPerUnit,
            brand = brand,
            upc = upc,
            description = description
        )
    }

    fun updateFoodItem(item: FoodItemData) {
        repository?.updateFoodItem(item)
    }

    fun deleteFoodItem(id: String) {
        repository?.deleteFoodItem(id)
    }

    // Meal Entry Operations
    fun logMeal(
        date: LocalDate = _selectedDate.value,
        mealTime: MealTime,
        foodName: String,
        unitName: String,
        unitQuantity: Double,
        caloriesPerUnit: Double,
        portionMultiplier: Double,
        foodId: String? = null,
        foodDescription: String? = null,
        brand: String? = null
    ): String? {
        return repository?.addMealEntry(
            date = date,
            mealTime = mealTime,
            foodName = foodName,
            unitName = unitName,
            unitQuantity = unitQuantity,
            caloriesPerUnit = caloriesPerUnit,
            portionMultiplier = portionMultiplier,
            totalCalories = portionMultiplier * caloriesPerUnit,
            foodId = foodId,
            foodDescription = foodDescription,
            brand = brand
        )
    }

    fun updateMealEntry(entry: MealEntryData) {
        repository?.updateMealEntry(entry)
    }

    fun deleteMealEntry(id: String) {
        repository?.deleteMealEntry(id)
    }
}
