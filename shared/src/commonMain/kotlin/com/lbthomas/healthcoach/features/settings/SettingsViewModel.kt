package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel  {
    val settings: StateFlow<SettingsData>
    val persistence: SettingsStore?

    constructor(persistence: SettingsStore): super(){
        this.persistence = persistence
        this.settings = persistence.settings
    }

    constructor(settings: StateFlow<SettingsData>):super() {
        this.persistence = null
        this.settings = settings
    }

    fun updateSettings(transform: (SettingsData) -> SettingsData) {
        persistence!!.updateSettings(transform)
    }

    fun setWeightUnit(unit: WeightUnit) {
        persistence!!.setWeightUnit(unit)
    }

    fun setWindowState(x: Int, y: Int, width: Int, height: Int, maximized: Boolean) {
        persistence!!.setWindowState(x, y, width, height, maximized)
    }

    fun setBloodPressureDisplaySettings(
        showDailyAverages: Boolean,
        showMonthlyAverages: Boolean,
        showDailyChanges: Boolean,
        showMonthlyChanges: Boolean
    ) {
        persistence!!.setBloodPressureDisplaySettings(
            showDailyAverages,
            showMonthlyAverages,
            showDailyChanges,
            showMonthlyChanges
        )
    }
}
