package com.lbthomas.healthcoach.features.graphs.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.utils.displayDate
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.MarkerCornerBasedShape
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import kotlinx.datetime.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

internal class TimeFrameChartRangeProvider(
    private val forcedMinX: Double?,
    private val forcedMaxX: Double?,
    private val forcedMinY: Double? = null,
    private val forcedMaxY: Double? = null,
    private val minPadding: Double = 5.0,
    private val maxPadding: Double = 5.0,
    private val yPaddingFraction: Double = 0.05,
    private val yStepMultiple: Double? = null
) : CartesianLayerRangeProvider {
    override fun getMinX(minX: Double, maxX: Double, extraStore: ExtraStore): Double {
        return forcedMinX ?: minX
    }

    override fun getMaxX(minX: Double, maxX: Double, extraStore: ExtraStore): Double {
        return (forcedMaxX ?: maxX) + 2.0
    }

    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        if (forcedMinY != null) return forcedMinY
        val diff = maxY - minY
        val padding = if (diff <= 0.0) minPadding else max(1.0, diff * yPaddingFraction)
        val rawMin = (minY - padding).coerceAtLeast(0.0)
        return if (yStepMultiple != null && yStepMultiple > 0.0) {
            floor(rawMin / yStepMultiple) * yStepMultiple
        } else {
            rawMin
        }
    }

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        if (forcedMaxY != null) return forcedMaxY
        val diff = maxY - minY
        val padding = if (diff <= 0.0) maxPadding else max(1.0, diff * yPaddingFraction)
        val rawMax = maxY + padding
        return if (yStepMultiple != null && yStepMultiple > 0.0) {
            ceil(rawMax / yStepMultiple) * yStepMultiple
        } else {
            rawMax
        }
    }
}

@Composable
internal fun rememberHealthChartMarker(
    hasWeight: Boolean,
    hasBp: Boolean,
    hasCalories: Boolean = false,
    weightUnit: WeightUnit,
    weightLineColor: Color,
    bpSystolicColor: Color,
    bpDiastolicColor: Color,
    bpPulseColor: Color,
    calorieColor: Color = Color(0xFF2E7D32),
    maintenanceLineColor: Color = Color.Unspecified,
    targetLineColor: Color = Color.Unspecified,
    unitLabel: String
): CartesianMarker {
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
        lineCount = 5,
        padding = Insets(horizontal = 10.dp, vertical = 6.dp),
        background = markerLabelBackground
    )

    val markerValueFormatter = remember(hasWeight, hasBp, hasCalories, weightUnit, maintenanceLineColor, targetLineColor) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val points = targets.filterIsInstance<LineCartesianLayerMarkerTarget>().flatMap { it.points }
            val columns = targets.filterIsInstance<ColumnCartesianLayerMarkerTarget>().flatMap { it.columns }

            val firstX = points.firstOrNull()?.entry?.x ?: columns.firstOrNull()?.entry?.x
            if (firstX != null) {
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
                        maintenanceLineColor -> lines.add("Maintenance: $formatted kcal")
                        targetLineColor -> lines.add("Target: $formatted kcal")
                        else -> lines.add(formatted)
                    }
                }

                columns.forEach { column ->
                    val y = column.entry.y
                    val rounded = round(y * 10) / 10.0
                    val formatted = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
                    lines.add("Calories: $formatted kcal")
                }

                lines.joinToString("\n")
            } else {
                ""
            }
        }
    }

    return rememberDefaultCartesianMarker(
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
}
