package com.lbthomas.healthcoach.features.foodjournal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.features.foodjournal.data.MealEntryData
import kotlinx.datetime.LocalDate

@Composable
fun MealTimeSectionCard(
    mealTime: MealTime,
    entries: List<MealEntryData>,
    subtotalCalories: Double,
    onAddFoodClick: (MealTime) -> Unit,
    onEditEntryClick: (MealEntryData) -> Unit,
    onDeleteEntryClick: (MealEntryData) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Title, Calorie Subtotal, and Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = mealTime.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (entries.isNotEmpty()) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    ) {
                        Text(
                            text = "${formatCalories(subtotalCalories)} kcal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (entries.isNotEmpty()) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Tooltip("Log food for ${mealTime.displayName}") {
                    FilledTonalIconButton(
                        onClick = { onAddFoodClick(mealTime) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log food for ${mealTime.displayName}",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "No items logged yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.5.dp
                            )
                        }
                        MealEntryRow(
                            entry = entry,
                            onEdit = { onEditEntryClick(entry) },
                            onDelete = { onDeleteEntryClick(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealEntryRow(
    entry: MealEntryData,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.foodName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!entry.brand.isNullOrBlank()) {
                    Text(
                        text = "(${entry.brand})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Calculation Details: Portion multiplier x unit amount @ calories
            val portionText = formatPortionCalculation(entry)
            Text(
                text = portionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${formatCalories(entry.totalCalories)} kcal",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Tooltip("Edit portion") {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit portion",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Tooltip("Remove entry") {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove entry",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

internal fun formatPortionCalculation(entry: MealEntryData): String {
    val multiplierStr = formatDecimal(entry.portionMultiplier)
    val unitQuantityStr = formatDecimal(entry.unitQuantity)
    val caloriesPerUnitStr = formatCalories(entry.caloriesPerUnit)

    return if (entry.portionMultiplier == 1.0) {
        "$unitQuantityStr ${entry.unitName} @ $caloriesPerUnitStr cal"
    } else {
        "$multiplierStr × ($unitQuantityStr ${entry.unitName} @ $caloriesPerUnitStr cal)"
    }
}

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        ((value * 100).toLong() / 100.0).toString()
    }
}

private fun formatCalories(calories: Double): String {
    return if (calories % 1.0 == 0.0) {
        calories.toLong().toString()
    } else {
        ((calories * 10).toLong() / 10.0).toString()
    }
}

@Preview(name = "Meal Slot - Populated")
@Composable
fun MealTimeSectionCardPopulatedPreview() {
    val sampleEntries = listOf(
        MealEntryData(
            id = "1",
            date = LocalDate(2023, 1, 5),
            mealTime = MealTime.BREAKFAST,
            foodId = "101",
            foodName = "Rolled Oats",
            foodDescription = "Organic whole oats",
            brand = "Quaker",
            unitName = "Cup",
            unitQuantity = 0.5,
            caloriesPerUnit = 150.0,
            portionMultiplier = 1.5,
            totalCalories = 225.0,
            updatedAt = 0L
        ),
        MealEntryData(
            id = "2",
            date = LocalDate(2023, 1, 5),
            mealTime = MealTime.BREAKFAST,
            foodId = "102",
            foodName = "Banana",
            foodDescription = null,
            brand = null,
            unitName = "serving",
            unitQuantity = 1.0,
            caloriesPerUnit = 105.0,
            portionMultiplier = 1.0,
            totalCalories = 105.0,
            updatedAt = 0L
        )
    )

    MaterialTheme {
        MealTimeSectionCard(
            mealTime = MealTime.BREAKFAST,
            entries = sampleEntries,
            subtotalCalories = 330.0,
            onAddFoodClick = {},
            onEditEntryClick = {},
            onDeleteEntryClick = {}
        )
    }
}

@Preview(name = "Meal Slot - Empty")
@Composable
fun MealTimeSectionCardEmptyPreview() {
    MaterialTheme {
        MealTimeSectionCard(
            mealTime = MealTime.LUNCH,
            entries = emptyList(),
            subtotalCalories = 0.0,
            onAddFoodClick = {},
            onEditEntryClick = {},
            onDeleteEntryClick = {}
        )
    }
}
