package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
internal fun SyncScheduleSubTab(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Background Sync") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = settings.sync.autoSyncOnClose,
                    onCheckedChange = { settingsViewModel.setAutoSyncOnClose(it) }
                )
                Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                Text(
                    text = "Auto-sync on application close/exit",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Periodic Auto-Sync",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            val intervals = listOf(
                0 to "Disabled (Manual Only)",
                15 to "Every 15 minutes",
                30 to "Every 30 minutes",
                60 to "Every 60 minutes"
            )

            intervals.forEach { (mins, label) ->
                val isSelected = settings.sync.autoSyncIntervalMinutes == mins
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = { settingsViewModel.setAutoSyncIntervalMinutes(mins) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview
@Composable
private fun SyncScheduleSubTabPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settings by settingsViewModel.settings.collectAsState()
            Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SyncScheduleSubTab(
                    settings = settings,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
