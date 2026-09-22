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
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.utils.displayDate
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
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
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.MarkerCornerBasedShape
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun GraphsView(modifier: Modifier = Modifier) {
    val weightViewModel = koinInject<WeightViewModel>()
    val settingsViewModel = koinInject<SettingsViewModel>()

    val rawEntries by weightViewModel.entries.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val sortedEntries = remember(rawEntries) {
        rawEntries.sortedBy { it.date }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (sortedEntries.isEmpty()) {
            EmptyGraphState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Weight History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
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
                        WeightLineChart(
                            entries = sortedEntries,
                            weightUnit = settings.weightUnit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
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

private object WeightChartRangeProvider : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val diff = maxY - minY
        val padding = if (diff <= 0.0) 5.0 else max(1.0, diff * 0.15)
        return (minY - padding).coerceAtLeast(0.0)
    }

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val diff = maxY - minY
        val padding = if (diff <= 0.0) 5.0 else max(1.0, diff * 0.15)
        return maxY + padding
    }
}

@Composable
private fun WeightLineChart(
    entries: List<WeightEntryData>,
    weightUnit: WeightUnit,
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

    val lineLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(line),
        rangeProvider = remember { WeightChartRangeProvider }
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

    val bottomAxisValueFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            val date = LocalDate.fromEpochDays(value.toLong())
            "${date.month.ordinal + 1}/${date.day}"
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
            guideline = null
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
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