package com.lbthomas.healthcoach.features.settings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.features.settings.ui.*
import kotlinx.coroutines.yield
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

internal enum class SettingsTab(val title: String, val icon: ImageVector) {
    APPEARANCE("Theme", Icons.Default.Palette),
    WEIGHT("Weight", Icons.Default.Scale),
    BLOOD_PRESSURE("BP", Icons.Default.Favorite),
    SYNC("Sync", Icons.Default.Sync),
    ABOUT("About", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
    isWide: Boolean = false,
    onDismiss: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var selectedTab by remember { mutableStateOf(SettingsTab.APPEARANCE) }

    LaunchedEffect(Unit) {
        yield()
        focusRequester.requestFocus()
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = (
            if (isWide) {
                Modifier.width(800.dp)
            } else {
                Modifier.fillMaxSize()
            }
        )
            .focusRequester(focusRequester)
            .focusable()
            .testTag("settings_dialog")
            .onDialogKeyEvents(
                onConfirm = onDismiss,
                onDismiss = onDismiss
            ),
    ) {
        Surface(
            shape = if (isWide) AlertDialogDefaults.shape else RectangleShape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = if (isWide) AlertDialogDefaults.TonalElevation else 0.dp,
            modifier = if (isWide) Modifier else Modifier.fillMaxSize()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 600.dp
                val sidebarWidth = if (isCompact) 68.dp else 88.dp
                val contentHorizontalPadding = if (isCompact) 12.dp else 24.dp
                val contentVerticalPadding = if (isCompact) 12.dp else 16.dp
                val dialogMinHeight = if (isCompact) 420.dp else 500.dp
                val dialogMaxHeight = if (isCompact) 580.dp else 680.dp

                Column(
                    modifier = if (isWide) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = dialogMinHeight, max = dialogMaxHeight)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentHorizontalPadding, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Master-Detail Body
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Compact Master Sidebar (navigation rail items with label)
                        Surface(
                            modifier = Modifier
                                .width(sidebarWidth)
                                .fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 2.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SettingsTab.entries.forEach { tab ->
                                    val isSelected = selectedTab == tab
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        },
                                        alwaysShowLabel = true
                                    )
                                }
                            }
                        }

                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Detail Pane
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 600.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                when (selectedTab) {
                                    SettingsTab.APPEARANCE -> AppearanceTabContent(settings, settingsViewModel)
                                    SettingsTab.WEIGHT -> WeightTabContent(settings, settingsViewModel)
                                    SettingsTab.BLOOD_PRESSURE -> BloodPressureTabContent(settings, settingsViewModel)
                                    SettingsTab.SYNC -> SyncTabContent(settings, settingsViewModel)
                                    SettingsTab.ABOUT -> AboutTabContent()
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentHorizontalPadding, vertical = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 800, heightDp = 700)
@Composable
private fun SettingsDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            SettingsDialog(
                settingsViewModel = settingsViewModel,
                isWide = false,
                onDismiss = {}
            )
        }
    )
}

@Preview(widthDp = 1050, heightDp = 650)
@Composable
private fun SettingsDialogWidePreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            SettingsDialog(
                settingsViewModel = settingsViewModel,
                isWide = true,
                onDismiss = {}
            )
        }
    )
}
