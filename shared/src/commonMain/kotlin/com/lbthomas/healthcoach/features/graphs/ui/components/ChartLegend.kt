package com.lbthomas.healthcoach.features.graphs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun ChartLegend(
    hasCalories: Boolean,
    calorieColor: Color,
    hasWeight: Boolean,
    weightColor: Color,
    weightUnitLabel: String,
    hasBp: Boolean,
    systolicColor: Color,
    diastolicColor: Color,
    showPulse: Boolean,
    pulseColor: Color,
    modifier: Modifier = Modifier
) {
    val showLegend = hasBp || (hasCalories && (hasWeight || hasBp))
    if (!showLegend) return

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        if (hasCalories) {
            LegendItem(color = calorieColor, label = "Calories (kcal)")
        }
        if (hasWeight) {
            LegendItem(color = weightColor, label = "Weight ($weightUnitLabel)")
        }
        if (hasBp) {
            LegendItem(color = systolicColor, label = "Systolic")
            LegendItem(color = diastolicColor, label = "Diastolic")
            if (showPulse) {
                LegendItem(color = pulseColor, label = "Pulse")
            }
        }
    }
}

@Composable
internal fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}
