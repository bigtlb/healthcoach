package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.utils.formatEpochMillis
import com.lbthomas.healthcoach.features.settings.data.SettingsData

@Composable
internal fun SyncDiagnosticsSubTab(
    settings: SettingsData
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Audit & Diagnostics") {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Configured Provider: ${when (settings.sync.syncProvider) { SyncProviderType.GOOGLE_DRIVE -> "Google Drive (appDataFolder)"; SyncProviderType.PEER_TO_PEER -> "Peer-to-Peer LAN"; else -> "Local Folder (/com.lbthomas.healthcoach)" }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Remote File: ${settings.sync.remoteFileName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Last Synced: ${settings.sync.lastSyncTime?.let { formatEpochMillis(it) } ?: "Never"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Remote Snapshot Hash: ${settings.sync.lastSyncHash ?: "None"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Last Sync Status: ${settings.sync.lastSyncStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (settings.sync.lastSyncFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Database Schema: Version 3 (PRAGMA user_version = 3)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (settings.sync.lastSyncFailed || !settings.sync.lastSyncError.isNullOrBlank()) {
            SettingsSection(title = "Error Diagnostics") {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Sync Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Detailed Error Report",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Failure Category: ${settings.sync.lastSyncStatus}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Text(
                            text = "Error Message:\n${settings.sync.lastSyncError ?: "Unknown error occurred during sync."}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Troubleshooting & Log Advice:\n• Check provider directory access and network connection.\n• Inspect application logs for the full stack trace:\n  - Desktop: healthcoach.log (in settings directory)\n  - Android: LogCat (tag: HealthCoach)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        SettingsSection(title = "Application Logging") {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• Desktop (JVM): Logs to console and rolling file `healthcoach.log` in app data directory.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Android: Logs to Android system LogCat (tag: HealthCoach / Kermit).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SyncDiagnosticsSubTabPreview() {
    Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        SyncDiagnosticsSubTab(settings = SettingsData())
    }
}
