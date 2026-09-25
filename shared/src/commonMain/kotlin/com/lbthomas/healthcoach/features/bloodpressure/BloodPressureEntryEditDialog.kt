package com.lbthomas.healthcoach.features.bloodpressure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.BloodPressureCategory
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.utils.*
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import kotlinx.coroutines.yield
import kotlinx.datetime.*
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodPressureEntryEditDialog(
    entry: BloodPressureEntryData,
    onConfirm: (BloodPressureEntryData) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNew = entry.id.isEmpty() || entry.id == "0"
    val title = if (isNew) "New Blood Pressure" else "Edit Blood Pressure"

    var selectedDate by remember { mutableStateOf(if (isNew) today else entry.date) }
    var includeTime by remember { mutableStateOf(if (isNew) false else entry.hasTime) }
    var selectedTime by remember { mutableStateOf(entry.time ?: nowLocal.time) }

    val initialSystolic = if (entry.systolic > 0) entry.systolic.toString() else ""
    var systolicFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialSystolic,
                selection = TextRange(initialSystolic.length)
            )
        )
    }

    val initialDiastolic = if (entry.diastolic > 0) entry.diastolic.toString() else ""
    var diastolicFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialDiastolic,
                selection = TextRange(initialDiastolic.length)
            )
        )
    }

    val initialPulse = entry.pulse?.toString() ?: ""
    var pulseFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialPulse,
                selection = TextRange(initialPulse.length)
            )
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val digitsPattern = remember { Regex("""^\d{0,3}$""") }

    val parsedSystolic = systolicFieldValue.text.toIntOrNull()
    val parsedDiastolic = diastolicFieldValue.text.toIntOrNull()
    val parsedPulse = pulseFieldValue.text.toIntOrNull()

    val isSystolicValid = parsedSystolic != null && parsedSystolic in 30..350
    val isDiastolicValid = parsedDiastolic != null && parsedDiastolic in 20..250
    val isPulseValid = pulseFieldValue.text.isEmpty() || (parsedPulse != null && parsedPulse in 20..300)
    val isValid = isSystolicValid && isDiastolicValid && isPulseValid

    fun confirmIfValid() {
        if (isValid) {
            val finalTime = if (includeTime) selectedTime else null
            val storageDateTime = formatBpStorageString(selectedDate, finalTime)
            onConfirm(
                entry.copy(
                    id = entry.id,
                    dateTime = storageDateTime,
                    systolic = parsedSystolic,
                    diastolic = parsedDiastolic,
                    pulse = parsedPulse
                )
            )
        }
    }

    val systolicInputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(entry) {
        yield()
        systolicInputFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .testTag("blood_pressure_entry_edit_dialog")
            .onDialogKeyEvents(
                onConfirm = { confirmIfValid() },
                onDismiss = onDismiss
            ),
        title = { Text(text = title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Selection
                EnterDateValue(
                    selectedDate = selectedDate,
                    onShowDatePicker = { showDatePicker = true }
                )

                // Optional Time Component
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeTime,
                        onCheckedChange = { includeTime = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Include time",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (includeTime) {
                    EnterTimeValue(
                        selectedTime = selectedTime,
                        onShowTimePicker = { showTimePicker = true }
                    )
                }

                // Systolic Input Field
                OutlinedTextField(
                    value = systolicFieldValue,
                    onValueChange = { input ->
                        if (input.text.isEmpty() || digitsPattern.matches(input.text)) {
                            systolicFieldValue = input
                        }
                    },
                    label = { Text("Systolic (mmHg)") },
                    singleLine = true,
                    isError = systolicFieldValue.text.isNotEmpty() && !isSystolicValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(systolicInputFocusRequester)
                )

                // Diastolic Input Field
                OutlinedTextField(
                    value = diastolicFieldValue,
                    onValueChange = { input ->
                        if (input.text.isEmpty() || digitsPattern.matches(input.text)) {
                            diastolicFieldValue = input
                        }
                    },
                    label = { Text("Diastolic (mmHg)") },
                    singleLine = true,
                    isError = diastolicFieldValue.text.isNotEmpty() && !isDiastolicValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Pulse Input Field
                OutlinedTextField(
                    value = pulseFieldValue,
                    onValueChange = { input ->
                        if (input.text.isEmpty() || digitsPattern.matches(input.text)) {
                            pulseFieldValue = input
                        }
                    },
                    label = { Text("Pulse bpm (optional)") },
                    singleLine = true,
                    isError = pulseFieldValue.text.isNotEmpty() && !isPulseValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // AHA Category indicator preview
                if (isSystolicValid && isDiastolicValid) {
                    val category = BloodPressureCategory.fromReadings(parsedSystolic, parsedDiastolic)
                    Tooltip(category.description) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = category.color.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, category.color)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(category.color, RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category.title,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirmIfValid() },
                enabled = isValid
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        BpDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { newDate ->
                selectedDate = newDate
                showDatePicker = false
            },
            onDismiss = {
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        BpTimePickerDialog(
            selectedTime = selectedTime,
            onTimeSelected = { newTime ->
                selectedTime = newTime
                showTimePicker = false
            },
            onDismiss = {
                showTimePicker = false
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EnterDateValue(
    selectedDate: LocalDate,
    onShowDatePicker: () -> Unit
) {
    OutlinedCard(
        onClick = onShowDatePicker,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.displayDate(),
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Select date"
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EnterTimeValue(
    selectedTime: LocalTime,
    onShowTimePicker: () -> Unit
) {
    OutlinedCard(
        onClick = onShowTimePicker,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedTime.displayTime(),
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Select time"
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BpDatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var dateChoice = selectedDate

    val initialEpochMillis = remember(dateChoice) {
        dateChoice.atTime(0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpochMillis)

    DatePickerDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dateChoice = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC)
                            .date
                    }
                    onDateSelected(dateChoice)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BpTimePickerDialog(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime(timePickerState.hour, timePickerState.minute, 0))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        }
    )
}

@Preview(name = "Blood Pressure Entry Dialog")
@Composable
fun BloodPressureEntryEditDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            BloodPressureEntryEditDialog(
                entry = BloodPressureEntryData(id = "0", dateTime = "2026-09-23T15:00:00Z", systolic = 120, diastolic = 80, pulse = 70),
                onConfirm = {},
                onDismiss = {}
            )
        }
    )
}


@Preview(name = "Blood Pressure Entry With Time")
@Composable
fun BloodPressureEntryWTimeEditDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            BloodPressureEntryEditDialog(
                entry = BloodPressureEntryData(id = "1", dateTime = "2026-09-23T15:00:00Z", systolic = 120, diastolic = 80, pulse = 70),
                onConfirm = {},
                onDismiss = {}
            )
        }
    )
}

@Preview(name = "Blood Pressure Time Picker Dialog")
@Composable
fun BloodPressureTimePickerDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            BpTimePickerDialog(
                selectedTime = LocalTime(11, 0, 0),
                onTimeSelected = {},
                onDismiss = {}
            )
        }
    )
}
