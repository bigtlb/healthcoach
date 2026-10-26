package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.FontSizePreference
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun AccessibilityTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        UiScaleOptions(settings, settingsViewModel)
        BodyTextSizeOptions(settings, settingsViewModel)
        LabelTextSizeOptions(settings, settingsViewModel)
    }
}

@Composable
private fun <T> SettingsDropdownField(
    title: String,
    selectedValueText: String,
    options: List<Pair<T, String>>,
    isSelected: (T) -> Boolean,
    onOptionSelected: (T) -> Unit,
    contentDescription: String = "Select $title"
) {
    var expanded by remember { mutableStateOf(false) }

    SettingsSection(title = title) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedValueText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 200.dp)
            ) {
                options.forEach { (option, label) ->
                    val selected = isSelected(option)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UiScaleOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    val scaleOptions = listOf(
        0.50f to "50%",
        0.75f to "75%",
        1.00f to "100% (Default)",
        1.25f to "125%",
        1.50f to "150%",
        1.75f to "175%",
        2.00f to "200%",
        2.25f to "225%",
        2.50f to "250%"
    )
    val currentScale = settings.accessibility.uiScale
    val currentOption = scaleOptions.firstOrNull { abs(it.first - currentScale) < 0.01f }
    val currentLabel = currentOption?.second ?: "${(currentScale * 100).roundToInt()}%"

    SettingsDropdownField(
        title = "Application Scaling",
        selectedValueText = currentLabel,
        options = scaleOptions,
        isSelected = { scale -> abs(scale - currentScale) < 0.01f },
        onOptionSelected = { scale -> settingsViewModel.setUiScale(scale) },
        contentDescription = "Select Scale"
    )
}

@Composable
private fun BodyTextSizeOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    val sizeOptions = FontSizePreference.entries.map { size ->
        val label = if (size == FontSizePreference.MEDIUM) "${size.displayName} (Default)" else size.displayName
        size to label
    }
    val currentSize = settings.appearance.bodyTextSize
    val currentOption = sizeOptions.firstOrNull { it.first == currentSize }
    val currentLabel = currentOption?.second ?: currentSize.displayName

    SettingsDropdownField(
        title = "Body Text Size",
        selectedValueText = currentLabel,
        options = sizeOptions,
        isSelected = { size -> size == currentSize },
        onOptionSelected = { size -> settingsViewModel.setBodyTextSize(size) },
        contentDescription = "Select Body Text Size"
    )
}

@Composable
private fun LabelTextSizeOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    val sizeOptions = FontSizePreference.entries.map { size ->
        val label = if (size == FontSizePreference.MEDIUM) "${size.displayName} (Default)" else size.displayName
        size to label
    }
    val currentSize = settings.appearance.labelTextSize
    val currentOption = sizeOptions.firstOrNull { it.first == currentSize }
    val currentLabel = currentOption?.second ?: currentSize.displayName

    SettingsDropdownField(
        title = "Label Text Size",
        selectedValueText = currentLabel,
        options = sizeOptions,
        isSelected = { size -> size == currentSize },
        onOptionSelected = { size -> settingsViewModel.setLabelTextSize(size) },
        contentDescription = "Select Label Text Size"
    )
}

@Preview
@Composable
private fun AccessibilityTabPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                AccessibilityTabContent(
                    settings = SettingsData(),
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
