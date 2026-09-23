package com.lbthomas.healthcoach.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.yield
import com.lbthomas.healthcoach.core.AppInfo
import com.lbthomas.healthcoach.core.OpenSourceAttribution
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.theme.AppTheme
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

private enum class SettingsTab(val title: String) {
    APPEARANCE("Appearance"),
    WEIGHT("Weight"),
    BLOOD_PRESSURE("Blood Pressure"),
    ABOUT("About")
}

@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var selectedTab by remember { mutableStateOf(SettingsTab.APPEARANCE) }

    LaunchedEffect(Unit) {
        yield()
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(min = 380.dp, max = 520.dp)
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
                    .fillMaxWidth()
                    .padding(SettingsDialogDefaults.DialogPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.title) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    when (selectedTab) {
                        SettingsTab.APPEARANCE -> AppearanceTabContent(settings, settingsViewModel)
                        SettingsTab.WEIGHT -> WeightTabContent(settings, settingsViewModel)
                        SettingsTab.BLOOD_PRESSURE -> BloodPressureTabContent(settings, settingsViewModel)
                        SettingsTab.ABOUT -> AboutTabContent()
                    }
                }
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
private fun AppearanceTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        ThemePaletteOptions(settings, settingsViewModel)
        ThemeModeOptions(settings, settingsViewModel)
        AdaptiveDisplayOptions(settings, settingsViewModel)
    }
}

@Composable
private fun WeightTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        WeightOptions(settings, settingsViewModel)
    }
}

@Composable
private fun BloodPressureTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        BloodPressureOptions(settings, settingsViewModel)
    }
}

@Composable
private fun ThemePaletteOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Color Theme") {
        AppTheme.entries.forEach { theme ->
            val isSelected = settings.appTheme == theme
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
private fun AdaptiveDisplayOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Display Options") {
        SettingCheckboxRow(
            text = "Adaptive Display",
            checked = settings.adaptiveDisplay,
            onCheckedChange = { checked ->
                settingsViewModel.setAdaptiveDisplay(checked)
            }
        )
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
            checked = settings.showPulseInGraph,
            onCheckedChange = { checked ->
                settingsViewModel.setShowPulseInGraph(checked)
            }
        )
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
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.ItemSpacing)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = SettingsDialogDefaults.RowPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
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
            .fillMaxWidth()
            .selectable(
                selected = checked,
                onClick = { onCheckedChange(!checked) },
                role = Role.Checkbox
            )
            .padding(vertical = SettingsDialogDefaults.RowPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AboutTabContent() {
    val uriHandler = LocalUriHandler.current
    var showLicenseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Health Coach") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${AppInfo.APP_NAME} (v${AppInfo.APP_VERSION})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        uriHandler.openUri(AppInfo.GITHUB_URL)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Author: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = AppInfo.AUTHOR,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "License: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = AppInfo.LICENSE_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        showLicenseDialog = true
                    }
                )
            }
        }

        SettingsSection(title = "Open Source Licenses & Attributions") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AppInfo.attributions) { attribution ->
                        AttributionItem(
                            attribution = attribution,
                            onOpenUrl = { url -> uriHandler.openUri(url) }
                        )
                    }
                }
            }
        }
    }

    if (showLicenseDialog) {
        AppLicenseDialog(onDismiss = { showLicenseDialog = false })
    }
}

@Composable
private fun AttributionItem(
    attribution: OpenSourceAttribution,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = attribution.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = attribution.licenseName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable { onOpenUrl(attribution.licenseUrl) }
                    )
                }
            }

            if (attribution.version != null) {
                Text(
                    text = "Version: ${attribution.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = attribution.copyright,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = attribution.projectUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onOpenUrl(attribution.projectUrl) }
            )
        }
    }
}

@Composable
private fun AppLicenseDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppInfo.LICENSE_NAME) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
            ) {
                Text(
                    text = "Health Coach is open-source software licensed under the ${AppInfo.LICENSE_NAME}.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = AppInfo.APACHE_LICENSE_TEXT,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(onClick = { uriHandler.openUri(AppInfo.LICENSE_URL) }) {
                Text("Open in Browser")
            }
        }
    )
}

@Preview
@Composable
fun SettingsDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            SettingsDialog(
                settingsViewModel = koinInject(),
                onDismiss = {}
            )
        }
    )
}
