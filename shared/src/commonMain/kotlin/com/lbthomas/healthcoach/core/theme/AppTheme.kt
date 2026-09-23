package com.lbthomas.healthcoach.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class AppTheme(
    val displayName: String,
    val previewPrimary: Color
) {
    DEFAULT("Default", Color(0xFF884B6A)) {
        override fun lightScheme(): ColorScheme = defaultLightScheme
        override fun darkScheme(): ColorScheme = defaultDarkScheme
        override fun lightExtendedColors(): ExtendedColorScheme = defaultExtendedLight
        override fun darkExtendedColors(): ExtendedColorScheme = defaultExtendedDark
    },
    BLUE("Blue", Color(0xFF4C5C92)) {
        override fun lightScheme(): ColorScheme = blueLightScheme
        override fun darkScheme(): ColorScheme = blueDarkScheme
        override fun lightExtendedColors(): ExtendedColorScheme = blueExtendedLight
        override fun darkExtendedColors(): ExtendedColorScheme = blueExtendedDark
    },
    GREEN("Green", Color(0xFF416835)) {
        override fun lightScheme(): ColorScheme = greenLightScheme
        override fun darkScheme(): ColorScheme = greenDarkScheme
        override fun lightExtendedColors(): ExtendedColorScheme = greenExtendedLight
        override fun darkExtendedColors(): ExtendedColorScheme = greenExtendedDark
    },
    TEAL("Teal", Color(0xFF006A61)) {
        override fun lightScheme(): ColorScheme = tealLightScheme
        override fun darkScheme(): ColorScheme = tealDarkScheme
        override fun lightExtendedColors(): ExtendedColorScheme = tealExtendedLight
        override fun darkExtendedColors(): ExtendedColorScheme = tealExtendedDark
    };

    abstract fun lightScheme(): ColorScheme
    abstract fun darkScheme(): ColorScheme
    abstract fun lightExtendedColors(): ExtendedColorScheme
    abstract fun darkExtendedColors(): ExtendedColorScheme

    @Composable
    open fun typography(): Typography = AppTypography
}
