package com.lbthomas.healthcoach.core.sync.p2p

import com.lbthomas.healthcoach.core.logging.LoggingConfig
import com.lbthomas.healthcoach.core.utils.currentEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

actual open class PeerDiscoveryAdvertiser {
    private var jmdns: JmDNS? = null
    private var registeredService: ServiceInfo? = null
    private var _isAdvertising = false

    actual open val isAdvertising: Boolean
        get() = _isAdvertising

    actual open fun startAdvertising(instanceId: String, deviceName: String, port: Int): Result<Unit> {
        return try {
            stopAdvertising()
            val hostAddress = getLocalNonLoopbackAddress() ?: InetAddress.getLocalHost()
            val dns = JmDNS.create(hostAddress, "HealthCoach-${instanceId.take(8)}")
            jmdns = dns

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
            Result.success(Unit)
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Failed to start mDNS advertising: ${e.message}", e)
            _isAdvertising = false
            Result.failure(e)
        }
    }

    actual open fun stopAdvertising(): Result<Unit> {
        return try {
            registeredService?.let {
                jmdns?.unregisterService(it)
                registeredService = null
            }
            jmdns?.close()
            jmdns = null
            _isAdvertising = false
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
                        list.filterNot { it.name == removedName || "${PeerDiscoveryConstants.SERVICE_NAME_PREFIX} - ${it.instanceId.take(8)}" == removedName }
                    }
                    LoggingConfig.discoveryLogger.d("mDNS service removed: $removedName")
                }

                override fun serviceResolved(event: ServiceEvent) {
                    val info = event.info ?: return
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
            }

            activeListener = listener
            dns.addServiceListener(PeerDiscoveryConstants.SERVICE_TYPE_JMDNS, listener)
            _isBrowsing = true
            LoggingConfig.discoveryLogger.i("mDNS service browsing started on $hostAddress")
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Failed to start mDNS browsing: ${e.message}", e)
            _isBrowsing = false
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
