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
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.features.graphs.data.BpGraphPoint
import com.lbthomas.healthcoach.features.graphs.data.CalorieGraphPoint
import com.lbthomas.healthcoach.features.graphs.ui.components.*
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
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
    showCalories: Boolean = false,
    weightEntries: List<WeightEntryData>,
    bpPoints: List<BpGraphPoint>,
    caloriePoints: List<CalorieGraphPoint> = emptyList(),
    maintenanceCalories: Double? = null,
    targetCalories: Double? = null,
    weightUnit: WeightUnit,
    weightMinEpoch: Double?,
    weightMaxEpoch: Double?,
    bpMinEpoch: Double?,
    bpMaxEpoch: Double?,
    calorieMinEpoch: Double? = null,
    calorieMaxEpoch: Double? = null,
    modifier: Modifier = Modifier
) {
    val hasWeight = showWeight && weightEntries.isNotEmpty()
    val hasBp = showBp && bpPoints.isNotEmpty()
    val hasCalories = showCalories && caloriePoints.isNotEmpty()
    val hasMetabolicLines = showCalories && (maintenanceCalories != null || targetCalories != null)
    val hasAnyCalories = hasCalories || hasMetabolicLines
    val hasPulseSeries = hasBp && showPulse && bpPoints.any { it.pulse != null }
    val pulsePoints = remember(hasPulseSeries, bpPoints) {
        if (hasPulseSeries) bpPoints.filter { it.pulse != null } else emptyList()
    }

    // Global X min/max calculation across active datasets
    val globalMinX = remember(hasWeight, hasBp, hasCalories, weightMinEpoch, bpMinEpoch, calorieMinEpoch, weightEntries, bpPoints, caloriePoints) {
        val mins = mutableListOf<Double>()
        if (hasWeight) {
            weightMinEpoch?.let { mins.add(it) } ?: weightEntries.firstOrNull()?.date?.toEpochDays()?.toDouble()?.let { mins.add(it) }
        }
        if (hasBp) {
            bpMinEpoch?.let { mins.add(it) } ?: bpPoints.firstOrNull()?.x?.let { mins.add(it) }
        }
        if (hasCalories) {
            calorieMinEpoch?.let { mins.add(it) } ?: caloriePoints.firstOrNull()?.x?.let { mins.add(it) }
        }
        mins.minOrNull()
    }

    val globalMaxX = remember(hasWeight, hasBp, hasCalories, weightMaxEpoch, bpMaxEpoch, calorieMaxEpoch, weightEntries, bpPoints, caloriePoints) {
        val maxs = mutableListOf<Double>()
        if (hasWeight) {
            weightMaxEpoch?.let { maxs.add(it) } ?: weightEntries.lastOrNull()?.date?.toEpochDays()?.toDouble()?.let { maxs.add(it) }
        }
        if (hasBp) {
            bpMaxEpoch?.let { maxs.add(it) } ?: bpPoints.lastOrNull()?.x?.let { maxs.add(it) }
        }
        if (hasCalories) {
            calorieMaxEpoch?.let { maxs.add(it) } ?: caloriePoints.lastOrNull()?.x?.let { maxs.add(it) }
        }
        maxs.maxOrNull()
    }

    val chartModel = remember(
        hasWeight, hasBp, hasCalories, hasMetabolicLines,
        weightEntries, bpPoints, caloriePoints,
        maintenanceCalories, targetCalories,
        globalMinX, globalMaxX,
        weightUnit, hasPulseSeries, pulsePoints
    ) {
        val models = mutableListOf<CartesianLayerModel>()
        if (hasCalories) {
            buildCalorieLayerModel(caloriePoints)?.let { models.add(it) }
        }
        if (hasMetabolicLines && globalMinX != null && globalMaxX != null) {
            buildMetabolicLinesLayerModel(
                globalMinX = globalMinX,
                globalMaxX = globalMaxX,
                maintenanceCalories = maintenanceCalories,
                targetCalories = targetCalories
            )?.let { models.add(it) }
        }
        if (hasWeight) {
            buildWeightLayerModel(weightEntries, weightUnit)?.let { models.add(it) }
        }
        if (hasBp) {
            buildBpLayerModel(bpPoints, hasPulseSeries, pulsePoints)?.let { models.add(it) }
        }
        CartesianChartModel(models)
    }

    val unitLabel = if (weightUnit == WeightUnit.METRIC) "kg" else "lb"
    val extColors = MaterialTheme.extendedColors
    val weightLineColor = extColors.graphWeight.color
    val bpSystolicColor = extColors.graphSystolic.color
    val bpDiastolicColor = extColors.graphDiastolic.color
    val bpPulseColor = extColors.graphPulse.color
    val calorieColor = extColors.graphCalories.color
    val maintenanceLineColor = MaterialTheme.colorScheme.outline
    val targetLineColor = MaterialTheme.colorScheme.primary

    // Weight Layer
    val weightLine = rememberWeightLine(weightLineColor)
    val weightLayer = if (hasWeight) {
        rememberWeightCartesianLayer(
            weightLine = weightLine,
            globalMinX = globalMinX,
            globalMaxX = globalMaxX,
            axisPosition = Axis.Position.Vertical.Start
        )
    } else null

    // Blood Pressure Layer
    val bpLines = rememberBpLines(
        systolicColor = bpSystolicColor,
        diastolicColor = bpDiastolicColor,
        pulseColor = bpPulseColor,
        includePulse = hasPulseSeries
    )
    val bpLayer = if (hasBp) {
        rememberBpCartesianLayer(
            bpLines = bpLines,
            globalMinX = globalMinX,
            globalMaxX = globalMaxX,
            axisPosition = if (hasWeight || hasAnyCalories) Axis.Position.Vertical.End else Axis.Position.Vertical.Start
        )
    } else null

    // Calorie & Metabolic Layer Axis Position:
    // If Weight is absent: Start (left axis)
    // Else if BP is absent: End (right axis)
    // Else (both Weight and BP present): null (unbound, uses global chart bounds with forcedMinY=0.0)
    val calorieAxisPosition = when {
        !hasWeight -> Axis.Position.Vertical.Start
        !hasBp -> Axis.Position.Vertical.End
        else -> null
    }

    // Calorie Layer
    val calorieLayer = if (hasCalories) {
        rememberCalorieCartesianLayer(
            globalMinX = globalMinX,
            globalMaxX = globalMaxX,
            color = calorieColor,
            axisPosition = calorieAxisPosition
        )
    } else null

    // Metabolic Reference Lines (Maintenance Baseline & Target Calorie Budget)
    val metabolicLines = rememberMetabolicLines(
        maintenanceColor = maintenanceLineColor,
        targetColor = targetLineColor,
        hasMaintenance = maintenanceCalories != null,
        hasTarget = targetCalories != null
    )
    val metabolicLayer = if (hasMetabolicLines && globalMinX != null && globalMaxX != null && metabolicLines.isNotEmpty()) {
        rememberMetabolicCartesianLayer(
            metabolicLines = metabolicLines,
            globalMinX = globalMinX,
            globalMaxX = globalMaxX,
            axisPosition = calorieAxisPosition
        )
    } else null

    val allLayers = buildList {
        // Column bars placed first so they render behind all line plots (Z-order)
        calorieLayer?.let { add(it) }
        metabolicLayer?.let { add(it) }
        weightLayer?.let { add(it) }
        bpLayer?.let { add(it) }
    }

    val marker = rememberHealthChartMarker(
        hasWeight = hasWeight,
        hasBp = hasBp,
        hasCalories = hasCalories,
        weightUnit = weightUnit,
        weightLineColor = weightLineColor,
        bpSystolicColor = bpSystolicColor,
        bpDiastolicColor = bpDiastolicColor,
        bpPulseColor = bpPulseColor,
        calorieColor = calorieColor,
        maintenanceLineColor = if (hasMetabolicLines) maintenanceLineColor else Color.Unspecified,
        targetLineColor = if (hasMetabolicLines) targetLineColor else Color.Unspecified,
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
            daySpan <= 30 -> 7
            daySpan <= 120 -> 14
            daySpan <= 365 -> 30
            daySpan <= 730 -> 90
            else -> 180
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

    val startAxisItemPlacer = remember(hasWeight, hasBp, hasCalories, hasMetabolicLines) {
        if (!hasWeight && hasAnyCalories) {
            VerticalAxis.ItemPlacer.step(step = { 50.0 })
        } else {
            VerticalAxis.ItemPlacer.count()
        }
    }

    val startAxisValueFormatter = remember(hasWeight, hasBp, hasCalories, hasMetabolicLines, weightUnit) {
        CartesianValueFormatter { _, value, _ ->
            if (hasWeight) {
                val rounded = round(value * 10) / 10.0
                val num = if (rounded % 1.0 == 0.0) "${rounded.toInt()}" else "$rounded"
                "$num $unitLabel"
            } else if (hasAnyCalories) {
                val rounded = (round(value / 50.0) * 50).toInt()
                "$rounded kcal"
            } else {
                val rounded = round(value).toInt()
                "$rounded mmHg"
            }
        }
    }

    val endAxisItemPlacer = remember(hasBp, hasWeight, hasCalories, hasMetabolicLines) {
        if (!hasBp && hasWeight && hasAnyCalories) {
            VerticalAxis.ItemPlacer.step(step = { 50.0 })
        } else {
            VerticalAxis.ItemPlacer.count()
        }
    }

    val endAxisValueFormatter = remember(hasBp, hasWeight, hasCalories, hasMetabolicLines) {
        CartesianValueFormatter { _, value, _ ->
            if (hasBp) {
                val rounded = round(value).toInt()
                "$rounded mmHg"
            } else {
                val rounded = (round(value / 50.0) * 50).toInt()
                "$rounded kcal"
            }
        }
    }

    val startAxis = VerticalAxis.rememberStart(
        horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
        valueFormatter = startAxisValueFormatter,
        itemPlacer = startAxisItemPlacer,
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

    val showEndAxis = (hasBp && (hasWeight || hasAnyCalories)) || (hasWeight && hasAnyCalories)

    val endAxis = if (showEndAxis) {
        VerticalAxis.rememberEnd(
            horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
            valueFormatter = endAxisValueFormatter,
            itemPlacer = endAxisItemPlacer,
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
        *allLayers.toTypedArray(),
        startAxis = startAxis,
        endAxis = endAxis,
        bottomAxis = bottomAxis,
        marker = marker,
        markerController = CartesianMarkerController.rememberShowOnHover(),
        getXStep = { _, _, _ -> 1.0 }
    )

    Column(modifier = modifier) {
        ChartLegend(
            hasCalories = hasCalories,
            calorieColor = calorieColor,
            maintenanceCalories = if (showCalories) maintenanceCalories else null,
            maintenanceColor = maintenanceLineColor,
            targetCalories = if (showCalories) targetCalories else null,
            targetColor = targetLineColor,
            hasWeight = hasWeight,
            weightColor = weightLineColor,
            weightUnitLabel = unitLabel,
            hasBp = hasBp,
            systolicColor = bpSystolicColor,
            diastolicColor = bpDiastolicColor,
            showPulse = showPulse,
            pulseColor = bpPulseColor
        )

        CartesianChartHost(
            chart = chart,
            model = chartModel,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

private val previewWeightEntries = listOf(
    WeightEntryData("1", LocalDate(2026, 1, 1), 175.0),
    WeightEntryData("2", LocalDate(2026, 1, 15), 174.2),
    WeightEntryData("3", LocalDate(2026, 2, 1), 173.0),
    WeightEntryData("4", LocalDate(2026, 2, 15), 171.5),
    WeightEntryData("5", LocalDate(2026, 3, 1), 170.0)
)

private val previewBpPoints = listOf(
    BpGraphPoint("1", LocalDate(2026, 1, 1).toEpochDays().toDouble(), LocalDate(2026, 1, 1), 120.0, 80.0, 70),
    BpGraphPoint("2", LocalDate(2026, 1, 15).toEpochDays().toDouble(), LocalDate(2026, 1, 15), 118.0, 78.0, 68),
    BpGraphPoint("3", LocalDate(2026, 2, 1).toEpochDays().toDouble(), LocalDate(2026, 2, 1), 122.0, 82.0, 72),
    BpGraphPoint("4", LocalDate(2026, 2, 15).toEpochDays().toDouble(), LocalDate(2026, 2, 15), 116.0, 76.0, 66),
    BpGraphPoint("5", LocalDate(2026, 3, 1).toEpochDays().toDouble(), LocalDate(2026, 3, 1), 114.0, 74.0, 64)
)

private val previewCaloriePoints = listOf(
    CalorieGraphPoint(LocalDate(2026, 1, 1), LocalDate(2026, 1, 1).toEpochDays().toDouble(), 1850.0),
    CalorieGraphPoint(LocalDate(2026, 1, 15), LocalDate(2026, 1, 15).toEpochDays().toDouble(), 2100.0),
    CalorieGraphPoint(LocalDate(2026, 2, 1), LocalDate(2026, 2, 1).toEpochDays().toDouble(), 1950.0),
    CalorieGraphPoint(LocalDate(2026, 2, 15), LocalDate(2026, 2, 15).toEpochDays().toDouble(), 2250.0),
    CalorieGraphPoint(LocalDate(2026, 3, 1), LocalDate(2026, 3, 1).toEpochDays().toDouble(), 2000.0)
)

@Preview(
    name = "Compound Chart - All Metrics",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun CompoundHealthChartAllMetricsPreview() {
    HealthCoachTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CompoundHealthChart(
                showWeight = true,
                showBp = true,
                showPulse = true,
                showCalories = true,
                weightEntries = previewWeightEntries,
                bpPoints = previewBpPoints,
                caloriePoints = previewCaloriePoints,
                maintenanceCalories = 2150.0,
                targetCalories = 1850.0,
                weightUnit = WeightUnit.US,
                weightMinEpoch = null,
                weightMaxEpoch = null,
                bpMinEpoch = null,
                bpMaxEpoch = null,
                calorieMinEpoch = null,
                calorieMaxEpoch = null
            )
        }
    }
}

@Preview(
    name = "Compound Chart - Weight Only",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun CompoundHealthChartWeightOnlyPreview() {
    HealthCoachTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CompoundHealthChart(
                showWeight = true,
                showBp = false,
                showPulse = false,
                showCalories = false,
                weightEntries = previewWeightEntries,
                bpPoints = emptyList(),
                caloriePoints = emptyList(),
                weightUnit = WeightUnit.US,
                weightMinEpoch = null,
                weightMaxEpoch = null,
                bpMinEpoch = null,
                bpMaxEpoch = null,
                calorieMinEpoch = null,
                calorieMaxEpoch = null
            )
        }
    }
}

@Preview(
    name = "Compound Chart - Blood Pressure Only",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun CompoundHealthChartBpOnlyPreview() {
    HealthCoachTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CompoundHealthChart(
                showWeight = false,
                showBp = true,
                showPulse = true,
                showCalories = false,
                weightEntries = emptyList(),
                bpPoints = previewBpPoints,
                caloriePoints = emptyList(),
                weightUnit = WeightUnit.US,
                weightMinEpoch = null,
                weightMaxEpoch = null,
                bpMinEpoch = null,
                bpMaxEpoch = null,
                calorieMinEpoch = null,
                calorieMaxEpoch = null
            )
        }
    }
}

@Preview(
    name = "Compound Chart - Calories Only",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun CompoundHealthChartCaloriesOnlyPreview() {
    HealthCoachTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CompoundHealthChart(
                showWeight = false,
                showBp = false,
                showPulse = false,
                showCalories = true,
                weightEntries = emptyList(),
                bpPoints = emptyList(),
                caloriePoints = previewCaloriePoints,
                maintenanceCalories = 2150.0,
                targetCalories = 1850.0,
                weightUnit = WeightUnit.US,
                weightMinEpoch = null,
                weightMaxEpoch = null,
                bpMinEpoch = null,
                bpMaxEpoch = null,
                calorieMinEpoch = null,
                calorieMaxEpoch = null
            )
        }
    }
}

@Preview(
    name = "Compound Chart - Weight and BP",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun CompoundHealthChartWeightAndBpPreview() {
    HealthCoachTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CompoundHealthChart(
                showWeight = true,
                showBp = true,
                showPulse = true,
                showCalories = false,
                weightEntries = previewWeightEntries,
                bpPoints = previewBpPoints,
                caloriePoints = emptyList(),
                weightUnit = WeightUnit.US,
                weightMinEpoch = null,
                weightMaxEpoch = null,
                bpMinEpoch = null,
                bpMaxEpoch = null,
                calorieMinEpoch = null,
                calorieMaxEpoch = null
            )
        }
    }
}

@Preview(
    name = "Compound Chart - Weight and Calories",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun CompoundHealthChartWeightAndCaloriesPreview() {
    HealthCoachTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CompoundHealthChart(
                showWeight = true,
                showBp = false,
                showPulse = false,
                showCalories = true,
                weightEntries = previewWeightEntries,
                bpPoints = emptyList(),
                caloriePoints = previewCaloriePoints,
                maintenanceCalories = 2150.0,
                targetCalories = 1850.0,
                weightUnit = WeightUnit.US,
                weightMinEpoch = null,
                weightMaxEpoch = null,
                bpMinEpoch = null,
                bpMaxEpoch = null,
                calorieMinEpoch = null,
                calorieMaxEpoch = null
            )
        }
    }
}

@Preview(
    name = "Compound Chart - BP and Calories",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun CompoundHealthChartBpAndCaloriesPreview() {
    HealthCoachTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CompoundHealthChart(
                showWeight = false,
                showBp = true,
                showPulse = true,
                showCalories = true,
                weightEntries = emptyList(),
                bpPoints = previewBpPoints,
                caloriePoints = previewCaloriePoints,
                maintenanceCalories = 2150.0,
                targetCalories = 1850.0,
                weightUnit = WeightUnit.US,
                weightMinEpoch = null,
                weightMaxEpoch = null,
                bpMinEpoch = null,
                bpMaxEpoch = null,
                calorieMinEpoch = null,
                calorieMaxEpoch = null
            )
        }
    }
}
