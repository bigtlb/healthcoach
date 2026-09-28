package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.logging.LoggingConfig
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.sync.p2p.*
import com.lbthomas.healthcoach.core.theme.AppTheme
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

    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    val serverStatus: StateFlow<PeerServerStatus>
        get() = peerServerManager?.serverStatus ?: MutableStateFlow(PeerServerStatus()).asStateFlow()

    val discoveredPeers: StateFlow<List<DiscoveredPeer>>
        get() = discoveryBrowser?.discoveredPeers ?: MutableStateFlow(emptyList<DiscoveredPeer>()).asStateFlow()

    constructor(
        persistence: SettingsStore,
        peerServerManager: PeerServerManager? = null,
        discoveryAdvertiser: PeerDiscoveryAdvertiser? = null,
        discoveryBrowser: PeerDiscoveryBrowser? = null
    ) {
        this.persistence = persistence
        this.settings = persistence.settings
        this.peerServerManager = peerServerManager
        this.discoveryAdvertiser = discoveryAdvertiser
        this.discoveryBrowser = discoveryBrowser
    }

    constructor(settings: StateFlow<SettingsData>) {
        this.persistence = null
        this.settings = settings
        this.peerServerManager = null
        this.discoveryAdvertiser = null
        this.discoveryBrowser = null
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

    fun setPeerServerEnabled(enabled: Boolean) {
        persistence?.setPeerServerEnabled(enabled) ?: updateSettings {
            it.copy(
                peerSync = it.peerSync.copy(
                    localServerEnabled = enabled,
                    isServerMode = if (enabled) true else it.peerSync.isServerMode
                )
            )
        }
        val current = settings.value
        if (enabled) {
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
            discoveryAdvertiser?.stopAdvertising()
            viewModelScope.launch {
                peerServerManager?.stop()
            }
        }
    }

    fun setPeerIsServerMode(isServer: Boolean) {
        persistence?.setPeerIsServerMode(isServer) ?: updateSettings {
            it.copy(peerSync = it.peerSync.copy(isServerMode = isServer))
        }
    }

    fun setPeerServerPort(port: Int) {
        updateSettings { it.copy(peerSync = it.peerSync.copy(localServerPort = port)) }
        val current = settings.value
        if (current.peerSync.localServerEnabled) {
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
        if (current.peerSync.localServerEnabled) {
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
        if (current.peerSync.localServerEnabled) {
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
        discoveryAdvertiser?.stopAdvertising()
        discoveryBrowser?.stopBrowsing()
        viewModelScope.launch {
            peerServerManager?.stop()
        }
    }
}
