package com.lbthomas.healthcoach.features.foodjournal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.foodjournal.FoodJournalViewModel
import com.lbthomas.healthcoach.features.foodjournal.data.FoodItemData
import com.lbthomas.healthcoach.features.foodjournal.data.MealEntryData
import kotlinx.coroutines.yield
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchAndLogDialog(
    viewModel: FoodJournalViewModel,
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
        modifier = modifier
            .testTag("food_search_and_log_dialog")
            .fillMaxWidth(0.97f)
            .fillMaxHeight(0.92f)
            .onDialogKeyEvents(
                onConfirm = {},
                onDismiss = onDismiss
            ),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add to ${initialMealTime.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Tooltip("Add new food to library") {
                    FloatingActionButton(
                        onClick = { showCreateFoodDialog = true },
                        modifier = Modifier.size(36.dp),
                        shape = FloatingActionButtonDefaults.smallShape,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add new food to library",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        viewModel.setSearchQuery(it)
                        isSearchSubmitted = false
                    },
                    placeholder = {
                        Text(
                            text = "Search by name, brand, or description...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Tooltip("Search food library") {
                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        isSearchSubmitted = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isSearchSubmitted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.clearSearchQuery()
                                    isSearchSubmitted = false
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                isSearchSubmitted = true
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                )

                // List Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isSearchSubmitted) {
                        // When Search is pressed: actual found foods results (weighted with recent & frequent)
                        if (searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No foods found matching \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            // Prepopulate name with current search query
                                            editingFoodItem = FoodItemData(
                                                id = "",
                                                name = searchQuery.trim(),
                                                unitName = "Cup",
                                                unitQuantity = 1.0,
                                                caloriesPerUnit = 0.0,
                                                updatedAt = 0L
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Create \"${searchQuery.trim()}\"")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Search Results (${searchResults.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                                    )
                                }
                                items(searchResults, key = { it.foodItem.id }) { result ->
                                    FoodItemCard(
                                        foodItem = result.foodItem,
                                        usageCount = result.usageStats?.usageCount,
                                        onSelect = { selectedFoodForPortion = result.foodItem },
                                        onEdit = { editingFoodItem = result.foodItem },
                                        onDelete = { deletingFoodItem = result.foodItem }
                                    )
                                }
                            }
                        }
                    } else {
                        // Dynamic Filter View: Recently Logged, Frequently Logged, All Foods filtered by typed text
                        if (allFoods.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Your food library is empty",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Create your first food item to start logging meals.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(onClick = { showCreateFoodDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Create Food Item")
                                    }
                                }
                            }
                        } else if (searchQuery.isNotBlank() && filteredRecentFoods.isEmpty() && filteredFrequentFoods.isEmpty() && filteredAllFoods.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No foods found matching \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            // Prepopulate name with current search query
                                            editingFoodItem = FoodItemData(
                                                id = "",
                                                name = searchQuery.trim(),
                                                unitName = "Cup",
                                                unitQuantity = 1.0,
                                                caloriesPerUnit = 0.0,
                                                updatedAt = 0L
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Create \"${searchQuery.trim()}\"")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (filteredRecentFoods.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Recently Logged",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(filteredRecentFoods, key = { "recent_${it.id}" }) { item ->
                                        FoodItemCard(
                                            foodItem = item,
                                            onSelect = { selectedFoodForPortion = item },
                                            onEdit = { editingFoodItem = item },
                                            onDelete = { deletingFoodItem = item }
                                        )
                                    }
                                }

                                if (filteredFrequentFoods.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Frequently Logged",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(filteredFrequentFoods, key = { "frequent_${it.id}" }) { item ->
                                        FoodItemCard(
                                            foodItem = item,
                                            onSelect = { selectedFoodForPortion = item },
                                            onEdit = { editingFoodItem = item },
                                            onDelete = { deletingFoodItem = item }
                                        )
                                    }
                                }

                                if (filteredAllFoods.isNotEmpty()) {
                                    item {
                                        val label = if (searchQuery.isBlank()) "All Foods (${allFoods.size})" else "All Foods (${filteredAllFoods.size})"
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(filteredAllFoods, key = { "all_${it.id}" }) { item ->
                                        FoodItemCard(
                                            foodItem = item,
                                            onSelect = { selectedFoodForPortion = item },
                                            onEdit = { editingFoodItem = item },
                                            onDelete = { deletingFoodItem = item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                selectedFoodForPortion = null
                onDismiss()
            },
            onDismiss = { selectedFoodForPortion = null }
        )
    }
}

@Composable
private fun FoodItemCard(
    foodItem: FoodItemData,
    usageCount: Long? = null,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = foodItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (usageCount != null && usageCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${usageCount}x",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!foodItem.brand.isNullOrBlank()) {
                        Text(
                            text = foodItem.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    val formattedQty = if (foodItem.unitQuantity % 1.0 == 0.0) foodItem.unitQuantity.toLong().toString() else foodItem.unitQuantity.toString()
                    val formattedCal = if (foodItem.caloriesPerUnit % 1.0 == 0.0) foodItem.caloriesPerUnit.toLong().toString() else foodItem.caloriesPerUnit.toString()
                    Text(
                        text = "$formattedQty ${foodItem.unitName} • $formattedCal kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!foodItem.description.isNullOrBlank()) {
                    Text(
                        text = foodItem.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit food",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete food",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview(name = "Food Search Dialog - Populated Library", widthDp = 800, heightDp = 700)
@Composable
fun FoodSearchAndLogDialogBrowserPreview() {
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
fun FoodSearchAndLogDialogFilteredPreview() {
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
fun FoodSearchAndLogDialogEmptyPreview() {
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

@Preview(name = "Food Item Card - Full Details with Usage")
@Composable
fun FoodItemCardFullPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        FoodItemCard(
            foodItem = FoodItemData(
                id = "1",
                name = "Rolled Oats",
                brand = "Quaker",
                description = "Old fashioned whole grain rolled oats",
                upc = "030000010402",
                unitName = "Cup",
                unitQuantity = 0.5,
                caloriesPerUnit = 150.0,
                updatedAt = 0L
            ),
            usageCount = 14L,
            onSelect = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(name = "Food Item Card - Minimal Details")
@Composable
fun FoodItemCardMinimalPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        FoodItemCard(
            foodItem = FoodItemData(
                id = "2",
                name = "Apple",
                brand = null,
                description = null,
                upc = null,
                unitName = "serving",
                unitQuantity = 1.0,
                caloriesPerUnit = 95.0,
                updatedAt = 0L
            ),
            usageCount = null,
            onSelect = {},
            onEdit = {},
            onDelete = {}
        )
    }
}
