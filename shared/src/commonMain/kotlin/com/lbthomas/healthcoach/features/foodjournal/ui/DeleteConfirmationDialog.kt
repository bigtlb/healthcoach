package com.lbthomas.healthcoach.features.foodjournal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    note: String? = null,
    confirmButtonText: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .testTag("delete_confirmation_dialog")
            .onDialogKeyEvents(
                onConfirm = onConfirm,
                onDismiss = onDismiss
            ),
        title = {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(name = "Delete Confirmation - Master Food (with Note)")
@Composable
fun DeleteConfirmationDialogMasterFoodPreview() {
    DeleteConfirmationDialog(
        title = "Delete Master Food Item",
        message = "Are you sure you want to delete \"Rolled Oats\" from your food library?",
        note = "Note: Historical meal entries previously logged using this food will remain preserved.",
        confirmButtonText = "Delete",
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(name = "Delete Confirmation - Meal Entry (Simple)")
@Composable
fun DeleteConfirmationDialogMealEntryPreview() {
    DeleteConfirmationDialog(
        title = "Delete Meal Entry",
        message = "Are you sure you want to delete this logged breakfast entry?",
        note = null,
        confirmButtonText = "Delete",
        onConfirm = {},
        onDismiss = {}
    )
}
