package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.enums.WeightUnit
import kotlinx.coroutines.flow.StateFlow

open class SettingsViewModel(private val persistence: SettingsStore)  {
    open val settings: StateFlow<SettingsData> get() = persistence.settings

    open fun updateSettings(transform: (SettingsData) -> SettingsData) {
        persistence.updateSettings(transform)
    }

    open fun setWeightUnit(unit: WeightUnit) {
        persistence.setWeightUnit(unit)
    }

    open fun setWindowState(x: Int, y: Int, width: Int, height: Int, maximized: Boolean) {
        persistence.setWindowState(x, y, width, height, maximized)
    }

    open fun setBloodPressureDisplaySettings(
        showDailyAverages: Boolean,
        showMonthlyAverages: Boolean,
        showDailyChanges: Boolean,
        showMonthlyChanges: Boolean
    ) {
        persistence.setBloodPressureDisplaySettings(
            showDailyAverages,
            showMonthlyAverages,
            showDailyChanges,
            showMonthlyChanges
        )
    }
}
