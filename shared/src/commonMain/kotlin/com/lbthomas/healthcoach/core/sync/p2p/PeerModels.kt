package com.lbthomas.healthcoach.core.sync.p2p

import kotlinx.serialization.Serializable

object PeerAuthStatus {
    const val ACCESS_GRANTED = "ACCESS_GRANTED"
    const val PAIRING_REQUIRED = "PAIRING_REQUIRED"
}

/**
 * Health & status response returned by P2P sync server.
 */
@Serializable
data class PeerStatusResponse(
    val status: String = "OK",
    val instanceId: String = "",
    val deviceName: String = "",
    val port: String = "",
    val interfaceVersion: String = "1.0",
    val isRunning: String = "true",
    val authStatus: String = PeerAuthStatus.PAIRING_REQUIRED
) {
    val isAccessGranted: Boolean
        get() = authStatus.equals(PeerAuthStatus.ACCESS_GRANTED, ignoreCase = true) ||
            authStatus.equals("access granted", ignoreCase = true)

    val isPairingRequired: Boolean
        get() = authStatus.equals(PeerAuthStatus.PAIRING_REQUIRED, ignoreCase = true) ||
            authStatus.equals("pairing required", ignoreCase = true)
}

/**
 * Information about a peer discovered on the local network via mDNS/NSD.
 */
@Serializable
data class DiscoveredPeer(
    val instanceId: String,
    val name: String,
    val host: String,
    val port: Int,
    val interfaceVersion: String = "1.0",
    val lastSeenTimestamp: Long = 0L
)

/**
 * Payload sent by client to request server to generate and display a one-time pairing PIN.
 */
@Serializable
data class PairInitRequest(
    val clientInstanceId: String,
    val clientName: String
)

/**
 * Payload sent by client to initiate pairing with server.
 */
@Serializable
data class PairRequest(
    val clientInstanceId: String,
    val clientName: String,
    val pin: String
)

/**
 * Payload returned by server on successful pairing.
 */
@Serializable
data class PairResponse(
    val token: String,
    val serverInstanceId: String,
    val serverName: String
)

/**
 * Metadata for remote database state exposed by P2P sync server.
 */
@Serializable
data class ServerSyncMetadata(
    val exists: Boolean,
    val sha256Hash: String = "",
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L
)

/**
 * Record of a connected/serviced client stored in server history.
 */
@Serializable
data class PeerClientRecord(
    val authToken: String = "",
    val clientInstanceId: String,
    val clientName: String,
    val ipAddress: String = "",
    val lastAccessTimestamp: Long,
    val lastAction: String = "CONNECTED"
)

/**
 * Active status information for the local embedded P2P server.
 */
@Serializable
data class PeerServerStatus(
    val isRunning: Boolean = false,
    val host: String = "",
    val port: Int = 8765,
    val activePin: String = "",
    val errorMessage: String? = null
)
