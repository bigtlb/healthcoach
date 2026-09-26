package com.lbthomas.healthcoach.features.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class SyncNotification(
    val message: String,
    val isError: Boolean = false
)

object SyncNotificationManager {
    private val _notifications = MutableSharedFlow<SyncNotification>(extraBufferCapacity = 10)
    val notifications: SharedFlow<SyncNotification> = _notifications.asSharedFlow()

    suspend fun postNotification(message: String, isError: Boolean = false) {
        _notifications.emit(SyncNotification(message, isError))
    }
}
