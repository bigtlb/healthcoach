package com.lbthomas.healthcoach.core

import androidx.compose.runtime.Composable

actual class Platform{
    actual val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform() = Platform()

@Composable
actual fun getPlatformContext(): Any? = null

actual fun getDefaultDeviceName(): String {
    val hostName = runCatching {
        java.net.InetAddress.getLocalHost().hostName
    }.getOrNull()?.takeIf { it.isNotBlank() && !it.equals("localhost", ignoreCase = true) }

    val envHost = System.getenv("HOSTNAME")?.takeIf { it.isNotBlank() }
        ?: System.getenv("COMPUTERNAME")?.takeIf { it.isNotBlank() }

    val name = hostName ?: envHost
    return if (!name.isNullOrBlank()) {
        "HealthCoach on $name"
    } else {
        "HealthCoach Desktop"
    }
}