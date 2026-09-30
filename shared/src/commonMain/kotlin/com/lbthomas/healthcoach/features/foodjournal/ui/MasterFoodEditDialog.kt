package com.lbthomas.healthcoach.features.foodjournal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.features.foodjournal.data.FoodItemData
import com.lbthomas.healthcoach.features.foodjournal.data.FoodUnitData
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterFoodEditDialog(
    foodItem: FoodItemData,
    availableUnits: List<FoodUnitData>,
    onConfirm: (FoodItemData) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNew = foodItem.id.isEmpty() || foodItem.id == "0"
    val title = if (isNew) "New Food Item" else "Edit Food Item"

    var name by remember { mutableStateOf(foodItem.name) }
    var brand by remember { mutableStateOf(foodItem.brand ?: "") }
    var description by remember { mutableStateOf(foodItem.description ?: "") }
    var upc by remember { mutableStateOf(foodItem.upc ?: "") }

    val defaultUnitName = foodItem.unitName.ifEmpty {
        availableUnits.firstOrNull { it.isDefault }?.name ?: availableUnits.firstOrNull()?.name ?: "Cup"
    }
    var unitName by remember { mutableStateOf(defaultUnitName) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val initialQty = if (foodItem.unitQuantity > 0.0) {
        if (foodItem.unitQuantity % 1.0 == 0.0) foodItem.unitQuantity.toLong().toString()
        else foodItem.unitQuantity.toString()
    } else "1.0"
    var unitQuantityText by remember {
        mutableStateOf(
            TextFieldValue(text = initialQty, selection = TextRange(initialQty.length))
        )
    }

    val initialCal = if (foodItem.caloriesPerUnit > 0.0) {
        if (foodItem.caloriesPerUnit % 1.0 == 0.0) foodItem.caloriesPerUnit.toLong().toString()
        else foodItem.caloriesPerUnit.toString()
    } else ""
    var caloriesText by remember {
        mutableStateOf(
            TextFieldValue(text = initialCal, selection = TextRange(initialCal.length))
        )
    }

    val parsedUnitQty = unitQuantityText.text.toDoubleOrNull()
    val isUnitQtyValid = parsedUnitQty != null && parsedUnitQty > 0.0

    val parsedCalories = caloriesText.text.toDoubleOrNull()
    val isCaloriesValid = parsedCalories != null && parsedCalories >= 0.0

    val isNameValid = name.isNotBlank()
    val isUnitNameValid = unitName.isNotBlank()

    val isValid = isNameValid && isUnitNameValid && isUnitQtyValid && isCaloriesValid

    fun confirmIfValid() {
        val qty = parsedUnitQty
        val cals = parsedCalories
        if (isNameValid && isUnitNameValid && qty != null && qty > 0.0 && cals != null && cals >= 0.0) {
            onConfirm(
                foodItem.copy(
                    id = foodItem.id,
                    name = name.trim(),
                    brand = brand.trim().ifEmpty { null },
                    description = description.trim().ifEmpty { null },
                    upc = upc.trim().ifEmpty { null },
                    unitName = unitName.trim(),
                    unitQuantity = qty,
                    caloriesPerUnit = cals,
                    updatedAt = foodItem.updatedAt
                )
            )
        }
    }

    val nameInputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(foodItem) {
        yield()
        nameInputFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .testTag("master_food_edit_dialog")
            .onDialogKeyEvents(
                onConfirm = { confirmIfValid() },
                onDismiss = onDismiss
            ),
        title = {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name (required)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name *") },
                    placeholder = { Text("e.g. Rolled Oats") },
                    singleLine = true,
                    isError = name.isNotEmpty() && !isNameValid,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameInputFocusRequester)
                )

                // Brand (optional)
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand (Optional)") },
                    placeholder = { Text("e.g. Quaker") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                // Unit and Quantity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unit Quantity
                    OutlinedTextField(
                        value = unitQuantityText,
                        onValueChange = { unitQuantityText = it },
                        label = { Text("Qty *") },
                        placeholder = { Text("1.0") },
                        singleLine = true,
                        isError = unitQuantityText.text.isNotEmpty() && !isUnitQtyValid,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Unit Selection Dropdown / Text Box
                    Box(modifier = Modifier.weight(1.5f)) {
                        OutlinedTextField(
                            value = unitName,
                            onValueChange = { unitName = it },
                            label = { Text("Unit *") },
                            placeholder = { Text("Cup, oz, g") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { unitDropdownExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select unit"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            val unitsToDisplay = if (availableUnits.isNotEmpty()) {
                                availableUnits.map { it.name }
                            } else {
                                listOf("Cup", "oz", "grams", "Tablespoon", "Teaspoon", "Lbs", "Package")
                            }
                            unitsToDisplay.distinct().forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        unitName = unit
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Calories per Unit
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Calories Per Unit *") },
                    placeholder = { Text("e.g. 150") },
                    singleLine = true,
                    isError = caloriesText.text.isNotEmpty() && !isCaloriesValid,
                    supportingText = {
                        if (isUnitQtyValid && isCaloriesValid && isUnitNameValid) {
                            Text(
                                "Portion baseline: ${unitQuantityText.text} $unitName = ${caloriesText.text} kcal",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description (optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. 100% whole grain, quick-cooking") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // UPC / Barcode (optional)
                OutlinedTextField(
                    value = upc,
                    onValueChange = { upc = it },
                    label = { Text("UPC / Barcode (Optional)") },
                    placeholder = { Text("e.g. 012345678905") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { confirmIfValid() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirmIfValid() },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(name = "New Food Item Dialog")
@Composable
fun MasterFoodEditDialogNewPreview() {
    MasterFoodEditDialog(
        foodItem = FoodItemData(
            id = "",
            name = "",
            brand = null,
            description = null,
            upc = null,
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 0.0,
            updatedAt = 0L
        ),
        availableUnits = listOf(
            FoodUnitData("1", "Cup", "cup", true, 0L),
            FoodUnitData("2", "oz", "oz", false, 0L),
            FoodUnitData("3", "grams", "g", false, 0L),
            FoodUnitData("4", "Tablespoon", "tbsp", false, 0L)
        ),
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(name = "Edit Food Item - Full Metadata")
@Composable
fun MasterFoodEditDialogEditFullPreview() {
    MasterFoodEditDialog(
        foodItem = FoodItemData(
            id = "1",
            name = "Rolled Oats",
            brand = "Quaker",
            description = "100% whole grain old fashioned oats",
            upc = "030000010402",
            unitName = "Cup",
            unitQuantity = 0.5,
            caloriesPerUnit = 150.0,
            updatedAt = 0L
        ),
        availableUnits = listOf(
            FoodUnitData("1", "Cup", "cup", true, 0L),
            FoodUnitData("2", "oz", "oz", false, 0L),
            FoodUnitData("3", "grams", "g", false, 0L),
            FoodUnitData("4", "Tablespoon", "tbsp", false, 0L)
        ),
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(name = "Edit Food Item - Minimal Metadata")
@Composable
fun MasterFoodEditDialogEditMinimalPreview() {
    MasterFoodEditDialog(
        foodItem = FoodItemData(
            id = "2",
            name = "Banana",
            brand = null,
            description = null,
            upc = null,
            unitName = "serving",
            unitQuantity = 1.0,
            caloriesPerUnit = 105.0,
            updatedAt = 0L
        ),
        availableUnits = listOf(
            FoodUnitData("1", "Cup", "cup", true, 0L),
            FoodUnitData("2", "oz", "oz", false, 0L),
            FoodUnitData("3", "grams", "g", false, 0L),
            FoodUnitData("4", "Tablespoon", "tbsp", false, 0L)
        ),
        onConfirm = {},
        onDismiss = {}
    )
}
