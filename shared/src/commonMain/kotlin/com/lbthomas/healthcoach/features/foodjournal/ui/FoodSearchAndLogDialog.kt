package com.lbthomas.healthcoach.features.foodjournal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.core.ui.DeleteConfirmationDialog
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.foodjournal.FoodJournalViewModel
import com.lbthomas.healthcoach.features.foodjournal.data.FoodItemData
import com.lbthomas.healthcoach.features.foodjournal.ui.components.FoodSearchHeader
import com.lbthomas.healthcoach.features.foodjournal.ui.components.FoodSearchResultsList
import kotlinx.coroutines.yield
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchAndLogDialog(
    viewModel: FoodJournalViewModel,
    isWide: Boolean = false,
    initialMealTime: MealTime = MealTime.BREAKFAST,
    initialDate: LocalDate = today,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentFoods by viewModel.recentFoodItems.collectAsState()
    val frequentFoods by viewModel.frequentFoodItems.collectAsState()
    val allFoods by viewModel.foodItems.collectAsState()
    val availableUnits by viewModel.foodUnits.collectAsState()

    var selectedFoodForPortion by remember { mutableStateOf<FoodItemData?>(null) }
    var editingFoodItem by remember { mutableStateOf<FoodItemData?>(null) }
    var deletingFoodItem by remember { mutableStateOf<FoodItemData?>(null) }
    var showCreateFoodDialog by remember { mutableStateOf(false) }
    var isSearchSubmitted by remember { mutableStateOf(false) }

    val filteredRecentFoods = remember(recentFoods, searchQuery) {
        if (searchQuery.isBlank()) recentFoods else recentFoods.filter { it.matchesQuery(searchQuery) }
    }
    val filteredFrequentFoods = remember(frequentFoods, searchQuery) {
        if (searchQuery.isBlank()) frequentFoods else frequentFoods.filter { it.matchesQuery(searchQuery) }
    }
    val filteredAllFoods = remember(allFoods, searchQuery) {
        if (searchQuery.isBlank()) allFoods else allFoods.filter { it.matchesQuery(searchQuery) }
    }

    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        yield()
        searchFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = (
            if (isWide) {
                Modifier.width(800.dp)
            } else {
                Modifier.fillMaxSize()
            }
        )
            .then(modifier)
            .testTag("food_search_and_log_dialog")
            .onDialogKeyEvents(
                onConfirm = {},
                onDismiss = onDismiss
            ),
        title = {
            FoodSearchHeader(
                title = "Add to ${initialMealTime.displayName}",
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    viewModel.setSearchQuery(it)
                    isSearchSubmitted = false
                },
                onClearSearch = {
                    viewModel.clearSearchQuery()
                    isSearchSubmitted = false
                },
                onSearchSubmit = { isSearchSubmitted = true },
                isSearchSubmitted = isSearchSubmitted,
                onAddNewFood = { showCreateFoodDialog = true },
                focusRequester = searchFocusRequester
            )
        },
        text = {
            FoodSearchResultsList(
                isSearchSubmitted = isSearchSubmitted,
                searchQuery = searchQuery,
                searchResults = searchResults,
                allFoods = allFoods,
                filteredRecentFoods = filteredRecentFoods,
                filteredFrequentFoods = filteredFrequentFoods,
                filteredAllFoods = filteredAllFoods,
                onSelectFood = { selectedFoodForPortion = it },
                onEditFood = { editingFoodItem = it },
                onDeleteFood = { deletingFoodItem = it },
                onCreateFoodWithQuery = { query ->
                    editingFoodItem = FoodItemData(
                        id = "",
                        name = query,
                        unitName = "Cup",
                        unitQuantity = 1.0,
                        caloriesPerUnit = 0.0,
                        updatedAt = 0L
                    )
                },
                onCreateNewFood = { showCreateFoodDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp)
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Create New Food Dialog
    if (showCreateFoodDialog) {
        MasterFoodEditDialog(
            foodItem = FoodItemData(
                id = "",
                name = "",
                unitName = availableUnits.firstOrNull { it.isDefault }?.name ?: "Cup",
                unitQuantity = 1.0,
                caloriesPerUnit = 0.0,
                updatedAt = 0L
            ),
            availableUnits = availableUnits,
            isWide = isWide,
            onConfirm = { newItem ->
                val newId = viewModel.addFoodItem(
                    name = newItem.name,
                    unitName = newItem.unitName,
                    unitQuantity = newItem.unitQuantity,
                    caloriesPerUnit = newItem.caloriesPerUnit,
                    brand = newItem.brand,
                    upc = newItem.upc,
                    description = newItem.description
                )
                showCreateFoodDialog = false
                if (newId != null) {
                    selectedFoodForPortion = newItem.copy(id = newId)
                }
            },
            onDismiss = { showCreateFoodDialog = false }
        )
    }

    // Edit Master Food Dialog
    editingFoodItem?.let { itemToEdit ->
        MasterFoodEditDialog(
            foodItem = itemToEdit,
            availableUnits = availableUnits,
            isWide = isWide,
            onConfirm = { updatedItem ->
                if (updatedItem.id.isEmpty()) {
                    val newId = viewModel.addFoodItem(
                        name = updatedItem.name,
                        unitName = updatedItem.unitName,
                        unitQuantity = updatedItem.unitQuantity,
                        caloriesPerUnit = updatedItem.caloriesPerUnit,
                        brand = updatedItem.brand,
                        upc = updatedItem.upc,
                        description = updatedItem.description
                    )
                    editingFoodItem = null
                    if (newId != null) {
                        selectedFoodForPortion = updatedItem.copy(id = newId)
                    }
                } else {
                    viewModel.updateFoodItem(updatedItem)
                    editingFoodItem = null
                }
            },
            onDismiss = { editingFoodItem = null }
        )
    }

    // Delete Master Food Confirmation
    deletingFoodItem?.let { itemToDelete ->
        DeleteConfirmationDialog(
            title = "Delete Master Food Item",
            message = "Are you sure you want to delete \"${itemToDelete.name}\" from your food library?",
            note = "Note: Historical meal entries previously logged using this food will remain preserved.",
            onConfirm = {
                viewModel.deleteFoodItem(itemToDelete.id)
                deletingFoodItem = null
            },
            onDismiss = { deletingFoodItem = null }
        )
    }

    // Portion Entry Dialog (Log Meal)
    selectedFoodForPortion?.let { food ->
        PortionEntryDialog(
            initialDate = initialDate,
            initialMealTime = initialMealTime,
            foodItem = food,
            isWide = isWide,
            onConfirm = { mealEntry ->
                viewModel.logMeal(
                    date = mealEntry.date,
                    mealTime = mealEntry.mealTime,
                    foodName = mealEntry.foodName,
                    unitName = mealEntry.unitName,
                    unitQuantity = mealEntry.unitQuantity,
                    caloriesPerUnit = mealEntry.caloriesPerUnit,
                    portionMultiplier = mealEntry.portionMultiplier,
                    foodId = mealEntry.foodId,
                    foodDescription = mealEntry.foodDescription,
                    brand = mealEntry.brand
                )
                viewModel.clearSearchQuery()
                selectedFoodForPortion = null
                onDismiss()
            },
            onDismiss = { selectedFoodForPortion = null }
        )
    }
}

@Preview(name = "Food Search Dialog - Populated Library", widthDp = 800, heightDp = 700)
@Composable
private fun FoodSearchAndLogDialogBrowserPreview() {
    val sampleUnits = listOf(
        com.lbthomas.healthcoach.features.foodjournal.data.FoodUnitData("1", "Cup", "cup", true, 0L),
        com.lbthomas.healthcoach.features.foodjournal.data.FoodUnitData("2", "oz", "oz", false, 0L),
        com.lbthomas.healthcoach.features.foodjournal.data.FoodUnitData("3", "grams", "g", false, 0L)
    )
    val sampleItems = listOf(
        FoodItemData("1", "Rolled Oats", "Quaker", "Old fashioned whole grain oats", null, "Cup", 0.5, 150.0, 0L),
        FoodItemData("2", "Greek Yogurt Plain", "Kirkland", "Non-fat plain Greek yogurt", null, "Cup", 0.75, 100.0, 0L),
        FoodItemData("3", "Banana", null, "Medium fresh banana", null, "serving", 1.0, 105.0, 0L)
    )
    val viewModel = FoodJournalViewModel(
        previewDate = LocalDate(2023, 1, 5),
        previewUnits = sampleUnits,
        previewItems = sampleItems
    )
    FoodSearchAndLogDialog(
        viewModel = viewModel,
        initialMealTime = MealTime.BREAKFAST,
        initialDate = LocalDate(2023, 1, 5),
        onDismiss = {}
    )
}

@Preview(name = "Food Search Dialog - Filtered Sections", widthDp = 800, heightDp = 700)
@Composable
private fun FoodSearchAndLogDialogFilteredPreview() {
    val sampleUnits = listOf(
        com.lbthomas.healthcoach.features.foodjournal.data.FoodUnitData("1", "Cup", "cup", true, 0L)
    )
    val sampleItems = listOf(
        FoodItemData("1", "Rolled Oats", "Quaker", "Old fashioned whole grain oats", null, "Cup", 0.5, 150.0, 0L),
        FoodItemData("2", "Greek Yogurt Plain", "Kirkland", "Non-fat plain Greek yogurt", null, "Cup", 0.75, 100.0, 0L),
        FoodItemData("3", "Banana", null, "Medium fresh banana", null, "serving", 1.0, 105.0, 0L)
    )
    val viewModel = FoodJournalViewModel(
        previewDate = LocalDate(2023, 1, 5),
        previewUnits = sampleUnits,
        previewItems = sampleItems,
        previewSearchQuery = "oat"
    )
    FoodSearchAndLogDialog(
        viewModel = viewModel,
        initialMealTime = MealTime.BREAKFAST,
        initialDate = LocalDate(2023, 1, 5),
        onDismiss = {}
    )
}

@Preview(name = "Food Search Dialog - Empty Library", widthDp = 800, heightDp = 700)
@Composable
private fun FoodSearchAndLogDialogEmptyPreview() {
    val viewModel = FoodJournalViewModel(
        previewDate = LocalDate(2023, 1, 5),
        previewUnits = emptyList(),
        previewItems = emptyList()
    )
    FoodSearchAndLogDialog(
        viewModel = viewModel,
        initialMealTime = MealTime.LUNCH,
        initialDate = LocalDate(2023, 1, 5),
        onDismiss = {}
    )
}
