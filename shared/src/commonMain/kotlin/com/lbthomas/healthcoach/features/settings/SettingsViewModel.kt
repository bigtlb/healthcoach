package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.theme.AppTheme
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
        updateSettings { it.copy(ui = it.ui.copy(selectedPage = page)) }
    }

    fun setSelectedGraphTimeFrame(timeFrame: GraphTimeFrame) {
        updateSettings { it.copy(ui = it.ui.copy(selectedGraphTimeFrame = timeFrame)) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        updateSettings { it.copy(appearance = it.appearance.copy(themeMode = themeMode)) }
    }

    fun setAppTheme(theme: AppTheme) {
        updateSettings { it.copy(appearance = it.appearance.copy(appTheme = theme)) }
    }

    fun setWeightUnit(unit: WeightUnit) {
        updateSettings { it.copy(weight = it.weight.copy(unit = unit)) }
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

    fun setSyncEnabled(enabled: Boolean) {
        updateSettings { it.copy(sync = it.sync.copy(syncEnabled = enabled)) }
    }

    fun setSyncProvider(provider: SyncProviderType) {
        updateSettings { it.copy(sync = it.sync.copy(syncProvider = provider)) }
    }

    fun setLocalSyncPath(path: String) {
        updateSettings { it.copy(sync = it.sync.copy(localSyncPath = path)) }
    }

    fun setGoogleSession(
        email: String,
        accessToken: String = "",
        refreshToken: String = "",
        tokenStatus: String = "Active",
        expiresAt: Long? = null,
        refreshTokenExpiresAt: Long? = null
    ) {
        updateSettings {
            it.copy(
                sync = it.sync.copy(
                    googleAccountEmail = email,
                    googleAccessToken = accessToken,
                    googleRefreshToken = if (refreshToken.isNotBlank()) refreshToken else it.sync.googleRefreshToken,
                    googleTokenStatus = tokenStatus,
                    googleTokenExpiresAt = expiresAt,
                    googleRefreshTokenExpiresAt = if (refreshToken.isNotBlank()) refreshTokenExpiresAt else it.sync.googleRefreshTokenExpiresAt
                )
            )
        }
    }

    fun clearGoogleSession() {
        updateSettings {
            it.copy(
                sync = it.sync.copy(
                    googleAccessToken = "",
                    googleRefreshToken = "",
                    googleAccountEmail = "",
                    googleTokenStatus = "Revoked",
                    googleTokenExpiresAt = null,
                    googleRefreshTokenExpiresAt = null
                )
            )
        }
    }

    fun setGoogleTokenExpiresAt(expiresAt: Long?) {
        updateSettings { it.copy(sync = it.sync.copy(googleTokenExpiresAt = expiresAt)) }
    }

    fun setGoogleRefreshTokenExpiresAt(expiresAt: Long?) {
        updateSettings { it.copy(sync = it.sync.copy(googleRefreshTokenExpiresAt = expiresAt)) }
    }

    fun setGoogleTokenStatus(status: String) {
        updateSettings { it.copy(sync = it.sync.copy(googleTokenStatus = status)) }
    }

    fun setGoogleAccessToken(token: String) {
        updateSettings { it.copy(sync = it.sync.copy(googleAccessToken = token)) }
    }

    fun setGoogleAccountEmail(email: String) {
        updateSettings { it.copy(sync = it.sync.copy(googleAccountEmail = email)) }
    }

    fun setRemoteFileName(name: String) {
        updateSettings { it.copy(sync = it.sync.copy(remoteFileName = name)) }
    }

    fun setAutoSyncOnClose(enabled: Boolean) {
        updateSettings { it.copy(sync = it.sync.copy(autoSyncOnClose = enabled)) }
    }

    fun setAutoSyncIntervalMinutes(minutes: Int) {
        updateSettings { it.copy(sync = it.sync.copy(autoSyncIntervalMinutes = minutes)) }
    }
}
