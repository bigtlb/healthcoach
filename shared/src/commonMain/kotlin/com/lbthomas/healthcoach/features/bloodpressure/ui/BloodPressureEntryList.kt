package com.lbthomas.healthcoach.features.bloodpressure.ui

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
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import kotlinx.datetime.yearMonth

@Composable
fun BloodPressureEntryList(
    entries: List<BloodPressureEntryData>,
    isWideLayout: Boolean = false,
    onClickEntry: (BloodPressureEntryData) -> Unit = {},
    onDeleteEntry: (BloodPressureEntryData) -> Unit = {},
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

@Preview
@Composable
private fun BloodPressureEntryListPreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        BloodPressureEntryList(
            entries = listOf(
                BloodPressureEntryData("1", "2026-01-15T08:30:00Z", 120, 80, 72),
                BloodPressureEntryData("2", "2026-01-10T09:00:00Z", 122, 82, 70),
                BloodPressureEntryData("3", "2025-12-28T08:00:00Z", 118, 78, 68)
            )
        )
    }
}
