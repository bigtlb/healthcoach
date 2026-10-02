package com.lbthomas.healthcoach.features.foodjournal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.features.foodjournal.data.FoodItemData

@Composable
fun FoodItemCard(
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

@Preview(name = "Food Item Card - Full Details with Usage")
@Composable
private fun FoodItemCardFullPreview() {
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
private fun FoodItemCardMinimalPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        FoodItemCard(
            foodItem = FoodItemData(
                id = "2",
                name = "Apple",
                brand = null,
                description = null,
                upc = null,
                unitName = "item",
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
