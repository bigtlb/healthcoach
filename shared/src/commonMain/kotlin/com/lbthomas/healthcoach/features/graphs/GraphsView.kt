package com.lbthomas.healthcoach.features.graphs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.core.utils.displayDate
import com.lbthomas.healthcoach.core.utils.formatTime
import com.lbthomas.healthcoach.features.bloodpressure.BloodPressureViewModel
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.MarkerCornerBasedShape
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import kotlinx.datetime.LocalDate
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import kotlin.math.max
import kotlin.math.round

internal data class WeightGraphEntries(
    val entries: List<WeightEntryData>,
    val minEpochDay: Double?,
    val maxEpochDay: Double?
)

internal data class BpGraphPoint(
    val id: Long,
    val x: Double,
    val date: LocalDate,
    val systolic: Double,
    val diastolic: Double,
    val pulse: Int? = null,
    val timeFormatted: String? = null
)

internal data class BpGraphEntries(
    val points: List<BpGraphPoint>,
    val minEpochDay: Double?,
    val maxEpochDay: Double?
)

internal fun buildWeightGraphEntries(
    rawEntries: List<WeightEntryData>,
    timeFrame: GraphTimeFrame
): WeightGraphEntries {
    val sorted = rawEntries.sortedBy { it.date }

    if (sorted.isEmpty()) {
        return WeightGraphEntries(
            entries = emptyList(),
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    if (timeFrame == GraphTimeFrame.ALL) {
        return WeightGraphEntries(
            entries = sorted,
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    val latestDate = sorted.last().date
    val latestEpochDay = latestDate.toEpochDays()
    val minEpochDay = if (timeFrame == GraphTimeFrame.YEAR_TO_DATE) {
        LocalDate(latestDate.year, 1, 1).toEpochDays()
    } else {
        val days = timeFrame.days ?: return WeightGraphEntries(
            entries = sorted,
            minEpochDay = null,
            maxEpochDay = null
        )
        latestEpochDay - days
    }
    val maxEpochDay = latestEpochDay.toDouble()

    val entriesInRange = sorted.filter { it.date.toEpochDays() >= minEpochDay }

    val previousEntry = sorted.lastOrNull { it.date.toEpochDays() < minEpochDay }
    val firstEntryInRange = entriesInRange.firstOrNull()

    fun interpolateEdgeEntry(): WeightEntryData? = if (previousEntry != null && firstEntryInRange != null && firstEntryInRange.date.toEpochDays() > minEpochDay) {
        val previousX = previousEntry.date.toEpochDays()
        val firstX = firstEntryInRange.date.toEpochDays()
        val rangeWidth = firstX - previousX

        if (rangeWidth > 0) {
            val progress = (minEpochDay - previousX).toDouble() / rangeWidth.toDouble()
            val interpolatedWeight =
                previousEntry.weight + ((firstEntryInRange.weight - previousEntry.weight) * progress)

            WeightEntryData(
                id = Long.MIN_VALUE,
                date = LocalDate.fromEpochDays(minEpochDay),
                weight = interpolatedWeight
            )
        } else {
            null
        }
    } else {
        null
    }

    return WeightGraphEntries(
        entries = listOfNotNull(interpolateEdgeEntry()) + entriesInRange,
        minEpochDay = minEpochDay.toDouble(),
        maxEpochDay = maxEpochDay
    )
}

internal fun buildBpGraphEntries(
    rawEntries: List<BloodPressureEntryData>,
    timeFrame: GraphTimeFrame
): BpGraphEntries {
    val sorted = rawEntries.sortedWith(
        compareBy<BloodPressureEntryData> { it.date }
            .thenBy { it.time?.toString() ?: "" }
    )

    if (sorted.isEmpty()) {
        return BpGraphEntries(
            points = emptyList(),
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    val points = sorted.map { entry ->
        val fractionOfDay = if (entry.hasTime && entry.time != null) {
            (entry.time!!.hour * 3600 + entry.time!!.minute * 60 + entry.time!!.second) / 86400.0
        } else {
            0.0
        }
        BpGraphPoint(
            id = entry.id,
            x = entry.date.toEpochDays().toDouble() + fractionOfDay,
            date = entry.date,
            systolic = entry.systolic.toDouble(),
            diastolic = entry.diastolic.toDouble(),
            pulse = entry.pulse,
            timeFormatted = entry.time?.formatTime()
        )
    }

    if (timeFrame == GraphTimeFrame.ALL) {
        return BpGraphEntries(
            points = points,
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    val latestDate = sorted.last().date
    val latestEpochDay = latestDate.toEpochDays()
    val minEpochDay = if (timeFrame == GraphTimeFrame.YEAR_TO_DATE) {
        LocalDate(latestDate.year, 1, 1).toEpochDays()
    } else {
        val days = timeFrame.days ?: return BpGraphEntries(
            points = points,
            minEpochDay = null,
            maxEpochDay = null
        )
        latestEpochDay - days
    }
    val maxEpochDay = latestEpochDay.toDouble()

    val filteredPoints = points.filter { it.x >= minEpochDay.toDouble() }

    return BpGraphEntries(
        points = filteredPoints,
        minEpochDay = minEpochDay.toDouble(),
        maxEpochDay = maxEpochDay
    )
}

@Composable
fun GraphsView(modifier: Modifier = Modifier) {
    val weightViewModel = koinInject<WeightViewModel>()
    val bloodPressureViewModel = koinInject<BloodPressureViewModel>()
    val settingsViewModel = koinInject<SettingsViewModel>()

    val rawWeightEntries by weightViewModel.entries.collectAsState()
    val rawBpEntries by bloodPressureViewModel.entries.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val selectedTimeFrame = settings.selectedGraphTimeFrame

    val showWeight = settings.showWeightInGraph
    val showBp = settings.showBloodPressureInGraph

    val weightGraphEntries = remember(rawWeightEntries, selectedTimeFrame) {
        buildWeightGraphEntries(
            rawEntries = rawWeightEntries,
            timeFrame = selectedTimeFrame
        )
    }

    val bpGraphEntries = remember(rawBpEntries, selectedTimeFrame) {
        buildBpGraphEntries(
            rawEntries = rawBpEntries,
            timeFrame = selectedTimeFrame
        )
    }

    val hasWeightData = showWeight && weightGraphEntries.entries.isNotEmpty()
    val hasBpData = showBp && bpGraphEntries.points.isNotEmpty()
    val hasAnyData = hasWeightData || hasBpData

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            GraphHeader(
                selectedTimeFrame = selectedTimeFrame,
                settings = settings,
                settingsViewModel = settingsViewModel
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (!hasAnyData) {
                        EmptyGraphState(
                            message = if (!showWeight && !showBp) {
                                "Enable Weight or Blood Pressure above to view graph."
                            } else {
                                "No data available for the selected series and timeframe."
                            }
                        )
                    } else {
                        CompoundHealthChart(
                            showWeight = showWeight,
                            showBp = showBp,
                            showPulse = settings.showPulseInGraph,
                            weightEntries = if (showWeight) weightGraphEntries.entries else emptyList(),
                            bpPoints = if (showBp) bpGraphEntries.points else emptyList(),
                            weightUnit = settings.weightUnit,
                            weightMinEpoch = weightGraphEntries.minEpochDay,
                            weightMaxEpoch = weightGraphEntries.maxEpochDay,
                            bpMinEpoch = bpGraphEntries.minEpochDay,
                            bpMaxEpoch = bpGraphEntries.maxEpochDay,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphHeader(
    selectedTimeFrame: GraphTimeFrame,
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Series Selection Checkboxes
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = settings.showWeightInGraph,
                    onCheckedChange = { settingsViewModel.setShowWeightInGraph(it) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Weight",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = settings.showBloodPressureInGraph,
                    onCheckedChange = { settingsViewModel.setShowBloodPressureInGraph(it) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Blood Pressure",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Time Frame Dropdown
        GraphTimeFrameDropdown(
            selectedTimeFrame = selectedTimeFrame,
            onTimeFrameSelected = { settingsViewModel.setSelectedGraphTimeFrame(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GraphTimeFrameDropdown(
    selectedTimeFrame: GraphTimeFrame,
    onTimeFrameSelected: (GraphTimeFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.width(160.dp)
    ) {
        OutlinedTextField(
            value = selectedTimeFrame.label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Time frame") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GraphTimeFrame.entries.forEach { timeFrame ->
                DropdownMenuItem(
                    text = { Text(timeFrame.label) },
                    onClick = {
                        onTimeFrameSelected(timeFrame)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyGraphState(
    message: String = "Log weight or blood pressure entries to view your progress graph.",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Graph Data Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private class TimeFrameChartRangeProvider(
    private val forcedMinX: Double?,
    private val forcedMaxX: Double?,
    private val minPadding: Double = 5.0,
    private val maxPadding: Double = 5.0
) : CartesianLayerRangeProvider {
    override fun getMinX(minX: Double, maxX: Double, extraStore: ExtraStore): Double {
        return forcedMinX ?: minX
    }

    override fun getMaxX(minX: Double, maxX: Double, extraStore: ExtraStore): Double {
        return (forcedMaxX ?: maxX) + 2.0
    }

    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val diff = maxY - minY
        val padding = if (diff <= 0.0) minPadding else max(1.0, diff * 0.05)
        return (minY - padding).coerceAtLeast(0.0)
    }

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val diff = maxY - minY
        val padding = if (diff <= 0.0) maxPadding else max(1.0, diff * 0.05)
        return maxY + padding
    }
}

@Composable
private fun CompoundHealthChart(
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

    val markerLabelBackground = rememberShapeComponent(
        fill = Fill(MaterialTheme.colorScheme.surfaceVariant),
        shape = MarkerCornerBasedShape(RoundedCornerShape(8.dp), tickSize = 6.dp),
        strokeFill = Fill(MaterialTheme.colorScheme.outlineVariant),
        strokeThickness = 1.dp
    )

    val markerLabel = rememberTextComponent(
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        lineCount = 4,
        padding = Insets(horizontal = 10.dp, vertical = 6.dp),
        background = markerLabelBackground
    )

    val markerValueFormatter = remember(hasWeight, hasBp, weightUnit) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val points = targets.filterIsInstance<LineCartesianLayerMarkerTarget>().flatMap { it.points }
            if (points.isNotEmpty()) {
                val firstX = points.first().entry.x
                val date = LocalDate.fromEpochDays(firstX.toLong())
                val lines = mutableListOf(date.displayDate())

                points.forEach { point ->
                    val y = point.entry.y
                    val rounded = round(y * 10) / 10.0
                    val formatted = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
                    when (point.color) {
                        weightLineColor -> lines.add("Weight: $formatted $unitLabel")
                        bpSystolicColor -> lines.add("Systolic: $formatted mmHg")
                        bpDiastolicColor -> lines.add("Diastolic: $formatted mmHg")
                        bpPulseColor -> lines.add("Pulse: $formatted bpm")
                        else -> lines.add(formatted)
                    }
                }
                lines.joinToString("\n")
            } else {
                ""
            }
        }
    }

    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = markerValueFormatter,
        labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
        indicator = { color ->
            ShapeComponent(
                fill = Fill(color),
                shape = CircleShape
            )
        },
        indicatorSize = 8.dp,
        guideline = null
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
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasWeight) {
                LegendItem(color = weightLineColor, label = "Weight ($unitLabel)")
            }
            if (hasWeight && hasBp) {
                Spacer(modifier = Modifier.width(16.dp))
            }
            if (hasBp) {
                LegendItem(color = bpSystolicColor, label = "Systolic (mmHg)")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = bpDiastolicColor, label = "Diastolic (mmHg)")
                if (showPulse) {
                    Spacer(modifier = Modifier.width(12.dp))
                    LegendItem(color = bpPulseColor, label = "Pulse (bpm)")
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
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun GraphsViewPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            GraphsView()
        }
    )
}
