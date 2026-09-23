package com.lbthomas.healthcoach.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

@Immutable
data class ExtendedColorScheme(
    val graphWeight: ColorFamily,
    val graphSystolic: ColorFamily,
    val graphDiastolic: ColorFamily,
    val graphPulse: ColorFamily,
    val weightIncrease: ColorFamily,
    val weightDecrease: ColorFamily,
    val weightNoChange: ColorFamily,
)

val LocalExtendedColorScheme = staticCompositionLocalOf<ExtendedColorScheme> {
    defaultExtendedLight
}

val MaterialTheme.extendedColors: ExtendedColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColorScheme.current
