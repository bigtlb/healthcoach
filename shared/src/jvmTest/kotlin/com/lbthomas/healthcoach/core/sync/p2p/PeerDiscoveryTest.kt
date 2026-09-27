package com.lbthomas.healthcoach.core.sync.p2p

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class PeerDiscoveryTest {

    @Test
    fun testPeerDiscoveryConstants() {
        assertEquals("_healthcoach-sync._tcp.local.", PeerDiscoveryConstants.SERVICE_TYPE_JMDNS)
        assertEquals("_healthcoach-sync._tcp", PeerDiscoveryConstants.SERVICE_TYPE_NSD)
        assertEquals("HealthCoach synch", PeerDiscoveryConstants.SERVICE_NAME_PREFIX)
        assertEquals("1.0", PeerDiscoveryConstants.INTERFACE_VERSION)
    }

    @Test
    fun testDiscoveredPeerModel() {
        val peer = DiscoveredPeer(
            instanceId = "peer-uuid-1",
            name = "Desktop Main",
            host = "192.168.1.100",
            port = 8765,
            interfaceVersion = "1.0",
            lastSeenTimestamp = 123456789L
        )

        assertEquals("peer-uuid-1", peer.instanceId)
        assertEquals("Desktop Main", peer.name)
        assertEquals("192.168.1.100", peer.host)
        assertEquals(8765, peer.port)
        assertEquals("1.0", peer.interfaceVersion)
        assertEquals(123456789L, peer.lastSeenTimestamp)
    }

    @Test
    fun testAdvertiserAndBrowserLifecycle_Jvm() {
        val advertiser = PeerDiscoveryAdvertiser()
        val browser = PeerDiscoveryBrowser()

        assertFalse(advertiser.isAdvertising)
        assertFalse(browser.isBrowsing)

        // Starting and stopping browser
        browser.startBrowsing(localInstanceId = "local-instance-id-123")
        // Browser state flow is initialized to empty
        val peers = browser.discoveredPeers.value
        assertNotNull(peers)

        browser.clearPeers()
        assertEquals(0, browser.discoveredPeers.value.size)

        browser.stopBrowsing()
        assertFalse(browser.isBrowsing)

        // Stop advertising when not started should succeed gracefully
        val stopResult = advertiser.stopAdvertising()
        assertTrue(stopResult.isSuccess)
    }
}
