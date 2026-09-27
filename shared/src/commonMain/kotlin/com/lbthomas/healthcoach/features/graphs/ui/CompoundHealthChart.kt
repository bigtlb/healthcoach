package com.lbthomas.healthcoach.features.graphs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.features.graphs.data.BpGraphPoint
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import kotlinx.datetime.LocalDate
import kotlin.math.round

@Composable
internal fun CompoundHealthChart(
    showWeight: Boolean,
    showBp: Boolean,
    showPulse: Boolean,
    weightEntries: List<WeightEntryData>,
    bpPoints: List<BpGraphPoint>,
    weightUnit: WeightUnit,
    weightMinEpoch: Double?,
    weightMaxEpoch: Double?,
    bpMinEpoch: Double?,
    bpMaxEpoch: Double?,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    val hasWeight = showWeight && weightEntries.isNotEmpty()
    val hasBp = showBp && bpPoints.isNotEmpty()
    val hasPulseSeries = hasBp && showPulse && bpPoints.any { it.pulse != null }
    val pulsePoints = remember(hasPulseSeries, bpPoints) {
        if (hasPulseSeries) bpPoints.filter { it.pulse != null } else emptyList()
    }

    // Global X min/max calculation across active datasets
    val globalMinX = remember(hasWeight, hasBp, weightMinEpoch, bpMinEpoch, weightEntries, bpPoints) {
        val mins = mutableListOf<Double>()
        if (hasWeight) {
            weightMinEpoch?.let { mins.add(it) } ?: weightEntries.firstOrNull()?.date?.toEpochDays()?.toDouble()?.let { mins.add(it) }
        }
        if (hasBp) {
            bpMinEpoch?.let { mins.add(it) } ?: bpPoints.firstOrNull()?.x?.let { mins.add(it) }
        }
        mins.minOrNull()
    }

    val globalMaxX = remember(hasWeight, hasBp, weightMaxEpoch, bpMaxEpoch, weightEntries, bpPoints) {
        val maxs = mutableListOf<Double>()
        if (hasWeight) {
            weightMaxEpoch?.let { maxs.add(it) } ?: weightEntries.lastOrNull()?.date?.toEpochDays()?.toDouble()?.let { maxs.add(it) }
        }
        if (hasBp) {
            bpMaxEpoch?.let { maxs.add(it) } ?: bpPoints.lastOrNull()?.x?.let { maxs.add(it) }
        }
        maxs.maxOrNull()
    }

    LaunchedEffect(hasWeight, hasBp, weightEntries, bpPoints, weightUnit, hasPulseSeries, pulsePoints) {
        modelProducer.runTransaction {
            if (hasWeight && hasBp) {
                // Layer 0: Weight (Left/Start Axis)
                lineModel {
                    series(
                        x = weightEntries.map { it.date.toEpochDays().toDouble() },
                        y = weightEntries.map { it.getWeightInCurrentUnits(weightUnit) }
                    )
                }
                // Layer 1: Blood Pressure and optional Pulse (Right/End Axis)
                lineModel {
                    series(
                        x = bpPoints.map { it.x },
                        y = bpPoints.map { it.systolic }
                    )
                    series(
                        x = bpPoints.map { it.x },
                        y = bpPoints.map { it.diastolic }
                    )
                    if (hasPulseSeries) {
                        series(
                            x = pulsePoints.map { it.x },
                            y = pulsePoints.map { it.pulse!!.toDouble() }
                        )
                    }
                }
            } else if (hasWeight) {
                lineModel {
                    series(
                        x = weightEntries.map { it.date.toEpochDays().toDouble() },
                        y = weightEntries.map { it.getWeightInCurrentUnits(weightUnit) }
                    )
                }
            } else if (hasBp) {
                lineModel {
                    series(
                        x = bpPoints.map { it.x },
                        y = bpPoints.map { it.systolic }
                    )
                    series(
                        x = bpPoints.map { it.x },
                        y = bpPoints.map { it.diastolic }
                    )
                    if (hasPulseSeries) {
                        series(
                            x = pulsePoints.map { it.x },
                            y = pulsePoints.map { it.pulse!!.toDouble() }
                        )
                    }
                }
            }
        }
    }

    val unitLabel = if (weightUnit == WeightUnit.METRIC) "kg" else "lb"
    val extColors = MaterialTheme.extendedColors
    val weightLineColor = extColors.graphWeight.color
    val bpSystolicColor = extColors.graphSystolic.color
    val bpDiastolicColor = extColors.graphDiastolic.color
    val bpPulseColor = extColors.graphPulse.color

    val weightLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(weightLineColor)),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.5.dp),
        pointProvider = null,
        areaFill = null
    )

    val bpSystolicLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(bpSystolicColor)),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.5.dp),
        pointProvider = null,
        areaFill = null
    )

    val bpDiastolicLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(bpDiastolicColor)),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.5.dp),
        pointProvider = null,
        areaFill = null
    )

    val bpPulseLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(bpPulseColor)),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.5.dp),
        pointProvider = null,
        areaFill = null
    )

    val globalRangeProvider = remember(globalMinX, globalMaxX) {
        TimeFrameChartRangeProvider(
            forcedMinX = globalMinX,
            forcedMaxX = globalMaxX
        )
    }

    val bpLines = if (hasPulseSeries) {
        listOf(bpSystolicLine, bpDiastolicLine, bpPulseLine)
    } else {
        listOf(bpSystolicLine, bpDiastolicLine)
    }

    val lineLayers = if (hasWeight && hasBp) {
        val weightLayer = rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(weightLine),
            rangeProvider = globalRangeProvider,
            verticalAxisPosition = Axis.Position.Vertical.Start
        )
        val bpLayer = rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(bpLines),
            rangeProvider = globalRangeProvider,
            verticalAxisPosition = Axis.Position.Vertical.End
        )
        listOf(weightLayer, bpLayer)
    } else if (hasWeight) {
        val weightLayer = rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(weightLine),
            rangeProvider = globalRangeProvider,
            verticalAxisPosition = Axis.Position.Vertical.Start
        )
        listOf(weightLayer)
    } else {
        val bpLayer = rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(bpLines),
            rangeProvider = globalRangeProvider,
            verticalAxisPosition = Axis.Position.Vertical.Start
        )
        listOf(bpLayer)
    }

    val marker = rememberHealthChartMarker(
        hasWeight = hasWeight,
        hasBp = hasBp,
        weightUnit = weightUnit,
        weightLineColor = weightLineColor,
        bpSystolicColor = bpSystolicColor,
        bpDiastolicColor = bpDiastolicColor,
        bpPulseColor = bpPulseColor,
        unitLabel = unitLabel
    )

    val daySpan = remember(globalMinX, globalMaxX) {
        if (globalMinX != null && globalMaxX != null) {
            (globalMaxX - globalMinX).toInt()
        } else {
            0
        }
    }

    val horizontalAxisSpacing = remember(daySpan) {
        when {
            daySpan <= 30 -> 7     // 1 week
            daySpan <= 120 -> 14   // 2 weeks
            daySpan <= 365 -> 30   // ~1 month
            daySpan <= 730 -> 90   // ~1 quarter
            else -> 180            // ~6 months
        }
    }

    val bottomAxisValueFormatter = remember(daySpan) {
        CartesianValueFormatter { _, value, _ ->
            val date = LocalDate.fromEpochDays(value.toLong())
            when {
                daySpan <= 120 -> "${date.month}/${date.day}"
                daySpan <= 365 -> "${date.month.name.take(3)} ${date.day}"
                daySpan <= 730 -> "${date.month.name.take(3)} '${date.year % 100}"
                else -> "${date.year}"
            }
        }
    }

    val startAxisValueFormatter = remember(hasWeight, hasBp, weightUnit) {
        CartesianValueFormatter { _, value, _ ->
            val rounded = round(value * 10) / 10.0
            val num = if (rounded % 1.0 == 0.0) "${rounded.toInt()}" else "$rounded"
            if (hasWeight) {
                "$num $unitLabel"
            } else {
                "$num mmHg"
            }
        }
    }

    val endAxisValueFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            val rounded = round(value).toInt()
            "$rounded mmHg"
        }
    }

    val startAxis = VerticalAxis.rememberStart(
        horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
        valueFormatter = startAxisValueFormatter,
        label = rememberAxisLabelComponent(
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        ),
        guideline = rememberAxisGuidelineComponent(
            fill = Fill(MaterialTheme.colorScheme.outlineVariant),
            thickness = 1.5.dp,
            shape = DashedShape(
                shape = CircleShape,
                dashLength = 3.dp,
                gapLength = 4.dp
            )
        )
    )

    val endAxis = if (hasWeight && hasBp) {
        VerticalAxis.rememberEnd(
            horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
            valueFormatter = endAxisValueFormatter,
            label = rememberAxisLabelComponent(
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            ),
            guideline = null
        )
    } else {
        null
    }

    val bottomAxis = HorizontalAxis.rememberBottom(
        itemPlacer = remember(horizontalAxisSpacing) {
            HorizontalAxis.ItemPlacer.aligned(spacing = { horizontalAxisSpacing })
        },
        valueFormatter = bottomAxisValueFormatter,
        label = rememberAxisLabelComponent(
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        ),
        guideline = null
    )

    val chart = rememberCartesianChart(
        *lineLayers.toTypedArray(),
        startAxis = startAxis,
        endAxis = endAxis,
        bottomAxis = bottomAxis,
        marker = marker,
        markerController = CartesianMarkerController.rememberShowOnHover()
    )

    Column(modifier = modifier) {
        // Legend: only shown when Blood Pressure is active to distinguish Systolic, Diastolic, and Pulse lines
        if (hasBp) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                if (hasWeight) {
                    LegendItem(color = weightLineColor, label = "Weight ($unitLabel)")
                }
                LegendItem(color = bpSystolicColor, label = "Systolic")
                LegendItem(color = bpDiastolicColor, label = "Diastolic")
                if (showPulse) {
                    LegendItem(color = bpPulseColor, label = "Pulse")
                }
            }
        }

        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
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

@Preview
@Composable
private fun CompoundHealthChartPreview() {
    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        CompoundHealthChart(
            showWeight = true,
            showBp = true,
            showPulse = true,
            weightEntries = listOf(
                WeightEntryData("1", LocalDate(2026, 1, 1), 175.0),
                WeightEntryData("2", LocalDate(2026, 1, 15), 174.2),
                WeightEntryData("3", LocalDate(2026, 2, 1), 173.0)
            ),
            bpPoints = listOf(
                BpGraphPoint("1", LocalDate(2026, 1, 1).toEpochDays().toDouble(), LocalDate(2026, 1, 1), 120.0, 80.0, 70),
                BpGraphPoint("2", LocalDate(2026, 1, 15).toEpochDays().toDouble(), LocalDate(2026, 1, 15), 118.0, 78.0, 68),
                BpGraphPoint("3", LocalDate(2026, 2, 1).toEpochDays().toDouble(), LocalDate(2026, 2, 1), 115.0, 75.0, 65)
            ),
            weightUnit = WeightUnit.US,
            weightMinEpoch = null,
            weightMaxEpoch = null,
            bpMinEpoch = null,
            bpMaxEpoch = null
        )
    }
}
