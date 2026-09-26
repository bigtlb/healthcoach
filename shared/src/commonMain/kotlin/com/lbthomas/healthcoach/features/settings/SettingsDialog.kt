package com.lbthomas.healthcoach.features.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.AppInfo
import com.lbthomas.healthcoach.core.OpenSourceAttribution
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.sync.StorageAdapterFactory
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.sync.auth.GoogleOAuthManager
import com.lbthomas.healthcoach.core.theme.AppTheme
import com.lbthomas.healthcoach.core.ui.onDialogKeyEvents
import com.lbthomas.healthcoach.core.ui.rememberDirectoryPicker
import com.lbthomas.healthcoach.core.utils.formatEpochMillis
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import kotlin.time.Clock

private object SettingsDialogDefaults {
    val DialogPadding = 8.dp
    val SectionSpacing = 16.dp
    val ItemSpacing = 8.dp
    val SectionIndent = 8.dp
    val RowPadding = 4.dp
    val LabelStartPadding = 4.dp
}

private fun formatTokenExpiration(expiresAt: Long?, nowMillis: Long = Clock.System.now().toEpochMilliseconds()): String {
    if (expiresAt == null || expiresAt <= 0) return "Unknown"
    val diffMillis = expiresAt - nowMillis
    val formattedDate = formatEpochMillis(expiresAt)
    return if (diffMillis <= 0) {
        "Expired ($formattedDate)"
    } else {
        val totalSecs = diffMillis / 1000
        val days = totalSecs / 86400
        val hours = (totalSecs % 86400) / 3600
        val mins = (totalSecs % 3600) / 60
        val remaining = when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
        "Expires in $remaining ($formattedDate)"
    }
}

private enum class SettingsTab(val title: String, val icon: ImageVector) {
    APPEARANCE("Theme", Icons.Default.Palette),
    WEIGHT("Weight", Icons.Default.Scale),
    BLOOD_PRESSURE("BP", Icons.Default.Favorite),
    SYNC("Sync", Icons.Default.Sync),
    ABOUT("About", Icons.Default.Info)
}

private enum class SyncSubTab(val title: String) {
    STORAGE_AUTH("Storage & Auth"),
    SCHEDULE("Schedule"),
    DIAGNOSTICS("Diagnostics")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
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
        modifier = Modifier
            .widthIn(min = 600.dp, max = 760.dp)
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusable()
            .testTag("settings_dialog")
            .onDialogKeyEvents(
                onConfirm = onDismiss,
                onDismiss = onDismiss
            ),
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 480.dp, max = 620.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
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
                    // Compact Master Sidebar (icons with short text beneath)
                    Surface(
                        modifier = Modifier
                            .width(88.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                            modifier = Modifier.size(22.dp)
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
                            .padding(horizontal = 24.dp, vertical = 16.dp)
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
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

@Composable
private fun AppearanceTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        ThemePaletteOptions(settings, settingsViewModel)
        ThemeModeOptions(settings, settingsViewModel)
        AdaptiveDisplayOptions(settings, settingsViewModel)
    }
}

@Composable
private fun WeightTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        WeightOptions(settings, settingsViewModel)
    }
}

@Composable
private fun BloodPressureTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        BloodPressureOptions(settings, settingsViewModel)
    }
}

@Composable
private fun SyncTabContent(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    var selectedSubTab by remember { mutableStateOf(SyncSubTab.STORAGE_AUTH) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Synchronization") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Database Sync",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Synchronize records across devices via cloud or local folders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.syncEnabled,
                    onCheckedChange = { settingsViewModel.setSyncEnabled(it) }
                )
            }
        }

        if (settings.syncEnabled) {
            SecondaryTabRow(
                selectedTabIndex = selectedSubTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                SyncSubTab.entries.forEach { subTab ->
                    Tab(
                        selected = selectedSubTab == subTab,
                        onClick = { selectedSubTab = subTab },
                        text = {
                            Text(
                                text = subTab.title,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }

            when (selectedSubTab) {
                SyncSubTab.STORAGE_AUTH -> SyncStorageAuthSubTab(settings, settingsViewModel)
                SyncSubTab.SCHEDULE -> SyncScheduleSubTab(settings, settingsViewModel)
                SyncSubTab.DIAGNOSTICS -> SyncDiagnosticsSubTab(settings)
            }
        }
    }
}

@Composable
private fun SyncStorageAuthSubTab(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var folderValidationResult by remember { mutableStateOf<Result<Unit>?>(null) }
    var isValidatingFolder by remember { mutableStateOf(false) }

    var isAuthorizing by remember { mutableStateOf(false) }
    var isRevoking by remember { mutableStateOf(false) }
    var isValidatingToken by remember { mutableStateOf(false) }
    var actionFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val openDirectoryPicker = rememberDirectoryPicker { selectedPath ->
        settingsViewModel.setLocalSyncPath(selectedPath)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Storage Provider") {
            // Local Folder Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = settings.syncProvider == SyncProviderType.LOCAL_FOLDER,
                        onClick = {
                            settingsViewModel.setSyncProvider(SyncProviderType.LOCAL_FOLDER)
                            folderValidationResult = null
                            actionFeedback = null
                        },
                        role = Role.RadioButton
                    )
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.syncProvider == SyncProviderType.LOCAL_FOLDER,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                Text(
                    text = "Local Folder / Mounted Drive",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Google Drive Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = settings.syncProvider == SyncProviderType.GOOGLE_DRIVE,
                        onClick = {
                            settingsViewModel.setSyncProvider(SyncProviderType.GOOGLE_DRIVE)
                            folderValidationResult = null
                            actionFeedback = null
                        },
                        role = Role.RadioButton
                    )
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.syncProvider == SyncProviderType.GOOGLE_DRIVE,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                Text(
                    text = "Google Drive (appDataFolder)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        SettingsSection(title = "Provider Configuration") {
            when (settings.syncProvider) {
                SyncProviderType.LOCAL_FOLDER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = settings.localSyncPath,
                            onValueChange = { settingsViewModel.setLocalSyncPath(it) },
                            label = { Text("Local Folder Path") },
                            placeholder = { Text("/path/to/folder") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { openDirectoryPicker() }) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Select Folder"
                                    )
                                }
                            }
                        )
                        OutlinedButton(
                            onClick = { openDirectoryPicker() },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Browse…")
                        }
                    }
                    Text(
                        text = "Database is stored in /com.lbthomas.healthcoach inside this folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                isValidatingFolder = true
                                folderValidationResult = null
                                coroutineScope.launch {
                                    val adapter = StorageAdapterFactory.createAdapter(settings.toSyncConfig())
                                    folderValidationResult = adapter.testConnection()
                                    isValidatingFolder = false
                                }
                            },
                            enabled = !isValidatingFolder && settings.localSyncPath.isNotBlank()
                        ) {
                            if (isValidatingFolder) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Validating...")
                            } else {
                                Text("Validate Folder")
                            }
                        }

                        folderValidationResult?.let { res ->
                            if (res.isSuccess) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Folder Validated",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = res.exceptionOrNull()?.message ?: "Validation failed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                SyncProviderType.GOOGLE_DRIVE -> {
                    val hasToken = settings.googleAccessToken.isNotBlank() || settings.googleRefreshToken.isNotBlank()
                    val tokenStatus = when {
                        settings.googleTokenStatus.isNotBlank() -> settings.googleTokenStatus
                        hasToken -> "Active"
                        else -> "Not Authorized"
                    }
                    val userEmail = if (settings.googleAccountEmail.isNotBlank()) settings.googleAccountEmail else "No account connected"

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info Notice",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HealthCoach connects to Google Drive using a private sandboxed App Data folder (appDataFolder). The application only accesses its own database files and cannot see or modify your personal files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Token Status & User Information Panel
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (hasToken && !tokenStatus.contains("Revoked", ignoreCase = true) && !tokenStatus.contains("Failed", ignoreCase = true) && !tokenStatus.contains("Invalid", ignoreCase = true)) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (hasToken && !tokenStatus.contains("Revoked", ignoreCase = true) && !tokenStatus.contains("Failed", ignoreCase = true) && !tokenStatus.contains("Invalid", ignoreCase = true)) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OAuth Tokens & Session",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = tokenStatus,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    icon = {
                                        val isOk = hasToken && !tokenStatus.contains("Invalid", ignoreCase = true) && !tokenStatus.contains("Failed", ignoreCase = true) && !tokenStatus.contains("Revoked", ignoreCase = true) && !tokenStatus.contains("Not", ignoreCase = true) && !tokenStatus.contains("Error", ignoreCase = true)
                                        val isErr = tokenStatus.contains("Invalid", ignoreCase = true) || tokenStatus.contains("Failed", ignoreCase = true) || tokenStatus.contains("Error", ignoreCase = true)
                                        Icon(
                                            imageVector = when {
                                                isOk -> Icons.Default.CheckCircle
                                                isErr -> Icons.Default.Error
                                                else -> Icons.Default.Info
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = when {
                                                isOk -> MaterialTheme.colorScheme.primary
                                                isErr -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "User / Account: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (settings.googleAccountEmail.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (settings.googleAccessToken.isNotBlank()) {
                                val preview = if (settings.googleAccessToken.length > 20) {
                                    "${settings.googleAccessToken.take(8)}...${settings.googleAccessToken.takeLast(6)}"
                                } else {
                                    settings.googleAccessToken
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Access Token: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = preview,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (settings.googleTokenExpiresAt != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Access Expiration: ",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatTokenExpiration(settings.googleTokenExpiresAt),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (settings.googleRefreshToken.isNotBlank()) {
                                val refreshPreview = if (settings.googleRefreshToken.length > 20) {
                                    "${settings.googleRefreshToken.take(8)}...${settings.googleRefreshToken.takeLast(6)}"
                                } else {
                                    settings.googleRefreshToken
                                }
                                val refreshExp = settings.googleRefreshTokenExpiresAt
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Refresh Token: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = refreshPreview,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Refresh Expiration: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (refreshExp != null) {
                                                "${formatTokenExpiration(refreshExp)} (Testing App 7-day TTL)"
                                            } else {
                                                "7-day TTL in Testing Mode (Active)"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }

                            if (hasToken) {
                                Text(
                                    text = "ℹ️ Note: Refresh tokens issued while Google Cloud OAuth status is in 'Testing' mode expire after 7 days. Switching to 'In Production' removes this 7-day expiration.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Action Buttons: Authorize, Revoke, Validate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Authorize Button
                        Button(
                            onClick = {
                                isAuthorizing = true
                                actionFeedback = null
                                coroutineScope.launch {
                                    val result = GoogleOAuthManager.authorize()
                                    if (result.isSuccess) {
                                        val session = result.getOrThrow()
                                        val now = Clock.System.now().toEpochMilliseconds()
                                        val expMins = session.expiresInSeconds?.let { " (expires in ${it / 60}m)" } ?: ""
                                        val accessExpiresAt = session.expiresInSeconds?.let { now + it * 1000 }
                                        val refreshExpiresAt = if (session.refreshToken.isNotBlank()) {
                                            now + (7L * 24 * 60 * 60 * 1000)
                                        } else null
                                        settingsViewModel.setGoogleSession(
                                            email = session.email,
                                            accessToken = session.accessToken,
                                            refreshToken = session.refreshToken,
                                            tokenStatus = "Active$expMins",
                                            expiresAt = accessExpiresAt,
                                            refreshTokenExpiresAt = refreshExpiresAt
                                        )
                                        actionFeedback = true to "Successfully authorized ${session.email.ifBlank { "Google account" }}!"
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: "Authorization failed"
                                        settingsViewModel.setGoogleTokenStatus("Authorization Failed")
                                        actionFeedback = false to err
                                    }
                                    isAuthorizing = false
                                }
                            },
                            enabled = !isAuthorizing && !isValidatingToken && !isRevoking,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isAuthorizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Authorizing...")
                            } else {
                                Text(if (hasToken) "Re-authorize" else "Authorize")
                            }
                        }

                        // Revoke Button
                        OutlinedButton(
                            onClick = {
                                isRevoking = true
                                actionFeedback = null
                                coroutineScope.launch {
                                    val tokenToRevoke = settings.googleAccessToken.ifBlank { settings.googleRefreshToken }
                                    GoogleOAuthManager.revokeToken(tokenToRevoke)
                                    settingsViewModel.clearGoogleSession()
                                    actionFeedback = true to "Authorization revoked."
                                    isRevoking = false
                                }
                            },
                            enabled = !isAuthorizing && !isValidatingToken && !isRevoking && (hasToken || settings.googleAccountEmail.isNotBlank()),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isRevoking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Revoking...")
                            } else {
                                Text("Revoke")
                            }
                        }

                        // Validate Button
                        Button(
                            onClick = {
                                isValidatingToken = true
                                actionFeedback = null
                                coroutineScope.launch {
                                    val result = GoogleOAuthManager.validateToken(
                                        accessToken = settings.googleAccessToken,
                                        refreshToken = settings.googleRefreshToken
                                    )
                                    if (result.isSuccess) {
                                        val valResult = result.getOrThrow()
                                        val now = Clock.System.now().toEpochMilliseconds()
                                        if (valResult.isValid) {
                                            if (!valResult.newAccessToken.isNullOrBlank()) {
                                                settingsViewModel.setGoogleAccessToken(valResult.newAccessToken)
                                            }
                                            if (valResult.expiresInSeconds != null) {
                                                settingsViewModel.setGoogleTokenExpiresAt(now + valResult.expiresInSeconds * 1000)
                                            }
                                            if (!valResult.email.isNullOrBlank() && settings.googleAccountEmail.isBlank()) {
                                                settingsViewModel.setGoogleAccountEmail(valResult.email)
                                            }
                                            if (settings.googleRefreshToken.isNotBlank() && settings.googleRefreshTokenExpiresAt == null) {
                                                settingsViewModel.setGoogleRefreshTokenExpiresAt(now + (7L * 24 * 60 * 60 * 1000))
                                            }
                                            val expStr = valResult.expiresInSeconds?.let { " (expires in ${it / 60}m)" } ?: ""
                                            settingsViewModel.setGoogleTokenStatus("Valid$expStr")
                                            actionFeedback = true to "Token is valid${valResult.email?.let { " for $it" } ?: ""}."
                                        } else {
                                            val err = valResult.errorMessage ?: "Access token is invalid or expired"
                                            settingsViewModel.setGoogleTokenStatus("Invalid / Expired")
                                            actionFeedback = false to err
                                        }
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: "Validation failed"
                                        settingsViewModel.setGoogleTokenStatus("Validation Error")
                                        actionFeedback = false to err
                                    }
                                    isValidatingToken = false
                                }
                            },
                            enabled = !isAuthorizing && !isValidatingToken && !isRevoking && settings.googleAccessToken.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isValidatingToken) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Validating...")
                            } else {
                                Text("Validate")
                            }
                        }
                    }

                    // Action feedback banner
                    actionFeedback?.let { (success, message) ->
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (success) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                SyncProviderType.SMB -> {
                    Text(
                        text = "SMB Storage is deferred to post Phase 1.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncScheduleSubTab(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Background Sync") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = settings.autoSyncOnClose,
                    onCheckedChange = { settingsViewModel.setAutoSyncOnClose(it) }
                )
                Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                Text(
                    text = "Auto-sync on application close/exit",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Periodic Auto-Sync",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            val intervals = listOf(
                0 to "Disabled (Manual Only)",
                15 to "Every 15 minutes",
                30 to "Every 30 minutes",
                60 to "Every 60 minutes"
            )

            intervals.forEach { (mins, label) ->
                val isSelected = settings.autoSyncIntervalMinutes == mins
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = { settingsViewModel.setAutoSyncIntervalMinutes(mins) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SyncDiagnosticsSubTab(
    settings: SettingsData
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Audit & Diagnostics") {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Configured Provider: ${when (settings.syncProvider) { SyncProviderType.GOOGLE_DRIVE -> "Google Drive (appDataFolder)"; SyncProviderType.SMB -> "SMB Network Share"; else -> "Local Folder (/com.lbthomas.healthcoach)" }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Remote File: ${settings.remoteFileName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Last Synced: ${settings.lastSyncTime?.let { formatEpochMillis(it) } ?: "Never"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Remote Snapshot Hash: ${settings.lastSyncHash ?: "None"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Last Sync Status: ${settings.lastSyncStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (settings.lastSyncFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Database Schema: Version 3 (PRAGMA user_version = 3)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (settings.lastSyncFailed || !settings.lastSyncError.isNullOrBlank()) {
            SettingsSection(title = "Error Diagnostics") {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Sync Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Detailed Error Report",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Failure Category: ${settings.lastSyncStatus}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Text(
                            text = "Error Message:\n${settings.lastSyncError ?: "Unknown error occurred during sync."}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Troubleshooting & Log Advice:\n• Check provider directory access and network connection.\n• Inspect application logs for the full stack trace:\n  - Desktop: healthcoach.log (in settings directory)\n  - Android: LogCat (tag: HealthCoach)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        SettingsSection(title = "Application Logging") {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• Desktop (JVM): Logs to console and rolling file `healthcoach.log` in app data directory.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Android: Logs to Android system LogCat (tag: HealthCoach / Kermit).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePaletteOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Color Theme") {
        AppTheme.entries.forEach { theme ->
            val isSelected = settings.appTheme == theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        onClick = { settingsViewModel.setAppTheme(theme) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(color = theme.previewPrimary, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ThemeModeOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Theme Mode") {
        ThemeMode.entries.forEach { mode ->
            val isSelected = settings.themeMode == mode
            SettingRadioRow(
                text = mode.displayName,
                selected = isSelected,
                onClick = { settingsViewModel.setThemeMode(mode) }
            )
        }
    }
}

@Composable
private fun AdaptiveDisplayOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Display Options") {
        SettingCheckboxRow(
            text = "Adaptive Display",
            checked = settings.adaptiveDisplay,
            onCheckedChange = { checked ->
                settingsViewModel.setAdaptiveDisplay(checked)
            }
        )
    }
}

@Composable
private fun BloodPressureOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Blood Pressure Display Options") {
        SettingCheckboxRow(
            text = "Include Pulse in Graph",
            checked = settings.showPulseInGraph,
            onCheckedChange = { checked ->
                settingsViewModel.setShowPulseInGraph(checked)
            }
        )
        SettingCheckboxRow(
            text = "Show Daily Averages",
            checked = settings.showDailyAverages,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(showDailyAverages = checked) }
            }
        )
        SettingCheckboxRow(
            text = "Show Monthly Averages",
            checked = settings.showMonthlyAverages,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(showMonthlyAverages = checked) }
            }
        )
        SettingCheckboxRow(
            text = "Show Daily Changes",
            checked = settings.showDailyChanges,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(showDailyChanges = checked) }
            }
        )
        SettingCheckboxRow(
            text = "Show Monthly Changes",
            checked = settings.showMonthlyChanges,
            onCheckedChange = { checked ->
                settingsViewModel.updateSettings { it.copy(showMonthlyChanges = checked) }
            }
        )
    }
}

@Composable
private fun WeightOptions(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    SettingsSection(title = "Weight Units") {
        WeightUnit.entries.forEach { unit ->
            val isSelected = settings.weightUnit == unit
            val label = when (unit) {
                WeightUnit.US -> "Pounds (lbs)"
                WeightUnit.METRIC -> "Kilograms (kg)"
            }
            SettingRadioRow(
                text = label,
                selected = isSelected,
                onClick = { settingsViewModel.setWeightUnit(unit) }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.ItemSpacing)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SettingsDialogDefaults.SectionIndent),
            verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.ItemSpacing),
            content = content
        )
    }
}

@Composable
private fun SettingRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = SettingsDialogDefaults.RowPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SettingCheckboxRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = checked,
                onClick = { onCheckedChange(!checked) },
                role = Role.Checkbox
            )
            .padding(vertical = SettingsDialogDefaults.RowPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AboutTabContent() {
    val uriHandler = LocalUriHandler.current
    var showLicenseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        SettingsSection(title = "Health Coach") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${AppInfo.APP_NAME} (v${AppInfo.APP_VERSION})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        uriHandler.openUri(AppInfo.GITHUB_URL)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Author: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = AppInfo.AUTHOR,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "License: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = AppInfo.LICENSE_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        showLicenseDialog = true
                    }
                )
            }
        }

        Text(
            text = "Open Source Licenses & Attributions",
            style = MaterialTheme.typography.titleMedium
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppInfo.attributions) { attribution ->
                    AttributionItem(
                        attribution = attribution,
                        onOpenUrl = { url -> uriHandler.openUri(url) }
                    )
                }
            }
        }
    }

    if (showLicenseDialog) {
        AppLicenseDialog(onDismiss = { showLicenseDialog = false })
    }
}

@Composable
private fun AttributionItem(
    attribution: OpenSourceAttribution,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = attribution.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = attribution.licenseName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable { onOpenUrl(attribution.licenseUrl) }
                    )
                }
            }

            if (attribution.version != null) {
                Text(
                    text = "Version: ${attribution.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = attribution.copyright,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = attribution.projectUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onOpenUrl(attribution.projectUrl) }
            )
        }
    }
}

@Composable
private fun AppLicenseDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppInfo.LICENSE_NAME) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
            ) {
                Text(
                    text = "Health Coach is open-source software licensed under the ${AppInfo.LICENSE_NAME}.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = AppInfo.APACHE_LICENSE_TEXT,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(onClick = { uriHandler.openUri(AppInfo.LICENSE_URL) }) {
                Text("Open in Browser")
            }
        }
    )
}

@Preview
@Composable
fun SettingsDialogPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            SettingsDialog(
                settingsViewModel = koinInject(),
                onDismiss = {}
            )
        }
    )
}
