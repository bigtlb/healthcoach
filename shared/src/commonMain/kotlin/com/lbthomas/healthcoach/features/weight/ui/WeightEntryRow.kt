package com.lbthomas.healthcoach.features.weight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.utils.DOW
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate

@Suppress("DefaultLocale")
@Composable
fun WeightEntryRow(
    currentAndPriorEntry: Pair<WeightEntryData, WeightEntryData?>,
    settings: SettingsData,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shape = MaterialTheme.shapes.small
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.bodyLarge
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowDate(currentAndPriorEntry.first)

                Spacer(modifier = Modifier.weight(1f))

                // Weight
                RowData(
                    entry = currentAndPriorEntry.first,
                    prior = currentAndPriorEntry.second,
                    settings = settings
                )

                // Delete button
                RowDelete(currentAndPriorEntry.first, onDelete)
            }
        }
    }
}

@Composable
internal fun RowData(
    entry: WeightEntryData,
    prior: WeightEntryData?,
    settings: SettingsData,
    modifier: Modifier = Modifier
) {
    val units = if (settings.weight.unit == WeightUnit.METRIC) "kgs" else "lbs"

    val change = prior?.let { p ->
        entry.getWeightInCurrentUnits(settings.weight.unit) - p.getWeightInCurrentUnits(settings.weight.unit)
    } ?: 0.0

    val extColors = MaterialTheme.extendedColors
    val (changeIcon, iconColor, changeDescription) = when {
        change < 0.0 -> Triple(Icons.Outlined.KeyboardDoubleArrowDown, extColors.weightDecrease.color, "Decrease")
        change > 0.0 -> Triple(Icons.Outlined.KeyboardDoubleArrowUp, extColors.weightIncrease.color, "Increase")
        else -> Triple(Icons.Outlined.Stop, extColors.weightNoChange.color, "No change")
    }

    val rowIconWidth = 28.dp
    val rowChangeWidth = 75.dp
    val rowWeightWidth = 75.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = changeIcon,
            contentDescription = changeDescription,
            tint = iconColor,
            modifier = Modifier.size(24.dp).width(rowIconWidth)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = String.format("%+.1f $units", change),
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            color = if (change > 0) extColors.weightIncrease.color else if (change < 0) extColors.weightDecrease.color else extColors.weightNoChange.color,
            textAlign = TextAlign.End,
            modifier = Modifier.width(rowChangeWidth)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = String.format("%.1f $units", entry.getWeightInCurrentUnits(settings.weight.unit)),
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp).width(rowWeightWidth),
            textAlign = TextAlign.End
        )
    }
}

@Composable
internal fun RowDelete(entry: WeightEntryData, onDelete: () -> Unit) {
    Tooltip(tooltip = "Delete entry for ${entry.date}") {
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(WeightViewDefaults.ButtonSize)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete entry",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
internal fun RowDate(entry: WeightEntryData) {
    Tooltip(tooltip = "Date: ${entry.date}") {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.onSecondaryContainer)
                .width(WeightViewDefaults.RowDateWidth)
                .padding(4.dp),
        ) {
            ProvideTextStyle(
                value = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = entry.date.DOW())
                    Text(text = entry.date.day.toString())
                }
            }
        }
    }
}

@Preview
@Composable
private fun WeightEntryRowPreview() {
    Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        WeightEntryRow(
            currentAndPriorEntry = Pair(
                WeightEntryData("1", LocalDate(2026, 1, 15), 175.0),
                WeightEntryData("2", LocalDate(2026, 1, 14), 176.0)
            ),
            settings = SettingsData(),
            onClick = {},
            onDelete = {}
        )
    }
}
