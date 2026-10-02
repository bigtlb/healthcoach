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
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
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
            forcedMinY = 0.0,
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

@Composable
internal fun rememberMetabolicLines(
    maintenanceColor: Color,
    targetColor: Color,
    hasMaintenance: Boolean,
    hasTarget: Boolean
): List<LineCartesianLayer.Line> {
    val lines = mutableListOf<LineCartesianLayer.Line>()
    if (hasMaintenance) {
        lines.add(
            LineCartesianLayer.rememberLine(
                fill = LineCartesianLayer.LineFill.single(Fill(maintenanceColor)),
                stroke = LineCartesianLayer.LineStroke.Dashed(
                    thickness = 1.5.dp,
                    dashLength = 5.dp,
                    gapLength = 4.dp
                ),
                pointProvider = null,
                areaFill = null
            )
        )
    }
    if (hasTarget) {
        lines.add(
            LineCartesianLayer.rememberLine(
                fill = LineCartesianLayer.LineFill.single(Fill(targetColor)),
                stroke = LineCartesianLayer.LineStroke.Dashed(
                    thickness = 2.5.dp,
                    dashLength = 8.dp,
                    gapLength = 4.dp
                ),
                pointProvider = null,
                areaFill = null
            )
        )
    }
    return lines
}

@Composable
internal fun rememberMetabolicCartesianLayer(
    metabolicLines: List<LineCartesianLayer.Line>,
    globalMinX: Double?,
    globalMaxX: Double?,
    axisPosition: Axis.Position.Vertical? = null
): LineCartesianLayer {
    val calorieRangeProvider = remember(globalMinX, globalMaxX) {
        TimeFrameChartRangeProvider(
            forcedMinX = globalMinX,
            forcedMaxX = globalMaxX,
            forcedMinY = 0.0,
            minPadding = 50.0,
            maxPadding = 100.0
        )
    }

    return rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(metabolicLines),
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

internal fun buildMetabolicLinesLayerModel(
    globalMinX: Double?,
    globalMaxX: Double?,
    maintenanceCalories: Double?,
    targetCalories: Double?
): LineCartesianLayerModel? {
    if (globalMinX == null || globalMaxX == null) return null
    if (maintenanceCalories == null && targetCalories == null) return null

    val xSpan = if (globalMinX == globalMaxX) {
        listOf(globalMinX - 0.5, globalMaxX + 0.5)
    } else {
        listOf(globalMinX, globalMaxX)
    }

    return LineCartesianLayerModel.build {
        maintenanceCalories?.let { maintenance ->
            series(
                x = xSpan,
                y = listOf(maintenance, maintenance)
            )
        }
        targetCalories?.let { target ->
            series(
                x = xSpan,
                y = listOf(target, target)
            )
        }
    }
}
