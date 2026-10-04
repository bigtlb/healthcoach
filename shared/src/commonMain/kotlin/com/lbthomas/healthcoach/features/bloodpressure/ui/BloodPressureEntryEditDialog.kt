package com.lbthomas.healthcoach.features.bloodpressure.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.BloodPressureCategory
import com.lbthomas.healthcoach.core.ui.AppDatePickerDialog
import com.lbthomas.healthcoach.core.ui.AppTimePickerDialog
import com.lbthomas.healthcoach.core.ui.DatePickerField
import com.lbthomas.healthcoach.core.ui.TimePickerField
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.utils.*
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.datetime.*
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodPressureEntryEditDialog(
    initialDate: LocalDate = today,
    initialTime: LocalTime? = null,
    getEntriesForDateTime: (LocalDate, LocalTime?) -> Pair<BloodPressureEntryData?, BloodPressureEntryData?>,
    onConfirm: (BloodPressureEntryData) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var includeTime by remember { mutableStateOf(initialTime != null) }
    var selectedTime by remember { mutableStateOf(initialTime ?: nowLocal.time) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val effectiveTime = if (includeTime) selectedTime else null

    val (currentEntry, priorEntry) = remember(selectedDate, effectiveTime) {
        getEntriesForDateTime(selectedDate, effectiveTime)
    }

    val currentEntryId = currentEntry?.id ?: ""
    val isEditMode = currentEntry != null && currentEntryId.isNotEmpty() && currentEntryId != "0"
    val title = if (isEditMode) "Edit Blood Pressure" else "New Blood Pressure"

    var systolicFieldValue by remember(selectedDate, effectiveTime) {
        val initialText = if (currentEntry != null && currentEntry.systolic > 0) {
            currentEntry.systolic.toString()
        } else {
            ""
        }
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(0, initialText.length)
            )
        )
    }

    var diastolicFieldValue by remember(selectedDate, effectiveTime) {
        val initialText = if (currentEntry != null && currentEntry.diastolic > 0) {
            currentEntry.diastolic.toString()
        } else {
            ""
        }
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(0, initialText.length)
            )
        )
    }

    var pulseFieldValue by remember(selectedDate, effectiveTime) {
        val initialText = if (currentEntry != null && currentEntry.pulse != null && currentEntry.pulse > 0) {
            currentEntry.pulse.toString()
        } else {
            ""
        }
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(0, initialText.length)
            )
        )
    }

    val systolicPlaceholder = remember(priorEntry) {
        if (priorEntry != null && priorEntry.systolic > 0) {
            priorEntry.systolic.toString()
        } else {
            "120"
        }
    }

    val diastolicPlaceholder = remember(priorEntry) {
        if (priorEntry != null && priorEntry.diastolic > 0) {
            priorEntry.diastolic.toString()
        } else {
            "80"
        }
    }

    val pulsePlaceholder = remember(priorEntry) {
        if (priorEntry != null && priorEntry.pulse != null && priorEntry.pulse > 0) {
            priorEntry.pulse.toString()
        } else {
            "70"
        }
    }

    val digitsPattern = remember { Regex("""^\d{0,3}$""") }

    val parsedSystolic = systolicFieldValue.text.toIntOrNull()
    val parsedDiastolic = diastolicFieldValue.text.toIntOrNull()
    val parsedPulse = pulseFieldValue.text.toIntOrNull()

    val isSystolicValid = parsedSystolic != null && parsedSystolic in 30..350
    val isDiastolicValid = parsedDiastolic != null && parsedDiastolic in 20..250
    val isPulseValid = pulseFieldValue.text.isEmpty() || (parsedPulse != null && parsedPulse in 20..300)
    val isValid = isSystolicValid && isDiastolicValid && isPulseValid

    fun confirmIfValid() {
        val systolic = parsedSystolic ?: return
        val diastolic = parsedDiastolic ?: return
        if (isValid) {
            val storageDateTime = formatBpStorageString(selectedDate, effectiveTime)
            onConfirm(
                BloodPressureEntryData(
                    id = currentEntryId,
                    dateTime = storageDateTime,
                    systolic = systolic,
                    diastolic = diastolic,
                    pulse = parsedPulse
                )
            )
        }
    }

    val systolicInputFocusRequester = remember { FocusRequester() }
    val diastolicInputFocusRequester = remember { FocusRequester() }
    val pulseInputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        yield()
        runCatching {
            systolicInputFocusRequester.requestFocus()
        }
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
                DatePickerField(
                    selectedDate = selectedDate,
                    onClick = { showDatePicker = true }
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
                    TimePickerField(
                        selectedTime = selectedTime,
                        onClick = { showTimePicker = true }
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
                    placeholder = { Text(systolicPlaceholder) },
                    singleLine = true,
                    isError = systolicFieldValue.text.isNotEmpty() && !isSystolicValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { diastolicInputFocusRequester.requestFocus() }
                    ),
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
                    placeholder = { Text(diastolicPlaceholder) },
                    singleLine = true,
                    isError = diastolicFieldValue.text.isNotEmpty() && !isDiastolicValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { pulseInputFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(diastolicInputFocusRequester)
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
                    placeholder = { Text(pulsePlaceholder) },
                    singleLine = true,
                    isError = pulseFieldValue.text.isNotEmpty() && !isPulseValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { confirmIfValid() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(pulseInputFocusRequester)
                )

                // AHA Category indicator preview
                if (isSystolicValid && isDiastolicValid) {
                    val category = BloodPressureCategory.fromReadings(parsedSystolic, parsedDiastolic)
                    Tooltip(category.description) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = category.color.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, category.color)
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
        AppDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { newDate ->
                selectedDate = newDate
                showDatePicker = false
                coroutineScope.launch {
                    yield()
                    delay(50)
                    runCatching {
                        systolicInputFocusRequester.requestFocus()
                    }
                }
            },
            onDismiss = {
                showDatePicker = false
                coroutineScope.launch {
                    yield()
                    delay(50)
                    runCatching {
                        systolicInputFocusRequester.requestFocus()
                    }
                }
            }
        )
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            selectedTime = selectedTime,
            onTimeSelected = { newTime ->
                selectedTime = newTime
                showTimePicker = false
                coroutineScope.launch {
                    yield()
                    delay(50)
                    runCatching {
                        systolicInputFocusRequester.requestFocus()
                    }
                }
            },
            onDismiss = {
                showTimePicker = false
                coroutineScope.launch {
                    yield()
                    delay(50)
                    runCatching {
                        systolicInputFocusRequester.requestFocus()
                    }
                }
            }
        )
    }
}

@Preview(name = "Blood Pressure Entry Dialog")
@Composable
fun BloodPressureEntryEditDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            BloodPressureEntryEditDialog(
                initialDate = today,
                initialTime = LocalTime(15, 0),
                getEntriesForDateTime = { date, time ->
                    Pair(
                        BloodPressureEntryData(id = "1", dateTime = "2026-09-23T15:00:00Z", systolic = 120, diastolic = 80, pulse = 70),
                        BloodPressureEntryData(id = "2", dateTime = "2026-09-22T15:00:00Z", systolic = 125, diastolic = 82, pulse = 72)
                    )
                },
                onConfirm = {},
                onDismiss = {}
            )
        }
    )
}

@Preview(name = "Add Blood Pressure Dialog")
@Composable
fun BloodPressureAddDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            BloodPressureEntryEditDialog(
                initialDate = today,
                initialTime = null,
                getEntriesForDateTime = { _, _ -> Pair(null, null) },
                onConfirm = {},
                onDismiss = {}
            )
        }
    )
}
