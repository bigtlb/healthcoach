package com.lbthomas.healthcoach.features.settings.data

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(
    val weightUnit: WeightUnit = WeightUnit.US,
    val selectedPage: SelectedPage = SelectedPage.WeightView,
    val selectedGraphTimeFrame: GraphTimeFrame = GraphTimeFrame.ALL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val windowX: Int = 100,
    val windowY: Int = 100,
    val windowWidth: Int = 800,
    val windowHeight: Int = 600,
    val windowMaximized: Boolean = false,
    val showDailyAverages: Boolean = true,
    val showMonthlyAverages: Boolean = true,
    val showDailyChanges: Boolean = true,
    val showMonthlyChanges: Boolean = true,
    val adaptiveDisplay: Boolean = true,
    val splitterPosition: Float = 0.5f
) {
    val graphTimeFrame: GraphTimeFrame get() = selectedGraphTimeFrame
}
