package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
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
        updateSettings { it.copy(selectedPage = page) }
    }

    fun setSelectedGraphTimeFrame(timeFrame: GraphTimeFrame) {
        updateSettings { it.copy(selectedGraphTimeFrame = timeFrame) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        updateSettings { it.copy(themeMode = themeMode) }
    }

    fun setAppTheme(theme: AppTheme) {
        updateSettings { it.copy(appTheme = theme) }
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

    fun setSyncEnabled(enabled: Boolean) {
        updateSettings { it.copy(syncEnabled = enabled) }
    }

    fun setSyncProvider(provider: com.lbthomas.healthcoach.core.sync.SyncProviderType) {
        updateSettings { it.copy(syncProvider = provider) }
    }

    fun setLocalSyncPath(path: String) {
        updateSettings { it.copy(localSyncPath = path) }
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
                googleAccountEmail = email,
                googleAccessToken = accessToken,
                googleRefreshToken = if (refreshToken.isNotBlank()) refreshToken else it.googleRefreshToken,
                googleTokenStatus = tokenStatus,
                googleTokenExpiresAt = expiresAt,
                googleRefreshTokenExpiresAt = if (refreshToken.isNotBlank()) refreshTokenExpiresAt else it.googleRefreshTokenExpiresAt
            )
        }
    }

    fun clearGoogleSession() {
        updateSettings {
            it.copy(
                googleAccessToken = "",
                googleRefreshToken = "",
                googleAccountEmail = "",
                googleTokenStatus = "Revoked",
                googleTokenExpiresAt = null,
                googleRefreshTokenExpiresAt = null
            )
        }
    }

    fun setGoogleTokenExpiresAt(expiresAt: Long?) {
        updateSettings { it.copy(googleTokenExpiresAt = expiresAt) }
    }

    fun setGoogleRefreshTokenExpiresAt(expiresAt: Long?) {
        updateSettings { it.copy(googleRefreshTokenExpiresAt = expiresAt) }
    }

    fun setGoogleTokenStatus(status: String) {
        updateSettings { it.copy(googleTokenStatus = status) }
    }

    fun setGoogleAccessToken(token: String) {
        updateSettings { it.copy(googleAccessToken = token) }
    }

    fun setGoogleAccountEmail(email: String) {
        updateSettings { it.copy(googleAccountEmail = email) }
    }

    fun setRemoteFileName(name: String) {
        updateSettings { it.copy(remoteFileName = name) }
    }

    fun setAutoSyncOnClose(enabled: Boolean) {
        updateSettings { it.copy(autoSyncOnClose = enabled) }
    }

    fun setAutoSyncIntervalMinutes(minutes: Int) {
        updateSettings { it.copy(autoSyncIntervalMinutes = minutes) }
    }
}
