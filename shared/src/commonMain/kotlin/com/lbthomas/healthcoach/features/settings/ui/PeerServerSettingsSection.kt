package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.utils.formatEpochMillis
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
internal fun PeerServerSettingsSection(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    val serverStatus by settingsViewModel.serverStatus.collectAsState()
    var portInput by remember(settings.peerSync.localServerPort) {
        mutableStateOf(settings.peerSync.localServerPort.toString())
    }
    var deviceNameInput by remember(settings.peerSync.deviceName) {
        mutableStateOf(settings.peerSync.deviceName)
    }
    val portFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Embedded Peer Server (LAN Host)") {
            // Enable Server Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Local Sync Server",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Allow other HealthCoach devices on this Wi-Fi network to discover and sync with this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.peerSync.localServerEnabled,
                    onCheckedChange = { settingsViewModel.setPeerServerEnabled(it) }
                )
            }

            // Server Status Banner
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (serverStatus.isStopping || serverStatus.isStarting) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                } else if (serverStatus.isRunning) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else if (!serverStatus.errorMessage.isNullOrBlank()) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                border = BorderStroke(
                    1.dp,
                    if (serverStatus.isStopping || serverStatus.isStarting) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    else if (serverStatus.isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else if (!serverStatus.errorMessage.isNullOrBlank()) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (serverStatus.isStopping || serverStatus.isStarting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        Icon(
                            imageVector = if (serverStatus.isRunning) Icons.Default.CheckCircle
                            else if (!serverStatus.errorMessage.isNullOrBlank()) Icons.Default.Error
                            else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (serverStatus.isRunning) MaterialTheme.colorScheme.primary
                            else if (!serverStatus.errorMessage.isNullOrBlank()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (serverStatus.isStopping) "Stopping Peer Server..."
                            else if (serverStatus.isStarting) "Starting Peer Server..."
                            else if (serverStatus.isRunning) "Server Active & Advertising"
                            else if (!serverStatus.errorMessage.isNullOrBlank()) "Server Error: ${serverStatus.errorMessage}"
                            else "Server Inactive",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (serverStatus.isStopping) {
                            Text(
                                text = "Closing network connections and unregistering services...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (serverStatus.isStarting) {
                            Text(
                                text = "Binding port and starting mDNS advertising...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (serverStatus.isRunning) {
                            Text(
                                text = "Listening on port ${serverStatus.port} • Local Node ID: ${settings.peerSync.instanceId.take(8)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Local Node Identity / Device Name
            OutlinedTextField(
                value = deviceNameInput,
                onValueChange = {
                    deviceNameInput = it
                    settingsViewModel.setDeviceName(it)
                },
                label = { Text("Advertised Device Name") },
                placeholder = { Text("e.g. Thomas's Laptop") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { portFocusRequester.requestFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Port Configuration
            OutlinedTextField(
                value = portInput,
                onValueChange = {
                    portInput = it
                    it.toIntOrNull()?.let { validPort ->
                        if (validPort in 1024..65535) {
                            settingsViewModel.setPeerServerPort(validPort)
                        }
                    }
                },
                label = { Text("Server Port (Default: 8765)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(portFocusRequester)
            )
        }

        // Connected Clients History Section
        SettingsSection(title = "Client Access History") {
            if (settings.peerSync.localServerHistory.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No peer client devices have connected yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    settings.peerSync.localServerHistory.take(5).forEach { record ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.clientName.ifBlank { "Client (${record.clientInstanceId.take(8)})" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "IP: ${record.ipAddress.ifBlank { "LAN" }} • ${formatEpochMillis(record.lastAccessTimestamp)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = record.lastAction,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { settingsViewModel.clearServerHistory() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear History")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PeerServerSettingsSectionPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settings by settingsViewModel.settings.collectAsState()
            Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                PeerServerSettingsSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
