package com.lbthomas.healthcoach.core.enums

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode(val displayName: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}
