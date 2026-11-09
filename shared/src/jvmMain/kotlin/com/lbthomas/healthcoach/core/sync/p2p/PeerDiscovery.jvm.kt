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
import java.io.File
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

        /**
         * Detects if we're running on a Linux environment where avahi-publish is available.
         * On Linux, we prefer avahi-publish because it registers services via D-Bus through
         * the Avahi daemon, bypassing multicast socket issues caused by systemd-resolved
         * conflicts or Flatpak sandbox restrictions.
         */
        private fun isLinuxWithAvahiPublish(): Boolean {
            val osName = System.getProperty("os.name", "").lowercase()
            if (!osName.contains("linux")) {
                LoggingConfig.discoveryLogger.d("mDNS advertising: not Linux (os.name=$osName), skipping avahi-publish detection")
                return false
            }

            // Check common locations for avahi-publish binary:
            // - /usr/bin/avahi-publish (standard Linux install)
            // - /bin/avahi-publish (some distributions)
            // - /app/bin/avahi-publish (Flatpak sandbox, bundled from SDK)
            val paths = listOf("/usr/bin/avahi-publish", "/bin/avahi-publish", "/app/bin/avahi-publish")
            for (path in paths) {
                if (File(path).exists()) {
                    LoggingConfig.discoveryLogger.d("mDNS advertising: avahi-publish found at $path, will use D-Bus registration")
                    return true
                }
            }
            LoggingConfig.discoveryLogger.d("mDNS advertising: avahi-publish not found, falling back to JmDNS")
            return false
        }

    }

    private var jmdns: JmDNS? = null
    private var registeredService: ServiceInfo? = null
    private var _isAdvertising = false
    private var heartbeatThread: Thread? = null
    private var avahiPublishProcess: Process? = null

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

            // Store parameters for heartbeat re-registration
            advertisingInstanceId = instanceId
            advertisingDeviceName = deviceName
            advertisingPort = port

            val serviceName = "${PeerDiscoveryConstants.SERVICE_NAME_PREFIX} - ${instanceId.take(8)}"

            // On Linux with avahi-publish, use D-Bus registration instead of raw multicast.
            // This bypasses systemd-resolved conflicts and Flatpak sandbox multicast restrictions.
            if (isLinuxWithAvahiPublish()) {
                val hostAddress = getLocalNonLoopbackAddress() ?: InetAddress.getLocalHost()
                advertisingHostAddress = hostAddress
                return startAvahiPublishAdvertising(serviceName, instanceId, deviceName, port, hostAddress)
            } else {
                // Non-Linux: use JmDNS with raw multicast
                val hostAddress = getLocalNonLoopbackAddress() ?: InetAddress.getLocalHost()
                advertisingHostAddress = hostAddress
                val dns = JmDNS.create(hostAddress, "HealthCoach-${instanceId.take(8)}")
                jmdns = dns

                val properties = mapOf(
                    "instanceId" to instanceId,
                    "deviceName" to deviceName,
                    "interfaceVersion" to PeerDiscoveryConstants.INTERFACE_VERSION,
                    "port" to port.toString()
                )

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
            }
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Failed to start mDNS advertising: ${e.message}", e)
            _isAdvertising = false
            Result.failure(e)
        }
    }

    /**
     * Starts mDNS advertising using avahi-publish via D-Bus on Linux.
     *
     * This approach registers the service through the Avahi daemon, which handles all
     * multicast traffic. It bypasses the common issues with raw multicast UDP:
     * - systemd-resolved conflicts on port 5353
     * - Flatpak sandbox silently dropping multicast packets
     * - Network interface changes and stale socket membership
     *
     * The avahi-publish process runs in the background until stopped.
     */
    private fun startAvahiPublishAdvertising(
        serviceName: String,
        instanceId: String,
        deviceName: String,
        port: Int,
        hostAddress: InetAddress
    ): Result<Unit> {
        return try {
            // avahi-publish -s <name> <type> <port> [<txt ...>]
            // The service type should not include ".local." suffix for avahi-publish
            val serviceType = PeerDiscoveryConstants.SERVICE_TYPE_JMDNS.removeSuffix(".local.")

            val txtProps = listOf(
                "instanceId=$instanceId",
                "deviceName=$deviceName",
                "interfaceVersion=${PeerDiscoveryConstants.INTERFACE_VERSION}",
                "port=$port"
            )

            val command = listOf(
                "avahi-publish",
                "-s",
                serviceName,
                serviceType,
                port.toString(),
            ) + txtProps

            val process = ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.PIPE).redirectError(ProcessBuilder.Redirect.PIPE).start()
            avahiPublishProcess = process
            _isAdvertising = true
            LoggingConfig.discoveryLogger.i("mDNS advertising registered via avahi-publish (D-Bus) for '$serviceName' on $hostAddress:$port")

            // Monitor the process in a background thread
            Thread {
                try {
                    val exitCode = process.waitFor()
                    if (_isAdvertising) {
                        LoggingConfig.discoveryLogger.w("avahi-publish process exited with code $exitCode, restarting after 1s delay")
                        Thread.sleep(1000)
                        if (_isAdvertising) {
                            val result = startAvahiPublishAdvertising(serviceName, instanceId, deviceName, port, hostAddress)
                            if (result.isFailure) {
                                LoggingConfig.discoveryLogger.e("Failed to restart avahi-publish: ${result.exceptionOrNull()?.message}")
                                _isAdvertising = false
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    // Expected when we stop advertising
                }
            }.apply {
                isDaemon = true
                name = "HealthCoach-avahi-publish-monitor"
                start()
            }

            Result.success(Unit)
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Failed to start avahi-publish advertising: ${e.message}", e)
            _isAdvertising = false
            Result.failure(e)
        }
    }

    /**
     * Background heartbeat that periodically refreshes the mDNS service registration.
     *
     * In sandboxed environments (Flatpak, containers), the multicast UDP socket may become
     * stale after hours of operation — the socket remains open but multicast group membership
     * is silently dropped by the network stack or sandbox. The heartbeat keeps the socket
     * alive by re-registering the service periodically, ensuring multicast announcements
     * continue to reach the network.
     *
     * Note: we cannot rely on [JmDNS.getServiceInfo] to detect stale sockets, because it
     * checks the local JmDNS registry cache, which always returns the service info for
     * locally-registered services even when the underlying multicast socket is stale.
     * The only reliable fix is to re-register unconditionally at regular intervals.
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

                    // Unconditionally re-register the service to keep the multicast socket alive.
                    // In sandboxed environments (Flatpak), multicast group membership may be
                    // silently dropped after extended idle periods. Re-registering sends new
                    // multicast DNS-SD announcements that refresh the socket and group membership.
                    // We cannot use getServiceInfo() to check first, because it queries the local
                    // JmDNS registry cache, which always returns the service for locally-registered
                    // services even when the multicast socket is stale.
                    val serviceName = "${PeerDiscoveryConstants.SERVICE_NAME_PREFIX} - ${advertisingInstanceId.take(8)}"
                    LoggingConfig.discoveryLogger.d("mDNS heartbeat: refreshing service registration for '$serviceName' on ${currentAddress.hostAddress}")
                    reRegisterService(dns)
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

            // Stop avahi-publish process if running
            avahiPublishProcess?.destroy()
            avahiPublishProcess = null

            // Stop JmDNS if running
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
