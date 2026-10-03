package com.lbthomas.healthcoach.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.lbthomas.healthcoach.core.enums.FontSizePreference
import com.lbthomas.healthcoach.core.enums.ThemeMode

@Composable
fun HealthCoachTheme(
    theme: AppTheme = AppTheme.DEFAULT,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    bodyTextSize: FontSizePreference = FontSizePreference.MEDIUM,
    labelTextSize: FontSizePreference = FontSizePreference.MEDIUM,
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

    CompositionLocalProvider(LocalExtendedColorScheme provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
