package com.lbthomas.healthcoach.features.graphs.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.features.graphs.ui.TimeFrameChartRangeProvider
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill

@Composable
internal fun rememberWeightLine(
    color: Color = MaterialTheme.extendedColors.graphWeight.color
): LineCartesianLayer.Line {
    return LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(color)),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.5.dp),
        pointProvider = null,
        areaFill = null
    )
}

@Composable
internal fun rememberWeightCartesianLayer(
    weightLine: LineCartesianLayer.Line,
    globalMinX: Double?,
    globalMaxX: Double?,
    axisPosition: Axis.Position.Vertical = Axis.Position.Vertical.Start,
    yStepMultiple: Double = 5.0
): LineCartesianLayer {
    val weightRangeProvider = remember(globalMinX, globalMaxX, yStepMultiple) {
        TimeFrameChartRangeProvider(
            forcedMinX = globalMinX,
            forcedMaxX = globalMaxX,
            minPadding = 3.0,
            maxPadding = 3.0,
            yStepMultiple = yStepMultiple
        )
    }

    return rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(weightLine),
        rangeProvider = weightRangeProvider,
        verticalAxisPosition = axisPosition
    )
}

internal fun buildWeightLayerModel(
    weightEntries: List<WeightEntryData>,
    weightUnit: WeightUnit
): LineCartesianLayerModel? {
    if (weightEntries.isEmpty()) return null
    return LineCartesianLayerModel.build {
        series(
            x = weightEntries.map { it.date.toEpochDays().toDouble() },
            y = weightEntries.map { it.getWeightInCurrentUnits(weightUnit) }
        )
    }
}
