package com.lbthomas.healthcoach.core.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

/**
 * Global logging configuration and diagnostic controls for HealthCoach.
 *
 * Single-line throttle control:
 * You can adjust [minSeverity] in one line of code to control output log levels across the entire application:
 * ```kotlin
 * LoggingConfig.minSeverity = Severity.Info  // Suppress verbose debug logs in production
 * LoggingConfig.minSeverity = Severity.Debug // Enable rich diagnostic logging for functional tests
 * ```
 */
object LoggingConfig {
    /**
     * Master log level throttle variable.
     * Setting this updates Kermit's active minimum severity threshold across all platforms.
     */
    var minSeverity: Severity = Severity.Debug
        set(value) {
            field = value
            Logger.setMinSeverity(value)
        }

    /**
     * Dedicated flag to enable/disable detailed P2P server network logs (HTTP requests, streaming bytes, pairing).
     */
    var p2pServerLogging: Boolean = true

    /**
     * Dedicated flag to enable/disable detailed P2P client network logs (discovery scans, downloads, uploads).
     */
    var p2pClientLogging: Boolean = true

    /**
     * Dedicated flag to enable/disable discovery mDNS/NSD broadcast and browsing logs.
     */
    var p2pDiscoveryLogging: Boolean = true

    // Tagged logger instances for structured subsystem diagnostics
    val serverLogger: Logger = Logger.withTag("P2P-Server")
    val clientLogger: Logger = Logger.withTag("P2P-Client")
    val discoveryLogger: Logger = Logger.withTag("P2P-Discovery")
    val syncLogger: Logger = Logger.withTag("SyncEngine")

    init {
        Logger.setMinSeverity(minSeverity)
    }
}
