package com.lbthomas.healthcoach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.lbthomas.healthcoach.core.di.appModule
import com.lbthomas.healthcoach.core.di.configurePlatformContext
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.sync.SyncNotificationManager
import healthcoach.shared.generated.resources.Res
import healthcoach.shared.generated.resources.scales_icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.awt.Component
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.max
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
        val icon = painterResource(Res.drawable.scales_icon)

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
                val needsServerStop = currentSettings.sync.syncEnabled && currentSettings.peerSync.isServerMode && currentSettings.peerSync.localServerEnabled

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

            ProvideDynamicDensity(window) {
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
}

private object ScaleResolver {
    fun getScaleForComponent(component: Component?): Float {
        val config = component?.graphicsConfiguration ?: getDefaultConfiguration()
        return calculateConfigScale(config)
    }

    private fun getDefaultConfiguration(): GraphicsConfiguration {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice
            .defaultConfiguration
    }

    fun calculateConfigScale(config: GraphicsConfiguration): Float {
        val transformScale = config.defaultTransform.scaleX.toFloat()
        val toolkitDpi = Toolkit.getDefaultToolkit().screenResolution
        val toolkitScale = toolkitDpi.toFloat() / 96.0f
        val xrdbScale = getXrdbScale()

        return if (transformScale > 1.0f) {
            transformScale
        } else {
            max(1.0f, max(toolkitScale, xrdbScale))
        }
    }

    private fun getXrdbScale(): Float {
        return try {
            val process = ProcessBuilder("xrdb", "-query").start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val dpiLine = output.lines().firstOrNull { it.startsWith("Xft.dpi:") }
            val dpi = dpiLine?.substringAfter(":")?.trim()?.toFloatOrNull() ?: 96.0f
            dpi / 96.0f
        } catch (_: Exception) {
            1.0f
        }
    }
}

@Composable
private fun rememberWindowScale(window: java.awt.Window): MutableState<Float> {
    val scaleState = remember { mutableStateOf(ScaleResolver.getScaleForComponent(window)) }

    DisposableEffect(window) {
        val listener = object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent?) {
                updateScale()
            }

            override fun componentResized(e: ComponentEvent?) {
                updateScale()
            }

            private fun updateScale() {
                val newScale = ScaleResolver.getScaleForComponent(window)
                if (scaleState.value != newScale) {
                    scaleState.value = newScale
                }
            }
        }

        window.addComponentListener(listener)
        scaleState.value = ScaleResolver.getScaleForComponent(window)

        onDispose {
            window.removeComponentListener(listener)
        }
    }

    return scaleState
}

@Composable
private fun ProvideDynamicDensity(window: java.awt.Window, content: @Composable () -> Unit) {
    val detectedScale by rememberWindowScale(window)
    val currentDensity = LocalDensity.current

    val effectiveDensity = if (currentDensity.density <= 1.0f && detectedScale > 1.0f) {
        detectedScale
    } else {
        currentDensity.density
    }

    val customDensity = remember(effectiveDensity, currentDensity.fontScale) {
        Density(density = effectiveDensity, fontScale = currentDensity.fontScale)
    }

    CompositionLocalProvider(LocalDensity provides customDensity) {
        content()
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