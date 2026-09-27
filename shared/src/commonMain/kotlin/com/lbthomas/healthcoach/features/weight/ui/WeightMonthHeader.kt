package com.lbthomas.healthcoach.features.weight.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.utils.displayName
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth

@Suppress("DefaultLocale")
@Composable
fun WeightMonthHeader(
    month: YearMonth,
    data: List<Pair<WeightEntryData, WeightEntryData?>>,
    nextGroupFirstEntry: WeightEntryData?,
    settings: SettingsData,
    modifier: Modifier = Modifier
) {
    val weightChange = data.first().first.weight - (nextGroupFirstEntry ?: data.last().first).weight

    val weightChangeText = if (weightChange != 0.0) {
        val unitLabel = if (settings.weight.unit == WeightUnit.METRIC) "kgs" else "lbs"
        val formattedWeight =
            String.format(
                "%+.1f",
                WeightEntryData("", today, weightChange)
                    .getWeightInCurrentUnits(settings.weight.unit)
            )

        "$formattedWeight $unitLabel"
    } else {
        null
    }

    Column(modifier = modifier) {
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

@Preview
@Composable
private fun WeightMonthHeaderPreview() {
    Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        WeightMonthHeader(
            month = YearMonth(2026, Month.JANUARY),
            data = listOf(
                Pair(WeightEntryData("1", LocalDate(2026, 1, 15), 175.0), null),
                Pair(WeightEntryData("2", LocalDate(2026, 1, 1), 178.0), null)
            ),
            nextGroupFirstEntry = null,
            settings = SettingsData()
        )
    }
}
