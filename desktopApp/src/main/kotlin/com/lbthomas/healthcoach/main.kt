package com.lbthomas.healthcoach

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.lbthomas.healthcoach.core.di.appModule
import com.lbthomas.healthcoach.core.di.configurePlatformContext
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
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

    // this is overkill for making the window reflect state changes
    // val settings by settingsViewModel.settings.collectAsState()

    val settings = settingsViewModel.settings.value

    val windowState = GetWindowState(settings)
    val icon = painterResource(Res.drawable.scales)

    if (isTraySupported) {
        Tray(
            icon = icon,
            tooltip = "HealthCoach",
        )
    }

    Window(
        onCloseRequest = {
            saveWindowState(windowState, settingsViewModel)

            exitApplication()
        },
        title = "HealthCoach",
        icon = icon,
        state = windowState
    ) {
        App()
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