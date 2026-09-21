package com.lbthomas.healthcoach

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.features.bloodpressure.BloodPressureView
import com.lbthomas.healthcoach.features.graphs.GraphsView
import com.lbthomas.healthcoach.features.settings.SettingsDialog
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.weight.WeightView
import healthcoach.shared.generated.resources.Res
import healthcoach.shared.generated.resources.scales
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

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
fun App() {
    val settingsViewModel = koinInject<SettingsViewModel>()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedTabIndex) {
        focusRequester.requestFocus()
    }

    MaterialTheme {
        Scaffold(
            modifier = Modifier
                .focusRequester(focusRequester)

                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)
                    ) {
                        when (keyEvent.key) {
                            Key.W -> selectedTabIndex = 0
                            Key.B -> selectedTabIndex = 1
                            Key.G -> selectedTabIndex = 2
                            Key.S, Key.Comma -> showSettings = true
                            else -> {}
                        }
                        true
                    } else {
                        false
                    }
                },
            topBar = {
                AppBar(
                    selectedTabIndex,
                    onSelection = { selectedTabIndex = it },
                    onShowSettings = { showSettings = true })
            }
        ) { innerPadding ->
            AppContent(
                modifier = Modifier.padding(innerPadding),
                selectedTabIndex = selectedTabIndex,
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
        when (AppDefaults.Tabs[selectedTabIndex].first) {
            "Weight" -> WeightView()
            "Blood Pressure" -> BloodPressureView()
            "Graphs" -> GraphsView()
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
    Tooltip("Settings (Ctrl + S or ,)") {
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
    Tooltip("$tabTitle (Ctrl + ${tabTitle.first()})") {
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

@Preview
@Composable
fun AppPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            App()
        })
}
