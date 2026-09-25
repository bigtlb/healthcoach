package com.lbthomas.healthcoach.features.bloodpressure

import androidx.compose.foundation.focusable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.yield
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.utils.formatTime
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun DeleteBloodPressureConfirmation(
    entry: BloodPressureEntryData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        yield()
        focusRequester.requestFocus()
    }

    val timeSuffix = if (entry.hasTime) " at ${entry.time?.formatTime()}" else ""
    val message = "Are you sure you want to delete the blood pressure entry for ${entry.date}$timeSuffix (${entry.systolic}/${entry.diastolic} mmHg)?"

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .testTag("delete_blood_pressure_confirmation_dialog")
            .onDialogKeyEvents(
                onConfirm = onConfirm,
                onDismiss = onDismiss
            ),
        title = { Text("Delete Entry") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
fun DeleteBloodPressureConfirmationPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            DeleteBloodPressureConfirmation(
                entry = BloodPressureEntryData(id = "1", dateTime = "2026-09-23T15:00:00Z", systolic = 120, diastolic = 80, pulse = 70),
                onConfirm = {},
                onDismiss = {}
            )
        }
    )
}
