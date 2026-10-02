package com.lbthomas.healthcoach.features.foodjournal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.features.foodjournal.data.FoodItemData
import com.lbthomas.healthcoach.features.foodjournal.data.FoodSearchResult

@Composable
fun FoodSearchResultsList(
    isSearchSubmitted: Boolean,
    searchQuery: String,
    searchResults: List<FoodSearchResult>,
    allFoods: List<FoodItemData>,
    filteredRecentFoods: List<FoodItemData>,
    filteredFrequentFoods: List<FoodItemData>,
    filteredAllFoods: List<FoodItemData>,
    onSelectFood: (FoodItemData) -> Unit,
    onEditFood: (FoodItemData) -> Unit,
    onDeleteFood: (FoodItemData) -> Unit,
    onCreateFoodWithQuery: (String) -> Unit,
    onCreateNewFood: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                            onClick = { onCreateFoodWithQuery(searchQuery.trim()) }
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
                            onSelect = { onSelectFood(result.foodItem) },
                            onEdit = { onEditFood(result.foodItem) },
                            onDelete = { onDeleteFood(result.foodItem) }
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
                        Button(onClick = onCreateNewFood) {
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
                            onClick = { onCreateFoodWithQuery(searchQuery.trim()) }
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
                                onSelect = { onSelectFood(item) },
                                onEdit = { onEditFood(item) },
                                onDelete = { onDeleteFood(item) }
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
                                onSelect = { onSelectFood(item) },
                                onEdit = { onEditFood(item) },
                                onDelete = { onDeleteFood(item) }
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
                                onSelect = { onSelectFood(item) },
                                onEdit = { onEditFood(item) },
                                onDelete = { onDeleteFood(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
