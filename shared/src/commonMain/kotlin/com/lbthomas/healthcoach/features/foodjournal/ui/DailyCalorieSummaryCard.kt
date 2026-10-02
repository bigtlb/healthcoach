package com.lbthomas.healthcoach.features.foodjournal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.utils.DOW
import com.lbthomas.healthcoach.core.utils.displayDate
import com.lbthomas.healthcoach.core.utils.today
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCalorieSummaryCard(
    selectedDate: LocalDate,
    totalCalories: Double,
    targetCalories: Double? = null,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val isOverBudget = targetCalories != null && totalCalories > targetCalories
    val isToday = selectedDate == today

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDayIn(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date
                            onDateSelected(localDate)
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

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isOverBudget) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Date Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Tooltip("Previous Day") {
                    IconButton(
                        onClick = onPreviousDay,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Day"
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Choose Date",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${selectedDate.DOW()}, ${selectedDate.displayDate()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!isToday) {
                        Tooltip("Jump to Today") {
                            IconButton(
                                onClick = onToday,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = "Today",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Tooltip("Next Day") {
                    IconButton(
                        onClick = onNextDay,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Day"
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // Bottom Section: Calorie Consumption & Budget Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Consumed Calories
                Column {
                    Text(
                        text = "Total Consumed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatCalories(totalCalories),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Right: Target & Caloric Balance
                if (targetCalories != null) {
                    val remaining = targetCalories - totalCalories
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Target: ${formatCalories(targetCalories)} kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Tooltip("Daily target calorie budget derived from your BMR, activity level, and weight goal.") {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Target Budget Info",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        val balanceTooltip = if (remaining >= 0) {
                            "Remaining daily calorie budget: ${formatCalories(remaining)} kcal."
                        } else {
                            "Daily intake exceeds target calorie budget by ${formatCalories(-remaining)} kcal."
                        }

                        Tooltip(balanceTooltip) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (remaining >= 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (remaining >= 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Text(
                                    text = if (remaining >= 0) {
                                        "${formatCalories(remaining)} kcal remaining"
                                    } else {
                                        "${formatCalories(-remaining)} kcal over target"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (remaining >= 0) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Tooltip("Configure your profile in the top bar to set a daily target calorie budget.") {
                        Text(
                            text = "No Target Set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Calorie Budget Intake Progress Bar
            if (targetCalories != null && targetCalories > 0.0) {
                val progress = (totalCalories / targetCalories).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (isOverBudget) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = if (isOverBudget) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    },
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

private fun formatCalories(calories: Double): String {
    val rounded = calories.roundToInt()
    val isExact = abs(calories - rounded) < 0.05
    return if (isExact) {
        formatThousands(rounded.toLong())
    } else {
        val oneDecimal = ((calories * 10).roundToInt() / 10.0)
        oneDecimal.toString()
    }
}

private fun formatThousands(value: Long): String {
    val str = value.toString()
    val isNegative = str.startsWith("-")
    val digits = if (isNegative) str.substring(1) else str
    val formattedDigits = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (isNegative) "-$formattedDigits" else formattedDigits
}

@Preview(name = "Daily Calorie Summary - Standard (No Target)")
@Composable
fun DailyCalorieSummaryCardStandardPreview() {
    MaterialTheme {
        DailyCalorieSummaryCard(
            selectedDate = LocalDate(2023, 1, 5),
            totalCalories = 1850.0,
            onPreviousDay = {},
            onNextDay = {},
            onToday = {},
            onDateSelected = {}
        )
    }
}

@Preview(name = "Daily Calorie Summary - Under Budget")
@Composable
fun DailyCalorieSummaryCardUnderBudgetPreview() {
    MaterialTheme {
        DailyCalorieSummaryCard(
            selectedDate = LocalDate(2023, 1, 5),
            totalCalories = 1650.0,
            targetCalories = 2000.0,
            onPreviousDay = {},
            onNextDay = {},
            onToday = {},
            onDateSelected = {}
        )
    }
}

@Preview(name = "Daily Calorie Summary - Over Budget Alert")
@Composable
fun DailyCalorieSummaryCardOverBudgetPreview() {
    MaterialTheme {
        DailyCalorieSummaryCard(
            selectedDate = LocalDate(2023, 1, 5),
            totalCalories = 2350.0,
            targetCalories = 2000.0,
            onPreviousDay = {},
            onNextDay = {},
            onToday = {},
            onDateSelected = {}
        )
    }
}
