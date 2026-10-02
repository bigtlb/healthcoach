package com.lbthomas.healthcoach.features.graphs.ui.components

import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.graphs.data.BpGraphPoint
import com.lbthomas.healthcoach.features.graphs.data.CalorieGraphPoint
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import kotlinx.datetime.LocalDate
import kotlin.math.round

internal fun calculateGlobalEpochBounds(
    hasWeight: Boolean,
    hasBp: Boolean,
    hasCalories: Boolean,
    weightEntries: List<WeightEntryData>,
    bpPoints: List<BpGraphPoint>,
    caloriePoints: List<CalorieGraphPoint>,
    weightMinEpoch: Double?,
    weightMaxEpoch: Double?,
    bpMinEpoch: Double?,
    bpMaxEpoch: Double?,
    calorieMinEpoch: Double?,
    calorieMaxEpoch: Double?
): Pair<Double?, Double?> {
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
    val min = mins.minOrNull()

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
    val max = maxs.maxOrNull()
    return Pair(min, max)
}

internal fun buildCompoundChartModel(
    hasCalories: Boolean,
    caloriePoints: List<CalorieGraphPoint>,
    hasMetabolicLines: Boolean,
    globalMinX: Double?,
    globalMaxX: Double?,
    maintenanceCalories: Double?,
    targetCalories: Double?,
    hasWeight: Boolean,
    weightEntries: List<WeightEntryData>,
    weightUnit: WeightUnit,
    hasBp: Boolean,
    bpPoints: List<BpGraphPoint>,
    hasPulseSeries: Boolean,
    pulsePoints: List<BpGraphPoint>
): CartesianChartModel {
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
    return CartesianChartModel(models)
}

internal fun calculateOptimalYStep(minY: Double?, maxY: Double?, defaultStep: Double = 5.0): Double {
    if (minY == null || maxY == null || maxY <= minY) return defaultStep
    val diff = maxY - minY
    val candidateSteps = if (defaultStep >= 50.0) {
        listOf(50.0, 100.0, 200.0, 250.0, 500.0, 1000.0)
    } else {
        listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 50.0, 100.0, 200.0)
    }
    return candidateSteps.firstOrNull { diff / it <= 7 } ?: candidateSteps.last()
}

internal fun calculateHorizontalAxisSpacing(daySpan: Int): Int {
    return when {
        daySpan <= 30 -> 7
        daySpan <= 120 -> 14
        daySpan <= 365 -> 30
        daySpan <= 730 -> 90
        else -> 180
    }
}

internal fun formatBottomAxisDate(date: LocalDate, daySpan: Int): String {
    val monthNum = date.month.ordinal + 1
    return when {
        daySpan <= 120 -> "$monthNum/${date.day}"
        daySpan <= 365 -> "${date.month.name.take(3)} ${date.day}"
        daySpan <= 730 -> "${date.month.name.take(3)} '${date.year % 100}"
        else -> {
            val yy = ((date.year % 100 + 100) % 100).toString().padStart(2, '0')
            val mm = monthNum.toString().padStart(2, '0')
            "$yy/$mm"
        }
    }
}

internal fun createBottomAxisValueFormatter(daySpan: Int): CartesianValueFormatter {
    return CartesianValueFormatter { _, value, _ ->
        val date = LocalDate.fromEpochDays(value.toLong())
        formatBottomAxisDate(date, daySpan)
    }
}

internal fun createStartAxisValueFormatter(
    hasWeight: Boolean,
    hasAnyCalories: Boolean,
    unitLabel: String
): CartesianValueFormatter {
    return CartesianValueFormatter { _, value, _ ->
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

internal fun createEndAxisValueFormatter(hasBp: Boolean): CartesianValueFormatter {
    return CartesianValueFormatter { _, value, _ ->
        if (hasBp) {
            val rounded = round(value).toInt()
            "$rounded mmHg"
        } else {
            val rounded = (round(value / 50.0) * 50).toInt()
            "$rounded kcal"
        }
    }
}
