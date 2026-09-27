package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
internal fun BloodPressureTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        BloodPressureOptions(settings, settingsViewModel)
    }
}

@Composable
private fun BloodPressureOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Blood Pressure Display Options") {
        SettingCheckboxRow(
            text = "Include Pulse in Graph",
            checked = settings.bloodPressure.showPulseInGraph,
            onCheckedChange = { checked ->
                settingsViewModel.setShowPulseInGraph(checked)
            }
        )
        SettingCheckboxRow(
            text = "Show Daily Averages",
            checked = settings.bloodPressure.showDailyAverages,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(bloodPressure = it.bloodPressure.copy(showDailyAverages = checked)) }
            }
        )
        SettingCheckboxRow(
            text = "Show Monthly Averages",
            checked = settings.bloodPressure.showMonthlyAverages,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(bloodPressure = it.bloodPressure.copy(showMonthlyAverages = checked)) }
            }
        )
        SettingCheckboxRow(
            text = "Show Daily Changes",
            checked = settings.bloodPressure.showDailyChanges,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(bloodPressure = it.bloodPressure.copy(showDailyChanges = checked)) }
            }
        )
        SettingCheckboxRow(
            text = "Show Monthly Changes",
            checked = settings.bloodPressure.showMonthlyChanges,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(bloodPressure = it.bloodPressure.copy(showMonthlyChanges = checked)) }
            }
        )
    }
}

@Preview
@Composable
private fun BloodPressureTabPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settings by settingsViewModel.settings.collectAsState()
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                BloodPressureTabContent(
                    settings = settings,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
