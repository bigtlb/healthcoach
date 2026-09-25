package com.lbthomas.healthcoach.features.weight.data

import com.lbthomas.healthcoach.core.enums.WeightUnit
import kotlinx.datetime.LocalDate

data class WeightEntryData(
    val id: String,
    val date: LocalDate,
    val weight: Double,
    val updatedAt: Long = 0L,
) {
    fun getWeightInCurrentUnits(weightUnit: WeightUnit) = if (weightUnit == WeightUnit.METRIC) weight else weight * 2.20462
    fun convertToKilograms(enteredWeight: Double, weightUnit: WeightUnit) = if (weightUnit == WeightUnit.METRIC) enteredWeight else enteredWeight / 2.20462
}
