package com.lbthomas.healthcoach.features.weight.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.ui.VerticalScrollbarBox
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth

@Composable
fun WeightEntryList(
    entries: List<WeightEntryData>,
    settings: SettingsData,
    isWideLayout: Boolean = false,
    onClickEntry: (WeightEntryData) -> Unit = {},
    onDeleteEntry: (WeightEntryData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    VerticalScrollbarBox(
        state = listState,
        modifier = modifier.fillMaxSize()
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
                item { WeightMonthHeader(month, data, nextGroupFirstEntry, settings) }
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

@Preview
@Composable
private fun WeightEntryListPreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        WeightEntryList(
            entries = listOf(
                WeightEntryData("1", LocalDate(2026, 1, 15), 175.0),
                WeightEntryData("2", LocalDate(2026, 1, 10), 176.5),
                WeightEntryData("3", LocalDate(2025, 12, 28), 178.0)
            ),
            settings = SettingsData()
        )
    }
}
