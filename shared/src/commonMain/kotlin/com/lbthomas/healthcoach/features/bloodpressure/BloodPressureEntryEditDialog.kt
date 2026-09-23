package com.lbthomas.healthcoach.features.bloodpressure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.BloodPressureCategory
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.utils.displayDate
import com.lbthomas.healthcoach.core.utils.formatBpStorageString
import com.lbthomas.healthcoach.core.utils.nowLocal
import com.lbthomas.healthcoach.core.utils.today
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
    val isNew = entry.id == 0L
    val title = if (isNew) "Add Blood Pressure Entry" else "Edit Blood Pressure Entry"

    var selectedDate by remember { mutableStateOf(if (isNew) today else entry.date) }
    var includeTime by remember { mutableStateOf(if (isNew) false else entry.hasTime) }

    val initialTime = entry.time ?: nowLocal.time
    var hourText by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialTime.hour.toString().padStart(2, '0'),
                selection = TextRange(2)
            )
        )
    }
    var minuteText by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialTime.minute.toString().padStart(2, '0'),
                selection = TextRange(2)
            )
        )
    }

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

    val digitsPattern = remember { Regex("""^\d{0,3}$""") }
    val timeDigitsPattern = remember { Regex("""^\d{0,2}$""") }

    val parsedSystolic = systolicFieldValue.text.toIntOrNull()
    val parsedDiastolic = diastolicFieldValue.text.toIntOrNull()
    val parsedPulse = pulseFieldValue.text.toIntOrNull()

    val parsedHour = hourText.text.toIntOrNull()
    val parsedMinute = minuteText.text.toIntOrNull()

    val isTimeValid = !includeTime || (parsedHour != null && parsedHour in 0..23 && parsedMinute != null && parsedMinute in 0..59)
    val isSystolicValid = parsedSystolic != null && parsedSystolic in 30..350
    val isDiastolicValid = parsedDiastolic != null && parsedDiastolic in 20..250
    val isPulseValid = pulseFieldValue.text.isEmpty() || (parsedPulse != null && parsedPulse in 20..300)
    val isValid = isSystolicValid && isDiastolicValid && isTimeValid && isPulseValid

    fun confirmIfValid() {
        if (isValid) {
            val finalTime = if (includeTime && parsedHour != null && parsedMinute != null) {
                LocalTime(parsedHour, parsedMinute, 0)
            } else {
                null
            }
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
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Selection
                if (isNew) {
                    EnterDateValue(
                        selectedDate = selectedDate,
                        onShowDatePicker = { showDatePicker = true }
                    )
                } else {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { input ->
                                if (input.text.isEmpty() || timeDigitsPattern.matches(input.text)) {
                                    hourText = input
                                }
                            },
                            label = { Text("Hour (00-23)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Text(":", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { input ->
                                if (input.text.isEmpty() || timeDigitsPattern.matches(input.text)) {
                                    minuteText = input
                                }
                            },
                            label = { Text("Min (00-59)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
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

@Preview
@Composable
fun BloodPressureEntryEditDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            BloodPressureEntryEditDialog(
                entry = BloodPressureEntryData(id = 0, dateTime = "2026-09-23T15:00:00Z", systolic = 120, diastolic = 80, pulse = 70),
                onConfirm = {},
                onDismiss = {}
            )
        }
    )
}
