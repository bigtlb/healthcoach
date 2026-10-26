package com.lbthomas.healthcoach.features.settings.data

import com.lbthomas.healthcoach.core.getDefaultDeviceName
import com.lbthomas.healthcoach.core.sync.p2p.PeerClientRecord
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
        if (peerSync.deviceName.isBlank() || peerSync.deviceName == "HealthCoach Device") {
            peerSync = peerSync.copy(deviceName = getDefaultDeviceName())
            updated = true
        }
        return if (updated) settings.copy(peerSync = peerSync) else settings
    }

    fun updateSettings(transform: (SettingsData) -> SettingsData) {
        val updated = _settings.updateAndGet(transform)
        saveSettings(updated)
    }

    fun setPeerServerPin(pin: String) {
        updateSettings { it.copy(peerSync = it.peerSync.copy(localServerPin = pin)) }
    }

    fun addServerHistoryRecord(record: PeerClientRecord) {
        updateSettings { current ->
            val existing = current.peerSync.localServerHistory.find { it.clientInstanceId == record.clientInstanceId }
            val tokenToUse = record.authToken.ifBlank { existing?.authToken.orEmpty() }
            val mergedRecord = record.copy(authToken = tokenToUse)
            val filtered = current.peerSync.localServerHistory.filter { it.clientInstanceId != record.clientInstanceId }
            current.copy(peerSync = current.peerSync.copy(localServerHistory = listOf(mergedRecord) + filtered))
        }
    }

    fun clearServerHistory() {
        updateSettings { it.copy(peerSync = it.peerSync.copy(localServerHistory = emptyList())) }
    }

    fun resetSettings() {
        val fresh = ensureInstanceIdentity(SettingsData())
        _settings.value = fresh
        saveSettings(fresh)
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
