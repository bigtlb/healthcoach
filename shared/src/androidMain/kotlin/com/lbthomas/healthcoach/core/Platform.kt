package com.lbthomas.healthcoach.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual class Platform {
    actual val name: String = "Android"
}

actual fun getPlatform(): Platform = Platform()

@Composable
actual fun getPlatformContext(): Any? = LocalContext.current

actual fun getDefaultDeviceName(): String {
    val model = android.os.Build.MODEL
    val manufacturer = android.os.Build.MANUFACTURER
    return if (!model.isNullOrBlank()) {
        if (!manufacturer.isNullOrBlank() && !model.startsWith(manufacturer, ignoreCase = true)) {
            "HealthCoach ($manufacturer $model)"
        } else {
            "HealthCoach ($model)"
        }
    } else {
        "HealthCoach Android"
    }
}
