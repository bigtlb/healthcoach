package com.lbthomas.healthcoach.features.settings.data

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.json.Json
import java.io.File

open class SettingsStore(private val settingsFile: File) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SettingsData> = _settings.asStateFlow()

    private fun loadSettings(): SettingsData {
        return runCatching {
            if (settingsFile.exists()) {
                json.decodeFromString<SettingsData>(settingsFile.readText())
            } else {
                SettingsData()
            }
        }.getOrDefault(SettingsData())
    }

    fun updateSettings(transform: (SettingsData) -> SettingsData) {
        val updated = _settings.updateAndGet(transform)
        saveSettings(updated)
    }

    fun setSelectedPage(page: SelectedPage) {
        updateSettings { it.copy(selectedPage = page) }
    }

    fun setSelectedGraphTimeFrame(timeFrame: GraphTimeFrame) {
        updateSettings { it.copy(selectedGraphTimeFrame = timeFrame) }
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

    fun setShowWeightInGraph(show: Boolean) {
        updateSettings { it.copy(showWeightInGraph = show) }
    }

    fun setShowBloodPressureInGraph(show: Boolean) {
        updateSettings { it.copy(showBloodPressureInGraph = show) }
    }

    fun setShowPulseInGraph(show: Boolean) {
        updateSettings { it.copy(showPulseInGraph = show) }
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

    private fun saveSettings(settingsToSave: SettingsData) {
        runCatching {
            val parent = settingsFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            settingsFile.writeText(json.encodeToString(settingsToSave))
        }
    }
}
