package com.lbthomas.healthcoach.features.sync

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.features.settings.data.SettingsData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncActionButton(
    syncViewModel: SyncViewModel,
    modifier: Modifier = Modifier
) {
    val syncState by syncViewModel.syncState.collectAsState()
    val settings by syncViewModel.settings.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    if (!settings.syncEnabled) {
        return
    }

    val isSyncing = syncState is SyncState.Syncing
    val hasError = syncState is SyncState.Error || settings.lastSyncFailed

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Tooltip(
        tooltip = when {
            isSyncing -> "Syncing in progress\nClick to cancel"
            hasError -> "Last sync failed\nClick to retry"
            else -> "Sync database"
        }
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    if (isSyncing) {
                        showCancelDialog = true
                    } else {
                        syncViewModel.syncNow()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    modifier = Modifier.rotate(if (isSyncing) rotation else 0f),
                    tint = if (hasError && !isSyncing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            if (hasError && !isSyncing) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = "Sync error",
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Sync?") },
            text = { Text("Are you sure you want to cancel the database synchronization currently in progress?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        syncViewModel.cancelSync()
                    }
                ) {
                    Text("Cancel Sync", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Syncing")
                }
            }
        )
    }
}
