package com.lbthomas.healthcoach

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.lbthomas.healthcoach.core.di.appModule
import com.lbthomas.healthcoach.core.di.configurePlatformContext
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import healthcoach.shared.generated.resources.Res
import healthcoach.shared.generated.resources.scales
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin


fun main() = application {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            configurePlatformContext(null)
            modules(appModule)
        }
    }

    val settingsViewModel = GlobalContext.get().get<SettingsViewModel>()
    val settings by settingsViewModel.settings.collectAsState()

    val windowState = rememberWindowState(
        position = WindowPosition(
            x = settings.windowX.dp,
            y = settings.windowY.dp
        ),
        size = DpSize(
            width = settings.windowWidth.dp,
            height = settings.windowHeight.dp
        ),
        placement = if (settings.windowMaximized) {
            WindowPlacement.Maximized
        } else {
            WindowPlacement.Floating
        }
    )

    val icon = painterResource(Res.drawable.scales)

    Tray(
        icon = icon,
        tooltip = "HealthCoach",
    )

    Window(
        onCloseRequest = {
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

            exitApplication()
        },
        title = "HealthCoach",
        icon = icon,
        state = windowState
    ) {
        App()
    }
}