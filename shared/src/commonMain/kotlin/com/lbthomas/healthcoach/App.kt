package com.lbthomas.healthcoach

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
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
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme
import com.lbthomas.healthcoach.core.ui.HorizontalSplitPane
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.features.bloodpressure.BloodPressureView
import com.lbthomas.healthcoach.features.graphs.GraphsView
import com.lbthomas.healthcoach.features.settings.SettingsDialog
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.sync.SyncActionButton
import com.lbthomas.healthcoach.features.sync.SyncNotificationManager
import com.lbthomas.healthcoach.features.sync.SyncViewModel
import com.lbthomas.healthcoach.features.weight.WeightView
import healthcoach.shared.generated.resources.Res
import healthcoach.shared.generated.resources.scales
import kotlinx.coroutines.yield
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
    val syncViewModel = koinInject<SyncViewModel>()
    val settings by settingsViewModel.settings.collectAsState()
    val selectedPage = settings.selectedPage
    var showSettings by remember { mutableStateOf(false) }
    var showAddWeightEntry by remember { mutableStateOf(false) }
    var showAddBloodPressureEntry by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        SyncNotificationManager.notifications.collect { notification ->
            snackbarHostState.showSnackbar(
                message = notification.message,
                duration = if (notification.isError) SnackbarDuration.Long else SnackbarDuration.Short
            )
        }
    }

    HealthCoachTheme(
        theme = settings.appTheme,
        themeMode = settings.themeMode
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideLayout = settings.adaptiveDisplay && maxWidth >= 1200.dp
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(selectedPage, showSettings, showAddWeightEntry, showAddBloodPressureEntry) {
                yield()
                runCatching {
                    focusRequester.requestFocus()
                }
            }

            LaunchedEffect(isWideLayout, selectedPage) {
                if (isWideLayout && selectedPage == SelectedPage.GraphsView) {
                    settingsViewModel.setSelectedPage(SelectedPage.WeightView)
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                                Key.G -> {
                                    if (isWideLayout) {
                                        settingsViewModel.setSelectedPage(SelectedPage.WeightView)
                                    } else {
                                        settingsViewModel.setSelectedPage(SelectedPage.GraphsView)
                                    }
                                }

                                Key.S, Key.Comma -> showSettings = true
                                Key.R -> syncViewModel.syncNow()
                                Key.N, Key.Plus, Key.NumPadAdd, Key.Equals -> {
                                    if (selectedPage == SelectedPage.WeightView) {
                                        showAddWeightEntry = true
                                    } else if (selectedPage == SelectedPage.BloodPressureView) {
                                        showAddBloodPressureEntry = true
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
                        isWideLayout = isWideLayout,
                        syncViewModel = syncViewModel,
                        onSelection = { settingsViewModel.setSelectedPage(it) },
                        onShowSettings = { showSettings = true }
                    )
                },
                bottomBar = {
                    if (!isWideLayout) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            AppDefaults.Tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedPage == tab.page,
                                    onClick = { settingsViewModel.setSelectedPage(tab.page) },
                                    icon = {
                                        Tooltip("${tab.title} (Ctrl + ${tab.title.first()})") {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!isWideLayout && (selectedPage == SelectedPage.WeightView || selectedPage == SelectedPage.BloodPressureView)) {
                        FloatingActionButton(
                            onClick = {
                                if (selectedPage == SelectedPage.WeightView) {
                                    showAddWeightEntry = true
                                } else {
                                    showAddBloodPressureEntry = true
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Tooltip("Add new entry\n(Ctrl + N or '+')") {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = if (selectedPage == SelectedPage.WeightView) {
                                        "Add new weight"
                                    } else {
                                        "Add new blood pressure"
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                AppContent(
                    modifier = Modifier.padding(innerPadding),
                    selectedPage = selectedPage,
                    isWideLayout = isWideLayout,
                    splitterPosition = settings.splitterPosition,
                    onSplitterPositionChange = { settingsViewModel.setSplitterPosition(it) },
                    showAddWeightEntry = showAddWeightEntry,
                    onAddWeightDismiss = {
                        showAddWeightEntry = false
                        runCatching { focusRequester.requestFocus() }
                    },
                    showAddBloodPressureEntry = showAddBloodPressureEntry,
                    onAddBloodPressureDismiss = {
                        showAddBloodPressureEntry = false
                        runCatching { focusRequester.requestFocus() }
                    },
                    onRequestFocus = {
                        runCatching { focusRequester.requestFocus() }
                    }
                )
            }

            if (showSettings) {
                SettingsDialog(
                    settingsViewModel = settingsViewModel,
                    onDismiss = {
                        showSettings = false
                        runCatching { focusRequester.requestFocus() }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppContent(
    selectedPage: SelectedPage,
    isWideLayout: Boolean,
    splitterPosition: Float = 0.5f,
    onSplitterPositionChange: (Float) -> Unit = {},
    showAddWeightEntry: Boolean,
    onAddWeightDismiss: () -> Unit = {},
    showAddBloodPressureEntry: Boolean,
    onAddBloodPressureDismiss: () -> Unit = {},
    onRequestFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(if (isWideLayout) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .then(if (isWideLayout) Modifier.padding(top = 5.dp) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isWideLayout && (selectedPage == SelectedPage.WeightView || selectedPage == SelectedPage.BloodPressureView)) {
            HorizontalSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialFraction = splitterPosition,
                onFractionChange = onSplitterPositionChange,
                first = {
                    if (selectedPage == SelectedPage.WeightView) {
                        WeightView(
                            showAddWeightEntry = showAddWeightEntry,
                            isWideLayout = true,
                            onAddDismiss = onAddWeightDismiss,
                            onRequestFocus = onRequestFocus
                        )
                    } else {
                        BloodPressureView(
                            showAddBloodPressureEntry = showAddBloodPressureEntry,
                            isWideLayout = true,
                            onAddDismiss = onAddBloodPressureDismiss,
                            onRequestFocus = onRequestFocus
                        )
                    }
                },
                second = {
                    GraphsView()
                }
            )
        } else {
            when (selectedPage) {
                SelectedPage.WeightView -> WeightView(
                    showAddWeightEntry = showAddWeightEntry,
                    isWideLayout = false,
                    onAddDismiss = onAddWeightDismiss,
                    onRequestFocus = onRequestFocus
                )

                SelectedPage.BloodPressureView -> BloodPressureView(
                    showAddBloodPressureEntry = showAddBloodPressureEntry,
                    isWideLayout = false,
                    onAddDismiss = onAddBloodPressureDismiss,
                    onRequestFocus = onRequestFocus
                )

                SelectedPage.GraphsView -> GraphsView()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    selectedPage: SelectedPage,
    isWideLayout: Boolean = false,
    syncViewModel: SyncViewModel? = null,
    onSelection: (SelectedPage) -> Unit,
    onShowSettings: () -> Unit
) {
    TopAppBar(
        title = {
            if (isWideLayout) {
                Icon(
                    painter = painterResource(Res.drawable.scales),
                    contentDescription = "HealthCoach",
                    modifier = Modifier.size(AppDefaults.TitleIconSize),
                    tint = Color.Unspecified
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.scales),
                        contentDescription = "HealthCoach",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppDefaults.Tabs.firstOrNull { it.page == selectedPage }?.title ?: "Health Coach",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        actions = {
            if (isWideLayout) {
                AppActionButtons(onSelection, selectedPage, isWideLayout = true, syncViewModel, onShowSettings)
            } else {
                if (syncViewModel != null) {
                    SyncActionButton(syncViewModel = syncViewModel)
                }
                SettingsButton(onShowSettings)
            }
        }
    )
}

@Composable
private fun AppActionButtons(
    onSelection: (SelectedPage) -> Unit,
    selectedPage: SelectedPage,
    isWideLayout: Boolean = false,
    syncViewModel: SyncViewModel? = null,
    onShowSettings: () -> Unit
) {
    val tabs = if (isWideLayout) {
        AppDefaults.Tabs.filter { it.page != SelectedPage.GraphsView }
    } else {
        AppDefaults.Tabs
    }

    tabs.forEach { tab ->
        AppFeatureButton(
            tabTitle = tab.title,
            onSelection = { onSelection(tab.page) },
            isSelected = selectedPage == tab.page,
            tabIcon = tab.icon
        )
    }

    if (syncViewModel != null) {
        SyncActionButton(syncViewModel = syncViewModel)
    }
    SettingsButton(onShowSettings)
}

@Composable
private fun SettingsButton(onShowSettings: () -> Unit) {
    Tooltip("Settings (Ctrl + S or ,)") {
        IconButton(
            onClick = { onShowSettings() },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
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
            checked = isSelected,
            colors = IconButtonDefaults.iconToggleButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
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
