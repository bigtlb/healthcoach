package com.lbthomas.healthcoach.features.settings

import co.touchlab.kermit.Severity
import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.logging.LoggingConfig
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.sync.p2p.PeerClientRecord
import com.lbthomas.healthcoach.core.theme.AppTheme
import com.lbthomas.healthcoach.features.settings.data.AppearanceSettings
import com.lbthomas.healthcoach.features.settings.data.BloodPressureSettings
import com.lbthomas.healthcoach.features.settings.data.PeerSyncSettings
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SyncSettings
import com.lbthomas.healthcoach.features.settings.data.UiSettings
import com.lbthomas.healthcoach.features.settings.data.WeightSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SettingsDataTest {

    @Test
    fun testDefaultSettingsValues() {
        val settings = SettingsData()

        // Appearance
        assertEquals(true, settings.appearance.adaptiveDisplay)
        assertEquals(AppTheme.DEFAULT, settings.appearance.appTheme)
        assertEquals(ThemeMode.SYSTEM, settings.appearance.themeMode)

        // Blood Pressure
        assertEquals(true, settings.bloodPressure.showDailyAverages)
        assertEquals(true, settings.bloodPressure.showDailyChanges)
        assertEquals(true, settings.bloodPressure.showInGraph)
        assertEquals(true, settings.bloodPressure.showMonthlyAverages)
        assertEquals(true, settings.bloodPressure.showMonthlyChanges)
        assertEquals(false, settings.bloodPressure.showPulseInGraph)

        // Peer Sync
        assertEquals("", settings.peerSync.deviceName)
        assertEquals("", settings.peerSync.instanceId)
        assertEquals(false, settings.peerSync.isServerMode)
        assertEquals(null, settings.peerSync.lastConnectedTimestamp)
        assertEquals(false, settings.peerSync.localServerEnabled)
        assertEquals(emptyList(), settings.peerSync.localServerHistory)
        assertEquals("", settings.peerSync.localServerPin)
        assertEquals(SyncConfig.DEFAULT_P2P_PORT, settings.peerSync.localServerPort)
        assertEquals("", settings.peerSync.serverHost)
        assertEquals(null, settings.peerSync.serverInstanceId)
        assertEquals("", settings.peerSync.serverName)
        assertEquals(SyncConfig.DEFAULT_P2P_PORT, settings.peerSync.serverPort)
        assertEquals("", settings.peerSync.serverToken)

        // Sync
        assertEquals(0, settings.sync.autoSyncIntervalMinutes)
        assertEquals(false, settings.sync.autoSyncOnClose)
        assertEquals("", settings.sync.googleAccessToken)
        assertEquals("", settings.sync.googleAccountEmail)
        assertEquals("", settings.sync.googleRefreshToken)
        assertEquals(null, settings.sync.googleRefreshTokenExpiresAt)
        assertEquals(null, settings.sync.googleTokenExpiresAt)
        assertEquals("", settings.sync.googleTokenStatus)
        assertEquals(null, settings.sync.lastSyncError)
        assertEquals(false, settings.sync.lastSyncFailed)
        assertEquals(null, settings.sync.lastSyncHash)
        assertEquals("Never", settings.sync.lastSyncStatus)
        assertEquals(null, settings.sync.lastSyncTime)
        assertEquals("", settings.sync.localSyncPath)
        assertEquals(SyncConfig.DEFAULT_REMOTE_DB_NAME, settings.sync.remoteFileName)
        assertEquals(false, settings.sync.syncEnabled)
        assertEquals(SyncProviderType.LOCAL_FOLDER, settings.sync.syncProvider)

        // UI
        assertEquals(GraphTimeFrame.ALL, settings.ui.selectedGraphTimeFrame)
        assertEquals(SelectedPage.WeightView, settings.ui.selectedPage)
        assertEquals(0.5f, settings.ui.splitterPosition)
        assertEquals(600, settings.ui.windowHeight)
        assertEquals(false, settings.ui.windowMaximized)
        assertEquals(800, settings.ui.windowWidth)
        assertEquals(100, settings.ui.windowX)
        assertEquals(100, settings.ui.windowY)

        // Weight
        assertEquals(true, settings.weight.showInGraph)
        assertEquals(WeightUnit.US, settings.weight.unit)

        // Convenience getters
        assertEquals(GraphTimeFrame.ALL, settings.graphTimeFrame)
    }

    @Test
    fun testSettingsSerialization() {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        val original = SettingsData(
            appearance = AppearanceSettings(
                adaptiveDisplay = false,
                appTheme = AppTheme.TEAL,
                themeMode = ThemeMode.DARK
            ),
            bloodPressure = BloodPressureSettings(
                showDailyAverages = false,
                showDailyChanges = false,
                showInGraph = false,
                showMonthlyAverages = false,
                showMonthlyChanges = false,
                showPulseInGraph = true
            ),
            peerSync = PeerSyncSettings(
                deviceName = "My Desktop Node",
                instanceId = "test-uuid-1234",
                isServerMode = true,
                lastConnectedTimestamp = 1710000000000L,
                localServerEnabled = true,
                localServerHistory = listOf(
                    PeerClientRecord(
                        authToken = "client_auth_token_123",
                        clientInstanceId = "client-uuid-5678",
                        clientName = "Pixel 8",
                        ipAddress = "192.168.1.100",
                        lastAccessTimestamp = 1710000000000L,
                        lastAction = "DOWNLOAD"
                    )
                ),
                localServerPin = "123456",
                localServerPort = 9000,
                serverHost = "192.168.1.50",
                serverInstanceId = "server-uuid-9999",
                serverName = "Home Server",
                serverPort = 9000,
                serverToken = "p2p_token_xyz"
            ),
            sync = SyncSettings(
                autoSyncIntervalMinutes = 15,
                autoSyncOnClose = true,
                googleAccessToken = "access_token_123",
                googleAccountEmail = "user@example.com",
                googleRefreshToken = "refresh_token_456",
                localSyncPath = "/data/sync",
                syncEnabled = true,
                syncProvider = SyncProviderType.PEER_TO_PEER
            ),
            ui = UiSettings(
                selectedGraphTimeFrame = GraphTimeFrame.YEAR_TO_DATE,
                selectedPage = SelectedPage.GraphsView,
                splitterPosition = 0.42f,
                windowHeight = 768,
                windowMaximized = true,
                windowWidth = 1024,
                windowX = 120,
                windowY = 80
            ),
            weight = WeightSettings(
                showInGraph = false,
                unit = WeightUnit.METRIC
            )
        )

        val serialized = json.encodeToString(SettingsData.serializer(), original)
        val deserialized = json.decodeFromString(SettingsData.serializer(), serialized)

        // Appearance
        assertEquals(false, deserialized.appearance.adaptiveDisplay)
        assertEquals(AppTheme.TEAL, deserialized.appearance.appTheme)
        assertEquals(ThemeMode.DARK, deserialized.appearance.themeMode)

        // Blood Pressure
        assertEquals(false, deserialized.bloodPressure.showDailyAverages)
        assertEquals(false, deserialized.bloodPressure.showDailyChanges)
        assertEquals(false, deserialized.bloodPressure.showInGraph)
        assertEquals(false, deserialized.bloodPressure.showMonthlyAverages)
        assertEquals(false, deserialized.bloodPressure.showMonthlyChanges)
        assertEquals(true, deserialized.bloodPressure.showPulseInGraph)

        // Peer Sync
        assertEquals("My Desktop Node", deserialized.peerSync.deviceName)
        assertEquals("test-uuid-1234", deserialized.peerSync.instanceId)
        assertEquals(true, deserialized.peerSync.isServerMode)
        assertEquals(1710000000000L, deserialized.peerSync.lastConnectedTimestamp)
        assertEquals(true, deserialized.peerSync.localServerEnabled)
        assertEquals("123456", deserialized.peerSync.localServerPin)
        assertEquals(9000, deserialized.peerSync.localServerPort)
        assertEquals(1, deserialized.peerSync.localServerHistory.size)
        assertEquals("client_auth_token_123", deserialized.peerSync.localServerHistory[0].authToken)
        assertEquals("client-uuid-5678", deserialized.peerSync.localServerHistory[0].clientInstanceId)
        assertEquals("Pixel 8", deserialized.peerSync.localServerHistory[0].clientName)
        assertEquals("192.168.1.100", deserialized.peerSync.localServerHistory[0].ipAddress)
        assertEquals("192.168.1.50", deserialized.peerSync.serverHost)
        assertEquals("server-uuid-9999", deserialized.peerSync.serverInstanceId)
        assertEquals("Home Server", deserialized.peerSync.serverName)
        // serverPort is @Transient so it restores to default
        assertEquals(SyncConfig.DEFAULT_P2P_PORT, deserialized.peerSync.serverPort)
        assertEquals("p2p_token_xyz", deserialized.peerSync.serverToken)

        // Sync
        assertEquals(15, deserialized.sync.autoSyncIntervalMinutes)
        assertEquals(true, deserialized.sync.autoSyncOnClose)
        assertEquals("access_token_123", deserialized.sync.googleAccessToken)
        assertEquals("user@example.com", deserialized.sync.googleAccountEmail)
        assertEquals("refresh_token_456", deserialized.sync.googleRefreshToken)
        assertEquals("/data/sync", deserialized.sync.localSyncPath)
        assertEquals(true, deserialized.sync.syncEnabled)
        assertEquals(SyncProviderType.PEER_TO_PEER, deserialized.sync.syncProvider)

        // UI
        assertEquals(GraphTimeFrame.YEAR_TO_DATE, deserialized.ui.selectedGraphTimeFrame)
        assertEquals(SelectedPage.GraphsView, deserialized.ui.selectedPage)
        assertEquals(0.42f, deserialized.ui.splitterPosition)
        assertEquals(768, deserialized.ui.windowHeight)
        assertEquals(true, deserialized.ui.windowMaximized)
        assertEquals(1024, deserialized.ui.windowWidth)
        assertEquals(120, deserialized.ui.windowX)
        assertEquals(80, deserialized.ui.windowY)

        // Weight
        assertEquals(false, deserialized.weight.showInGraph)
        assertEquals(WeightUnit.METRIC, deserialized.weight.unit)

        // toSyncConfig() Parity
        val syncConfig = deserialized.toSyncConfig()
        assertEquals(SyncProviderType.PEER_TO_PEER, syncConfig.providerType)
        assertEquals("192.168.1.50", syncConfig.peerServerHost)
        assertEquals(SyncConfig.DEFAULT_P2P_PORT, syncConfig.peerServerPort)
        assertEquals("p2p_token_xyz", syncConfig.peerServerToken)
        assertEquals("Home Server", syncConfig.peerServerName)
    }

    @Test
    fun testLoggingThrottleConfiguration() {
        assertNotNull(LoggingConfig.serverLogger)
        assertNotNull(LoggingConfig.clientLogger)
        assertNotNull(LoggingConfig.discoveryLogger)
        assertNotNull(LoggingConfig.syncLogger)

        // Verify single throttle variable can be toggled to any severity
        LoggingConfig.minSeverity = Severity.Info
        assertEquals(Severity.Info, LoggingConfig.minSeverity)

        LoggingConfig.minSeverity = Severity.Debug
        assertEquals(Severity.Debug, LoggingConfig.minSeverity)

        LoggingConfig.p2pServerLogging = false
        assertEquals(false, LoggingConfig.p2pServerLogging)
        LoggingConfig.p2pServerLogging = true
        assertEquals(true, LoggingConfig.p2pServerLogging)
    }

    @Test
    fun testSettingsViewModelUpdates() {
        val stateFlow = MutableStateFlow(SettingsData())
        val viewModel = SettingsViewModel(settings = stateFlow)

        viewModel.setSelectedPage(SelectedPage.BloodPressureView)
        assertEquals(SelectedPage.BloodPressureView, viewModel.settings.value.ui.selectedPage)

        viewModel.setSelectedGraphTimeFrame(GraphTimeFrame.YEAR_TO_DATE)
        assertEquals(GraphTimeFrame.YEAR_TO_DATE, viewModel.settings.value.ui.selectedGraphTimeFrame)

        viewModel.setSelectedGraphTimeFrame(GraphTimeFrame.THREE_MONTHS)
        assertEquals(GraphTimeFrame.THREE_MONTHS, viewModel.settings.value.ui.selectedGraphTimeFrame)

        viewModel.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.settings.value.appearance.themeMode)

        viewModel.setAppTheme(AppTheme.BLUE)
        assertEquals(AppTheme.BLUE, viewModel.settings.value.appearance.appTheme)

        viewModel.setAdaptiveDisplay(false)
        assertEquals(false, viewModel.settings.value.appearance.adaptiveDisplay)

        viewModel.setSplitterPosition(0.65f)
        assertEquals(0.65f, viewModel.settings.value.ui.splitterPosition)

        viewModel.setWeightUnit(WeightUnit.METRIC)
        assertEquals(WeightUnit.METRIC, viewModel.settings.value.weight.unit)

        viewModel.setShowWeightInGraph(false)
        assertEquals(false, viewModel.settings.value.weight.showInGraph)

        viewModel.setShowBloodPressureInGraph(false)
        assertEquals(false, viewModel.settings.value.bloodPressure.showInGraph)

        viewModel.setShowPulseInGraph(true)
        assertEquals(true, viewModel.settings.value.bloodPressure.showPulseInGraph)

        viewModel.setBloodPressureDisplaySettings(
            showDailyAverages = false,
            showMonthlyAverages = false,
            showDailyChanges = false,
            showMonthlyChanges = false
        )
        assertEquals(false, viewModel.settings.value.bloodPressure.showDailyAverages)
        assertEquals(false, viewModel.settings.value.bloodPressure.showMonthlyAverages)
        assertEquals(false, viewModel.settings.value.bloodPressure.showDailyChanges)
        assertEquals(false, viewModel.settings.value.bloodPressure.showMonthlyChanges)

        viewModel.setWindowState(x = 150, y = 120, width = 1200, height = 800, maximized = true)
        assertEquals(150, viewModel.settings.value.ui.windowX)
        assertEquals(120, viewModel.settings.value.ui.windowY)
        assertEquals(1200, viewModel.settings.value.ui.windowWidth)
        assertEquals(800, viewModel.settings.value.ui.windowHeight)
        assertEquals(true, viewModel.settings.value.ui.windowMaximized)

        viewModel.setSyncEnabled(true)
        assertEquals(true, viewModel.settings.value.sync.syncEnabled)

        viewModel.setSyncProvider(SyncProviderType.GOOGLE_DRIVE)
        assertEquals(SyncProviderType.GOOGLE_DRIVE, viewModel.settings.value.sync.syncProvider)

        viewModel.setLocalSyncPath("/path/to/sync")
        assertEquals("/path/to/sync", viewModel.settings.value.sync.localSyncPath)

        viewModel.setAutoSyncOnClose(true)
        assertEquals(true, viewModel.settings.value.sync.autoSyncOnClose)

        viewModel.setAutoSyncIntervalMinutes(30)
        assertEquals(30, viewModel.settings.value.sync.autoSyncIntervalMinutes)

        viewModel.setGoogleSession(
            email = "user@gmail.com",
            accessToken = "access123",
            refreshToken = "refresh456",
            tokenStatus = "Active (expires in 60m)",
            expiresAt = 1700000000000L,
            refreshTokenExpiresAt = 1700604800000L
        )
        assertEquals("user@gmail.com", viewModel.settings.value.sync.googleAccountEmail)
        assertEquals("access123", viewModel.settings.value.sync.googleAccessToken)
        assertEquals("refresh456", viewModel.settings.value.sync.googleRefreshToken)
        assertEquals("Active (expires in 60m)", viewModel.settings.value.sync.googleTokenStatus)
        assertEquals(1700000000000L, viewModel.settings.value.sync.googleTokenExpiresAt)
        assertEquals(1700604800000L, viewModel.settings.value.sync.googleRefreshTokenExpiresAt)

        viewModel.clearGoogleSession()
        assertEquals("", viewModel.settings.value.sync.googleAccessToken)
        assertEquals("", viewModel.settings.value.sync.googleRefreshToken)
        assertEquals("", viewModel.settings.value.sync.googleAccountEmail)
        assertEquals("Revoked", viewModel.settings.value.sync.googleTokenStatus)
        assertEquals(null, viewModel.settings.value.sync.googleTokenExpiresAt)
        assertEquals(null, viewModel.settings.value.sync.googleRefreshTokenExpiresAt)
    }

    @Test
    fun testSettingsViewModel_PeerToPeerMutations() {
        val flow = MutableStateFlow(SettingsData())
        val viewModel = SettingsViewModel(flow)

        viewModel.setPeerIsServerMode(true)
        assertEquals(true, viewModel.settings.value.peerSync.isServerMode)

        viewModel.setPeerServerEnabled(true)
        assertEquals(true, viewModel.settings.value.peerSync.localServerEnabled)
        assertEquals(true, viewModel.settings.value.peerSync.isServerMode)

        viewModel.setPeerServerPort(9000)
        assertEquals(9000, viewModel.settings.value.peerSync.localServerPort)

        viewModel.setPeerServerPin("9876")
        assertEquals("9876", viewModel.settings.value.peerSync.localServerPin)

        viewModel.setDeviceName("Studio PC")
        assertEquals("Studio PC", viewModel.settings.value.peerSync.deviceName)

        viewModel.setPeerClientTarget(
            instanceId = "remote-node-1",
            host = "192.168.1.50",
            port = 9000,
            token = "session-token-xyz",
            name = "Living Room Node"
        )
        assertEquals("remote-node-1", viewModel.settings.value.peerSync.serverInstanceId)
        assertEquals("192.168.1.50", viewModel.settings.value.peerSync.serverHost)
        assertEquals(9000, viewModel.settings.value.peerSync.serverPort)
        assertEquals("session-token-xyz", viewModel.settings.value.peerSync.serverToken)
        assertEquals("Living Room Node", viewModel.settings.value.peerSync.serverName)

        viewModel.disconnectPeerClient()
        assertEquals(null, viewModel.settings.value.peerSync.serverInstanceId)
        assertEquals("", viewModel.settings.value.peerSync.serverHost)
        assertEquals(SyncConfig.DEFAULT_P2P_PORT, viewModel.settings.value.peerSync.serverPort)
        assertEquals("", viewModel.settings.value.peerSync.serverToken)
        assertEquals("", viewModel.settings.value.peerSync.serverName)
    }
}
