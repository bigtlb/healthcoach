package com.lbthomas.healthcoach.core.share

import androidx.compose.ui.graphics.ImageBitmap
import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.ShareImageType
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData

/**
 * Renders weight progress data to an [ImageBitmap] for sharing.
 *
 * Uses expect/actual to handle platform differences:
 * - JVM: Java2D/AWT Graphics2D
 * - Android: Android Canvas API
 */
expect object ChartImageRenderer {

    /**
     * Render a chart or icon image based on the [imageType].
     * @param entries filtered entries for the selected time frame (may include interpolated edge points)
     * @param allEntries all actual weight entries for extrapolation of the starting weight
     */
    fun render(
        entries: List<WeightEntryData>,
        allEntries: List<WeightEntryData>,
        timeFrame: GraphTimeFrame,
        weightUnit: WeightUnit,
        imageType: ShareImageType,
        showAbsoluteWeight: Boolean
    ): ImageBitmap?

    /**
     * Render a high-resolution weight line chart image (~1200x800 pixels).
     * @param entries filtered entries for the selected time frame (may include interpolated edge points)
     * @param allEntries all actual weight entries for extrapolation of the starting weight
     */
    fun renderChart(
        entries: List<WeightEntryData>,
        allEntries: List<WeightEntryData>,
        timeFrame: GraphTimeFrame,
        weightUnit: WeightUnit,
        showAbsoluteWeight: Boolean
    ): ImageBitmap?

    /**
     * Render an icon-based image with the app logo and progress text.
     * @param entries filtered entries for the selected time frame (may include interpolated edge points)
     * @param allEntries all actual weight entries for extrapolation of the starting weight
     */
    fun renderIcon(
        entries: List<WeightEntryData>,
        allEntries: List<WeightEntryData>,
        timeFrame: GraphTimeFrame,
        weightUnit: WeightUnit,
        showAbsoluteWeight: Boolean
    ): ImageBitmap?
}
