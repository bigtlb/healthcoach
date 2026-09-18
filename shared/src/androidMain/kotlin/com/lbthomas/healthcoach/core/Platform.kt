package com.lbthomas.healthcoach.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual class Platform {
    actual val name: String = "Android"
}

actual fun getPlatform(): Platform = Platform()

@Composable
actual fun getPlatformContext(): Any? = LocalContext.current
