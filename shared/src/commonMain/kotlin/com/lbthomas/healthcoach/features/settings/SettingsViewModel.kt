package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
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
        if (persistence != null) {
            persistence.updateSettings(transform)
        } else if (settings is MutableStateFlow<SettingsData>) {
            settings.value = transform(settings.value)
        }
    }

    fun setSelectedPage(page: SelectedPage) {
        updateSettings { it.copy(selectedPage = page) }
    }

    fun setSelectedGraphTimeFrame(timeFrame: GraphTimeFrame) {
        updateSettings { it.copy(selectedGraphTimeFrame = timeFrame) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        updateSettings { it.copy(themeMode = themeMode) }
    }

    fun setWeightUnit(unit: WeightUnit) {
        updateSettings { it.copy(weightUnit = unit) }
    }

    fun setWindowState(x: Int, y: Int, width: Int, height: Int, maximized: Boolean) {
        updateSettings {
            it.copy(
                windowX = x,
                windowY = y,
                windowWidth = width,
                windowHeight = height,
                windowMaximized = maximized
            )
        }
    }

    fun setAdaptiveDisplay(adaptiveDisplay: Boolean) {
        updateSettings { it.copy(adaptiveDisplay = adaptiveDisplay) }
    }

    fun setSplitterPosition(position: Float) {
        updateSettings { it.copy(splitterPosition = position) }
    }

    fun setBloodPressureDisplaySettings(
        showDailyAverages: Boolean,
        showMonthlyAverages: Boolean,
        showDailyChanges: Boolean,
        showMonthlyChanges: Boolean
    ) {
        updateSettings {
            it.copy(
                showDailyAverages = showDailyAverages,
                showMonthlyAverages = showMonthlyAverages,
                showDailyChanges = showDailyChanges,
                showMonthlyChanges = showMonthlyChanges
            )
        }
    }
}
