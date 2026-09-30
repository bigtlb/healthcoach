package com.lbthomas.healthcoach.features.graphs.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.features.graphs.data.CalorieGraphPoint
import com.lbthomas.healthcoach.features.graphs.ui.TimeFrameChartRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent

@Composable
internal fun rememberCalorieCartesianLayer(
    globalMinX: Double?,
    globalMaxX: Double?,
    color: Color = MaterialTheme.extendedColors.graphCalories.color,
    axisPosition: Axis.Position.Vertical? = null
): ColumnCartesianLayer {
    val calorieRangeProvider = remember(globalMinX, globalMaxX) {
        TimeFrameChartRangeProvider(
            forcedMinX = globalMinX,
            forcedMaxX = globalMaxX,
            minPadding = 50.0,
            maxPadding = 100.0
        )
    }

    val daySpan = remember(globalMinX, globalMaxX) {
        if (globalMinX != null && globalMaxX != null) {
            (globalMaxX - globalMinX).toInt().coerceAtLeast(1)
        } else {
            30
        }
    }

    // Dynamic bar thickness proportional to the horizontal distance between dates
    val columnThickness = remember(daySpan) {
        when {
            daySpan <= 7 -> 36.dp
            daySpan <= 14 -> 28.dp
            daySpan <= 30 -> 20.dp
            daySpan <= 60 -> 16.dp
            daySpan <= 90 -> 12.dp
            daySpan <= 180 -> 8.dp
            daySpan <= 365 -> 5.dp
            else -> 3.dp
        }
    }

    return rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
            rememberLineComponent(
                fill = Fill(color.copy(alpha = 0.5f)),
                thickness = columnThickness
            )
        ),
        rangeProvider = calorieRangeProvider,
        verticalAxisPosition = axisPosition
    )
}

internal fun buildCalorieLayerModel(
    caloriePoints: List<CalorieGraphPoint>
): ColumnCartesianLayerModel? {
    if (caloriePoints.isEmpty()) return null
    return ColumnCartesianLayerModel.build {
        series(
            x = caloriePoints.map { it.x },
            y = caloriePoints.map { it.calories }
        )
    }
}
