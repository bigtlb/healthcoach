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
    val (globalMinX, globalMaxX) = remember(hasWeight, hasBp, hasCalories, weightMinEpoch, bpMinEpoch, calorieMinEpoch, weightEntries, bpPoints, caloriePoints) {
        calculateGlobalEpochBounds(
            hasWeight = hasWeight,
            hasBp = hasBp,
            hasCalories = hasCalories,
            weightEntries = weightEntries,
            bpPoints = bpPoints,
            caloriePoints = caloriePoints,
            weightMinEpoch = weightMinEpoch,
            weightMaxEpoch = weightMaxEpoch,
            bpMinEpoch = bpMinEpoch,
            bpMaxEpoch = bpMaxEpoch,
            calorieMinEpoch = calorieMinEpoch,
            calorieMaxEpoch = calorieMaxEpoch
        )
    }

    val chartModel = remember(
        hasWeight, hasBp, hasCalories, hasMetabolicLines,
        weightEntries, bpPoints, caloriePoints,
        maintenanceCalories, targetCalories,
        globalMinX, globalMaxX,
        weightUnit, hasPulseSeries, pulsePoints
    ) {
        buildCompoundChartModel(
            hasCalories = hasCalories,
            caloriePoints = caloriePoints,
            hasMetabolicLines = hasMetabolicLines,
            globalMinX = globalMinX,
            globalMaxX = globalMaxX,
            maintenanceCalories = maintenanceCalories,
            targetCalories = targetCalories,
            hasWeight = hasWeight,
            weightEntries = weightEntries,
            weightUnit = weightUnit,
            hasBp = hasBp,
            bpPoints = bpPoints,
            hasPulseSeries = hasPulseSeries,
            pulsePoints = pulsePoints
        )
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

    // Dynamic Y-axis Step Calculations (multiples of 5 or 50 adapted to data range)
    val weightMinY = remember(weightEntries, weightUnit) {
        weightEntries.minOfOrNull { it.getWeightInCurrentUnits(weightUnit) }
    }
    val weightMaxY = remember(weightEntries, weightUnit) {
        weightEntries.maxOfOrNull { it.getWeightInCurrentUnits(weightUnit) }
    }
    val weightStep = remember(weightMinY, weightMaxY) {
        calculateOptimalYStep(weightMinY, weightMaxY, defaultStep = 5.0)
    }

    val bpMinY = remember(bpPoints, hasPulseSeries, pulsePoints) {
        val allPoints = if (hasPulseSeries) bpPoints + pulsePoints else bpPoints
        allPoints.minOfOrNull { minOf(it.systolic, it.diastolic, it.pulse?.toDouble() ?: it.diastolic) }
    }
    val bpMaxY = remember(bpPoints, hasPulseSeries, pulsePoints) {
        val allPoints = if (hasPulseSeries) bpPoints + pulsePoints else bpPoints
        allPoints.maxOfOrNull { maxOf(it.systolic, it.diastolic, it.pulse?.toDouble() ?: it.diastolic) }
    }
    val bpStep = remember(bpMinY, bpMaxY) {
        calculateOptimalYStep(bpMinY, bpMaxY, defaultStep = 5.0)
    }

    val calorieMaxY = remember(caloriePoints, maintenanceCalories, targetCalories) {
        val maxPoints = caloriePoints.maxOfOrNull { it.calories } ?: 0.0
        maxOf(maxPoints, maintenanceCalories ?: 0.0, targetCalories ?: 0.0)
    }
    val calorieStep = remember(calorieMaxY) {
        calculateOptimalYStep(0.0, calorieMaxY, defaultStep = 50.0)
    }

    // Weight Layer
    val weightLine = rememberWeightLine(weightLineColor)
    val weightLayer = if (hasWeight) {
        rememberWeightCartesianLayer(
            weightLine = weightLine,
            globalMinX = globalMinX,
            globalMaxX = globalMaxX,
            axisPosition = Axis.Position.Vertical.Start,
            yStepMultiple = weightStep
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
            axisPosition = if (hasWeight || hasAnyCalories) Axis.Position.Vertical.End else Axis.Position.Vertical.Start,
            yStepMultiple = bpStep
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
            axisPosition = calorieAxisPosition,
            yStepMultiple = calorieStep
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
            axisPosition = calorieAxisPosition,
            yStepMultiple = calorieStep
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

    val horizontalAxisSpacing = remember(daySpan) { calculateHorizontalAxisSpacing(daySpan) }

    val bottomAxisValueFormatter = remember(daySpan) { createBottomAxisValueFormatter(daySpan) }

    val startAxisItemPlacer = remember(hasWeight, hasBp, hasCalories, hasMetabolicLines, weightStep, calorieStep, bpStep) {
        if (hasWeight) {
            VerticalAxis.ItemPlacer.step(step = { weightStep })
        } else if (hasAnyCalories) {
            VerticalAxis.ItemPlacer.step(step = { calorieStep })
        } else {
            VerticalAxis.ItemPlacer.step(step = { bpStep })
        }
    }

    val startAxisValueFormatter = remember(hasWeight, hasBp, hasCalories, hasMetabolicLines, weightUnit) {
        createStartAxisValueFormatter(hasWeight, hasAnyCalories, unitLabel)
    }

    val endAxisItemPlacer = remember(hasBp, hasWeight, hasCalories, hasMetabolicLines, bpStep, calorieStep) {
        if (hasBp) {
            VerticalAxis.ItemPlacer.step(step = { bpStep })
        } else {
            VerticalAxis.ItemPlacer.step(step = { calorieStep })
        }
    }

    val endAxisValueFormatter = remember(hasBp, hasWeight, hasCalories, hasMetabolicLines) {
        createEndAxisValueFormatter(hasBp)
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
        markerController = CartesianMarkerController.rememberShowOnPress(),
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
