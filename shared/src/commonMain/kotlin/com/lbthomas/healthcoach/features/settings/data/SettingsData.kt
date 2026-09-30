package com.lbthomas.healthcoach.features.settings.data

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.sync.p2p.PeerClientRecord
import com.lbthomas.healthcoach.core.theme.AppTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class AppearanceSettings(
    val adaptiveDisplay: Boolean = true,
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

@Serializable
data class BloodPressureSettings(
    val showDailyAverages: Boolean = true,
    val showDailyChanges: Boolean = true,
    val showInGraph: Boolean = true,
    val showMonthlyAverages: Boolean = true,
    val showMonthlyChanges: Boolean = true,
    val showPulseInGraph: Boolean = false
)

@Serializable
data class FoodJournalSettings(
    val showInGraph: Boolean = true
)

@Serializable
data class PeerSyncSettings(
    val deviceName: String = "",
    val instanceId: String = "",
    val isServerMode: Boolean = false,
    val lastConnectedTimestamp: Long? = null,
    val localServerEnabled: Boolean = false,
    val localServerHistory: List<PeerClientRecord> = emptyList(),
    val localServerPin: String = "",
    val localServerPort: Int = SyncConfig.DEFAULT_P2P_PORT,
    val serverHost: String = "",
    val serverInstanceId: String? = null,
    val serverName: String = "",
    @Transient
    val serverPort: Int = SyncConfig.DEFAULT_P2P_PORT,
    val serverToken: String = ""
)

@Serializable
data class SyncSettings(
    val autoSyncIntervalMinutes: Int = 0,
    val autoSyncOnClose: Boolean = false,
    val googleAccessToken: String = "",
    val googleAccountEmail: String = "",
    val googleRefreshToken: String = "",
    val googleRefreshTokenExpiresAt: Long? = null,
    val googleTokenExpiresAt: Long? = null,
    val googleTokenStatus: String = "",
    val lastSyncError: String? = null,
    val lastSyncFailed: Boolean = false,
    val lastSyncHash: String? = null,
    val lastSyncStatus: String = "Never",
    val lastSyncTime: Long? = null,
    val localSyncPath: String = "",
    val remoteFileName: String = SyncConfig.DEFAULT_REMOTE_DB_NAME,
    val syncEnabled: Boolean = false,
    val syncProvider: SyncProviderType = SyncProviderType.LOCAL_FOLDER
)

@Serializable
data class UiSettings(
    val selectedGraphTimeFrame: GraphTimeFrame = GraphTimeFrame.ALL,
    val selectedPage: SelectedPage = SelectedPage.WeightView,
    val splitterPosition: Float = 0.5f,
    val windowHeight: Int = 600,
    val windowMaximized: Boolean = false,
    val windowWidth: Int = 800,
    val windowX: Int = 100,
    val windowY: Int = 100
)

@Serializable
data class WeightSettings(
    val showInGraph: Boolean = true,
    val unit: WeightUnit = WeightUnit.US
)

@Serializable
data class SettingsData(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val bloodPressure: BloodPressureSettings = BloodPressureSettings(),
    val foodJournal: FoodJournalSettings = FoodJournalSettings(),
    val peerSync: PeerSyncSettings = PeerSyncSettings(),
    val sync: SyncSettings = SyncSettings(),
    val ui: UiSettings = UiSettings(),
    val weight: WeightSettings = WeightSettings()
) {
    val graphTimeFrame: GraphTimeFrame get() = ui.selectedGraphTimeFrame

    fun toSyncConfig(): SyncConfig {
        return SyncConfig(
            providerType = sync.syncProvider,
            remoteFileName = sync.remoteFileName.ifBlank { SyncConfig.DEFAULT_REMOTE_DB_NAME },
            localFolderPath = sync.localSyncPath,
            googleAccountEmail = sync.googleAccountEmail,
            googleAccessToken = sync.googleAccessToken,
            googleRefreshToken = sync.googleRefreshToken,
            peerServerHost = peerSync.serverHost,
            peerServerPort = peerSync.serverPort,
            peerServerToken = peerSync.serverToken,
            peerServerName = peerSync.serverName,
            clientInstanceId = peerSync.instanceId,
            clientDeviceName = peerSync.deviceName
        )
    }
}
