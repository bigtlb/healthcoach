package com.lbthomas.healthcoach.features.weight

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
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun DeleteConfirmation(
    entry: WeightEntryData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        yield()
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .testTag("delete_confirmation_dialog")
            .onDialogKeyEvents(
                onConfirm = onConfirm,
                onDismiss = onDismiss
            ),
        title = { Text("Delete Entry") },
        text = { Text("Are you sure you want to delete the weight entry for ${entry.date}?") },
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
fun DeleteConfirmationPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            DeleteConfirmation(
                entry = WeightEntryData(id = 1, date = today, weight = 70.0),
                onConfirm = {},
                onDismiss = {}
            )
        }
    )
}
