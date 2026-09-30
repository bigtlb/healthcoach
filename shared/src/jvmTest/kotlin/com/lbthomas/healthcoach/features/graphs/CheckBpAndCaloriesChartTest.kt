package com.lbthomas.healthcoach.features.graphs

import com.lbthomas.healthcoach.features.graphs.data.BpGraphPoint
import com.lbthomas.healthcoach.features.graphs.data.CalorieGraphPoint
import com.lbthomas.healthcoach.features.graphs.ui.components.buildBpLayerModel
import com.lbthomas.healthcoach.features.graphs.ui.components.buildCalorieLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import kotlinx.datetime.LocalDate
import kotlin.test.Test

class CheckBpAndCaloriesChartTest {
    @Test
    fun testBpAndCaloriesModels() {
        val previewBpPoints = listOf(
            BpGraphPoint("1", LocalDate(2026, 1, 1).toEpochDays().toDouble(), LocalDate(2026, 1, 1), 120.0, 80.0, 70),
            BpGraphPoint("2", LocalDate(2026, 1, 15).toEpochDays().toDouble(), LocalDate(2026, 1, 15), 118.0, 78.0, 68),
            BpGraphPoint("3", LocalDate(2026, 2, 1).toEpochDays().toDouble(), LocalDate(2026, 2, 1), 122.0, 82.0, 72),
            BpGraphPoint("4", LocalDate(2026, 2, 15).toEpochDays().toDouble(), LocalDate(2026, 2, 15), 116.0, 76.0, 66),
            BpGraphPoint("5", LocalDate(2026, 3, 1).toEpochDays().toDouble(), LocalDate(2026, 3, 1), 114.0, 74.0, 64)
        )
        val previewCaloriePoints = listOf(
            CalorieGraphPoint(LocalDate(2026, 1, 1), LocalDate(2026, 1, 1).toEpochDays().toDouble(), 1850.0),
            CalorieGraphPoint(LocalDate(2026, 1, 15), LocalDate(2026, 1, 15).toEpochDays().toDouble(), 2100.0),
            CalorieGraphPoint(LocalDate(2026, 2, 1), LocalDate(2026, 2, 1).toEpochDays().toDouble(), 1950.0),
            CalorieGraphPoint(LocalDate(2026, 2, 15), LocalDate(2026, 2, 15).toEpochDays().toDouble(), 2250.0),
            CalorieGraphPoint(LocalDate(2026, 3, 1), LocalDate(2026, 3, 1).toEpochDays().toDouble(), 2000.0)
        )

        val calModel = buildCalorieLayerModel(previewCaloriePoints)
        val bpModel = buildBpLayerModel(previewBpPoints, true, previewBpPoints)

        println("calModel: $calModel")
        println("bpModel: $bpModel")
        val chartModel = CartesianChartModel(listOfNotNull(calModel, bpModel))
        println("chartModel.models: ${chartModel.models.size}")
    }
}
