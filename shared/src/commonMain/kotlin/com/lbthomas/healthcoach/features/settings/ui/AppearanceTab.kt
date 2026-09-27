package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.theme.AppTheme
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
internal fun AppearanceTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        ThemePaletteOptions(settings, settingsViewModel)
        ThemeModeOptions(settings, settingsViewModel)
        AdaptiveDisplayOptions(settings, settingsViewModel)
    }
}

@Composable
private fun ThemePaletteOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Color Theme") {
        AppTheme.entries.forEach { theme ->
            val isSelected = settings.appearance.appTheme == theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        onClick = { settingsViewModel.setAppTheme(theme) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(color = theme.previewPrimary, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ThemeModeOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Theme Mode") {
        ThemeMode.entries.forEach { mode ->
            val isSelected = settings.appearance.themeMode == mode
            SettingRadioRow(
                text = mode.displayName,
                selected = isSelected,
                onClick = { settingsViewModel.setThemeMode(mode) }
            )
        }
    }
}

@Composable
private fun AdaptiveDisplayOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Display Options") {
        SettingCheckboxRow(
            text = "Adaptive Display",
            checked = settings.appearance.adaptiveDisplay,
            onCheckedChange = { checked ->
                settingsViewModel.setAdaptiveDisplay(checked)
            }
        )
    }
}

@Preview
@Composable
private fun AppearanceTabPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settings by settingsViewModel.settings.collectAsState()
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                AppearanceTabContent(
                    settings = settings,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
