package com.lbthomas.healthcoach.features.graphs

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
import com.lbthomas.healthcoach.core.utils.displayDate
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
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

internal data class GraphEntries(
    val entries: List<WeightEntryData>,
    val minEpochDay: Double?,
    val maxEpochDay: Double?
)

internal fun buildGraphEntries(
    rawEntries: List<WeightEntryData>,
    timeFrame: GraphTimeFrame
): GraphEntries {
    val sorted = rawEntries.sortedBy { it.date }

    if (sorted.isEmpty()) {
        return GraphEntries(
            entries = emptyList(),
            minEpochDay = null,
            maxEpochDay = null
        )
    }

    if (timeFrame == GraphTimeFrame.ALL) {
        return GraphEntries(
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
        val days = timeFrame.days ?: return GraphEntries(
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


    return GraphEntries(
        entries = listOfNotNull(interpolateEdgeEntry()) + entriesInRange,
        minEpochDay = minEpochDay.toDouble(),
        maxEpochDay = maxEpochDay
    )
}

@Composable
fun GraphsView(modifier: Modifier = Modifier) {
    val weightViewModel = koinInject<WeightViewModel>()
    val settingsViewModel = koinInject<SettingsViewModel>()

    val rawEntries by weightViewModel.entries.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val selectedTimeFrame = settings.selectedGraphTimeFrame

    val graphEntries = remember(rawEntries, selectedTimeFrame) {
        buildGraphEntries(
            rawEntries = rawEntries,
            timeFrame = selectedTimeFrame
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (graphEntries.entries.isEmpty()) {
            EmptyGraphState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                GraphHeader(selectedTimeFrame, settingsViewModel)

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
                        WeightLineChart(
                            entries = graphEntries.entries,
                            weightUnit = settings.weightUnit,
                            minEpochDay = graphEntries.minEpochDay,
                            maxEpochDay = graphEntries.maxEpochDay,
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
    settingsViewModel: SettingsViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Weight History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
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
private fun EmptyGraphState(modifier: Modifier = Modifier) {
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
                text = "No Weight Data Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Log weight entries in the Weight tab to view your progress graph.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private class TimeFrameChartRangeProvider(
    private val forcedMinX: Double?,
    private val forcedMaxX: Double?
) : CartesianLayerRangeProvider {
    override fun getMinX(minX: Double, maxX: Double, extraStore: ExtraStore): Double {
        return forcedMinX ?: minX
    }

    override fun getMaxX(minX: Double, maxX: Double, extraStore: ExtraStore): Double {
        return (forcedMaxX ?: maxX) + 2.0
    }

    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val diff = maxY - minY
        val padding = if (diff <= 0.0) 5.0 else max(1.0, diff * 0.05)
        return (minY - padding).coerceAtLeast(0.0)
    }

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val diff = maxY - minY
        val padding = if (diff <= 0.0) 5.0 else max(1.0, diff * 0.05)
        return maxY + padding
    }
}


@Composable
private fun WeightLineChart(
    entries: List<WeightEntryData>,
    weightUnit: WeightUnit,
    minEpochDay: Double?,
    maxEpochDay: Double?,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(entries, weightUnit) {
        val xValues = entries.map { it.date.toEpochDays().toDouble() }
        val yValues = entries.map { it.getWeightInCurrentUnits(weightUnit) }
        modelProducer.runTransaction {
            lineModel {
                series(x = xValues, y = yValues)
            }
        }
    }

    val unitLabel = if (weightUnit == WeightUnit.METRIC) "kg" else "lb"
    val chartLineColor = Color(0xFF1E88E5)

    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(chartLineColor)),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.5.dp),
        pointProvider = null,
        areaFill = null
    )

    val rangeProvider = remember(minEpochDay, maxEpochDay) {
        TimeFrameChartRangeProvider(
            forcedMinX = minEpochDay,
            forcedMaxX = maxEpochDay
        )
    }

    val lineLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(line),
        rangeProvider = rangeProvider
    )

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
        lineCount = 2,
        padding = Insets(horizontal = 10.dp, vertical = 6.dp),
        background = markerLabelBackground
    )

    val markerValueFormatter = remember(weightUnit) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val lineTarget = targets.filterIsInstance<LineCartesianLayerMarkerTarget>().firstOrNull()
            val point = lineTarget?.points?.firstOrNull()
            if (point != null) {
                val date = LocalDate.fromEpochDays(point.entry.x.toLong())
                val dateText = date.displayDate()
                val roundedWeight = round(point.entry.y * 10) / 10.0
                val weightFormatted = if (roundedWeight % 1.0 == 0.0) {
                    roundedWeight.toInt().toString()
                } else {
                    roundedWeight.toString()
                }
                "$dateText\n$weightFormatted $unitLabel"
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


    val daySpan = remember(entries) {
        if (entries.size >= 2) {
            entries.last().date.toEpochDays() - entries.first().date.toEpochDays()
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

    val startAxisValueFormatter = remember(weightUnit) {
        CartesianValueFormatter { _, value, _ ->
            val rounded = round(value * 10) / 10.0
            if (rounded % 1.0 == 0.0) {
                "${rounded.toInt()} $unitLabel"
            } else {
                "$rounded $unitLabel"
            }
        }
    }

    val chart = rememberCartesianChart(
        lineLayer,
        startAxis = VerticalAxis.rememberStart(
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
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
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
        ),
        marker = marker,
        markerController = CartesianMarkerController.rememberShowOnHover()
    )


    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        modifier = modifier
    )
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