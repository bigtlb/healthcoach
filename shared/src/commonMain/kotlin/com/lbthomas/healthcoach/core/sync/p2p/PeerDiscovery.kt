package com.lbthomas.healthcoach.core.sync.p2p

import kotlinx.coroutines.flow.StateFlow

/**
 * Common service type and constants for local network peer discovery.
 */
object PeerDiscoveryConstants {
    const val SERVICE_TYPE_JMDNS = "_healthcoach-sync._tcp.local."
    const val SERVICE_TYPE_NSD = "_healthcoach-sync._tcp"
    const val SERVICE_NAME_PREFIX = "HealthCoach synch"
    const val INTERFACE_VERSION = "1.0"
}

/**
 * Platform abstraction for advertising local HealthCoach P2P server instances via mDNS / DNS-SD.
 */
expect open class PeerDiscoveryAdvertiser {
    open val isAdvertising: Boolean
    open fun startAdvertising(instanceId: String, deviceName: String, port: Int): Result<Unit>
    open fun stopAdvertising(): Result<Unit>
}

/**
 * Platform abstraction for discovering remote HealthCoach P2P server instances via mDNS / DNS-SD.
 */
expect open class PeerDiscoveryBrowser {
    open val isBrowsing: Boolean
    open val discoveredPeers: StateFlow<List<DiscoveredPeer>>
    open fun startBrowsing(localInstanceId: String)
    open fun stopBrowsing()
    open fun clearPeers()
}
