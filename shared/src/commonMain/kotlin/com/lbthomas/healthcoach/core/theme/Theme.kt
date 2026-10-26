package com.lbthomas.healthcoach.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.lbthomas.healthcoach.core.enums.FontSizePreference
import com.lbthomas.healthcoach.core.enums.ThemeMode

@Composable
fun HealthCoachTheme(
    theme: AppTheme = AppTheme.DEFAULT,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    bodyTextSize: FontSizePreference = FontSizePreference.MEDIUM,
    labelTextSize: FontSizePreference = FontSizePreference.MEDIUM,
    uiScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) theme.darkScheme() else theme.lightScheme()
    val extendedColors = if (isDark) theme.darkExtendedColors() else theme.lightExtendedColors()
    val typography = theme.typography(bodyTextSize = bodyTextSize, labelTextSize = labelTextSize)

    val currentDensity = LocalDensity.current
    val effectiveScale = uiScale.coerceIn(0.5f, 2.5f)
    val scaledDensity = remember(currentDensity, effectiveScale) {
        Density(
            density = currentDensity.density * effectiveScale,
            fontScale = currentDensity.fontScale
        )
    }

    CompositionLocalProvider(
        LocalExtendedColorScheme provides extendedColors,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
