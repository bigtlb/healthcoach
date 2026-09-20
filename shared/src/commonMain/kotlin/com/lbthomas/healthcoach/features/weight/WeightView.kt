package com.lbthomas.healthcoach.features.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.ui.VerticalScrollbarBox
import com.lbthomas.healthcoach.core.utils.DOW
import com.lbthomas.healthcoach.core.utils.displayName
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

private object WeightViewDefaults {
    val MonthHeaderRowPadding = 32.dp
    val MonthSpacerPadding = 16.dp
    val MonthHeaderDividerPadding = 4.dp
    val RowDateWidth = 64.dp
    val ButtonSize = 36.dp
    val ButtonPadding = 8.dp
}

@Composable
fun WeightView(modifier: Modifier = Modifier) {
    val viewModel = koinInject<WeightViewModel>()
    val settings by koinInject<SettingsViewModel>().settings.collectAsState()
    val entries by viewModel.entries.collectAsState()

    var entryToDelete by remember { mutableStateOf<WeightEntryData?>(null) }
    var entryToEdit by remember { mutableStateOf<WeightEntryData?>(null) }

    entryToDelete?.let { entry ->
        DeleteConfirmation(
            entry = entry,
            onConfirm = {
                entryToDelete = null
            },
            onDismiss = {
                entryToDelete = null
            }
        )
    }

    entryToEdit?.let { entry ->
        WeightEntryEditDialog(
            entry = entry,
            onConfirm = { updatedEntry ->
                entryToEdit = null
            },
            onDismiss = {
                entryToEdit = null
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        AddWeightEntryButton(
            onClick = {
                entryToEdit = WeightEntryData(id = 0, date = today, weight = 0.0)
            },
            Modifier.align(Alignment.End)
        )
        WeightEntryList(
            entries = entries,
            settings = settings,
            onClickEntry = { entry ->
                entryToEdit = entry
            },
            onDeleteEntry = { entry ->
                entryToDelete = entry
            }
        )
    }
}

@Composable
private fun AddWeightEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tooltip("Add new weight entry", modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .padding(WeightViewDefaults.ButtonPadding)
                .size(WeightViewDefaults.ButtonSize),
            shape = FloatingActionButtonDefaults.smallShape,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new weight entry",
            )
        }
    }
}

@Composable
fun WeightEntryList(
    entries: List<WeightEntryData>,
    settings: SettingsData,
    onClickEntry: (WeightEntryData) -> Unit = {},
    onDeleteEntry: (WeightEntryData) -> Unit = {}
) {
    val listState = rememberLazyListState()
    VerticalScrollbarBox(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val sortedEntries = entries.sortedByDescending { it.date }
            val groups = sortedEntries
                .mapIndexed { index, entry -> entry to sortedEntries.getOrNull(index + 1) }
                .groupBy { it.first.date.yearMonth }
                .entries
                .toList()

            groups.forEachIndexed { index, group ->
                val month = group.key
                val data = group.value
                val nextGroupFirstEntry = groups
                    .getOrNull(index + 1)
                    ?.value
                    ?.firstOrNull()
                    ?.first
                item { MonthHeader(month, data, nextGroupFirstEntry, settings) }
                data.forEach { currentAndPrior ->
                    item {
                        WeightEntryRow(
                            currentAndPriorEntry = currentAndPrior,
                            settings = settings,
                            onClick = { onClickEntry(currentAndPrior.first) },
                            onDelete = { onDeleteEntry(currentAndPrior.first) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(WeightViewDefaults.MonthSpacerPadding)) }
            }
        }
    }
}

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
        modifier = Modifier
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
private fun RowData(
    entry: WeightEntryData,
    prior: WeightEntryData?,
    settings: SettingsData,
    modifier: Modifier = Modifier
) {
    val units = if (settings.weightUnit == WeightUnit.METRIC) "kgs" else "lbs"
    val singleUnit = units.dropLast(1)

    val change = prior?.let { prior ->
        entry.getWeightInCurrentUnits(settings.weightUnit) - prior.getWeightInCurrentUnits(settings.weightUnit)
    } ?: 0.0

    val (changeIcon, iconColor, changeDescription) = when {
        change < 0.0 -> Triple(Icons.Outlined.KeyboardDoubleArrowDown, Color(0xFF2E7D32), "Decrease")
        change > 0.0 -> Triple(Icons.Outlined.KeyboardDoubleArrowUp, Color.Red, "Increase")
        else -> Triple(Icons.Outlined.Stop, Color.Blue, "No change")
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
            color = if (change > 0) Color.Red else Color.Blue,
            textAlign = TextAlign.End,
            modifier = Modifier.width(rowChangeWidth)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = String.format("%.1f $units", entry.getWeightInCurrentUnits(settings.weightUnit)),
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp).width(rowWeightWidth),
            textAlign = TextAlign.End
        )

    }
}

@Composable
private fun RowDelete(entry: WeightEntryData, onDelete: () -> Unit) {
    Tooltip(tooltip = "Delete entry for ${entry.date}") {
        IconButton(
            onClick = {
                onDelete()
            },
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
private fun RowDate(entry: WeightEntryData) {
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

@Suppress("DefaultLocale")
@Composable
fun MonthHeader(
    month: YearMonth,
    data: List<Pair<WeightEntryData, WeightEntryData?>>,
    nextGroupFirstEntry: WeightEntryData?,
    settings: SettingsData
) {
    val weightChange = data.first().first.weight - (nextGroupFirstEntry ?: data.last().first).weight

    val weightChangeText = if (weightChange != 0.0) {
        val unitLabel = if (settings.weightUnit == WeightUnit.METRIC) "kgs" else "lbs"
        val formattedWeight =
            String.format(
                "%+.1f",
                WeightEntryData(0, today, weightChange)
                    .getWeightInCurrentUnits(settings.weightUnit)
            )

        "$formattedWeight $unitLabel"
    } else {
        null
    }

    Column {
        ProvideTextStyle(
            value = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(end = WeightViewDefaults.MonthHeaderRowPadding), // Add right padding to avoid overlap with Add button
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = month.displayName())

                weightChangeText?.let {
                    Text(
                        text = weightChangeText,
                        color = if (weightChange < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = WeightViewDefaults.MonthHeaderDividerPadding),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun WeightViewPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            WeightView()
        })
}

