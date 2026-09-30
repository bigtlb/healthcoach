package com.lbthomas.healthcoach

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.features.settings.SettingsDialog
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.sync.SyncNotificationManager
import com.lbthomas.healthcoach.features.sync.SyncViewModel
import com.lbthomas.healthcoach.ui.navigation.AppBottomBar
import com.lbthomas.healthcoach.ui.navigation.AppContent
import com.lbthomas.healthcoach.ui.navigation.AppTopBar
import kotlinx.coroutines.yield
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    val settingsViewModel = koinInject<SettingsViewModel>()
    val syncViewModel = koinInject<SyncViewModel>()
    val settings by settingsViewModel.settings.collectAsState()
    val selectedPage = settings.ui.selectedPage
    var showSettings by remember { mutableStateOf(false) }
    var showAddWeightEntry by remember { mutableStateOf(false) }
    var showAddBloodPressureEntry by remember { mutableStateOf(false) }
    var showLogFood by remember { mutableStateOf(false) }
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
        theme = settings.appearance.appTheme,
        themeMode = settings.appearance.themeMode
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideLayout = settings.appearance.adaptiveDisplay && maxWidth >= 1200.dp
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(selectedPage, showSettings, showAddWeightEntry, showAddBloodPressureEntry, showLogFood) {
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
                                Key.J, Key.F -> settingsViewModel.setSelectedPage(SelectedPage.JournalView)
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
                                    } else if (selectedPage == SelectedPage.JournalView) {
                                        showLogFood = true
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
                    AppTopBar(
                        selectedPage = selectedPage,
                        isWideLayout = isWideLayout,
                        syncViewModel = syncViewModel,
                        onSelection = { settingsViewModel.setSelectedPage(it) },
                        onShowSettings = { showSettings = true }
                    )
                },
                bottomBar = {
                    if (!isWideLayout) {
                        AppBottomBar(
                            selectedPage = selectedPage,
                            onSelection = { settingsViewModel.setSelectedPage(it) }
                        )
                    }
                },
                floatingActionButton = {
                    if (!isWideLayout && (selectedPage == SelectedPage.WeightView || selectedPage == SelectedPage.BloodPressureView || selectedPage == SelectedPage.JournalView)) {
                        FloatingActionButton(
                            onClick = {
                                when (selectedPage) {
                                    SelectedPage.WeightView -> showAddWeightEntry = true
                                    SelectedPage.BloodPressureView -> showAddBloodPressureEntry = true
                                    SelectedPage.JournalView -> showLogFood = true
                                    SelectedPage.GraphsView -> {}
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Tooltip("Add new entry\n(Ctrl + N or '+')") {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = when (selectedPage) {
                                        SelectedPage.WeightView -> "Add new weight"
                                        SelectedPage.BloodPressureView -> "Add new blood pressure"
                                        SelectedPage.JournalView -> "Log food"
                                        SelectedPage.GraphsView -> "Add new entry"
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
                    splitterPosition = settings.ui.splitterPosition,
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
                    showLogFood = showLogFood,
                    onLogFoodDismiss = {
                        showLogFood = false
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

            val pairingPrompt by SyncNotificationManager.pairingPinPrompt.collectAsState()

            pairingPrompt?.let { prompt ->
                AlertDialog(
                    onDismissRequest = { SyncNotificationManager.dismissPairingPrompt() },
                    title = {
                        Text(
                            text = "Peer Pairing Request",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Device '${prompt.clientName}' wants to pair with this device.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Enter this One-Time PIN on the client device:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = prompt.pin,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { SyncNotificationManager.dismissPairingPrompt() }) {
                            Text("Dismiss")
                        }
                    }
                )
            }
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
        }
    )
}
