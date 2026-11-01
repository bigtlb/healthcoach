package com.lbthomas.healthcoach.core.sync.p2p

import com.lbthomas.healthcoach.core.logging.LoggingConfig
import com.lbthomas.healthcoach.core.utils.currentEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

actual open class PeerDiscoveryAdvertiser {
    companion object {
        // Re-register the service every 5 minutes to keep the multicast socket alive.
        // This counters sandbox environments (Flatpak, containers) where multicast group
        // membership may be silently dropped after extended idle periods.
        private const val HEARTBEAT_INTERVAL_MS = 5 * 60 * 1000L
    }

    private var jmdns: JmDNS? = null
    private var registeredService: ServiceInfo? = null
    private var _isAdvertising = false
    private var heartbeatThread: Thread? = null

    // Stored advertising parameters for heartbeat re-registration
    private var advertisingInstanceId: String = ""
    private var advertisingDeviceName: String = ""
    private var advertisingPort: Int = 0
    private var advertisingHostAddress: InetAddress? = null

    actual open val isAdvertising: Boolean
        get() = _isAdvertising

    actual open fun startAdvertising(instanceId: String, deviceName: String, port: Int): Result<Unit> {
        return try {
            stopAdvertising()
            val hostAddress = getLocalNonLoopbackAddress() ?: InetAddress.getLocalHost()
            val dns = JmDNS.create(hostAddress, "HealthCoach-${instanceId.take(8)}")
            jmdns = dns

            // Store parameters for heartbeat re-registration
            advertisingInstanceId = instanceId
            advertisingDeviceName = deviceName
            advertisingPort = port
            advertisingHostAddress = hostAddress

            val properties = mapOf(
                "instanceId" to instanceId,
                "deviceName" to deviceName,
                "interfaceVersion" to PeerDiscoveryConstants.INTERFACE_VERSION,
                "port" to port.toString()
            )

            val serviceName = "${PeerDiscoveryConstants.SERVICE_NAME_PREFIX} - ${instanceId.take(8)}"
            val serviceInfo = ServiceInfo.create(
                PeerDiscoveryConstants.SERVICE_TYPE_JMDNS,
                serviceName,
                port,
                0,
                0,
                properties
            )

            dns.registerService(serviceInfo)
            registeredService = serviceInfo
            _isAdvertising = true
            LoggingConfig.discoveryLogger.i("mDNS advertising registered for '$serviceName' on $hostAddress:$port")

            // Start heartbeat thread to keep the multicast socket alive
            startHeartbeat(dns)

            Result.success(Unit)
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Failed to start mDNS advertising: ${e.message}", e)
            _isAdvertising = false
            Result.failure(e)
        }
    }

    /**
     * Background heartbeat that periodically checks the mDNS service health.
     *
     * In sandboxed environments (Flatpak, containers), the multicast UDP socket may become
     * stale after hours of operation — the socket remains open but multicast group membership
     * is silently dropped by the network stack or sandbox. The heartbeat detects this by
     * querying the service registration status, and only re-registers when the service is
     * no longer found, avoiding unnecessary multicast traffic during normal operation.
     *
     * Additionally, the heartbeat detects DHCP IP address changes and restarts advertising
     * on the new address, so the service remains discoverable after lease renewals.
     */
    private fun startHeartbeat(dns: JmDNS) {
        heartbeatThread = Thread {
            while (_isAdvertising) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS)
                    if (!_isAdvertising || jmdns == null) break

                    val currentAddress = getLocalNonLoopbackAddress() ?: InetAddress.getLocalHost()

                    // Check if the IP address has changed (e.g., DHCP lease renewal)
                    val previousAddress = advertisingHostAddress
                    if (previousAddress != null && !currentAddress.hostAddress.equals(previousAddress.hostAddress, ignoreCase = true)) {
                        LoggingConfig.discoveryLogger.i("mDNS heartbeat: IP changed from ${previousAddress.hostAddress} to ${currentAddress.hostAddress}, restarting advertising")
                        stopAdvertising()
                        val result = startAdvertising(advertisingInstanceId, advertisingDeviceName, advertisingPort)
                        if (result.isFailure) {
                            LoggingConfig.discoveryLogger.e("Failed to restart mDNS advertising after IP change: ${result.exceptionOrNull()?.message}")
                            _isAdvertising = false
                            break
                        }
                        continue
                    }

                    // Check-then-fix: verify the service is still registered before re-registering
                    val serviceName = "${PeerDiscoveryConstants.SERVICE_NAME_PREFIX} - ${advertisingInstanceId.take(8)}"
                    val existingService = dns.getServiceInfo(PeerDiscoveryConstants.SERVICE_TYPE_JMDNS, serviceName)

                    if (existingService == null) {
                        // Service is no longer registered — re-register it
                        LoggingConfig.discoveryLogger.d("mDNS heartbeat: service not found, re-registering for '$serviceName' on ${currentAddress.hostAddress}")
                        reRegisterService(dns)
                    }
                    // Service is still registered — no action needed, socket is healthy
                } catch (e: InterruptedException) {
                    // Thread is being interrupted (likely stopAdvertising)
                    break
                } catch (e: Throwable) {
                    LoggingConfig.discoveryLogger.w("mDNS advertising heartbeat check failed: ${e.message}")
                    // Try to recover by restarting advertising
                    if (_isAdvertising) {
                        LoggingConfig.discoveryLogger.i("Attempting to restart mDNS advertising after heartbeat failure")
                        try {
                            stopAdvertising()
                            val result = startAdvertising(advertisingInstanceId, advertisingDeviceName, advertisingPort)
                            if (result.isFailure) {
                                LoggingConfig.discoveryLogger.e("Failed to restart mDNS advertising: ${result.exceptionOrNull()?.message}")
                                _isAdvertising = false
                                break
                            }
                        } catch (e: Throwable) {
                            LoggingConfig.discoveryLogger.e("Fatal error restarting mDNS advertising: ${e.message}")
                            _isAdvertising = false
                            break
                        }
                    }
                }
            }
        }.apply {
            isDaemon = true
            name = "HealthCoach-mDNS-Heartbeat"
            start()
        }
    }

    /**
     * Re-registers the mDNS service with stored advertising parameters.
     */
    private fun reRegisterService(dns: JmDNS) {
        val properties = mapOf(
            "instanceId" to advertisingInstanceId,
            "deviceName" to advertisingDeviceName,
            "interfaceVersion" to PeerDiscoveryConstants.INTERFACE_VERSION,
            "port" to advertisingPort.toString()
        )

        val serviceName = "${PeerDiscoveryConstants.SERVICE_NAME_PREFIX} - ${advertisingInstanceId.take(8)}"
        val serviceInfo = ServiceInfo.create(
            PeerDiscoveryConstants.SERVICE_TYPE_JMDNS,
            serviceName,
            advertisingPort,
            0,
            0,
            properties
        )

        try {
            dns.unregisterService(registeredService ?: serviceInfo)
        } catch (_: Exception) {
            // Service may have already been unregistered; ignore
        }

        dns.registerService(serviceInfo)
        registeredService = serviceInfo
    }

    actual open fun stopAdvertising(): Result<Unit> {
        return try {
            _isAdvertising = false
            heartbeatThread?.interrupt()
            heartbeatThread = null
            registeredService?.let {
                jmdns?.unregisterService(it)
                registeredService = null
            }
            jmdns?.close()
            jmdns = null
            advertisingInstanceId = ""
            advertisingDeviceName = ""
            advertisingPort = 0
            advertisingHostAddress = null
            LoggingConfig.discoveryLogger.i("mDNS advertising stopped")
            Result.success(Unit)
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Error stopping mDNS advertising: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getLocalNonLoopbackAddress(): InetAddress? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (networkInterface in interfaces.asSequence()) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (address in networkInterface.inetAddresses.asSequence()) {
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}

actual open class PeerDiscoveryBrowser {
    private var jmdns: JmDNS? = null
    private var activeListener: ServiceListener? = null
    private var _isBrowsing = false
    private var localInstanceId: String = ""

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    actual open val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    actual open val isBrowsing: Boolean
        get() = _isBrowsing

    actual open fun startBrowsing(localInstanceId: String) {
        if (_isBrowsing) return
        this.localInstanceId = localInstanceId
        try {
            val hostAddress = getLocalNonLoopbackAddress() ?: InetAddress.getLocalHost()
            val dns = JmDNS.create(hostAddress, "HealthCoachBrowser")
            jmdns = dns

            val listener = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    dns.requestServiceInfo(event.type, event.name, 1000)
                }

                override fun serviceRemoved(event: ServiceEvent) {
                    val removedName = event.name
                    _discoveredPeers.update { list ->
                        list.filterNot { peer ->
                            peer.name == removedName ||
                            "${PeerDiscoveryConstants.SERVICE_NAME_PREFIX} - ${peer.instanceId.take(8)}" == removedName ||
                            removedName.contains(peer.instanceId.take(8))
                        }
                    }
                    LoggingConfig.discoveryLogger.d("mDNS service removed: $removedName")
                }

                override fun serviceResolved(event: ServiceEvent) {
                    val info = event.info ?: return
                    handleResolvedService(info)
                }
            }

            activeListener = listener
            dns.addServiceListener(PeerDiscoveryConstants.SERVICE_TYPE_JMDNS, listener)
            _isBrowsing = true
            LoggingConfig.discoveryLogger.i("mDNS service browsing started on $hostAddress")

            Thread {
                try {
                    val existing = dns.list(PeerDiscoveryConstants.SERVICE_TYPE_JMDNS, 2000)
                    for (info in existing) {
                        handleResolvedService(info)
                    }
                } catch (_: Exception) {}
            }.apply {
                isDaemon = true
                name = "HealthCoach-PeerDiscovery-Scanner"
                start()
            }
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Failed to start mDNS browsing: ${e.message}", e)
            _isBrowsing = false
        }
    }

    private fun handleResolvedService(info: ServiceInfo) {
        val peerInstanceId = info.getPropertyString("instanceId") ?: ""

        // Filter out local instance
        if (peerInstanceId.isBlank() || peerInstanceId == this@PeerDiscoveryBrowser.localInstanceId) {
            return
        }

        val name = info.getPropertyString("deviceName")?.takeIf { it.isNotBlank() } ?: info.name
        val port = info.port
        val host = info.inet4Addresses.firstOrNull()?.hostAddress
            ?: info.hostAddresses.firstOrNull()
            ?: ""

        if (host.isNotBlank() && port > 0) {
            val peer = DiscoveredPeer(
                instanceId = peerInstanceId,
                name = name,
                host = host,
                port = port,
                interfaceVersion = info.getPropertyString("interfaceVersion") ?: PeerDiscoveryConstants.INTERFACE_VERSION,
                lastSeenTimestamp = currentEpochMillis()
            )

            _discoveredPeers.update { current ->
                val updated = current.filterNot { it.instanceId == peer.instanceId }.toMutableList()
                updated.add(peer)
                updated.sortedBy { it.name }
            }
            LoggingConfig.discoveryLogger.i("Discovered peer: '$name' ($host:$port, id: $peerInstanceId)")
        }
    }

    actual open fun stopBrowsing() {
        try {
            activeListener?.let {
                jmdns?.removeServiceListener(PeerDiscoveryConstants.SERVICE_TYPE_JMDNS, it)
                activeListener = null
            }
            jmdns?.close()
            jmdns = null
            _isBrowsing = false
            LoggingConfig.discoveryLogger.i("mDNS service browsing stopped")
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Error stopping mDNS browsing: ${e.message}", e)
        }
    }

    actual open fun clearPeers() {
        _discoveredPeers.value = emptyList()
    }

    actual open suspend fun resolvePeer(targetInstanceId: String, timeoutMillis: Long): DiscoveredPeer? {
        if (targetInstanceId.isBlank()) return null

        val existing = _discoveredPeers.value.firstOrNull { it.instanceId == targetInstanceId }
        if (existing != null) return existing

        val wasBrowsing = _isBrowsing
        if (!wasBrowsing) {
            startBrowsing("")
        }

        return try {
            withTimeoutOrNull(timeoutMillis) {
                _discoveredPeers.filter { list ->
                    list.any { it.instanceId == targetInstanceId }
                }.first().firstOrNull { it.instanceId == targetInstanceId }
            }
        } finally {
            if (!wasBrowsing) {
                stopBrowsing()
            }
        }
    }

    private fun getLocalNonLoopbackAddress(): InetAddress? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (networkInterface in interfaces.asSequence()) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (address in networkInterface.inetAddresses.asSequence()) {
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
