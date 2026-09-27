package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

internal enum class SyncSubTab(val title: String) {
    STORAGE_AUTH("Storage & Auth"),
    SCHEDULE("Schedule"),
    DIAGNOSTICS("Diagnostics")
}

@Composable
internal fun SyncTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    var selectedSubTab by remember { mutableStateOf(SyncSubTab.STORAGE_AUTH) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Synchronization") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Database Sync",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Synchronize records across devices via cloud or local folders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.sync.syncEnabled,
                    onCheckedChange = { settingsViewModel.setSyncEnabled(it) }
                )
            }
        }

        if (settings.sync.syncEnabled) {
            SecondaryTabRow(
                selectedTabIndex = selectedSubTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                SyncSubTab.entries.forEach { subTab ->
                    Tab(
                        selected = selectedSubTab == subTab,
                        onClick = { selectedSubTab = subTab },
                        text = {
                            Text(
                                text = subTab.title,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }

            when (selectedSubTab) {
                SyncSubTab.STORAGE_AUTH -> SyncStorageAuthSubTab(settings, settingsViewModel)
                SyncSubTab.SCHEDULE -> SyncScheduleSubTab(settings, settingsViewModel)
                SyncSubTab.DIAGNOSTICS -> SyncDiagnosticsSubTab(settings)
            }
        }
    }
}

@Preview
@Composable
private fun SyncTabPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settings by settingsViewModel.settings.collectAsState()
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                SyncTabContent(
                    settings = settings,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
