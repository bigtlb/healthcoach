package com.lbthomas.healthcoach.core.share

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.ShareImageType
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import healthcoach.shared.generated.resources.Res
import kotlinx.coroutines.runBlocking
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual object ChartImageRenderer {

    private const val IMAGE_WIDTH = 600
    private const val IMAGE_HEIGHT = 400
    private const val CHART_MARGIN_TOP = 50
    private const val CHART_MARGIN_BOTTOM = 80
    private const val CHART_MARGIN_LEFT = 70
    private const val CHART_MARGIN_RIGHT = 30
    private const val BRANDING_HEIGHT = 40

    // Colors matching the HealthCoach theme
    private val BG_COLOR = Color(0xF8, 0xF9, 0xFA)
    private val CHART_LINE_COLOR = Color(0xC2, 0x18, 0x5D) // Pink scale color
    private val TEXT_COLOR = Color(0x1A, 0x1A, 0x2E)
    private val AXIS_COLOR = Color(0xBD, 0xBD, 0xBD)
    private val GRID_COLOR = Color(0xE5, 0xE5, 0xE5)
    private val BRANDING_COLOR = Color(0x9E, 0x9E, 0x9E)

    private fun loadIconImage(): BufferedImage? {
        return try {
            val bytes = runBlocking { Res.readBytes("drawable/scales_icon.png") }
            ImageIO.read(ByteArrayInputStream(bytes))
        } catch (e: Exception) {
            null
        }
    }

    actual fun render(
        entries: List<WeightEntryData>,
        allEntries: List<WeightEntryData>,
        timeFrame: GraphTimeFrame,
        weightUnit: WeightUnit,
        imageType: ShareImageType,
        showAbsoluteWeight: Boolean
    ): ImageBitmap? {
        return when (imageType) {
            ShareImageType.CHART -> renderChart(entries, allEntries, timeFrame, weightUnit, showAbsoluteWeight)
            ShareImageType.ICON -> renderIcon(entries, allEntries, timeFrame, weightUnit, showAbsoluteWeight)
            ShareImageType.NONE -> null
        }
    }

    actual fun renderChart(
        entries: List<WeightEntryData>,
        allEntries: List<WeightEntryData>,
        timeFrame: GraphTimeFrame,
        weightUnit: WeightUnit,
        showAbsoluteWeight: Boolean
    ): ImageBitmap? {
        if (entries.isEmpty()) return null

        val sorted = entries.sortedBy { it.date }
        val weights = sorted.map { it.getWeightInCurrentUnits(weightUnit) }
        val minWeight = weights.minOrNull() ?: 0.0
        val maxWeight = weights.maxOrNull() ?: 0.0
        val weightRange = maxWeight - minWeight

        // Add padding to weight range
        val paddedMin = if (weightRange > 0) minWeight - weightRange * 0.1 else minWeight - 2.0
        val paddedMax = if (weightRange > 0) maxWeight + weightRange * 0.1 else maxWeight + 2.0
        val paddedRange = paddedMax - paddedMin

        val chartWidth = IMAGE_WIDTH - CHART_MARGIN_LEFT - CHART_MARGIN_RIGHT
        val chartHeight = IMAGE_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM - BRANDING_HEIGHT

        val image = BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics() as Graphics2D
        g.addRenderingHints(RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON))
        g.addRenderingHints(RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON))

        // Background
        g.color = BG_COLOR
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)

        // Title
        g.color = TEXT_COLOR
        g.font = Font("SansSerif", Font.BOLD, 18)
        val title = "Weight Progress (${timeFrame.label})"
        val titleMetrics = g.fontMetrics
        val titleWidth = titleMetrics.stringWidth(title)
        g.drawString(title, (IMAGE_WIDTH - titleWidth) / 2, 30)

        // Grid lines
        g.color = GRID_COLOR
        g.font = Font("SansSerif", Font.PLAIN, 11)
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = CHART_MARGIN_TOP + (chartHeight * i / gridLines).toInt()
            g.drawLine(CHART_MARGIN_LEFT, y, IMAGE_WIDTH - CHART_MARGIN_RIGHT, y)

            if (showAbsoluteWeight) {
                val value = paddedMax - (paddedRange * i.toDouble() / gridLines)
                val label = "%.1f".format(value)
                g.color = AXIS_COLOR
                val labelMetrics = g.fontMetrics
                g.drawString(label, CHART_MARGIN_LEFT - labelMetrics.stringWidth(label) - 5, y + 4)
                g.color = GRID_COLOR
            }
        }

        // Y-axis label
        if (showAbsoluteWeight) {
            g.color = AXIS_COLOR
            g.font = Font("SansSerif", Font.PLAIN, 10)
            val unitLabel = if (weightUnit == WeightUnit.METRIC) "kg" else "lbs"
            g.drawString(unitLabel, 5, CHART_MARGIN_TOP + 10)
        }

        // Draw the weight line
        if (sorted.size > 1) {
            g.color = CHART_LINE_COLOR
            g.stroke = BasicStroke(3f)

            val firstDate = sorted.first().date.toEpochDays()
            val lastDate = sorted.last().date.toEpochDays()
            val dateRange = (lastDate - firstDate).coerceAtLeast(1)

            for (i in 0 until sorted.size - 1) {
                val entry1 = sorted[i]
                val entry2 = sorted[i + 1]
                val x1 = CHART_MARGIN_LEFT + ((entry1.date.toEpochDays() - firstDate).toDouble() / dateRange * chartWidth).toInt()
                val w1 = entry1.getWeightInCurrentUnits(weightUnit)
                val y1 = CHART_MARGIN_TOP + chartHeight - ((w1 - paddedMin) / paddedRange * chartHeight).toInt()
                val x2 = CHART_MARGIN_LEFT + ((entry2.date.toEpochDays() - firstDate).toDouble() / dateRange * chartWidth).toInt()
                val w2 = entry2.getWeightInCurrentUnits(weightUnit)
                val y2 = CHART_MARGIN_TOP + chartHeight - ((w2 - paddedMin) / paddedRange * chartHeight).toInt()
                g.drawLine(x1, y1, x2, y2)
            }

            // Draw data points
            g.color = CHART_LINE_COLOR
            for (entry in sorted) {
                val x = CHART_MARGIN_LEFT + ((entry.date.toEpochDays() - firstDate).toDouble() / dateRange * chartWidth).toInt()
                val weight = entry.getWeightInCurrentUnits(weightUnit)
                val y = CHART_MARGIN_TOP + chartHeight - ((weight - paddedMin) / paddedRange * chartHeight).toInt()
                g.fillOval(x - 3, y - 3, 6, 6)
            }
        } else if (sorted.size == 1) {
            // Single point
            val entry = sorted.first()
            val x = CHART_MARGIN_LEFT + chartWidth / 2
            val weight = entry.getWeightInCurrentUnits(weightUnit)
            val y = CHART_MARGIN_TOP + chartHeight - ((weight - paddedMin) / paddedRange * chartHeight).toInt()
            g.color = CHART_LINE_COLOR
            g.fillOval(x - 5, y - 5, 10, 10)
        }

        // Date axis labels
        g.color = AXIS_COLOR
        g.font = Font("SansSerif", Font.PLAIN, 10)
        val dateLabels = minOf(sorted.size, 6)
        for (i in 0 until dateLabels) {
            val index = if (sorted.size > 1) i * (sorted.size - 1) / (dateLabels - 1) else 0
            val entry = sorted[index]
            val dateStr = "${entry.date.month.name.take(3)} ${entry.date.day}"
            val x = if (sorted.size > 1) {
                val firstDate = sorted.first().date.toEpochDays()
                val lastDate = sorted.last().date.toEpochDays()
                val dateRange = (lastDate - firstDate).coerceAtLeast(1)
                CHART_MARGIN_LEFT + ((entry.date.toEpochDays() - firstDate).toDouble() / dateRange * chartWidth).toInt()
            } else {
                CHART_MARGIN_LEFT + chartWidth / 2
            }
            val metrics = g.fontMetrics
            val centerX = x - metrics.stringWidth(dateStr) / 2
            val y = CHART_MARGIN_TOP + chartHeight + 15
            g.drawString(dateStr, centerX, y)
        }

        // Summary text
        val summaryY = CHART_MARGIN_TOP + chartHeight + 40
        g.color = TEXT_COLOR
        g.font = Font("SansSerif", Font.PLAIN, 13)
        val formatted = ShareTextFormatter.format(entries, allEntries, timeFrame, weightUnit, showAbsoluteWeight)
        val summaryText = formatted.first
        val dateRangeText = formatted.second
        val summaryMetrics = g.fontMetrics
        val summaryWidth = summaryMetrics.stringWidth(summaryText)
        g.drawString(summaryText, (IMAGE_WIDTH - summaryWidth) / 2, summaryY)

        // Date range line
        if (dateRangeText.isNotEmpty()) {
            g.font = Font("SansSerif", Font.PLAIN, 11)
            g.color = AXIS_COLOR
            val dateRangeMetrics = g.fontMetrics
            val dateRangeWidth = dateRangeMetrics.stringWidth(dateRangeText)
            g.drawString(dateRangeText, (IMAGE_WIDTH - dateRangeWidth) / 2, summaryY + 18)
        }

        // Branding
        g.color = BRANDING_COLOR
        g.font = Font("SansSerif", Font.PLAIN, 10)
        val branding = "Powered by HealthCoach"
        val brandingMetrics = g.fontMetrics
        val brandingWidth = brandingMetrics.stringWidth(branding)
        g.drawString(branding, (IMAGE_WIDTH - brandingWidth) / 2, IMAGE_HEIGHT - 15)

        // Draw small icon next to branding
        val iconImage = loadIconImage()
        if (iconImage != null) {
            val iconSize = 20
            val iconX = IMAGE_WIDTH - brandingWidth - iconSize - 30
            val iconY = IMAGE_HEIGHT - BRANDING_HEIGHT + 5
            g.drawImage(iconImage, iconX, iconY, iconSize, iconSize, null)
        }

        g.dispose()
        return image.toComposeImageBitmap()
    }

    actual fun renderIcon(
        entries: List<WeightEntryData>,
        allEntries: List<WeightEntryData>,
        timeFrame: GraphTimeFrame,
        weightUnit: WeightUnit,
        showAbsoluteWeight: Boolean
    ): ImageBitmap? {
        if (entries.isEmpty()) return null

        // Smaller square image for icon type
        val width = 500
        val height = 500

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics() as Graphics2D
        g.addRenderingHints(RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON))
        g.addRenderingHints(RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON))

        // Background
        g.color = BG_COLOR
        g.fillRect(0, 0, width, height)

        // Draw large app icon (centered vertically with text below)
        val iconImage = loadIconImage()
        val iconSize = 200
        val iconY = 80
        if (iconImage != null) {
            val iconX = (width - iconSize) / 2
            g.drawImage(iconImage, iconX, iconY, iconSize, iconSize, null)
        }

        // Summary text (includes the full progress phrase, so no separate title needed)
        val summaryY = iconY + iconSize + 30
        g.color = TEXT_COLOR
        val formatted = ShareTextFormatter.format(entries, allEntries, timeFrame, weightUnit, showAbsoluteWeight)
        val summaryText = formatted.first
        val dateRangeText = formatted.second
        g.font = Font("SansSerif", Font.BOLD, 16)
        val summaryMetrics = g.fontMetrics
        val summaryWidth = summaryMetrics.stringWidth(summaryText)
        g.drawString(summaryText, (width - summaryWidth) / 2, summaryY)

        // Date range line
        if (dateRangeText.isNotEmpty()) {
            g.font = Font("SansSerif", Font.PLAIN, 13)
            g.color = AXIS_COLOR
            val dateRangeMetrics = g.fontMetrics
            val dateRangeWidth = dateRangeMetrics.stringWidth(dateRangeText)
            g.drawString(dateRangeText, (width - dateRangeWidth) / 2, summaryY + 25)
        }

        // Branding
        g.color = BRANDING_COLOR
        g.font = Font("SansSerif", Font.PLAIN, 10)
        val branding = "Powered by HealthCoach"
        val brandingMetrics = g.fontMetrics
        val brandingWidth = brandingMetrics.stringWidth(branding)
        g.drawString(branding, (width - brandingWidth) / 2, height - 15)

        g.dispose()
        return image.toComposeImageBitmap()
    }
}
