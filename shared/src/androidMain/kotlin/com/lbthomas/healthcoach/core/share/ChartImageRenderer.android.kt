package com.lbthomas.healthcoach.core.share

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.ShareImageType
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import healthcoach.shared.generated.resources.Res
import kotlinx.coroutines.runBlocking

actual object ChartImageRenderer {

    private const val IMAGE_WIDTH = 600
    private const val IMAGE_HEIGHT = 400
    private const val RENDER_SCALE = 2
    private const val CHART_MARGIN_TOP = 50
    private const val CHART_MARGIN_BOTTOM = 80
    private const val CHART_MARGIN_LEFT = 70
    private const val CHART_MARGIN_RIGHT = 30
    private const val BRANDING_HEIGHT = 40

    // Colors matching the HealthCoach theme
    private val BG_COLOR = Color.argb(0xFF, 0xF8, 0xF9, 0xFA)
    private val CHART_LINE_COLOR = Color.argb(0xFF, 0xC2, 0x18, 0x5D) // Pink scale color
    private val TEXT_COLOR = Color.argb(0xFF, 0x1A, 0x1A, 0x2E)
    private val AXIS_COLOR = Color.argb(0xFF, 0xBD, 0xBD, 0xBD)
    private val GRID_COLOR = Color.argb(0xFF, 0xE5, 0xE5, 0xE5)
    private val BRANDING_COLOR = Color.argb(0xFF, 0x9E, 0x9E, 0x9E)

    private fun loadIconBitmap(): Bitmap? {
        return try {
            val bytes = runBlocking { Res.readBytes("drawable/scales_icon.png") }
            val options = BitmapFactory.Options().apply {
                inScaled = false  // Return exact pixel dimensions, no density scaling
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
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

        val paddedMin = if (weightRange > 0) minWeight - weightRange * 0.1 else minWeight - 2.0
        val paddedMax = if (weightRange > 0) maxWeight + weightRange * 0.1 else maxWeight + 2.0
        val paddedRange = paddedMax - paddedMin

        val chartMarginLeft = if (showAbsoluteWeight) CHART_MARGIN_LEFT else CHART_MARGIN_RIGHT
        val chartWidth = IMAGE_WIDTH - chartMarginLeft - CHART_MARGIN_RIGHT
        val chartHeight = IMAGE_HEIGHT - CHART_MARGIN_TOP - CHART_MARGIN_BOTTOM - BRANDING_HEIGHT

        val bitmap = Bitmap.createBitmap(
            IMAGE_WIDTH * RENDER_SCALE,
            IMAGE_HEIGHT * RENDER_SCALE,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.scale(RENDER_SCALE.toFloat(), RENDER_SCALE.toFloat())

        // Background
        canvas.drawColor(BG_COLOR)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_COLOR
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            isDither = true
            isAntiAlias = true
        }
        val title = "Weight Progress (${timeFrame.label})"
        val titleWidth = titlePaint.measureText(title)
        canvas.drawText(title, (IMAGE_WIDTH - titleWidth) / 2f, 30f, titlePaint)

        // Grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRID_COLOR
            strokeWidth = 1f
            isDither = true
            isAntiAlias = true
        }
        val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AXIS_COLOR
            textSize = 11f
            isDither = true
            isAntiAlias = true
        }

        val gridLines = 5
        for (i in 0..gridLines) {
            val y = CHART_MARGIN_TOP + (chartHeight * i / gridLines).toInt()
            canvas.drawLine(chartMarginLeft.toFloat(), y.toFloat(), (IMAGE_WIDTH - CHART_MARGIN_RIGHT).toFloat(), y.toFloat(), gridPaint)

            if (showAbsoluteWeight) {
                val value = paddedMax - (paddedRange * i.toDouble() / gridLines)
                val label = String.format("%.1f", value)
                canvas.drawText(label, chartMarginLeft - axisLabelPaint.measureText(label) - 5f, y + 4f, axisLabelPaint)
            }
        }

        // Y-axis label
        if (showAbsoluteWeight) {
            val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AXIS_COLOR
                textSize = 10f
                isDither = true
                isAntiAlias = true
            }
            val unitLabel = if (weightUnit == WeightUnit.METRIC) "kg" else "lbs"
            canvas.drawText(unitLabel, 5f, CHART_MARGIN_TOP + 10f, unitPaint)
        }

        // Draw the weight line
        if (sorted.size > 1) {
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CHART_LINE_COLOR
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isDither = true
                isAntiAlias = true
            }

            val firstDate = sorted.first().date.toEpochDays()
            val lastDate = sorted.last().date.toEpochDays()
            val dateRange = (lastDate - firstDate).coerceAtLeast(1)

            for (i in 0 until sorted.size - 1) {
                val entry1 = sorted[i]
                val entry2 = sorted[i + 1]
                val x1 = (chartMarginLeft + ((entry1.date.toEpochDays() - firstDate).toDouble() / dateRange * chartWidth)).toFloat()
                val w1 = entry1.getWeightInCurrentUnits(weightUnit)
                val y1 = (CHART_MARGIN_TOP + chartHeight - ((w1 - paddedMin) / paddedRange * chartHeight)).toFloat()
                val x2 = (chartMarginLeft + ((entry2.date.toEpochDays() - firstDate).toDouble() / dateRange * chartWidth)).toFloat()
                val w2 = entry2.getWeightInCurrentUnits(weightUnit)
                val y2 = (CHART_MARGIN_TOP + chartHeight - ((w2 - paddedMin) / paddedRange * chartHeight)).toFloat()
                canvas.drawLine(x1, y1, x2, y2, linePaint)
            }

            // Draw data points
            val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CHART_LINE_COLOR
                style = Paint.Style.FILL
                isDither = true
                isAntiAlias = true
            }
            for (entry in sorted) {
                val x = (chartMarginLeft + ((entry.date.toEpochDays() - firstDate).toDouble() / dateRange * chartWidth)).toFloat()
                val weight = entry.getWeightInCurrentUnits(weightUnit)
                val y = (CHART_MARGIN_TOP + chartHeight - ((weight - paddedMin) / paddedRange * chartHeight)).toFloat()
                canvas.drawCircle(x, y, 3f, pointPaint)
            }
        } else if (sorted.size == 1) {
            val entry = sorted.first()
            val x = (chartMarginLeft + chartWidth / 2).toFloat()
            val weight = entry.getWeightInCurrentUnits(weightUnit)
            val y = (CHART_MARGIN_TOP + chartHeight - ((weight - paddedMin) / paddedRange * chartHeight)).toFloat()
            val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CHART_LINE_COLOR
                style = Paint.Style.FILL
                isDither = true
                isAntiAlias = true
            }
            canvas.drawCircle(x, y, 5f, pointPaint)
        }

        // Date axis labels
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AXIS_COLOR
            textSize = 10f
            isDither = true
            isAntiAlias = true
        }
        val dateLabels = minOf(sorted.size, 6)
        for (i in 0 until dateLabels) {
            val index = if (sorted.size > 1) i * (sorted.size - 1) / (dateLabels - 1) else 0
            val entry = sorted[index]
            val dateStr = "${entry.date.month.name.take(3)} ${entry.date.day}"
            val x = if (sorted.size > 1) {
                val firstDate = sorted.first().date.toEpochDays()
                val lastDate = sorted.last().date.toEpochDays()
                val dateRange = (lastDate - firstDate).coerceAtLeast(1)
                (chartMarginLeft + ((entry.date.toEpochDays() - firstDate).toDouble() / dateRange * chartWidth)).toFloat()
            } else {
                (chartMarginLeft + chartWidth / 2).toFloat()
            }
            val centerX = x - datePaint.measureText(dateStr) / 2f
            val y = (CHART_MARGIN_TOP + chartHeight + 15).toFloat()
            canvas.drawText(dateStr, centerX, y, datePaint)
        }

        // Summary text
        val summaryY = (CHART_MARGIN_TOP + chartHeight + 40).toFloat()
        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_COLOR
            textSize = 13f
            isDither = true
            isAntiAlias = true
        }
        val formatted = ShareTextFormatter.format(entries, allEntries, timeFrame, weightUnit, showAbsoluteWeight)
        val summaryText = formatted.first
        val dateRangeText = formatted.second
        val summaryParts = summaryText.split(':', limit = 2)
        val summaryLabel = if (summaryParts.size > 1) "${summaryParts[0]}:" else summaryText
        val summaryValue = summaryParts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
        val summaryLabelWidth = summaryPaint.measureText(summaryLabel)
        canvas.drawText(summaryLabel, (IMAGE_WIDTH - summaryLabelWidth) / 2f, summaryY, summaryPaint)
        if (summaryValue != null) {
            val summaryValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = TEXT_COLOR
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                isDither = true
                isAntiAlias = true
            }
            val summaryValueWidth = summaryValuePaint.measureText(summaryValue)
            canvas.drawText(summaryValue, (IMAGE_WIDTH - summaryValueWidth) / 2f, summaryY + 18f, summaryValuePaint)
        }

        // Date range line
        if (dateRangeText.isNotEmpty()) {
            val dateRangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AXIS_COLOR
                textSize = 11f
                isDither = true
                isAntiAlias = true
            }
            val dateRangeWidth = dateRangePaint.measureText(dateRangeText)
            canvas.drawText(dateRangeText, (IMAGE_WIDTH - dateRangeWidth) / 2f, summaryY + if (summaryValue != null) 36f else 18f, dateRangePaint)
        }

        // Branding
        val brandingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BRANDING_COLOR
            textSize = 10f
            isDither = true
            isAntiAlias = true
        }
        val branding = "Powered by HealthCoach"
        val brandingWidth = brandingPaint.measureText(branding)
        canvas.drawText(branding, (IMAGE_WIDTH - brandingWidth) / 2f, IMAGE_HEIGHT - 15f, brandingPaint)

        // Draw small icon next to branding
        val iconBitmap = loadIconBitmap()
        if (iconBitmap != null) {
            val iconSize = 24
            val iconX = IMAGE_WIDTH - brandingWidth - iconSize - 30
            val iconY = IMAGE_HEIGHT - BRANDING_HEIGHT + (BRANDING_HEIGHT - iconSize) / 2
            val iconPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                isAntiAlias = true
            }
            canvas.drawBitmap(
                iconBitmap,
                null,
                RectF(iconX.toFloat(), iconY.toFloat(), (iconX + iconSize).toFloat(), (iconY + iconSize).toFloat()),
                iconPaint
            )
        }

        return bitmap.asImageBitmap()
    }

    actual fun renderIcon(
        entries: List<WeightEntryData>,
        allEntries: List<WeightEntryData>,
        timeFrame: GraphTimeFrame,
        weightUnit: WeightUnit,
        showAbsoluteWeight: Boolean
    ): ImageBitmap? {
        if (entries.isEmpty()) return null

        // Square image for icon type
        val width = 500
        val height = 500

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(BG_COLOR)

        // Draw large app icon (centered vertically with text below)
        val iconBitmap = loadIconBitmap()
        val iconSize = 200
        val iconY = 40
        if (iconBitmap != null) {
            val iconX = (width - iconSize) / 2
            val scaledIcon = Bitmap.createScaledBitmap(iconBitmap, iconSize, iconSize, true)
            val iconPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                isAntiAlias = true
            }
            canvas.drawBitmap(scaledIcon, iconX.toFloat(), iconY.toFloat(), iconPaint)
            scaledIcon.recycle()
        }

        // Summary text
        val summaryY = (iconY + iconSize + 60).toFloat()
        val formatted = ShareTextFormatter.format(entries, allEntries, timeFrame, weightUnit, showAbsoluteWeight)
        val summaryText = formatted.first
        val dateRangeText = formatted.second
        val summaryParts = summaryText.split(':', limit = 2)
        val summaryLabel = if (summaryParts.size > 1) "${summaryParts[0]}:" else summaryText
        val summaryValue = summaryParts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_COLOR
            textSize = 32f
            isDither = true
            isAntiAlias = true
        }
        val summaryLabelWidth = summaryPaint.measureText(summaryLabel)
        canvas.drawText(summaryLabel, (width - summaryLabelWidth) / 2f, summaryY, summaryPaint)
        if (summaryValue != null) {
            val summaryValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = TEXT_COLOR
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
                isDither = true
                isAntiAlias = true
            }
            val summaryValueWidth = summaryValuePaint.measureText(summaryValue)
            canvas.drawText(summaryValue, (width - summaryValueWidth) / 2f, summaryY + 44f, summaryValuePaint)
        }

        // Date range line
        if (dateRangeText.isNotEmpty()) {
            val dateRangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AXIS_COLOR
                textSize = 26f
                isDither = true
                isAntiAlias = true
            }
            val dateRangeWidth = dateRangePaint.measureText(dateRangeText)
            canvas.drawText(dateRangeText, (width - dateRangeWidth) / 2f, summaryY + if (summaryValue != null) 88f else 50f, dateRangePaint)
        }

        // Branding
        val brandingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BRANDING_COLOR
            textSize = 20f
            isDither = true
            isAntiAlias = true
        }
        val branding = "Powered by HealthCoach"
        val brandingWidth = brandingPaint.measureText(branding)
        canvas.drawText(branding, (width - brandingWidth) / 2f, height - 30f, brandingPaint)

        return bitmap.asImageBitmap()
    }
}
