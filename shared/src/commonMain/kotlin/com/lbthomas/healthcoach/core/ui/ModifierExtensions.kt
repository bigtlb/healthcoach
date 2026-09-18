package com.lbthomas.healthcoach.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*

/**
 * Common modifier extension for handling Enter and Escape key events in dialogs.
 *
 * @param onConfirm Callback executed when Enter or NumPadEnter is pressed.
 * @param onDismiss Callback executed when Escape is pressed.
 */
fun Modifier.onDialogKeyEvents(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
): Modifier = this.onKeyEvent { keyEvent ->
    if (keyEvent.type == KeyEventType.KeyDown) {
        when (keyEvent.key) {
            Key.Escape -> {
                onDismiss()
                true
            }
            Key.Enter, Key.NumPadEnter -> {
                onConfirm()
                true
            }
            else -> false
        }
    } else {
        false
    }
}
