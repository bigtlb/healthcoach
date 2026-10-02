package com.lbthomas.healthcoach.features.graphs.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.features.graphs.data.BpGraphPoint
import com.lbthomas.healthcoach.features.graphs.ui.TimeFrameChartRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill

@Composable
internal fun rememberBpLines(
    systolicColor: Color = MaterialTheme.extendedColors.graphSystolic.color,
    diastolicColor: Color = MaterialTheme.extendedColors.graphDiastolic.color,
    pulseColor: Color = MaterialTheme.extendedColors.graphPulse.color,
    includePulse: Boolean = false
): List<LineCartesianLayer.Line> {
    val systolicLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(systolicColor)),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.5.dp),
        pointProvider = null,
        areaFill = null
    )
    val diastolicLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(diastolicColor)),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.5.dp),
        pointProvider = null,
        areaFill = null
    )

    return if (includePulse) {
        val pulseLine = LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(pulseColor)),
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.5.dp),
            pointProvider = null,
            areaFill = null
        )
        listOf(systolicLine, diastolicLine, pulseLine)
    } else {
        listOf(systolicLine, diastolicLine)
    }
}

@Composable
internal fun rememberBpCartesianLayer(
    bpLines: List<LineCartesianLayer.Line>,
    globalMinX: Double?,
    globalMaxX: Double?,
    axisPosition: Axis.Position.Vertical = Axis.Position.Vertical.End,
    yStepMultiple: Double = 5.0
): LineCartesianLayer {
    val bpRangeProvider = remember(globalMinX, globalMaxX, yStepMultiple) {
        TimeFrameChartRangeProvider(
            forcedMinX = globalMinX,
            forcedMaxX = globalMaxX,
            minPadding = 5.0,
            maxPadding = 5.0,
            yPaddingFraction = 0.15,
            yStepMultiple = yStepMultiple
        )
    }

    return rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(bpLines),
        rangeProvider = bpRangeProvider,
        verticalAxisPosition = axisPosition
    )
}

internal fun buildBpLayerModel(
    bpPoints: List<BpGraphPoint>,
    hasPulseSeries: Boolean,
    pulsePoints: List<BpGraphPoint>
): LineCartesianLayerModel? {
    if (bpPoints.isEmpty()) return null
    return LineCartesianLayerModel.build {
        series(
            x = bpPoints.map { it.x },
            y = bpPoints.map { it.systolic }
        )
        series(
            x = bpPoints.map { it.x },
            y = bpPoints.map { it.diastolic }
        )
        if (hasPulseSeries && pulsePoints.isNotEmpty()) {
            series(
                x = pulsePoints.map { it.x },
                y = pulsePoints.map { it.pulse!!.toDouble() }
            )
        }
    }
}
