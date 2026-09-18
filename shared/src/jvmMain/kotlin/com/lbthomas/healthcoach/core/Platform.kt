package com.lbthomas.healthcoach.core

import androidx.compose.runtime.Composable

actual class Platform{
    actual val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform() = Platform()

@Composable
actual fun getPlatformContext(): Any? = null