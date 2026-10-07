package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.AppInfo
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.sync.StorageAdapterFactory
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.sync.auth.GoogleOAuthManager
import com.lbthomas.healthcoach.core.ui.rememberDirectoryPicker
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import kotlin.time.Clock

@Composable
internal fun SyncStorageAuthSubTab(
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

    var showGoogleDriveDetectedDialog by remember { mutableStateOf(false) }
    var showGoogleDriveConfirmResetDialog by remember { mutableStateOf(false) }
    var isResettingGoogleDrive by remember { mutableStateOf(false) }

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
                        selected = settings.sync.syncProvider == SyncProviderType.LOCAL_FOLDER,
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
                    selected = settings.sync.syncProvider == SyncProviderType.LOCAL_FOLDER,
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
                        selected = settings.sync.syncProvider == SyncProviderType.GOOGLE_DRIVE,
                        onClick = {
                            settingsViewModel.setSyncProvider(SyncProviderType.GOOGLE_DRIVE)
                            folderValidationResult = null
                            actionFeedback = null
                            if (settings.sync.googleAccessToken.isNotBlank() || settings.sync.googleRefreshToken.isNotBlank()) {
                                coroutineScope.launch {
                                    if (settingsViewModel.checkGoogleDriveExistingFile()) {
                                        showGoogleDriveDetectedDialog = true
                                    }
                                }
                            }
                        },
                        role = Role.RadioButton
                    )
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.sync.syncProvider == SyncProviderType.GOOGLE_DRIVE,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                Text(
                    text = "Google Drive (appDataFolder)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Peer-to-Peer Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = settings.sync.syncProvider == SyncProviderType.PEER_TO_PEER,
                        onClick = {
                            settingsViewModel.setSyncProvider(SyncProviderType.PEER_TO_PEER)
                            folderValidationResult = null
                            actionFeedback = null
                        },
                        role = Role.RadioButton
                    )
                    .padding(vertical = SettingsDialogDefaults.RowPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.sync.syncProvider == SyncProviderType.PEER_TO_PEER,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(SettingsDialogDefaults.LabelStartPadding))
                Text(
                    text = "Peer-to-Peer (Local Wi-Fi Network)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (settings.sync.syncProvider != SyncProviderType.PEER_TO_PEER) {
            SettingsSection(title = "Provider Configuration") {
                when (settings.sync.syncProvider) {
                    SyncProviderType.LOCAL_FOLDER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = settings.sync.localSyncPath,
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
                            enabled = !isValidatingFolder && settings.sync.localSyncPath.isNotBlank()
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
                    val hasToken = settings.sync.googleAccessToken.isNotBlank() || settings.sync.googleRefreshToken.isNotBlank()
                    val tokenStatus = when {
                        settings.sync.googleTokenStatus.isNotBlank() -> settings.sync.googleTokenStatus
                        hasToken -> "Active"
                        else -> "Not Authorized"
                    }
                    val userEmail = if (settings.sync.googleAccountEmail.isNotBlank()) settings.sync.googleAccountEmail else "No account connected"

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
                    val isOk = hasToken && !tokenStatus.contains("Invalid", ignoreCase = true) && !tokenStatus.contains("Failed", ignoreCase = true) && !tokenStatus.contains("Revoked", ignoreCase = true) && !tokenStatus.contains("Not", ignoreCase = true) && !tokenStatus.contains("Error", ignoreCase = true)
                    val isErr = tokenStatus.contains("Invalid", ignoreCase = true) || tokenStatus.contains("Failed", ignoreCase = true) || tokenStatus.contains("Error", ignoreCase = true)

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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = when {
                                        isOk -> MaterialTheme.colorScheme.primaryContainer
                                        isErr -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isOk -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            isErr -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isOk -> Icons.Default.CheckCircle
                                                isErr -> Icons.Default.Error
                                                else -> Icons.Default.Info
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = when {
                                                isOk -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isErr -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = tokenStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            color = when {
                                                isOk -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isErr -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
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
                                    fontWeight = if (settings.sync.googleAccountEmail.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (settings.sync.googleAccessToken.isNotBlank()) {
                                val preview = if (settings.sync.googleAccessToken.length > 20) {
                                    "${settings.sync.googleAccessToken.take(8)}...${settings.sync.googleAccessToken.takeLast(6)}"
                                } else {
                                    settings.sync.googleAccessToken
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
                                    if (settings.sync.googleTokenExpiresAt != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Access Expiration: ",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatTokenExpiration(settings.sync.googleTokenExpiresAt),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (settings.sync.googleRefreshToken.isNotBlank()) {
                                val refreshPreview = if (settings.sync.googleRefreshToken.length > 20) {
                                    "${settings.sync.googleRefreshToken.take(8)}...${settings.sync.googleRefreshToken.takeLast(6)}"
                                } else {
                                    settings.sync.googleRefreshToken
                                }
                                val refreshExp = settings.sync.googleRefreshTokenExpiresAt
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // OAuth Actions: Authorize, Revoke, Validate
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        if (settingsViewModel.checkGoogleDriveExistingFile()) {
                                            showGoogleDriveDetectedDialog = true
                                        }
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: "Authorization failed"
                                        settingsViewModel.setGoogleTokenStatus("Authorization Failed")
                                        actionFeedback = false to err
                                    }
                                    isAuthorizing = false
                                }
                            },
                            enabled = !isAuthorizing && !isValidatingToken && !isRevoking && !isResettingGoogleDrive,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minWidth = 100.dp)
                        ) {
                            if (isAuthorizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Authorizing...", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            } else {
                                Text(if (hasToken) "Re-authorize" else "Authorize", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Revoke Button
                        OutlinedButton(
                            onClick = {
                                isRevoking = true
                                actionFeedback = null
                                coroutineScope.launch {
                                    val tokenToRevoke = settings.sync.googleAccessToken.ifBlank { settings.sync.googleRefreshToken }
                                    GoogleOAuthManager.revokeToken(tokenToRevoke)
                                    settingsViewModel.clearGoogleSession()
                                    actionFeedback = true to "Authorization revoked."
                                    isRevoking = false
                                }
                            },
                            enabled = !isAuthorizing && !isValidatingToken && !isRevoking && !isResettingGoogleDrive && (hasToken || settings.sync.googleAccountEmail.isNotBlank()),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minWidth = 80.dp)
                        ) {
                            if (isRevoking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Revoking...", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            } else {
                                Text("Revoke", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Validate Button
                        Button(
                            onClick = {
                                isValidatingToken = true
                                actionFeedback = null
                                coroutineScope.launch {
                                    val result = GoogleOAuthManager.validateToken(
                                        accessToken = settings.sync.googleAccessToken,
                                        refreshToken = settings.sync.googleRefreshToken
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
                                            if (!valResult.email.isNullOrBlank() && settings.sync.googleAccountEmail.isBlank()) {
                                                settingsViewModel.setGoogleAccountEmail(valResult.email)
                                            }
                                            if (settings.sync.googleRefreshToken.isNotBlank() && settings.sync.googleRefreshTokenExpiresAt == null) {
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
                            enabled = !isAuthorizing && !isValidatingToken && !isRevoking && !isResettingGoogleDrive && settings.sync.googleAccessToken.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minWidth = 80.dp)
                        ) {
                            if (isValidatingToken) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Validating...", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            } else {
                                Text("Validate", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Reset File Button
                        OutlinedButton(
                            onClick = {
                                showGoogleDriveConfirmResetDialog = true
                            },
                            enabled = !isAuthorizing && !isValidatingToken && !isRevoking && !isResettingGoogleDrive && hasToken,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minWidth = 80.dp)
                        ) {
                            if (isResettingGoogleDrive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resetting...", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset File", maxLines = 1, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
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
                SyncProviderType.PEER_TO_PEER -> {}
            }
        }
    } else {
        val uriHandler = LocalUriHandler.current
        val isServerMode = settings.peerSync.isServerMode

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Need HealthCoach on your other device?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Download desktop and mobile companion apps from our download hub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { uriHandler.openUri(AppInfo.WEBSITE_URL) }
                ) {
                    Text("Get Apps")
                }
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            SegmentedButton(
                selected = !isServerMode,
                onClick = { settingsViewModel.setPeerIsServerMode(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Connect as Client", maxLines = 1)
            }
            SegmentedButton(
                selected = isServerMode,
                onClick = { settingsViewModel.setPeerIsServerMode(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Host as Server", maxLines = 1)
            }
        }

        if (!isServerMode) {
            PeerClientSettingsSection(
                settings = settings,
                settingsViewModel = settingsViewModel
            )
        } else {
            PeerServerSettingsSection(
                settings = settings,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

if (showGoogleDriveDetectedDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDriveDetectedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Existing Google Drive Sync File Detected",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "An existing synchronization database file was detected in your Google Drive appDataFolder.\n\nWould you like to reset the remote sync file?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoogleDriveDetectedDialog = false
                        showGoogleDriveConfirmResetDialog = true
                    }
                ) {
                    Text("Yes, Reset File")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGoogleDriveDetectedDialog = false }) {
                    Text("No, Keep Existing File")
                }
            }
        )
    }

    if (showGoogleDriveConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDriveConfirmResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Remote File Reset",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently reset and delete the remote sync file on Google Drive? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoogleDriveConfirmResetDialog = false
                        isResettingGoogleDrive = true
                        coroutineScope.launch {
                            val result = settingsViewModel.resetRemoteDestination(SyncProviderType.GOOGLE_DRIVE)
                            isResettingGoogleDrive = false
                            if (result.isSuccess) {
                                actionFeedback = true to "Google Drive sync file has been reset successfully."
                            } else {
                                val err = result.exceptionOrNull()?.message ?: "Failed to reset Google Drive sync file."
                                actionFeedback = false to err
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Yes, Permanently Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGoogleDriveConfirmResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview
@Composable
private fun SyncStorageAuthSubTabPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settings by settingsViewModel.settings.collectAsState()
            Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SyncStorageAuthSubTab(
                    settings = settings,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
