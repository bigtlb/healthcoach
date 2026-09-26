package com.lbthomas.healthcoach.features.settings.data

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.theme.AppTheme
import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(
    val weightUnit: WeightUnit = WeightUnit.US,
    val selectedPage: SelectedPage = SelectedPage.WeightView,
    val selectedGraphTimeFrame: GraphTimeFrame = GraphTimeFrame.ALL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.DEFAULT,
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
    val splitterPosition: Float = 0.5f,
    val showWeightInGraph: Boolean = true,
    val showBloodPressureInGraph: Boolean = true,
    val showPulseInGraph: Boolean = false,
    // Sync configuration
    val syncEnabled: Boolean = false,
    val syncProvider: SyncProviderType = SyncProviderType.LOCAL_FOLDER,
    val localSyncPath: String = "",
    val googleAccountEmail: String = "",
    val googleAccessToken: String = "",
    val googleRefreshToken: String = "",
    val googleTokenStatus: String = "",
    val googleTokenExpiresAt: Long? = null,
    val googleRefreshTokenExpiresAt: Long? = null,
    val remoteFileName: String = SyncConfig.DEFAULT_REMOTE_DB_NAME,
    val autoSyncOnClose: Boolean = false,
    val autoSyncIntervalMinutes: Int = 0,
    val lastSyncStatus: String = "Never",
    val lastSyncTime: Long? = null,
    val lastSyncHash: String? = null,
    val lastSyncError: String? = null,
    val lastSyncFailed: Boolean = false
) {
    val graphTimeFrame: GraphTimeFrame get() = selectedGraphTimeFrame

    fun toSyncConfig(): SyncConfig {
        return SyncConfig(
            providerType = syncProvider,
            remoteFileName = remoteFileName.ifBlank { SyncConfig.DEFAULT_REMOTE_DB_NAME },
            localFolderPath = localSyncPath,
            googleAccountEmail = googleAccountEmail,
            googleAccessToken = googleAccessToken,
            googleRefreshToken = googleRefreshToken
        )
    }
}
