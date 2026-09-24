package com.lbthomas.healthcoach.features.bloodpressure

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.BloodPressureCategory
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.core.ui.VerticalScrollbarBox
import com.lbthomas.healthcoach.core.utils.DOW
import com.lbthomas.healthcoach.core.utils.displayName
import com.lbthomas.healthcoach.core.utils.formatTime
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

private object BloodPressureViewDefaults {
    val MonthHeaderRowPadding = 32.dp
    val MonthSpacerPadding = 16.dp
    val MonthHeaderDividerPadding = 4.dp
    val RowDateWidth = 68.dp
    val ButtonSize = 36.dp
    val ButtonPadding = 8.dp
    const val AHA_GUIDE_URL =
        "https://www.heart.org/en/health-topics/high-blood-pressure/understanding-blood-pressure-readings"
}

@Composable
fun BloodPressureView(
    showAddBloodPressureEntry: Boolean,
    modifier: Modifier = Modifier,
    isWideLayout: Boolean = false,
    onAddDismiss: () -> Unit = {},
    onRequestFocus: () -> Unit = {}
) {
    val viewModel = koinInject<BloodPressureViewModel>()
    val entries by viewModel.entries.collectAsState()

    var entryToDelete by remember { mutableStateOf<BloodPressureEntryData?>(null) }
    var entryToEdit by remember { mutableStateOf<BloodPressureEntryData?>(null) }

    LaunchedEffect(showAddBloodPressureEntry) {
        if (showAddBloodPressureEntry) {
            entryToEdit = BloodPressureEntryData(
                id = 0,
                dateTime = today.toString(),
                systolic = 0,
                diastolic = 0,
                pulse = null
            )
        }
    }

    entryToDelete?.let { entry ->
        DeleteBloodPressureConfirmation(
            entry = entry,
            onConfirm = {
                viewModel.deleteEntry(entry.id)
                entryToDelete = null
                onRequestFocus()
            },
            onDismiss = {
                entryToDelete = null
                onRequestFocus()
            }
        )
    }

    entryToEdit?.let { entry ->
        BloodPressureEntryEditDialog(
            entry = entry,
            onConfirm = { updatedEntry ->
                if (updatedEntry.id == 0L) {
                    viewModel.addEntry(
                        dateTime = updatedEntry.dateTime,
                        systolic = updatedEntry.systolic,
                        diastolic = updatedEntry.diastolic,
                        pulse = updatedEntry.pulse
                    )
                } else {
                    viewModel.updateEntry(updatedEntry)
                }
                entryToEdit = null
                if (showAddBloodPressureEntry) onAddDismiss()
                onRequestFocus()
            },
            onDismiss = {
                entryToEdit = null
                if (showAddBloodPressureEntry) onAddDismiss()
                onRequestFocus()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        if (isWideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Blood Pressure",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AddBloodPressureEntryButton(
                    onClick = {
                        entryToEdit = BloodPressureEntryData(
                            id = 0,
                            dateTime = today.toString(),
                            systolic = 0,
                            diastolic = 0,
                            pulse = null
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            BloodPressureEntryList(
                entries = entries,
                isWideLayout = isWideLayout,
                onClickEntry = { entry ->
                    entryToEdit = entry
                },
                onDeleteEntry = { entry ->
                    entryToDelete = entry
                }
            )
        }

        AhaGuideLinkFooter(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp)
        )
    }
}

@Composable
private fun AddBloodPressureEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tooltip("Add new blood pressure\n(Ctrl + N or '+')", modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .padding(BloodPressureViewDefaults.ButtonPadding)
                .size(BloodPressureViewDefaults.ButtonSize),
            shape = FloatingActionButtonDefaults.smallShape,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new blood pressure",
            )
        }
    }
}

@Composable
fun BloodPressureEntryList(
    entries: List<BloodPressureEntryData>,
    isWideLayout: Boolean = false,
    onClickEntry: (BloodPressureEntryData) -> Unit = {},
    onDeleteEntry: (BloodPressureEntryData) -> Unit = {}
) {
    val listState = rememberLazyListState()
    VerticalScrollbarBox(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = 4.dp,
                bottom = if (isWideLayout) 8.dp else 80.dp
            ),
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val sortedEntries = entries.sortedWith(
                compareByDescending<BloodPressureEntryData> { it.date }
                    .thenByDescending { it.time?.toString() ?: "" }
            )
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
                item { BloodPressureMonthHeader(month, data, nextGroupFirstEntry) }
                data.forEach { currentAndPrior ->
                    item {
                        BloodPressureEntryRow(
                            currentAndPriorEntry = currentAndPrior,
                            onClick = { onClickEntry(currentAndPrior.first) },
                            onDelete = { onDeleteEntry(currentAndPrior.first) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(BloodPressureViewDefaults.MonthSpacerPadding)) }
            }
        }
    }
}

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
private fun CategoryIndicator(category: BloodPressureCategory) {
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
private fun RowBpData(
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
private fun RowDelete(entry: BloodPressureEntryData, onDelete: () -> Unit) {
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
private fun RowDate(entry: BloodPressureEntryData) {
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

@Suppress("DefaultLocale")
@Composable
fun BloodPressureMonthHeader(
    month: YearMonth,
    data: List<Pair<BloodPressureEntryData, BloodPressureEntryData?>>,
    nextGroupFirstEntry: BloodPressureEntryData?
) {
    val avgSystolic = data.map { it.first.systolic }.average().toInt()
    val avgDiastolic = data.map { it.first.diastolic }.average().toInt()
    val count = data.size

    Column {
        ProvideTextStyle(
            value = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(end = BloodPressureViewDefaults.MonthHeaderRowPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = month.displayName())

                Text(
                    text = "Avg $avgSystolic/$avgDiastolic mmHg ($count)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = BloodPressureViewDefaults.MonthHeaderDividerPadding),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )
    }
}

@Composable
private fun AhaGuideLinkFooter(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Tooltip(tooltip = "Open AHA Blood Pressure Categories Guide") {
        Row(
            modifier = modifier
                .clickable {
                    uriHandler.openUri(BloodPressureViewDefaults.AHA_GUIDE_URL)
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blood Pressure Guide",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open AHA Guide",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun BloodPressureViewPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            BloodPressureView(showAddBloodPressureEntry = false)
        }
    )
}
