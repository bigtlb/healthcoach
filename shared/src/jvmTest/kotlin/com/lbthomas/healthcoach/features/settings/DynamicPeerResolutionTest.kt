package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.sync.adapters.PeerToPeerStorageAdapter
import com.lbthomas.healthcoach.core.sync.auth.ProviderCredentials
import com.lbthomas.healthcoach.core.sync.p2p.DiscoveredPeer
import com.lbthomas.healthcoach.core.sync.p2p.PeerDiscoveryBrowser
import com.lbthomas.healthcoach.features.settings.data.PeerSyncSettings
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlin.test.*

private class FakeTestBrowser : PeerDiscoveryBrowser() {
    val peersFlow = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    override val discoveredPeers: StateFlow<List<DiscoveredPeer>> = peersFlow.asStateFlow()

    var resolveTarget: DiscoveredPeer? = null
    var resolveCallCount = 0

    override suspend fun resolvePeer(targetInstanceId: String, timeoutMillis: Long): DiscoveredPeer? {
        resolveCallCount++
        return resolveTarget?.takeIf { it.instanceId == targetInstanceId }
            ?: discoveredPeers.value.firstOrNull { it.instanceId == targetInstanceId }
    }

    override fun clearPeers() {
        peersFlow.value = emptyList()
    }
}

class DynamicPeerResolutionTest {

    @Test
    fun testSettingsViewModelAutoUpdatesHostWhenMatchingPeerDiscovered() = runBlocking {
        val fakeBrowser = FakeTestBrowser()
        val settingsFlow = MutableStateFlow(
            SettingsData(
                peerSync = PeerSyncSettings(
                    serverInstanceId = "target-server-123",
                    serverHost = "192.168.1.10",
                    serverPort = 8765,
                    serverName = "Old Server Name"
                )
            )
        )

        val viewModel = SettingsViewModel(
            settings = settingsFlow,
            discoveryBrowser = fakeBrowser
        )

        // Initial state
        assertEquals("192.168.1.10", viewModel.settings.value.peerSync.serverHost)

        // Emit new peer announcement for target server at a new IP
        fakeBrowser.peersFlow.update {
            listOf(
                DiscoveredPeer(
                    instanceId = "target-server-123",
                    name = "New Server Name",
                    host = "192.168.1.55",
                    port = 9000,
                    interfaceVersion = "1.0",
                    lastSeenTimestamp = 123456L
                )
            )
        }

        // Settings should be automatically updated
        assertEquals("192.168.1.55", viewModel.settings.value.peerSync.serverHost)
        assertEquals(9000, viewModel.settings.value.peerSync.serverPort)
        assertEquals("New Server Name", viewModel.settings.value.peerSync.serverName)
    }

    @Test
    fun testTestPeerConnectionFallsBackToMDNSResolutionWhenHostUnreachable() = runBlocking {
        val fakeBrowser = FakeTestBrowser()
        fakeBrowser.resolveTarget = DiscoveredPeer(
            instanceId = "server-target-abc",
            name = "Resolved Server",
            host = "127.0.0.1",
            port = 8765,
            interfaceVersion = "1.0",
            lastSeenTimestamp = 99999L
        )

        val settingsFlow = MutableStateFlow(
            SettingsData(
                peerSync = PeerSyncSettings(
                    serverInstanceId = "server-target-abc",
                    serverHost = "192.0.2.1", // Unreachable TEST-NET IP
                    serverPort = 1234,
                    serverToken = "test-token"
                )
            )
        )

        val viewModel = SettingsViewModel(
            settings = settingsFlow,
            discoveryBrowser = fakeBrowser
        )

        // Attempting test connection will fail initial host and trigger mDNS resolve fallback
        val result = viewModel.testPeerConnection(
            host = "192.0.2.1",
            port = 1234,
            token = "test-token",
            targetInstanceId = "server-target-abc"
        )

        // Browser resolve should have been invoked
        assertTrue(fakeBrowser.resolveCallCount > 0, "mDNS resolvePeer should have been invoked on connection failure")
        // Settings should be updated to the resolved host/port
        assertEquals("127.0.0.1", viewModel.settings.value.peerSync.serverHost)
        assertEquals(8765, viewModel.settings.value.peerSync.serverPort)
    }

    @Test
    fun testAdapterDynamicPeerResolutionUpdatesAddressCallback() = runBlocking {
        val fakeBrowser = FakeTestBrowser()
        fakeBrowser.resolveTarget = DiscoveredPeer(
            instanceId = "server-target-xyz",
            name = "Migrated Server",
            host = "127.0.0.1",
            port = 8765,
            interfaceVersion = "1.0",
            lastSeenTimestamp = 11111L
        )

        var updatedHost = ""
        var updatedPort = 0

        val adapter = PeerToPeerStorageAdapter(
            serverHost = "192.0.2.1", // Stale IP
            serverPort = 9999,
            serverInstanceId = "server-target-xyz",
            discoveryBrowser = fakeBrowser,
            onServerAddressUpdated = { host, port ->
                updatedHost = host
                updatedPort = port
            }
        )

        // Testing connection will attempt resolution fallback
        val testResult = adapter.testConnection()
        assertTrue(fakeBrowser.resolveCallCount > 0)
        assertEquals("127.0.0.1", updatedHost)
        assertEquals(8765, updatedPort)
        assertEquals("127.0.0.1", adapter.serverHost)
        assertEquals(8765, adapter.serverPort)
    }
}
