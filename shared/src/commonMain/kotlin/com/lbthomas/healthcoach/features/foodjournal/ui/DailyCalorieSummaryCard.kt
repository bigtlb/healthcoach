package com.lbthomas.healthcoach.features.foodjournal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.utils.DOW
import com.lbthomas.healthcoach.core.utils.displayDate
import com.lbthomas.healthcoach.core.utils.today
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
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
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

            // Bottom Row: Calorie Consumption & Budget Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                if (targetCalories != null) {
                    val remaining = targetCalories - totalCalories
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Target Budget: ${formatCalories(targetCalories)} kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}

private fun formatCalories(calories: Double): String {
    return if (calories % 1.0 == 0.0) {
        calories.toLong().toString()
    } else {
        ((calories * 10).toLong() / 10.0).toString()
    }
}

@Preview(name = "Daily Calorie Summary - Standard")
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
