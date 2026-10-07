package com.lbthomas.healthcoach.features.settings

import app.cash.sqldelight.db.SqlDriver
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.enums.*
import com.lbthomas.healthcoach.core.logging.LoggingConfig
import com.lbthomas.healthcoach.core.sync.StorageAdapterFactory
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncEngine
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.sync.p2p.*
import com.lbthomas.healthcoach.core.theme.AppTheme
import com.lbthomas.healthcoach.features.foodjournal.data.DefaultFoodData
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SettingsViewModel {
    val settings: StateFlow<SettingsData>
    val persistence: SettingsStore?
    val peerServerManager: PeerServerManager?
    val discoveryAdvertiser: PeerDiscoveryAdvertiser?
    val discoveryBrowser: PeerDiscoveryBrowser?
    val syncEngine: SyncEngine?
    val driverFactory: DriverFactory?
    val sqlDriver: SqlDriver?

    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    val serverStatus: StateFlow<PeerServerStatus>
        get() = peerServerManager?.serverStatus ?: MutableStateFlow(PeerServerStatus()).asStateFlow()

    val discoveredPeers: StateFlow<List<DiscoveredPeer>>
        get() = discoveryBrowser?.discoveredPeers ?: MutableStateFlow(emptyList<DiscoveredPeer>()).asStateFlow()

    constructor(
        persistence: SettingsStore,
        peerServerManager: PeerServerManager? = null,
        discoveryAdvertiser: PeerDiscoveryAdvertiser? = null,
        discoveryBrowser: PeerDiscoveryBrowser? = null,
        syncEngine: SyncEngine? = null,
        driverFactory: DriverFactory? = null,
        sqlDriver: SqlDriver? = null
    ) {
        this.persistence = persistence
        this.settings = persistence.settings
        this.peerServerManager = peerServerManager
        this.discoveryAdvertiser = discoveryAdvertiser
        this.discoveryBrowser = discoveryBrowser
        this.syncEngine = syncEngine
        this.driverFactory = driverFactory
        this.sqlDriver = sqlDriver
    }

    constructor(
        settings: StateFlow<SettingsData>,
        peerServerManager: PeerServerManager? = null,
        discoveryAdvertiser: PeerDiscoveryAdvertiser? = null,
        discoveryBrowser: PeerDiscoveryBrowser? = null,
        syncEngine: SyncEngine? = null,
        driverFactory: DriverFactory? = null,
        sqlDriver: SqlDriver? = null
    ) {
        this.persistence = null
        this.settings = settings
        this.peerServerManager = peerServerManager
        this.discoveryAdvertiser = discoveryAdvertiser
        this.discoveryBrowser = discoveryBrowser
        this.syncEngine = syncEngine
        this.driverFactory = driverFactory
        this.sqlDriver = sqlDriver
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

    fun setBodyTextSize(size: FontSizePreference) {
        updateSettings { it.copy(appearance = it.appearance.copy(bodyTextSize = size)) }
    }

    fun setLabelTextSize(size: FontSizePreference) {
        updateSettings { it.copy(appearance = it.appearance.copy(labelTextSize = size)) }
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

    fun setShowFoodJournalInGraph(show: Boolean) {
        updateSettings { it.copy(foodJournal = it.foodJournal.copy(showInGraph = show)) }
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

    fun isPeerServerEligible(settingsData: SettingsData = settings.value): Boolean {
        return settingsData.sync.syncEnabled &&
            settingsData.sync.syncProvider == SyncProviderType.PEER_TO_PEER &&
            settingsData.peerSync.isServerMode &&
            settingsData.peerSync.localServerEnabled
    }

    fun setSyncEnabled(enabled: Boolean) {
        updateSettings { it.copy(sync = it.sync.copy(syncEnabled = enabled)) }
        val current = settings.value
        if (isPeerServerEligible(current)) {
            viewModelScope.launch {
                val res = peerServerManager?.start(current.peerSync.localServerPort)
                val boundPort = res?.getOrNull() ?: current.peerSync.localServerPort
                discoveryAdvertiser?.startAdvertising(
                    instanceId = current.peerSync.instanceId,
                    deviceName = current.peerSync.deviceName,
                    port = boundPort
                )
            }
        } else {
            viewModelScope.launch {
                discoveryAdvertiser?.stopAdvertising()
                peerServerManager?.stop()
            }
        }
    }

    fun setSyncProvider(provider: SyncProviderType) {
        updateSettings { it.copy(sync = it.sync.copy(syncProvider = provider)) }
        val current = settings.value
        if (isPeerServerEligible(current)) {
            viewModelScope.launch {
                val res = peerServerManager?.start(current.peerSync.localServerPort)
                val boundPort = res?.getOrNull() ?: current.peerSync.localServerPort
                discoveryAdvertiser?.startAdvertising(
                    instanceId = current.peerSync.instanceId,
                    deviceName = current.peerSync.deviceName,
                    port = boundPort
                )
            }
        } else {
            viewModelScope.launch {
                discoveryAdvertiser?.stopAdvertising()
                peerServerManager?.stop()
            }
        }
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

    fun setPeerServerEnabled(enabled: Boolean) {
        persistence?.setPeerServerEnabled(enabled) ?: updateSettings {
            it.copy(peerSync = it.peerSync.copy(localServerEnabled = enabled))
        }
        val current = settings.value
        if (isPeerServerEligible(current)) {
            viewModelScope.launch {
                val res = peerServerManager?.start(current.peerSync.localServerPort)
                val boundPort = res?.getOrNull() ?: current.peerSync.localServerPort
                discoveryAdvertiser?.startAdvertising(
                    instanceId = current.peerSync.instanceId,
                    deviceName = current.peerSync.deviceName,
                    port = boundPort
                )
            }
        } else {
            viewModelScope.launch {
                discoveryAdvertiser?.stopAdvertising()
                peerServerManager?.stop()
            }
        }
    }

    fun setPeerIsServerMode(isServer: Boolean) {
        persistence?.setPeerIsServerMode(isServer) ?: updateSettings {
            it.copy(peerSync = it.peerSync.copy(isServerMode = isServer))
        }
        val current = settings.value
        if (isPeerServerEligible(current)) {
            viewModelScope.launch {
                val res = peerServerManager?.start(current.peerSync.localServerPort)
                val boundPort = res?.getOrNull() ?: current.peerSync.localServerPort
                discoveryAdvertiser?.startAdvertising(
                    instanceId = current.peerSync.instanceId,
                    deviceName = current.peerSync.deviceName,
                    port = boundPort
                )
            }
        } else {
            viewModelScope.launch {
                discoveryAdvertiser?.stopAdvertising()
                peerServerManager?.stop()
            }
        }
    }

    fun setPeerServerPort(port: Int) {
        updateSettings { it.copy(peerSync = it.peerSync.copy(localServerPort = port)) }
        val current = settings.value
        if (isPeerServerEligible(current)) {
            viewModelScope.launch {
                val res = peerServerManager?.restart(port)
                val boundPort = res?.getOrNull() ?: port
                discoveryAdvertiser?.startAdvertising(
                    instanceId = current.peerSync.instanceId,
                    deviceName = current.peerSync.deviceName,
                    port = boundPort
                )
            }
        }
    }

    fun setPeerServerPin(pin: String) {
        updateSettings { it.copy(peerSync = it.peerSync.copy(localServerPin = pin)) }
    }

    fun setDeviceName(deviceName: String) {
        updateSettings { it.copy(peerSync = it.peerSync.copy(deviceName = deviceName)) }
        val current = settings.value
        if (isPeerServerEligible(current)) {
            val port = peerServerManager?.serverStatus?.value?.port ?: current.peerSync.localServerPort
            discoveryAdvertiser?.startAdvertising(
                instanceId = current.peerSync.instanceId,
                deviceName = deviceName,
                port = port
            )
        }
    }

    fun startDiscovery() {
        discoveryBrowser?.startBrowsing(settings.value.peerSync.instanceId)
    }

    fun stopDiscovery() {
        discoveryBrowser?.stopBrowsing()
    }

    fun clearDiscoveredPeers() {
        discoveryBrowser?.clearPeers()
    }

    fun refreshDiscovery() {
        discoveryBrowser?.stopBrowsing()
        discoveryBrowser?.clearPeers()
        discoveryBrowser?.startBrowsing(settings.value.peerSync.instanceId)
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

    fun disconnectPeerClient() {
        setPeerClientTarget(null, "", SyncConfig.DEFAULT_P2P_PORT, "", "")
    }

    fun clearServerHistory() {
        peerServerManager?.clearAllSessions()
        persistence?.clearServerHistory() ?: updateSettings {
            it.copy(peerSync = it.peerSync.copy(localServerHistory = emptyList()))
        }
    }

    suspend fun requestPairingPin(
        host: String,
        port: Int
    ): Result<Unit> {
        val current = settings.value
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    prettyPrint = false
                })
            }
        }

        return try {
            val response = client.post("http://${host.trim()}:$port/api/v1/auth/request-pin") {
                contentType(ContentType.Application.Json)
                setBody(
                    PairInitRequest(
                        clientInstanceId = current.peerSync.instanceId,
                        clientName = current.peerSync.deviceName.ifBlank { "HealthCoach Client" }
                    )
                )
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Server returned HTTP ${response.status} when requesting PIN"))
            }
        } catch (e: Exception) {
            LoggingConfig.clientLogger.e("Request pairing PIN failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            client.close()
        }
    }

    suspend fun pairWithPeer(
        host: String,
        port: Int,
        pin: String
    ): Result<PairResponse> {
        val current = settings.value
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    prettyPrint = false
                })
            }
        }

        return try {
            val response = client.post("http://${host.trim()}:$port/api/v1/auth/pair") {
                contentType(ContentType.Application.Json)
                setBody(
                    PairRequest(
                        clientInstanceId = current.peerSync.instanceId,
                        clientName = current.peerSync.deviceName.ifBlank { "HealthCoach Client" },
                        pin = pin
                    )
                )
            }

            if (response.status == HttpStatusCode.OK) {
                val pairResp = response.body<PairResponse>()
                setPeerClientTarget(
                    instanceId = pairResp.serverInstanceId,
                    host = host.trim(),
                    port = port,
                    token = pairResp.token,
                    name = pairResp.serverName
                )
                Result.success(pairResp)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                Result.failure(IllegalStateException("Invalid PIN entered for server"))
            } else {
                Result.failure(IllegalStateException("Pairing failed (HTTP ${response.status})"))
            }
        } catch (e: Exception) {
            LoggingConfig.clientLogger.e("Pairing request failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            client.close()
        }
    }

    suspend fun testPeerConnection(
        host: String = settings.value.peerSync.serverHost,
        port: Int = settings.value.peerSync.serverPort,
        token: String = settings.value.peerSync.serverToken
    ): Result<PeerStatusResponse> {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    prettyPrint = false
                })
            }
        }
        return try {
            val response = client.get("http://${host.trim()}:$port/api/v1/status") {
                if (token.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            if (response.status == HttpStatusCode.OK) {
                val statusResponse = response.body<PeerStatusResponse>()
                Result.success(statusResponse)
            } else {
                Result.failure(IllegalStateException("Server returned HTTP ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            client.close()
        }
    }

    fun initializeServerIfEnabled() {
        val current = settings.value
        if (isPeerServerEligible(current)) {
            viewModelScope.launch {
                val res = peerServerManager?.start(current.peerSync.localServerPort)
                val boundPort = res?.getOrNull() ?: current.peerSync.localServerPort
                discoveryAdvertiser?.startAdvertising(
                    instanceId = current.peerSync.instanceId,
                    deviceName = current.peerSync.deviceName,
                    port = boundPort
                )
            }
        }
    }

    fun shutdownServerAndDiscovery() {
        viewModelScope.launch {
            discoveryAdvertiser?.stopAdvertising()
            discoveryBrowser?.stopBrowsing()
            peerServerManager?.stop()
        }
    }

    /**
     * Resets local database by clearing all metric entries, restoring default food items,
     * resetting sync cache/metadata, and resetting user settings to defaults.
     */
    suspend fun resetDatabaseAndSettings(): Result<Unit> {
        return try {
            if (driverFactory != null) {
                val driver = driverFactory.createDriver()
                try {
                    driver.execute(null, "DELETE FROM weightEntry", 0, null)
                    driver.execute(null, "DELETE FROM bloodPressureEntry", 0, null)
                    driver.execute(null, "DELETE FROM mealEntry", 0, null)
                    driver.execute(null, "DELETE FROM foodItem", 0, null)
                    driver.execute(null, "DELETE FROM foodUnit", 0, null)
                    driver.execute(null, "DELETE FROM profileSetting", 0, null)

                    val db = createDatabaseForDriver(driver)
                    DefaultFoodData.ensureDefaultFoodData(db)
                    driver.notifyListeners("weightEntry", "bloodPressureEntry", "foodUnit", "foodItem", "mealEntry", "profileSetting")
                } finally {
                    try { driver.close() } catch (_: Exception) {}
                }
            }

            sqlDriver?.notifyListeners("weightEntry", "bloodPressureEntry", "foodUnit", "foodItem", "mealEntry", "profileSetting")

            // Clear local sync staging and cache files
            syncEngine?.clearLocalSyncCache()

            // Reset settings store
            persistence?.resetSettings() ?: run {
                if (settings is MutableStateFlow<SettingsData>) {
                    settings.value = SettingsData()
                }
            }

            LoggingConfig.clientLogger.i("Database and settings have been reset to defaults.")
            Result.success(Unit)
        } catch (e: Exception) {
            LoggingConfig.clientLogger.e("Failed to reset database and settings: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Resets the remote sync destination file for a specific provider.
     */
    suspend fun resetRemoteDestination(
        providerType: SyncProviderType? = null,
        localFolderOverride: String? = null
    ): Result<Unit> {
        val targetType = providerType ?: settings.value.sync.syncProvider
        if (targetType == SyncProviderType.PEER_TO_PEER) {
            return Result.failure(IllegalStateException("Peer to Peer storage cannot be remotely reset."))
        }

        val baseConfig = settings.value.toSyncConfig().copy(
            providerType = targetType,
            localFolderPath = localFolderOverride ?: settings.value.sync.localSyncPath
        )
        val adapter = StorageAdapterFactory.createAdapter(baseConfig)

        return if (syncEngine != null) {
            syncEngine.resetRemoteDestination(adapter)
        } else {
            val deleteSuccess = adapter.deleteFile()
            if (deleteSuccess) Result.success(Unit) else Result.failure(Exception("Failed to delete remote destination file"))
        }
    }

    /**
     * Checks if an existing backup file is present in Google Drive appDataFolder.
     */
    suspend fun checkGoogleDriveExistingFile(): Boolean {
        val googleConfig = settings.value.toSyncConfig().copy(providerType = SyncProviderType.GOOGLE_DRIVE)
        val adapter = StorageAdapterFactory.createAdapter(googleConfig)
        return try {
            adapter.fileExists()
        } catch (e: Exception) {
            LoggingConfig.clientLogger.w("Failed to check Google Drive existing file: ${e.message}")
            false
        }
    }
}
