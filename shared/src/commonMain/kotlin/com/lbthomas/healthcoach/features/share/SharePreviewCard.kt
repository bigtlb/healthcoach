package com.lbthomas.healthcoach.features.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.ShareImageType
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import healthcoach.shared.generated.resources.Res
import healthcoach.shared.generated.resources.scales
import org.jetbrains.compose.resources.painterResource

// Matches the HealthCoach pink scale color used in ChartImageRenderer
private val HealthCoachPink = Color(0xFFC2185D)

@Composable
fun SharePreviewCard(
    shareText: String,
    dateRange: String,
    imageType: ShareImageType,
    hasData: Boolean,
    imageBitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasData && shareText.isNotEmpty()) {
                    when (imageType) {
                        ShareImageType.CHART -> {
                            imageBitmap?.let { ShareImagePreview(it, "Weight progress chart") }
                        }
                        ShareImageType.ICON -> {
                            imageBitmap?.let { ShareImagePreview(it, "Weight progress icon") }
                        }
                        ShareImageType.NONE -> {
                            val summaryParts = shareText.split(':', limit = 2)
                            if (summaryParts.size > 1) {
                                Text(
                                    text = "${summaryParts[0]}:",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = summaryParts[1].trim(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = shareText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (dateRange.isNotEmpty()) {
                                Text(
                                    text = dateRange,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No weight data for this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareImagePreview(imageBitmap: ImageBitmap, contentDescription: String) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val aspectRatio = imageBitmap.width.toFloat() / imageBitmap.height
        val imageWidth = minOf(maxWidth, 300.dp * aspectRatio)
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(imageWidth)
                .aspectRatio(aspectRatio)
        )
    }
}

/**
 * Mini line chart preview drawn with Compose Canvas.
 * Shows a simplified version of the weight progress line.
 */
@Composable
fun MiniChartPreview(
    entries: List<WeightEntryData>,
    modifier: Modifier = Modifier
) {
    if (entries.size < 2) return

    val weights = entries.map { it.weight }
    val minWeight = weights.min()
    val maxWeight = weights.max()
    val weightRange = (maxWeight - minWeight).coerceAtLeast(0.1)

    val padding = 8f
    val chartColor = HealthCoachPink
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val w = size.width
        val h = size.height
        val usableW = w - padding * 2
        val usableH = h - padding * 2

        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padding + (usableH * i / gridLines)
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(padding, y),
                end = androidx.compose.ui.geometry.Offset(w - padding, y),
                strokeWidth = 1f
            )
        }

        // Draw the weight line — space points by their actual dates, not by index,
        // so the chart matches the regular Vico graph appearance.
        val dates = entries.map { it.date }
        val minDate = dates.minOrNull()?.toEpochDays() ?: 0
        val maxDate = dates.maxOrNull()?.toEpochDays() ?: 0
        val dateRange = (maxDate - minDate).coerceAtLeast(1)

        val points = entries.mapIndexed { index, _ ->
            val dateDay = dates[index].toEpochDays()
            val x = if (dateRange == 0L) w / 2 else padding + (usableW * (dateDay - minDate).toFloat() / dateRange)
            val weightNorm = ((weights[index] - minWeight) / weightRange).toFloat()
            val y = padding + usableH * (1f - weightNorm)
            androidx.compose.ui.geometry.Offset(x, y)
        }

        // Draw line segments
        for (i in 0 until points.size - 1) {
            drawLine(
                color = chartColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2f
            )
        }

        // Draw data points
        points.forEach { point ->
            drawCircle(
                color = chartColor,
                radius = 3f,
                center = point
            )
        }
    }
}

/**
 * Mini icon preview showing the app's scale icon.
 */
@Composable
fun MiniIconPreview(
    modifier: Modifier = Modifier
) {
    val iconPainter = painterResource(Res.drawable.scales)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
fun ShareWarningCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

private val previewWeightEntries = listOf(
    WeightEntryData("1", kotlinx.datetime.LocalDate(2023, 10, 15), 83.5),
    WeightEntryData("2", kotlinx.datetime.LocalDate(2023, 10, 22), 82.8),
    WeightEntryData("3", kotlinx.datetime.LocalDate(2023, 10, 29), 82.1),
    WeightEntryData("4", kotlinx.datetime.LocalDate(2023, 11, 5), 81.4),
    WeightEntryData("5", kotlinx.datetime.LocalDate(2023, 11, 12), 80.7),
    WeightEntryData("6", kotlinx.datetime.LocalDate(2023, 11, 19), 80.0),
    WeightEntryData("7", kotlinx.datetime.LocalDate(2023, 11, 21), 79.4)
)

private val previewWeightGainEntries = listOf(
    WeightEntryData("1", kotlinx.datetime.LocalDate(2023, 10, 15), 79.4),
    WeightEntryData("2", kotlinx.datetime.LocalDate(2023, 10, 22), 80.0),
    WeightEntryData("3", kotlinx.datetime.LocalDate(2023, 10, 29), 80.7),
    WeightEntryData("4", kotlinx.datetime.LocalDate(2023, 11, 5), 81.4),
    WeightEntryData("5", kotlinx.datetime.LocalDate(2023, 11, 12), 82.1),
    WeightEntryData("6", kotlinx.datetime.LocalDate(2023, 11, 19), 82.8),
    WeightEntryData("7", kotlinx.datetime.LocalDate(2023, 11, 21), 83.5)
)

@Preview(name = "Preview Card - With Weights Chart")
@Composable
fun SharePreviewCardWithWeightsChart() {
    HealthCoachTheme {
        Surface {
            SharePreviewCard(
                shareText = "My 30-day weight progress:\n185.2 → 178.6 lbs",
                dateRange = "Oct 22, 2023 - Nov 21, 2023",
                imageType = ShareImageType.CHART,
                hasData = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Preview Card - With Weights Icon")
@Composable
fun SharePreviewCardWithWeightsIcon() {
    HealthCoachTheme {
        Surface {
            SharePreviewCard(
                shareText = "My 30-day weight progress:\n185.2 → 178.6 lbs",
                dateRange = "Oct 22, 2023 - Nov 21, 2023",
                imageType = ShareImageType.ICON,
                hasData = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Preview Card - With Weights Text Only")
@Composable
fun SharePreviewCardWithWeightsTextOnly() {
    HealthCoachTheme {
        Surface {
            SharePreviewCard(
                shareText = "My 30-day weight progress:\n185.2 → 178.6 lbs",
                dateRange = "Oct 22, 2023 - Nov 21, 2023",
                imageType = ShareImageType.NONE,
                hasData = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Preview Card - Privacy Mode")
@Composable
fun SharePreviewCardPrivacyMode() {
    HealthCoachTheme {
        Surface {
            SharePreviewCard(
                shareText = "My 30-day weight progress:\n-6.6 lbs",
                dateRange = "Oct 22, 2023 - Nov 21, 2023",
                imageType = ShareImageType.CHART,
                hasData = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Preview Card - Weight Gain")
@Composable
fun SharePreviewCardWeightGain() {
    HealthCoachTheme {
        Surface {
            SharePreviewCard(
                shareText = "My 7-day weight progress:\n+2.4 lbs",
                dateRange = "Nov 14, 2023 - Nov 21, 2023",
                imageType = ShareImageType.CHART,
                hasData = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Preview Card - Kilograms")
@Composable
fun SharePreviewCardKilograms() {
    HealthCoachTheme {
        Surface {
            SharePreviewCard(
                shareText = "My 30-day weight progress:\n-3.0 kg",
                dateRange = "Oct 22, 2023 - Nov 21, 2023",
                imageType = ShareImageType.CHART,
                hasData = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Preview Card - No Data")
@Composable
fun SharePreviewCardNoData() {
    HealthCoachTheme {
        Surface {
            SharePreviewCard(
                shareText = "",
                dateRange = "",
                imageType = ShareImageType.CHART,
                hasData = false,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Mini Chart Preview - Weight Loss")
@Composable
fun MiniChartPreviewPreview() {
    HealthCoachTheme {
        Surface {
            MiniChartPreview(
                entries = previewWeightEntries,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Mini Chart Preview - Weight Gain")
@Composable
fun MiniChartPreviewWeightGainPreview() {
    HealthCoachTheme {
        Surface {
            MiniChartPreview(
                entries = previewWeightGainEntries,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Mini Icon Preview")
@Composable
fun MiniIconPreviewPreview() {
    HealthCoachTheme {
        Surface {
            MiniIconPreview(modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview(name = "Warning Card")
@Composable
fun ShareWarningCardPreview() {
    HealthCoachTheme {
        Surface {
            ShareWarningCard(
                message = "No weight data available for the selected time period.",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
