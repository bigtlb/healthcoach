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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.features.settings.data.PeerSyncSettings
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SyncSettings
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncActionButton(
    syncViewModel: SyncViewModel,
    modifier: Modifier = Modifier
) {
    val syncState by syncViewModel.syncState.collectAsState()
    val settings by syncViewModel.settings.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    if (!settings.sync.syncEnabled) {
        return
    }

    val isServerHost = settings.sync.syncProvider == SyncProviderType.PEER_TO_PEER &&
        (settings.peerSync.isServerMode || settings.peerSync.localServerEnabled)
    val isSyncing = syncState is SyncState.Syncing
    val hasError = !isServerHost && (syncState is SyncState.Error || settings.sync.lastSyncFailed)
    val hostDeviceName = settings.peerSync.deviceName.ifBlank { "HealthCoach Host" }

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
            isServerHost -> "Currently hosting as Peer-to-Peer server ($hostDeviceName)\nClients synchronize directly with this device"
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
                enabled = !isServerHost,
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
                    contentDescription = if (isServerHost) "P2P Host ($hostDeviceName)" else "Sync",
                    modifier = Modifier.rotate(if (isSyncing) rotation else 0f),
                    tint = when {
                        isServerHost -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        hasError && !isSyncing -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
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

@Preview(name = "Sync Button - Idle")
@Composable
fun SyncActionButtonIdlePreview() {
    val settings = SettingsData(
        sync = SyncSettings(syncEnabled = true)
    )
    val viewModel = SyncViewModel(
        syncState = MutableStateFlow(SyncState.Idle),
        settings = MutableStateFlow(settings)
    )
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        SyncActionButton(syncViewModel = viewModel)
    }
}

@Preview(name = "Sync Button - Syncing")
@Composable
fun SyncActionButtonSyncingPreview() {
    val settings = SettingsData(
        sync = SyncSettings(syncEnabled = true)
    )
    val viewModel = SyncViewModel(
        syncState = MutableStateFlow(SyncState.Syncing),
        settings = MutableStateFlow(settings)
    )
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        SyncActionButton(syncViewModel = viewModel)
    }
}

@Preview(name = "Sync Button - Error State")
@Composable
fun SyncActionButtonErrorPreview() {
    val settings = SettingsData(
        sync = SyncSettings(syncEnabled = true, lastSyncFailed = true)
    )
    val viewModel = SyncViewModel(
        syncState = MutableStateFlow(SyncState.Error("Connection timed out")),
        settings = MutableStateFlow(settings)
    )
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        SyncActionButton(syncViewModel = viewModel)
    }
}

@Preview(name = "Sync Button - P2P Server Host")
@Composable
fun SyncActionButtonServerModePreview() {
    val settings = SettingsData(
        sync = SyncSettings(syncEnabled = true, syncProvider = SyncProviderType.PEER_TO_PEER),
        peerSync = PeerSyncSettings(isServerMode = true, deviceName = "Living Room Hub")
    )
    val viewModel = SyncViewModel(
        syncState = MutableStateFlow(SyncState.Idle),
        settings = MutableStateFlow(settings)
    )
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        SyncActionButton(syncViewModel = viewModel)
    }
}
