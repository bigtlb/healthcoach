package com.lbthomas.healthcoach.features.profile

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
import com.lbthomas.healthcoach.core.di.previewAppModuleWith
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.features.profile.ui.GenderGuideDialog
import com.lbthomas.healthcoach.features.profile.ui.ProfileGoalsTab
import com.lbthomas.healthcoach.features.profile.ui.ProfilePersonalMetricsTab
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import kotlinx.coroutines.yield
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

enum class ProfileDialogTab(val title: String, val icon: ImageVector) {
    PERSONAL_METRICS("Personal Metrics", Icons.Default.Person),
    GOALS("Calories & Weight Goals", Icons.Default.Flag)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    profileViewModel: ProfileViewModel,
    settingsViewModel: SettingsViewModel,
    isWide: Boolean = false,
    initialTab: ProfileDialogTab = ProfileDialogTab.PERSONAL_METRICS,
    onDismiss: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val weightUnit = settings.weight.unit
    val focusRequester = remember { FocusRequester() }

    var showGenderGuideDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(initialTab) }

    LaunchedEffect(Unit) {
        profileViewModel.loadFromProfile(weightUnit)
        yield()
        focusRequester.requestFocus()
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = (
            if (isWide) {
                Modifier.width(760.dp)
            } else {
                Modifier.fillMaxSize()
            }
        )
            .focusRequester(focusRequester)
            .focusable()
            .testTag("profile_dialog")
            .onDialogKeyEvents(
                onConfirm = {
                    if (profileViewModel.saveProfile(weightUnit)) {
                        onDismiss()
                    }
                },
                onDismiss = onDismiss
            )
    ) {
        Surface(
            shape = if (isWide) AlertDialogDefaults.shape else RectangleShape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = if (isWide) AlertDialogDefaults.TonalElevation else 0.dp,
            modifier = if (isWide) Modifier else Modifier.fillMaxSize()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 600.dp
                val contentHorizontalPadding = if (isCompact) 16.dp else 24.dp
                val contentVerticalPadding = if (isCompact) 12.dp else 16.dp

                Column(
                    modifier = if (isWide) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 450.dp, max = 900.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "User Profile & Goals",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Tab Row
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileDialogTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding)
                            .imePadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (selectedTab) {
                            ProfileDialogTab.PERSONAL_METRICS -> {
                                ProfilePersonalMetricsTab(
                                    profileViewModel = profileViewModel,
                                    weightUnit = weightUnit,
                                    onShowGenderGuide = { showGenderGuideDialog = true }
                                )
                            }
                            ProfileDialogTab.GOALS -> {
                                ProfileGoalsTab(
                                    profileViewModel = profileViewModel,
                                    weightUnit = weightUnit
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Footer Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentHorizontalPadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (profileViewModel.saveProfile(weightUnit)) {
                                    onDismiss()
                                }
                            }
                        ) {
                            Text("Save Profile")
                        }
                    }
                }
            }
        }
    }

    if (showGenderGuideDialog) {
        GenderGuideDialog(onDismiss = { showGenderGuideDialog = false })
    }
}

@Preview(widthDp = 500)
@Composable
private fun ProfileDialogPreview() {
    previewAppModuleWith().let { module ->
        KoinApplication(
            configuration = koinConfiguration(declaration = { modules(module) }),
            content = {
                val profileViewModel: ProfileViewModel = koinInject()
                val settingsViewModel: SettingsViewModel = koinInject()
                ProfileDialog(
                    profileViewModel = profileViewModel,
                    settingsViewModel = settingsViewModel,
                    isWide = false,
                    initialTab = ProfileDialogTab.PERSONAL_METRICS,
                    onDismiss = {}
                )
            }
        )
    }
}

@Preview(widthDp = 500)
@Composable
private fun ProfileDialogGoalsTabPreview() {
    previewAppModuleWith().let { module ->
        KoinApplication(
            configuration = koinConfiguration(declaration = { modules(module) }),
            content = {
                val profileViewModel: ProfileViewModel = koinInject()
                val settingsViewModel: SettingsViewModel = koinInject()
                ProfileDialog(
                    profileViewModel = profileViewModel,
                    settingsViewModel = settingsViewModel,
                    isWide = false,
                    initialTab = ProfileDialogTab.GOALS,
                    onDismiss = {}
                )
            }
        )
    }
}

@Preview
@Composable
private fun ProfileDialogWidePreview() {
    previewAppModuleWith().let { module ->
        KoinApplication(
            configuration = koinConfiguration(declaration = { modules(module) }),
            content = {
                val profileViewModel: ProfileViewModel = koinInject()
                val settingsViewModel: SettingsViewModel = koinInject()
                ProfileDialog(
                    profileViewModel = profileViewModel,
                    settingsViewModel = settingsViewModel,
                    isWide = true,
                    initialTab = ProfileDialogTab.PERSONAL_METRICS,
                    onDismiss = {}
                )
            }
        )
    }
}

@Preview
@Composable
private fun ProfileDialogGoalsTabWidePreview() {
    previewAppModuleWith().let { module ->
        KoinApplication(
            configuration = koinConfiguration(declaration = { modules(module) }),
            content = {
                val profileViewModel: ProfileViewModel = koinInject()
                val settingsViewModel: SettingsViewModel = koinInject()
                ProfileDialog(
                    profileViewModel = profileViewModel,
                    settingsViewModel = settingsViewModel,
                    isWide = true,
                    initialTab = ProfileDialogTab.GOALS,
                    onDismiss = {}
                )
            }
        )
    }
}
