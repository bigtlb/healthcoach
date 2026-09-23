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
import com.lbthomas.healthcoach.core.enums.SelectedPage
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

private data class AppTab(
    val page: SelectedPage,
    val title: String,
    val icon: ImageVector
)

private object AppDefaults {
    val Tabs = listOf(
        AppTab(SelectedPage.WeightView, "Weight", Icons.Default.Scale),
        AppTab(SelectedPage.BloodPressureView, "Blood Pressure", Icons.Default.Favorite),
        AppTab(SelectedPage.GraphsView, "Graphs", Icons.AutoMirrored.Filled.ShowChart)
    )
    val TitleIconSize = 32.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val settingsViewModel = koinInject<SettingsViewModel>()
    val settings by settingsViewModel.settings.collectAsState()
    val selectedPage = settings.selectedPage
    var showSettings by remember { mutableStateOf(false) }
    var showAddWeightEntry by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedPage, showSettings, showAddWeightEntry) {
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
                            Key.W -> settingsViewModel.setSelectedPage(SelectedPage.WeightView)
                            Key.B -> settingsViewModel.setSelectedPage(SelectedPage.BloodPressureView)
                            Key.G -> settingsViewModel.setSelectedPage(SelectedPage.GraphsView)
                            Key.S, Key.Comma -> showSettings = true
                            Key.N, Key.Plus, Key.NumPadAdd, Key.Equals -> {
                                if (selectedPage == SelectedPage.WeightView) {
                                    showAddWeightEntry = true
                                }
                            }
                            else -> {
                                return@onPreviewKeyEvent false
                            }
                        }
                        true
                    } else {
                        false
                    }
                },
            topBar = {
                AppBar(
                    selectedPage = selectedPage,
                    onSelection = { settingsViewModel.setSelectedPage(it) },
                    onShowSettings = { showSettings = true })
            }
        ) { innerPadding ->
            AppContent(
                modifier = Modifier.padding(innerPadding),
                selectedPage = selectedPage,
                showAddWeightEntry = showAddWeightEntry,
                onAddDismiss = { showAddWeightEntry = false }
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
private fun AppContent(
    selectedPage: SelectedPage,
    showAddWeightEntry: Boolean,
    modifier: Modifier = Modifier,
    onAddDismiss: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .fillMaxSize()
            .padding(top = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (selectedPage) {
            SelectedPage.WeightView -> WeightView(
                showAddWeightEntry = showAddWeightEntry,
                onAddDismiss = onAddDismiss
            )
            SelectedPage.BloodPressureView -> BloodPressureView()
            SelectedPage.GraphsView -> GraphsView()
        }
    }
}


@Composable
fun AppBar(
    selectedPage: SelectedPage,
    onSelection: (SelectedPage) -> Unit,
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
            AppActionButtons(onSelection, selectedPage, onShowSettings)
        }
    )


}

@Composable
private fun AppActionButtons(
    onSelection: (SelectedPage) -> Unit,
    selectedPage: SelectedPage,
    onShowSettings: () -> Unit
) {
    AppDefaults.Tabs.forEach { tab ->
        AppFeatureButton(
            tabTitle = tab.title,
            onSelection = { onSelection(tab.page) },
            isSelected = selectedPage == tab.page,
            tabIcon = tab.icon
        )
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
    onSelection: () -> Unit,
    isSelected: Boolean,
    tabIcon: ImageVector
) {
    Tooltip("$tabTitle (Ctrl + ${tabTitle.first()})") {
        IconToggleButton(
            onCheckedChange = { checked -> if (checked) onSelection() },
            checked = isSelected
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
