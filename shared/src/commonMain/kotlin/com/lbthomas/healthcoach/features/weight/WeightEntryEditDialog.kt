package com.lbthomas.healthcoach.features.weight

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun WeightEntryEditDialog(
    entry: WeightEntryData,
    onConfirm: (WeightEntryData) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (entry.id == 0L) "Add Weight Entry" else "Edit Weight Entry"

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .focusable()
            .testTag("weight_entry_edit_dialog")
            .onDialogKeyEvents(
                onConfirm = { onConfirm(entry) },
                onDismiss = onDismiss
            ),
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Date: ${entry.date}")
                Text("Weight: ${entry.weight}")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(entry) }) {
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
fun WeightEntryEditDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            WeightEntryEditDialog(
                entry = WeightEntryData(id = 1, date = today, weight = 75.0),
                onConfirm = {},
                onDismiss = {}
            )
        }
    )
}
