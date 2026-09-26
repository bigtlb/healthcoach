package com.lbthomas.healthcoach.core.ui

import androidx.compose.runtime.Composable

/**
 * Provides a platform-appropriate directory picker launcher.
 *
 * @param onDirectorySelected Callback invoked with the selected absolute directory path or URI.
 * @return A lambda function that, when invoked, triggers the platform folder picker dialog.
 */
@Composable
expect fun rememberDirectoryPicker(onDirectorySelected: (String) -> Unit): () -> Unit
