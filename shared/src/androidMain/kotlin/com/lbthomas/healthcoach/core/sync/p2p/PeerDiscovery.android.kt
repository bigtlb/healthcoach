package com.lbthomas.healthcoach.core.sync.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.lbthomas.healthcoach.core.logging.LoggingConfig
import com.lbthomas.healthcoach.core.utils.currentEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

actual open class PeerDiscoveryAdvertiser(private val context: Context) {
    private val nsdManager: NsdManager? by lazy {
        try {
            context.getSystemService(Context.NSD_SERVICE) as? NsdManager
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Could not get NsdManager for advertiser: ${e.message}", e)
            null
        }
    }
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var _isAdvertising = false

    actual open val isAdvertising: Boolean
        get() = _isAdvertising

    actual open fun startAdvertising(instanceId: String, deviceName: String, port: Int): Result<Unit> {
        val manager = nsdManager ?: return Result.failure(IllegalStateException("NsdManager not available on this device"))
        return try {
            stopAdvertising()
            acquireMulticastLock()

            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "${PeerDiscoveryConstants.SERVICE_NAME_PREFIX} - ${instanceId.take(8)}"
                serviceType = PeerDiscoveryConstants.SERVICE_TYPE_NSD
                setPort(port)
                setAttribute("instanceId", instanceId)
                setAttribute("deviceName", deviceName)
                setAttribute("interfaceVersion", PeerDiscoveryConstants.INTERFACE_VERSION)
                setAttribute("port", port.toString())
            }

            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                    _isAdvertising = true
                    LoggingConfig.discoveryLogger.i("Android NSD service registered: ${NsdServiceInfo.serviceName} on port $port")
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    _isAdvertising = false
                    LoggingConfig.discoveryLogger.e("Android NSD registration failed: errorCode $errorCode")
                }

                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                    _isAdvertising = false
                    LoggingConfig.discoveryLogger.i("Android NSD service unregistered: ${arg0.serviceName}")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    LoggingConfig.discoveryLogger.e("Android NSD unregistration failed: errorCode $errorCode")
                }
            }

            registrationListener = listener
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
            Result.success(Unit)
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Failed to register Android NSD service: ${e.message}", e)
            _isAdvertising = false
            releaseMulticastLock()
            Result.failure(e)
        }
    }

    actual open fun stopAdvertising(): Result<Unit> {
        val manager = nsdManager ?: return Result.success(Unit)
        return try {
            registrationListener?.let {
                manager.unregisterService(it)
                registrationListener = null
            }
            releaseMulticastLock()
            _isAdvertising = false
            Result.success(Unit)
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Error unregistering Android NSD service: ${e.message}", e)
            releaseMulticastLock()
            Result.failure(e)
        }
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("HealthCoachAdvertiserLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            LoggingConfig.discoveryLogger.w("Could not acquire multicast lock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
        } catch (_: Exception) {}
    }
}

actual open class PeerDiscoveryBrowser(private val context: Context) {
    private val nsdManager: NsdManager? by lazy {
        try {
            context.getSystemService(Context.NSD_SERVICE) as? NsdManager
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Could not get NsdManager for browser: ${e.message}", e)
            null
        }
    }
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var _isBrowsing = false
    private var localInstanceId: String = ""

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    actual open val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    actual open val isBrowsing: Boolean
        get() = _isBrowsing

    actual open fun startBrowsing(localInstanceId: String) {
        if (_isBrowsing) return
        val manager = nsdManager ?: return
        this.localInstanceId = localInstanceId
        try {
            acquireMulticastLock()

            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    _isBrowsing = true
                    LoggingConfig.discoveryLogger.i("Android NSD service discovery started for $regType")
                }

                override fun onServiceFound(service: NsdServiceInfo) {
                    if (service.serviceType.contains("_healthcoach-sync")) {
                        resolveFoundService(service)
                    }
                }

                override fun onServiceLost(service: NsdServiceInfo) {
                    val lostName = service.serviceName
                    _discoveredPeers.update { list ->
                        list.filterNot { it.name == lostName || "${PeerDiscoveryConstants.SERVICE_NAME_PREFIX} - ${it.instanceId.take(8)}" == lostName }
                    }
                    LoggingConfig.discoveryLogger.d("Android NSD service lost: $lostName")
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    _isBrowsing = false
                    LoggingConfig.discoveryLogger.i("Android NSD service discovery stopped: $serviceType")
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    _isBrowsing = false
                    LoggingConfig.discoveryLogger.e("Android NSD start discovery failed: errorCode $errorCode")
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    LoggingConfig.discoveryLogger.e("Android NSD stop discovery failed: errorCode $errorCode")
                }
            }

            discoveryListener = listener
            manager.discoverServices(PeerDiscoveryConstants.SERVICE_TYPE_NSD, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Failed to start Android NSD discovery: ${e.message}", e)
            _isBrowsing = false
            releaseMulticastLock()
        }
    }

    private fun resolveFoundService(serviceInfo: NsdServiceInfo) {
        val manager = nsdManager ?: return
        try {
            manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    LoggingConfig.discoveryLogger.w("Android NSD resolve failed for ${serviceInfo.serviceName}: errorCode $errorCode")
                }

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                    val attributes = resolvedInfo.attributes
                    val peerInstanceId = attributes["instanceId"]?.let { String(it, Charsets.UTF_8) } ?: ""

                    // Filter out local instance
                    if (peerInstanceId.isBlank() || peerInstanceId == this@PeerDiscoveryBrowser.localInstanceId) {
                        return
                    }

                    val deviceName = attributes["deviceName"]?.let { String(it, Charsets.UTF_8) }
                        ?.takeIf { it.isNotBlank() }
                        ?: resolvedInfo.serviceName

                    val host = resolvedInfo.host?.hostAddress ?: ""
                    val port = resolvedInfo.port

                    if (host.isNotBlank() && port > 0) {
                        val peer = DiscoveredPeer(
                            instanceId = peerInstanceId,
                            name = deviceName,
                            host = host,
                            port = port,
                            interfaceVersion = attributes["interfaceVersion"]?.let { String(it, Charsets.UTF_8) } ?: PeerDiscoveryConstants.INTERFACE_VERSION,
                            lastSeenTimestamp = currentEpochMillis()
                        )

                        _discoveredPeers.update { current ->
                            val updated = current.filterNot { it.instanceId == peer.instanceId }.toMutableList()
                            updated.add(peer)
                            updated.sortedBy { it.name }
                        }
                        LoggingConfig.discoveryLogger.i("Discovered peer on Android: '$deviceName' ($host:$port, id: $peerInstanceId)")
                    }
                }
            })
        } catch (e: Exception) {
            LoggingConfig.discoveryLogger.w("Error initiating resolve for ${serviceInfo.serviceName}: ${e.message}")
        }
    }

    actual open fun stopBrowsing() {
        val manager = nsdManager ?: return
        try {
            discoveryListener?.let {
                manager.stopServiceDiscovery(it)
                discoveryListener = null
            }
            releaseMulticastLock()
            _isBrowsing = false
        } catch (e: Throwable) {
            LoggingConfig.discoveryLogger.e("Error stopping Android NSD discovery: ${e.message}", e)
            releaseMulticastLock()
        }
    }

    actual open fun clearPeers() {
        _discoveredPeers.value = emptyList()
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("HealthCoachBrowserLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            LoggingConfig.discoveryLogger.w("Could not acquire multicast lock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
        } catch (_: Exception) {}
    }
}
