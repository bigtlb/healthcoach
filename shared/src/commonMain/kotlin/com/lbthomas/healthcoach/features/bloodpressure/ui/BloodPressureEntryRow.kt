package com.lbthomas.healthcoach.features.bloodpressure.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.lbthomas.healthcoach.core.enums.BloodPressureCategory
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.utils.DOW
import com.lbthomas.healthcoach.core.utils.formatTime
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData

@Composable
fun BloodPressureEntryRow(
    currentAndPriorEntry: Pair<BloodPressureEntryData, BloodPressureEntryData?>,
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
                    .clickable(onClick = onClick)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowDate(currentAndPriorEntry.first)

                Spacer(modifier = Modifier.width(8.dp))

                // AHA Category Indicator with Tooltip
                CategoryIndicator(currentAndPriorEntry.first.category)

                Spacer(modifier = Modifier.weight(1f))

                // Readings & Change
                RowBpData(
                    entry = currentAndPriorEntry.first,
                    prior = currentAndPriorEntry.second
                )

                // Delete button
                RowDelete(currentAndPriorEntry.first, onDelete)
            }
        }
    }
}

@Composable
internal fun CategoryIndicator(category: BloodPressureCategory) {
    Tooltip(tooltip = category.description) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(36.dp)
                .background(
                    color = category.color,
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}

@Suppress("DefaultLocale")
@Composable
internal fun RowBpData(
    entry: BloodPressureEntryData,
    prior: BloodPressureEntryData?,
    modifier: Modifier = Modifier
) {
    val systolicDiff = prior?.let { entry.systolic - it.systolic } ?: 0
    val diastolicDiff = prior?.let { entry.diastolic - it.diastolic } ?: 0

    val extColors = MaterialTheme.extendedColors
    val (changeIcon, iconColor, changeDescription) = when {
        systolicDiff < 0 && diastolicDiff <= 0 || systolicDiff <= 0 && diastolicDiff < 0 ->
            Triple(Icons.Outlined.KeyboardDoubleArrowDown, extColors.weightDecrease.color, "Decreased BP")

        systolicDiff > 0 || diastolicDiff > 0 ->
            Triple(Icons.Outlined.KeyboardDoubleArrowUp, extColors.weightIncrease.color, "Increased BP")

        else ->
            Triple(Icons.Outlined.Stop, extColors.weightNoChange.color, "No change")
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = changeIcon,
            contentDescription = changeDescription,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Pulse (if present)
        if (entry.pulse != null) {
            Text(
                text = "${entry.pulse} bpm",
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(64.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Systolic / Diastolic
        Text(
            text = "${entry.systolic}/${entry.diastolic} mmHg",
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
internal fun RowDelete(entry: BloodPressureEntryData, onDelete: () -> Unit) {
    val timeSuffix = if (entry.hasTime) " at ${entry.time?.formatTime()}" else ""
    Tooltip(tooltip = "Delete entry for ${entry.date}$timeSuffix") {
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(BloodPressureViewDefaults.ButtonSize)
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
internal fun RowDate(entry: BloodPressureEntryData) {
    val timeSuffix = if (entry.hasTime) " ${entry.time?.formatTime()}" else ""
    Tooltip(tooltip = "Date: ${entry.date}$timeSuffix") {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.onSecondaryContainer)
                .width(BloodPressureViewDefaults.RowDateWidth)
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
                    Text(text = entry.date.DOW(), fontSize = 12.sp)
                    Text(text = entry.date.day.toString(), fontWeight = FontWeight.Bold)
                    if (entry.hasTime && entry.time != null) {
                        Text(
                            text = entry.time!!.formatTime(),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BloodPressureEntryRowPreview() {
    Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        BloodPressureEntryRow(
            currentAndPriorEntry = Pair(
                BloodPressureEntryData("1", "2026-01-15T08:30:00Z", 120, 80, 72),
                BloodPressureEntryData("2", "2026-01-14T08:30:00Z", 124, 82, 70)
            ),
            onClick = {},
            onDelete = {}
        )
    }
}
