package com.lbthomas.healthcoach.features.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.p2p.DiscoveredPeer
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.PeerSyncSettings
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
internal fun PeerClientSettingsSection(
    settings: SettingsData,
    settingsViewModel: SettingsViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val discoveredPeers by settingsViewModel.discoveredPeers.collectAsState()

    var activePairingTarget by remember { mutableStateOf<DiscoveredPeer?>(null) }
    var showManualPairDialog by remember { mutableStateOf(false) }

    var testConnectionResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    // Start discovery while on this screen
    DisposableEffect(Unit) {
        settingsViewModel.startDiscovery()
        onDispose {
            settingsViewModel.stopDiscovery()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsDialogDefaults.SectionSpacing)
    ) {
        // Active Target Server Section
        SettingsSection(title = "Connected Target Server") {
            if (settings.peerSync.serverToken.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Paired",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = settings.peerSync.serverName.ifBlank { "HealthCoach Peer Server" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = "Paired & Ready",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Text(
                            text = "Server Address: ${settings.peerSync.serverHost}:${settings.peerSync.serverPort}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.wrapContentWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isTestingConnection = true
                                    testConnectionResult = null
                                    coroutineScope.launch {
                                        val result = settingsViewModel.testPeerConnection(
                                            host = settings.peerSync.serverHost,
                                            port = settings.peerSync.serverPort,
                                            token = settings.peerSync.serverToken
                                        )
                                        isTestingConnection = false
                                        testConnectionResult = if (result.isSuccess) {
                                            val status = result.getOrNull()
                                            if (status != null && status.isAccessGranted) {
                                                true to "Connection verified. Access granted to '${status.deviceName.ifBlank { "server" }}'."
                                            } else {
                                                false to "Endpoint reachable, but pairing is required (PIN or token invalid/expired)."
                                            }
                                        } else {
                                            false to (result.exceptionOrNull()?.message ?: "Could not connect to peer server.")
                                        }
                                    }
                                },
                                enabled = !isTestingConnection,
                                shape = MaterialTheme.shapes.small.copy(
                                    topEnd = CornerSize(0.dp),
                                    bottomEnd = CornerSize(0.dp)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Testing...", maxLines = 1, softWrap = false)
                                } else {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test", maxLines = 1, softWrap = false)
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    settingsViewModel.disconnectPeerClient()
                                    testConnectionResult = null
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = MaterialTheme.shapes.small.copy(
                                    topStart = CornerSize(0.dp),
                                    bottomStart = CornerSize(0.dp)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disconnect", maxLines = 1, softWrap = false)
                            }
                        }

                        testConnectionResult?.let { (success, message) ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (success) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
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
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "No server paired yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Select a discovered server from the list below, or enter an IP address manually to pair with your PIN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Discovered LAN Servers List Section
        SettingsSection(title = "Discovered LAN Servers") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby HealthCoach Nodes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = {
                        settingsViewModel.refreshDiscovery()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan Network")
                }
            }

            if (discoveredPeers.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Scanning for HealthCoach servers on Wi-Fi...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ensure server mode is enabled on the other device and both devices are connected to the same local network.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    discoveredPeers.forEach { peer ->
                        val isCurrentTarget = settings.peerSync.serverHost == peer.host && settings.peerSync.serverPort == peer.port
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isCurrentTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = peer.name.ifBlank { "HealthCoach Node" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${peer.host}:${peer.port} • ID: ${peer.instanceId.take(8)}...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isCurrentTarget && settings.peerSync.serverToken.isNotBlank()) {
                                    FilledTonalButton(
                                        onClick = { activePairingTarget = peer },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Re-Pair")
                                    }
                                } else {
                                    Button(
                                        onClick = { activePairingTarget = peer },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pair")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Manual Connection Fallback Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showManualPairDialog = true }) {
                    Text("Manual IP Connection...")
                }
            }
        }
    }

    // Modal PIN Pairing Dialog for Discovered Peer
    activePairingTarget?.let { peer ->
        PeerPairingModalDialog(
            host = peer.host,
            port = peer.port,
            serverName = peer.name,
            settingsViewModel = settingsViewModel,
            onDismiss = { activePairingTarget = null },
            onPaired = {
                activePairingTarget = null
                testConnectionResult = true to "Successfully paired with '${peer.name}'"
            }
        )
    }

    // Modal PIN Pairing Dialog for Manual Connection
    if (showManualPairDialog) {
        ManualPeerPairingModalDialog(
            settingsViewModel = settingsViewModel,
            onDismiss = { showManualPairDialog = false },
            onPaired = {
                showManualPairDialog = false
                testConnectionResult = true to "Successfully paired with server."
            }
        )
    }
}

@Composable
private fun PeerPairingModalDialog(
    host: String,
    port: Int,
    serverName: String,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onPaired: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var pinInput by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var isPairing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val pinFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        settingsViewModel.requestPairingPin(host, port)
        yield()
        runCatching {
            pinFocusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Pair with $serverName")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "A one-time 4-digit PIN is displayed on '$serverName'. Enter it below to authorize this device:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.trim() },
                    label = { Text("One-Time Server PIN") },
                    placeholder = { Text("e.g. 1234") },
                    singleLine = true,
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (pinInput.isNotBlank() && !isPairing) {
                                isPairing = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val result = settingsViewModel.pairWithPeer(host, port, pinInput)
                                    isPairing = false
                                    if (result.isSuccess) {
                                        onPaired()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Pairing handshake failed."
                                    }
                                }
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (pinVisible) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(pinFocusRequester)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                settingsViewModel.requestPairingPin(host, port)
                            }
                        }
                    ) {
                        Text("Resend PIN Prompt")
                    }
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinInput.isBlank()) {
                        errorMessage = "Please enter the server PIN."
                        return@Button
                    }
                    isPairing = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = settingsViewModel.pairWithPeer(host, port, pinInput)
                        isPairing = false
                        if (result.isSuccess) {
                            onPaired()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Pairing handshake failed."
                        }
                    }
                },
                enabled = !isPairing && pinInput.isNotBlank()
            ) {
                if (isPairing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pairing...")
                } else {
                    Text("Pair & Authorize")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPairing) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ManualPeerPairingModalDialog(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onPaired: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var hostInput by remember { mutableStateOf("") }
    var portInput by remember { mutableStateOf(SyncConfig.DEFAULT_P2P_PORT.toString()) }
    var pinInput by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var isPairing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hostFocusRequester = remember { FocusRequester() }
    val portFocusRequester = remember { FocusRequester() }
    val pinFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        yield()
        runCatching {
            hostFocusRequester.requestFocus()
        }
    }

    fun submitPairing() {
        val port = portInput.toIntOrNull()
        if (hostInput.isBlank()) {
            errorMessage = "Please enter server IP or hostname."
            return
        }
        if (port == null || port !in 1..65535) {
            errorMessage = "Please enter a valid port number."
            return
        }
        if (pinInput.isBlank()) {
            errorMessage = "Please enter the server PIN."
            return
        }
        isPairing = true
        errorMessage = null
        coroutineScope.launch {
            val result = settingsViewModel.pairWithPeer(hostInput, port, pinInput)
            isPairing = false
            if (result.isSuccess) {
                onPaired()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Pairing handshake failed."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Manual Server Connection")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Enter the IP address, port, and PIN of the HealthCoach sync server.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it.trim() },
                    label = { Text("Server IP / Hostname") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { portFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(hostFocusRequester)
                )

                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it.trim() },
                    label = { Text("Server Port") },
                    placeholder = { Text("8765") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { pinFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(portFocusRequester)
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.trim() },
                    label = { Text("Server PIN") },
                    placeholder = { Text("e.g. 1234") },
                    singleLine = true,
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitPairing() }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (pinVisible) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(pinFocusRequester)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val port = portInput.toIntOrNull() ?: SyncConfig.DEFAULT_P2P_PORT
                            if (hostInput.isNotBlank()) {
                                coroutineScope.launch {
                                    settingsViewModel.requestPairingPin(hostInput, port)
                                }
                            }
                        },
                        enabled = hostInput.isNotBlank()
                    ) {
                        Text("Show PIN on Server")
                    }
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submitPairing() },
                enabled = !isPairing && hostInput.isNotBlank() && pinInput.isNotBlank()
            ) {
                if (isPairing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connecting...")
                } else {
                    Text("Connect & Pair")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPairing) {
                Text("Cancel")
            }
        }
    )
}

@Preview(name="PeerSyncData not present",
    device = "spec:width=250dp,height=780dp,dpi=420")
@Composable
private fun PeerClientSettingsSectionPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settings by settingsViewModel.settings.collectAsState()
            Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                PeerClientSettingsSection(
                    settings = settings,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}

@Preview(
    name="PeerSyncData present",
    device = "spec:width=250dp,height=780dp,dpi=420")
@Composable
private fun PeerClientSettingsSectionWithServerPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settings by settingsViewModel.settings.collectAsState()
            Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                PeerClientSettingsSection(
                    settings = settings.copy(
                        peerSync = PeerSyncSettings(
                            deviceName="TestDevice",
                            serverToken = "TestToken")),
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
