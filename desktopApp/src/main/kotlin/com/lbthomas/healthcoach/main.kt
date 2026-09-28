package com.lbthomas.healthcoach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.lbthomas.healthcoach.core.di.appModule
import com.lbthomas.healthcoach.core.di.configurePlatformContext
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.sync.SyncNotificationManager
import healthcoach.shared.generated.resources.Res
import healthcoach.shared.generated.resources.scales
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import kotlin.time.Duration.Companion.milliseconds


fun main() {
    System.setProperty("skiko.vsync.enabled", "false")
    application {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                configurePlatformContext(null)
                modules(appModule)
            }
        }

        val settingsViewModel = GlobalContext.get().get<SettingsViewModel>()
        settingsViewModel.initializeServerIfEnabled()

        val settings = settingsViewModel.settings.value

        val windowState = GetWindowState(settings)
        val icon = painterResource(Res.drawable.scales)

        if (isTraySupported) {
            Tray(
                icon = icon,
                tooltip = "HealthCoach",
            )
        }

        val appScope = rememberCoroutineScope()
        var isShuttingDown by remember { mutableStateOf(false) }
        var shutdownMessage by remember { mutableStateOf("") }

        Window(
            onCloseRequest = {
                if (isShuttingDown) return@Window
                saveWindowState(windowState, settingsViewModel)
                val currentSettings = settingsViewModel.settings.value
                val needsSync = currentSettings.sync.syncEnabled && currentSettings.sync.autoSyncOnClose
                val needsServerStop = currentSettings.peerSync.localServerEnabled

                if (needsSync || needsServerStop) {
                    isShuttingDown = true
                    appScope.launch {
                        if (needsSync) {
                            shutdownMessage = "Syncing database before exit..."
                            SyncNotificationManager.postNotification(shutdownMessage)
                            runCatching {
                                val syncEngine = GlobalContext.get().getOrNull<com.lbthomas.healthcoach.core.sync.SyncEngine>()
                                withTimeoutOrNull(5000L.milliseconds) {
                                    syncEngine?.sync(currentSettings.toSyncConfig())
                                }
                            }
                        }
                        if (needsServerStop) {
                            shutdownMessage = "Shutting down Peer-to-Peer server..."
                            SyncNotificationManager.postNotification(shutdownMessage)
                            runCatching {
                                val peerServerManager = GlobalContext.get().getOrNull<com.lbthomas.healthcoach.core.sync.p2p.PeerServerManager>()
                                withTimeoutOrNull(1000L.milliseconds) {
                                    peerServerManager?.stop()
                                }
                            }
                        }
                        delay(500L.milliseconds)
                        settingsViewModel.shutdownServerAndDiscovery()
                        exitApplication()
                    }
                } else {
                    settingsViewModel.shutdownServerAndDiscovery()
                    exitApplication()
                }
            },
            title = "HealthCoach",
            icon = icon,
            state = windowState
        ) {
            LaunchedEffect(window) {
                window.addWindowStateListener {
                    window.revalidate()
                    window.repaint()
                }
                window.addWindowFocusListener(object : java.awt.event.WindowFocusListener {
                    override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                        window.repaint()
                    }
                    override fun windowLostFocus(e: java.awt.event.WindowEvent?) {}
                })
            }

            Box(modifier = Modifier.fillMaxSize()) {
            App()

            if (isShuttingDown) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = shutdownMessage.ifBlank { "Shutting down..." },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

private fun saveWindowState(
    windowState: WindowState,
    settingsViewModel: SettingsViewModel
) {
    val position = windowState.position
    val size = windowState.size
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    settingsViewModel.setWindowState(
        x = position.x.value.toInt(),
        y = position.y.value.toInt(),
        width = size.width.value.toInt(),
        height = size.height.value.toInt(),
        maximized = isMaximized
    )
}

@Composable
private fun GetWindowState(settings: SettingsData): WindowState = rememberWindowState(
    position = WindowPosition(
        x = settings.ui.windowX.dp,
        y = settings.ui.windowY.dp
    ),
    size = DpSize(
        width = settings.ui.windowWidth.dp,
        height = settings.ui.windowHeight.dp
    ),
    placement = if (settings.ui.windowMaximized) {
        WindowPlacement.Maximized
    } else {
        WindowPlacement.Floating
    }
)