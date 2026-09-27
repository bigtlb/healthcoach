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
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
internal fun WeightTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        WeightOptions(settings, settingsViewModel)
    }
}

@Composable
private fun WeightOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Weight Units") {
        WeightUnit.entries.forEach { unit ->
            val isSelected = settings.weight.unit == unit
            val label = when (unit) {
                WeightUnit.US -> "Pounds (lbs)"
                WeightUnit.METRIC -> "Kilograms (kg)"
            }
            SettingRadioRow(
                text = label,
                selected = isSelected,
                onClick = { settingsViewModel.setWeightUnit(unit) }
            )
        }
    }
}

@Preview
@Composable
private fun WeightTabPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settings by settingsViewModel.settings.collectAsState()
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                WeightTabContent(
                    settings = settings,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
