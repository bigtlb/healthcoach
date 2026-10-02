package com.lbthomas.healthcoach.features.weight.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.ui.AppDatePickerDialog
import com.lbthomas.healthcoach.core.ui.DatePickerField
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.coroutines.yield
import kotlinx.datetime.LocalDate
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Suppress("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightEntryEditDialog(
    entry: WeightEntryData,
    onConfirm: (WeightEntryData) -> Unit,
    onDismiss: () -> Unit,
    settings: SettingsData,
    modifier: Modifier = Modifier
) {
    val title = if (entry.id.isEmpty() || entry.id == "0") "New Weight" else "Edit Weight"
    val units = if (settings.weight.unit == WeightUnit.METRIC) "kgs" else "lbs"

    var selectedDate by remember { mutableStateOf(entry.date) }
    val initialText = if (entry.weight > 0.0) String.format("%.1f",entry.getWeightInCurrentUnits(settings.weight.unit)) else ""
    var weightFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(0, initialText.length)
            )
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // Matches up to 3 digits before decimal, optional decimal point and up to 1 digit after: nnn or nnn.n
    val weightPattern = remember { Regex("""^\d{0,3}(\.\d{0,1})?$""") }

    val parsedWeight = weightFieldValue.text.toDoubleOrNull()
    val isWeightValid = parsedWeight != null && parsedWeight > 0.0

    fun confirmIfValid() {
        if (isWeightValid) {
            onConfirm(entry.copy(id = entry.id, date = selectedDate, weight = entry.convertToKilograms(parsedWeight, settings.weight.unit)))
        }
    }

    val weightInputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(entry) {
        yield()
        weightInputFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .testTag("weight_entry_edit_dialog")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (entry.id.isEmpty() || entry.id == "0") {
                    // Date Selection
                    DatePickerField(
                        selectedDate = selectedDate,
                        onClick = { showDatePicker = true }
                    )
                } else {
                    Text(text = entry.date.toString())
                }

                // Weight Input Field (nnn.n)
                EnterWeightValue(
                    weightFieldValue = weightFieldValue,
                    onValueChanged = { weightFieldValue = it },
                    weightPattern = weightPattern,
                    units = units,
                    isWeightValid = isWeightValid,
                    weightInputFocusRequester = weightInputFocusRequester,
                    onConfirm = { confirmIfValid() }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirmIfValid() },
                enabled = isWeightValid
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
            },
            onDismiss = {
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun EnterWeightValue(
    weightFieldValue: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
    weightPattern: Regex,
    units: String,
    isWeightValid: Boolean,
    weightInputFocusRequester: FocusRequester,
    onConfirm: () -> Unit
) {
    OutlinedTextField(
        value = weightFieldValue,
        onValueChange = { input ->
            if (input.text.isEmpty() || weightPattern.matches(input.text)) {
               onValueChanged(input)
            }
        },
        label = { Text("Weight ($units)") },
        placeholder = { Text("000.0") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onConfirm() }
        ),
        isError = weightFieldValue.text.isNotEmpty() && !isWeightValid,
        supportingText = {
            if (weightFieldValue.text.isNotEmpty() && !isWeightValid) {
                Text("Enter a valid weight (e.g. 175.5)")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(weightInputFocusRequester)
    )
}

@Preview(name = "Editable Weight")
@Composable
fun WeightEntryEditDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            WeightEntryEditDialog(
                entry = WeightEntryData(id = "1", date = today, weight = 75.0),
                onConfirm = {},
                onDismiss = {},
                koinInject<SettingsViewModel>().settings.collectAsState().value
            )
        }
    )
}

@Preview(name = "Add Weight")
@Composable
fun WeightEntryAddDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            WeightEntryEditDialog(
                entry = WeightEntryData(id = "0", date = today, weight = 75.0),
                onConfirm = {},
                onDismiss = {},
                koinInject<SettingsViewModel>().settings.collectAsState().value
            )
        }
    )
}
