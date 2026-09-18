package com.lbthomas.healthcoach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.appModule
import com.lbthomas.healthcoach.core.di.configurePlatformContext
import com.lbthomas.healthcoach.core.getPlatformContext
import com.lbthomas.healthcoach.features.settings.SettingsDialog
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import healthcoach.shared.generated.resources.Res
import healthcoach.shared.generated.resources.scales
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

private object AppDefaults {
    val Tabs = listOf(
        "Weight" to Icons.Default.Scale,
        "Blood Pressure" to Icons.Default.Favorite,
        "Graphs" to Icons.AutoMirrored.Filled.ShowChart
    )
    val TitleIconSize = 32.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val platformContext = getPlatformContext()
    // Initialize Koin for previews if not already started
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            configurePlatformContext(platformContext)
            modules(appModule)
        }
    }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val settingsViewModel: SettingsViewModel = koinInject()

    MaterialTheme {
        Scaffold(
            topBar = {
                AppBar(
                    selectedTabIndex,
                    onSelection = { selectedTabIndex = it },
                    onShowSettings = { showSettings = true })
            }
        ) { innerPadding ->
            AppContent(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.padding(innerPadding)
            )
        }

        if (showSettings) {
            SettingsDialog(
                settingsViewModel = settingsViewModel,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun AppContent(selectedTabIndex: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .fillMaxSize()
            .padding(top = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (selectedTabIndex) {
            0 -> Text(AppDefaults.Tabs[0].first)
            1 -> Text(AppDefaults.Tabs[1].first)
            2 -> Text(AppDefaults.Tabs[2].first)
        }
    }
}


@Composable
fun AppBar(
    selectedTabIndex: Int,
    onSelection: (Int) -> Unit,
    onShowSettings: () -> Unit
) {

    TopAppBar(
        title = {
            Icon(
                painter = painterResource(Res.drawable.scales),
                contentDescription = "HealthCoach",
                modifier = Modifier.size(AppDefaults.TitleIconSize),
                tint = Color.Unspecified
            )
        },
        colors = TopAppBarDefaults.topAppBarColors()
            .copy(MaterialTheme.colorScheme.primaryFixedDim),
        actions = {
            AppActionButtons(onSelection, selectedTabIndex, onShowSettings)
        }
    )


}

@Composable
private fun AppActionButtons(
    onSelection: (Int) -> Unit,
    selectedTabIndex: Int,
    onShowSettings: () -> Unit
) {
    AppDefaults.Tabs.forEachIndexed { index, pair ->
        val tabTitle = pair.first
        val tabIcon = pair.second

        AppFeatureButton(tabTitle, onSelection, index, selectedTabIndex, tabIcon)
    }

    SettingsButton(onShowSettings)
}

@Composable
private fun SettingsButton(onShowSettings: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip {
                Text("Settings")
            }
        },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = { onShowSettings() }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
private fun AppFeatureButton(
    tabTitle: String,
    onSelection: (Int) -> Unit,
    index: Int,
    selectedTabIndex: Int,
    tabIcon: ImageVector
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip {
                Text(tabTitle)
            }
        },
        state = rememberTooltipState()
    ) {
        IconToggleButton(
            onCheckedChange = { checked -> if (checked) onSelection(index) },
            checked = selectedTabIndex == index
        ) {
            Icon(
                imageVector = tabIcon,
                contentDescription = tabTitle
            )
        }
    }
}