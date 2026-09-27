package com.lbthomas.healthcoach.features.settings.data

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.sync.p2p.PeerClientRecord
import com.lbthomas.healthcoach.core.theme.AppTheme
import com.lbthomas.healthcoach.core.utils.generateUuid
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
        val loaded = runCatching {
            if (settingsFile.exists()) {
                json.decodeFromString<SettingsData>(settingsFile.readText())
            } else {
                SettingsData()
            }
        }.getOrDefault(SettingsData())

        val ensured = ensureInstanceIdentity(loaded)
        if (ensured != loaded) {
            saveSettings(ensured)
        }
        return ensured
    }

    private fun ensureInstanceIdentity(settings: SettingsData): SettingsData {
        var peerSync = settings.peerSync
        var updated = false
        if (peerSync.instanceId.isBlank()) {
            peerSync = peerSync.copy(instanceId = generateUuid())
            updated = true
        }
        if (peerSync.deviceName.isBlank()) {
            peerSync = peerSync.copy(deviceName = "HealthCoach Device")
            updated = true
        }
        return if (updated) settings.copy(peerSync = peerSync) else settings
    }

    fun updateSettings(transform: (SettingsData) -> SettingsData) {
        val updated = _settings.updateAndGet(transform)
        saveSettings(updated)
    }

    fun setDeviceName(name: String) {
        updateSettings { it.copy(peerSync = it.peerSync.copy(deviceName = name)) }
    }

    fun setPeerServerEnabled(enabled: Boolean) {
        updateSettings { it.copy(peerSync = it.peerSync.copy(localServerEnabled = enabled)) }
    }

    fun setPeerServerPort(port: Int) {
        updateSettings { it.copy(peerSync = it.peerSync.copy(localServerPort = port)) }
    }

    fun setPeerServerPin(pin: String) {
        updateSettings { it.copy(peerSync = it.peerSync.copy(localServerPin = pin)) }
    }

    fun setPeerClientTarget(
        instanceId: String?,
        host: String,
        port: Int,
        token: String,
        name: String
    ) {
        updateSettings {
            it.copy(
                peerSync = it.peerSync.copy(
                    serverInstanceId = instanceId,
                    serverHost = host,
                    serverPort = port,
                    serverToken = token,
                    serverName = name
                )
            )
        }
    }

    fun addServerHistoryRecord(record: PeerClientRecord) {
        updateSettings { current ->
            val filtered = current.peerSync.localServerHistory.filter { it.clientInstanceId != record.clientInstanceId }
            current.copy(peerSync = current.peerSync.copy(localServerHistory = listOf(record) + filtered))
        }
    }

    fun setSelectedPage(page: SelectedPage) {
        updateSettings { it.copy(ui = it.ui.copy(selectedPage = page)) }
    }

    fun setSelectedGraphTimeFrame(timeFrame: GraphTimeFrame) {
        updateSettings { it.copy(ui = it.ui.copy(selectedGraphTimeFrame = timeFrame)) }
    }

    fun setWeightUnit(unit: WeightUnit) {
        updateSettings { it.copy(weight = it.weight.copy(unit = unit)) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        updateSettings { it.copy(appearance = it.appearance.copy(themeMode = themeMode)) }
    }

    fun setAppTheme(theme: AppTheme) {
        updateSettings { it.copy(appearance = it.appearance.copy(appTheme = theme)) }
    }

    fun setWindowState(x: Int, y: Int, width: Int, height: Int, maximized: Boolean) {
        updateSettings {
            it.copy(
                ui = it.ui.copy(
                    windowX = x,
                    windowY = y,
                    windowWidth = width,
                    windowHeight = height,
                    windowMaximized = maximized
                )
            )
        }
    }

    fun setAdaptiveDisplay(adaptiveDisplay: Boolean) {
        updateSettings { it.copy(appearance = it.appearance.copy(adaptiveDisplay = adaptiveDisplay)) }
    }

    fun setSplitterPosition(position: Float) {
        updateSettings { it.copy(ui = it.ui.copy(splitterPosition = position)) }
    }

    fun setShowWeightInGraph(show: Boolean) {
        updateSettings { it.copy(weight = it.weight.copy(showInGraph = show)) }
    }

    fun setShowBloodPressureInGraph(show: Boolean) {
        updateSettings { it.copy(bloodPressure = it.bloodPressure.copy(showInGraph = show)) }
    }

    fun setShowPulseInGraph(show: Boolean) {
        updateSettings { it.copy(bloodPressure = it.bloodPressure.copy(showPulseInGraph = show)) }
    }

    fun setBloodPressureDisplaySettings(
        showDailyAverages: Boolean,
        showMonthlyAverages: Boolean,
        showDailyChanges: Boolean,
        showMonthlyChanges: Boolean
    ) {
        updateSettings {
            it.copy(
                bloodPressure = it.bloodPressure.copy(
                    showDailyAverages = showDailyAverages,
                    showMonthlyAverages = showMonthlyAverages,
                    showDailyChanges = showDailyChanges,
                    showMonthlyChanges = showMonthlyChanges
                )
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
