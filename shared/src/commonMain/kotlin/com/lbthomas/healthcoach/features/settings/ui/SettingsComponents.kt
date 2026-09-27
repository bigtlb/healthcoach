package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.utils.formatEpochMillis
import kotlin.time.Clock

internal object SettingsDialogDefaults {
    val DialogPadding = 8.dp
    val SectionSpacing = 16.dp
    val ItemSpacing = 8.dp
    val SectionIndent = 8.dp
    val RowPadding = 4.dp
    val LabelStartPadding = 4.dp
}

internal fun formatTokenExpiration(expiresAt: Long?, nowMillis: Long = Clock.System.now().toEpochMilliseconds()): String {
    if (expiresAt == null || expiresAt <= 0) return "Unknown"
    val diffMillis = expiresAt - nowMillis
    val formattedDate = formatEpochMillis(expiresAt)
    return if (diffMillis <= 0) {
        "Expired ($formattedDate)"
    } else {
        val totalSecs = diffMillis / 1000
        val days = totalSecs / 86400
        val hours = (totalSecs % 86400) / 3600
        val mins = (totalSecs % 3600) / 60
        val remaining = when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
        "Expires in $remaining ($formattedDate)"
    }
}

@Composable
internal fun SettingsSection(
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
internal fun SettingRadioRow(
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
internal fun SettingCheckboxRow(
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

@Preview
@Composable
private fun SettingsComponentsPreview() {
    Surface(modifier = Modifier.padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSection(title = "Example Settings Section") {
                SettingRadioRow(
                    text = "Selected Option",
                    selected = true,
                    onClick = {}
                )
                SettingRadioRow(
                    text = "Unselected Option",
                    selected = false,
                    onClick = {}
                )
                SettingCheckboxRow(
                    text = "Enabled Option",
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }
    }
}
