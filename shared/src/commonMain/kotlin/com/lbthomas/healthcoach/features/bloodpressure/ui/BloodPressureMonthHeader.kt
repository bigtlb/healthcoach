package com.lbthomas.healthcoach.features.bloodpressure.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.utils.displayName
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth

@Suppress("DefaultLocale")
@Composable
fun BloodPressureMonthHeader(
    month: YearMonth,
    data: List<Pair<BloodPressureEntryData, BloodPressureEntryData?>>,
    nextGroupFirstEntry: BloodPressureEntryData?,
    modifier: Modifier = Modifier
) {
    val avgSystolic = data.map { it.first.systolic }.average().toInt()
    val avgDiastolic = data.map { it.first.diastolic }.average().toInt()
    val count = data.size

    Column(modifier = modifier) {
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

@Preview
@Composable
private fun BloodPressureMonthHeaderPreview() {
    Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        BloodPressureMonthHeader(
            month = YearMonth(2026, Month.JANUARY),
            data = listOf(
                Pair(BloodPressureEntryData("1", "2026-01-15T08:00:00Z", 120, 80, 70), null),
                Pair(BloodPressureEntryData("2", "2026-01-10T08:00:00Z", 124, 82, 72), null)
            ),
            nextGroupFirstEntry = null
        )
    }
}
