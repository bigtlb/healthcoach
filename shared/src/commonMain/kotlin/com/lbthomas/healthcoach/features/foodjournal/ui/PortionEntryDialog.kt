package com.lbthomas.healthcoach.features.foodjournal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.foodjournal.data.FoodItemData
import com.lbthomas.healthcoach.features.foodjournal.data.MealEntryData
import kotlinx.coroutines.yield
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortionEntryDialog(
    initialDate: LocalDate = today,
    initialMealTime: MealTime = MealTime.BREAKFAST,
    foodItem: FoodItemData? = null,
    existingMealEntry: MealEntryData? = null,
    onConfirm: (MealEntryData) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEdit = existingMealEntry != null
    val title = if (isEdit) "Edit Meal Entry" else "Log Food Entry"

    val foodName = existingMealEntry?.foodName ?: foodItem?.name ?: ""
    val brand = existingMealEntry?.brand ?: foodItem?.brand
    val description = existingMealEntry?.foodDescription ?: foodItem?.description
    val unitName = existingMealEntry?.unitName ?: foodItem?.unitName ?: "serving"
    val unitQuantity = existingMealEntry?.unitQuantity ?: foodItem?.unitQuantity ?: 1.0
    val caloriesPerUnit = existingMealEntry?.caloriesPerUnit ?: foodItem?.caloriesPerUnit ?: 0.0
    val foodId = existingMealEntry?.foodId ?: foodItem?.id

    var selectedDate by remember { mutableStateOf(existingMealEntry?.date ?: initialDate) }
    var selectedMealTime by remember { mutableStateOf(existingMealEntry?.mealTime ?: initialMealTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var mealTimeDropdownExpanded by remember { mutableStateOf(false) }

    val initialMultiplierStr = if (existingMealEntry != null) {
        if (existingMealEntry.portionMultiplier % 1.0 == 0.0) existingMealEntry.portionMultiplier.toLong().toString()
        else existingMealEntry.portionMultiplier.toString()
    } else "1.0"

    var portionText by remember {
        mutableStateOf(
            TextFieldValue(text = initialMultiplierStr, selection = TextRange(initialMultiplierStr.length))
        )
    }

    val parsedMultiplier = portionText.text.toDoubleOrNull()
    val isMultiplierValid = parsedMultiplier != null && parsedMultiplier > 0.0
    val totalCalories = if (parsedMultiplier != null && parsedMultiplier > 0.0) {
        (parsedMultiplier * caloriesPerUnit * 10.0).roundToInt() / 10.0
    } else 0.0

    val focusRequester = remember { FocusRequester() }

    fun confirmIfValid() {
        val multiplier = parsedMultiplier
        if (multiplier != null && multiplier > 0.0) {
            val entry = MealEntryData(
                id = existingMealEntry?.id ?: "",
                date = selectedDate,
                mealTime = selectedMealTime,
                foodId = foodId,
                foodName = foodName,
                foodDescription = description,
                brand = brand,
                unitName = unitName,
                unitQuantity = unitQuantity,
                caloriesPerUnit = caloriesPerUnit,
                portionMultiplier = multiplier,
                totalCalories = totalCalories,
                updatedAt = existingMealEntry?.updatedAt ?: 0L
            )
            onConfirm(entry)
        }
    }

    LaunchedEffect(Unit) {
        yield()
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .testTag("portion_entry_dialog")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Food Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = foodName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!brand.isNullOrBlank()) {
                            Text(
                                text = brand,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!description.isNullOrBlank()) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val formattedUnitQty = if (unitQuantity % 1.0 == 0.0) unitQuantity.toLong().toString() else unitQuantity.toString()
                        val formattedCalPerUnit = if (caloriesPerUnit % 1.0 == 0.0) caloriesPerUnit.toLong().toString() else caloriesPerUnit.toString()
                        Text(
                            text = "Baseline: $formattedUnitQty $unitName = $formattedCalPerUnit kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Date Picker Card
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedDate.toString(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date"
                        )
                    }
                }

                // Meal Time Dropdown Box
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { mealTimeDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Meal Slot",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedMealTime.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Meal Time"
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = mealTimeDropdownExpanded,
                        onDismissRequest = { mealTimeDropdownExpanded = false }
                    ) {
                        MealTime.entries.forEach { mealTime ->
                            DropdownMenuItem(
                                text = { Text(mealTime.displayName) },
                                onClick = {
                                    selectedMealTime = mealTime
                                    mealTimeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Portion Multiplier Input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = portionText,
                        onValueChange = { portionText = it },
                        label = { Text("Portion Multiplier") },
                        placeholder = { Text("1.0") },
                        singleLine = true,
                        isError = portionText.text.isNotEmpty() && !isMultiplierValid,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { confirmIfValid() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    // Quick portion preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.5, 1.0, 1.5, 2.0).forEach { preset ->
                            val presetText = if (preset % 1.0 == 0.0) "${preset.toLong()}x" else "${preset}x"
                            val isSelected = parsedMultiplier == preset
                            SuggestionChip(
                                onClick = {
                                    portionText = TextFieldValue(
                                        text = preset.toString(),
                                        selection = TextRange(preset.toString().length)
                                    )
                                },
                                label = { Text(presetText) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }

                // Total Calories Banner Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculated Intake:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val totalCalFormatted = if (totalCalories % 1.0 == 0.0) totalCalories.toLong().toString() else totalCalories.toString()
                        Text(
                            text = "$totalCalFormatted kcal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirmIfValid() },
                enabled = isMultiplierValid
            ) {
                Text(if (isEdit) "Save" else "Log Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val initialEpochMillis = remember(selectedDate) {
            selectedDate.atTime(0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpochMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(name = "Log Food Entry - Brand & Description")
@Composable
fun PortionEntryDialogLogFoodPreview() {
    PortionEntryDialog(
        initialDate = LocalDate(2023, 1, 5),
        initialMealTime = MealTime.BREAKFAST,
        foodItem = FoodItemData(
            id = "item_1",
            name = "Rolled Oats",
            brand = "Quaker",
            description = "Old fashioned whole grain oats",
            upc = null,
            unitName = "Cup",
            unitQuantity = 0.5,
            caloriesPerUnit = 150.0,
            updatedAt = 0L
        ),
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(name = "Edit Meal Entry - Custom Portion")
@Composable
fun PortionEntryDialogEditEntryPreview() {
    PortionEntryDialog(
        existingMealEntry = MealEntryData(
            id = "entry_1",
            date = LocalDate(2023, 1, 5),
            mealTime = MealTime.LUNCH,
            foodId = "item_2",
            foodName = "Greek Yogurt Plain",
            foodDescription = "Non-fat plain Greek yogurt",
            brand = "Kirkland",
            unitName = "Cup",
            unitQuantity = 0.75,
            caloriesPerUnit = 100.0,
            portionMultiplier = 1.5,
            totalCalories = 150.0,
            updatedAt = 0L
        ),
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(name = "Log Food Entry - Minimal Metadata")
@Composable
fun PortionEntryDialogMinimalFoodPreview() {
    PortionEntryDialog(
        initialDate = LocalDate(2023, 1, 5),
        initialMealTime = MealTime.MIDDAY_SNACK,
        foodItem = FoodItemData(
            id = "item_3",
            name = "Banana",
            brand = null,
            description = null,
            upc = null,
            unitName = "serving",
            unitQuantity = 1.0,
            caloriesPerUnit = 105.0,
            updatedAt = 0L
        ),
        onConfirm = {},
        onDismiss = {}
    )
}
