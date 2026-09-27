package com.lbthomas.healthcoach.features.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncNotification(
    val message: String,
    val isError: Boolean = false
)

data class PairingPinPrompt(
    val clientName: String,
    val pin: String
)

object SyncNotificationManager {
    private val _notifications = MutableSharedFlow<SyncNotification>(extraBufferCapacity = 10)
    val notifications: SharedFlow<SyncNotification> = _notifications.asSharedFlow()

    private val _pairingPinPrompt = MutableStateFlow<PairingPinPrompt?>(null)
    val pairingPinPrompt: StateFlow<PairingPinPrompt?> = _pairingPinPrompt.asStateFlow()

    suspend fun postNotification(message: String, isError: Boolean = false) {
        _notifications.emit(SyncNotification(message, isError))
    }

    fun showPairingPrompt(clientName: String, pin: String) {
        _pairingPinPrompt.value = PairingPinPrompt(clientName, pin)
    }

    fun dismissPairingPrompt() {
        _pairingPinPrompt.value = null
    }
}
