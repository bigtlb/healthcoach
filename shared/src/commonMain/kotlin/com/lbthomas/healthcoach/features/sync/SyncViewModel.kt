package com.lbthomas.healthcoach.features.sync

import co.touchlab.kermit.Logger
import com.lbthomas.healthcoach.core.sync.SyncEngine
import com.lbthomas.healthcoach.core.sync.SyncResult
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Success(val message: String) : SyncState
    data class Error(val message: String) : SyncState
    data object Cancelled : SyncState
}

class SyncViewModel {
    val syncEngine: SyncEngine?
    val settingsStore: SettingsStore?
    val syncState: StateFlow<SyncState>
    val settings: StateFlow<SettingsData>
    val isSyncing: StateFlow<Boolean>

    private val scope: CoroutineScope
    private var syncJob: Job? = null
    private var periodicJob: Job? = null
    private val _syncState: MutableStateFlow<SyncState>

    constructor(
        syncEngine: SyncEngine,
        settingsStore: SettingsStore
    ) {
        this.syncEngine = syncEngine
        this.settingsStore = settingsStore
        this.scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        this._syncState = MutableStateFlow<SyncState>(SyncState.Idle)
        this.syncState = _syncState.asStateFlow()
        this.settings = settingsStore.settings
        this.isSyncing = _syncState.map { it is SyncState.Syncing }
            .stateIn(scope, SharingStarted.Eagerly, false)

        scope.launch {
            settings.collectLatest { s ->
                periodicJob?.cancel()
                if (s.syncEnabled && s.autoSyncIntervalMinutes > 0) {
                    val delayMillis = s.autoSyncIntervalMinutes * 60 * 1000L
                    periodicJob = this@launch.launch {
                        while (isActive) {
                            delay(delayMillis.milliseconds)
                            if (_syncState.value !is SyncState.Syncing) {
                                Logger.i("Triggering periodic background sync (${s.autoSyncIntervalMinutes}m interval)")
                                performSync(showNotifications = false)
                            }
                        }
                    }
                }
            }
        }
    }

    constructor(
        syncState: StateFlow<SyncState> = MutableStateFlow(SyncState.Idle),
        settings: StateFlow<SettingsData> = MutableStateFlow(SettingsData())
    ) {
        this.syncEngine = null
        this.settingsStore = null
        this.scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        this._syncState = (syncState as? MutableStateFlow<SyncState>) ?: MutableStateFlow(syncState.value)
        this.syncState = syncState
        this.settings = settings
        this.isSyncing = syncState.map { it is SyncState.Syncing }
            .stateIn(scope, SharingStarted.Eagerly, false)
    }

    fun syncNow() {
        performSync(showNotifications = true)
    }

    fun cancelSync() {
        syncJob?.cancel()
        _syncState.value = SyncState.Cancelled
        scope.launch {
            SyncNotificationManager.postNotification("Sync cancelled by user", isError = false)
        }
    }

    private fun performSync(showNotifications: Boolean) {
        val engine = syncEngine ?: return
        if (_syncState.value is SyncState.Syncing) return

        val config = settings.value.toSyncConfig()
        if (!settings.value.syncEnabled) return

        syncJob = scope.launch {
            _syncState.value = SyncState.Syncing
            if (showNotifications) {
                SyncNotificationManager.postNotification("Syncing health records...", isError = false)
            }

            when (val result = engine.sync(config)) {
                is SyncResult.Success -> {
                    _syncState.value = SyncState.Success(result.message)
                    if (showNotifications) {
                        SyncNotificationManager.postNotification(result.message, isError = false)
                    }
                    delay(3000.milliseconds)
                    if (_syncState.value is SyncState.Success) {
                        _syncState.value = SyncState.Idle
                    }
                }
                is SyncResult.ConflictResolved -> {
                    _syncState.value = SyncState.Success(result.message)
                    if (showNotifications) {
                        SyncNotificationManager.postNotification(result.message, isError = false)
                    }
                    delay(3000.milliseconds)
                    if (_syncState.value is SyncState.Success) {
                        _syncState.value = SyncState.Idle
                    }
                }
                is SyncResult.Error -> {
                    _syncState.value = SyncState.Error(result.message)
                    if (showNotifications) {
                        SyncNotificationManager.postNotification("Sync failed: ${result.message}", isError = true)
                    }
                }
                is SyncResult.Cancelled -> {
                    _syncState.value = SyncState.Cancelled
                }
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
