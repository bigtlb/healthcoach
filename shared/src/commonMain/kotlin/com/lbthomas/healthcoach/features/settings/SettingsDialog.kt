package com.lbthomas.healthcoach.features.settings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

private object SettingsDialogDefaults {
    val DialogPadding = 8.dp
    val SectionSpacing = 16.dp
    val ItemSpacing = 8.dp
    val SectionIndent = 8.dp
    val RowPadding = 4.dp
    val LabelStartPadding = 4.dp
}

@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .testTag("settings_dialog")
            .onDialogKeyEvents(
                onConfirm = onDismiss,
                onDismiss = onDismiss
            ),
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(SettingsDialogDefaults.DialogPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
            ) {
                // Theme Section
                ThemeOptions(settings, settingsViewModel)

                // Weight Units Section
                WeightOptions(settings, settingsViewModel)

                // Blood Pressure Display Settings Section
                BloodPressureOptions(settings, settingsViewModel)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun BloodPressureOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Blood Pressure Display Options") {
        SettingCheckboxRow(
            text = "Show Daily Averages",
            checked = settings.showDailyAverages,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(showDailyAverages = checked) }
            }
        )
        SettingCheckboxRow(
            text = "Show Monthly Averages",
            checked = settings.showMonthlyAverages,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(showMonthlyAverages = checked) }
            }
        )
        SettingCheckboxRow(
            text = "Show Daily Changes",
            checked = settings.showDailyChanges,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(showDailyChanges = checked) }
            }
        )
        SettingCheckboxRow(
            text = "Show Monthly Changes",
            checked = settings.showMonthlyChanges,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(showMonthlyChanges = checked) }
            }
        )
    }
}

@Composable
private fun ThemeOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Theme") {
        ThemeMode.entries.forEach { mode ->
            val isSelected = settings.themeMode == mode
            SettingRadioRow(
                text = mode.displayName,
                selected = isSelected,
                onClick = { settingsViewModel.setThemeMode(mode) }
            )
        }
    }
}

@Composable
private fun WeightOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Weight Units") {
        WeightUnit.entries.forEach { unit ->
            val isSelected = settings.weightUnit == unit
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

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.ItemSpacing)
    ) {
        Text(text = title)
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(start = SettingsDialogDefaults.SectionIndent),
            verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.ItemSpacing),
            content = content
        )
    }
}

@Composable
private fun SettingRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .wrapContentSize()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(SettingsDialogDefaults.RowPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = SettingsDialogDefaults.LabelStartPadding)
        )
    }
}

@Composable
private fun SettingCheckboxRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .wrapContentSize()
            .selectable(
                selected = checked,
                onClick = { onCheckedChange(!checked) },
                role = Role.Checkbox
            )
            .padding(SettingsDialogDefaults.RowPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = SettingsDialogDefaults.LabelStartPadding)
        )
    }
}

@Preview
@Composable
fun SettingsDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule)}),
        content = {
            val settingsViewModel = koinInject<SettingsViewModel>()

            // Create a preview of the SettingsDialog
            SettingsDialog(
                settingsViewModel = settingsViewModel,
                onDismiss = {}
            )
        })
}

