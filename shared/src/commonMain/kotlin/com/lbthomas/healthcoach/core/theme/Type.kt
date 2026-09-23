package com.lbthomas.healthcoach.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import healthcoach.shared.generated.resources.Res
import healthcoach.shared.generated.resources.inter_bold
import healthcoach.shared.generated.resources.inter_medium
import healthcoach.shared.generated.resources.inter_regular
import healthcoach.shared.generated.resources.inter_semibold
import healthcoach.shared.generated.resources.outfit_bold
import healthcoach.shared.generated.resources.outfit_medium
import healthcoach.shared.generated.resources.outfit_regular
import healthcoach.shared.generated.resources.outfit_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun defaultDisplayFontFamily(): FontFamily = FontFamily(
    Font(Res.font.outfit_regular, FontWeight.Normal),
    Font(Res.font.outfit_medium, FontWeight.Medium),
    Font(Res.font.outfit_semibold, FontWeight.SemiBold),
    Font(Res.font.outfit_bold, FontWeight.Bold),
)

@Composable
fun defaultBodyFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

@Composable
fun createTypography(
    displayFontFamily: FontFamily? = defaultDisplayFontFamily(),
    bodyFontFamily: FontFamily? = defaultBodyFontFamily(),
    baseline: Typography = Typography()
): Typography {
    return Typography(
        displayLarge = baseline.displayLarge.let { if (displayFontFamily != null) it.copy(fontFamily = displayFontFamily) else it },
        displayMedium = baseline.displayMedium.let { if (displayFontFamily != null) it.copy(fontFamily = displayFontFamily) else it },
        displaySmall = baseline.displaySmall.let { if (displayFontFamily != null) it.copy(fontFamily = displayFontFamily) else it },
        headlineLarge = baseline.headlineLarge.let { if (displayFontFamily != null) it.copy(fontFamily = displayFontFamily) else it },
        headlineMedium = baseline.headlineMedium.let { if (displayFontFamily != null) it.copy(fontFamily = displayFontFamily) else it },
        headlineSmall = baseline.headlineSmall.let { if (displayFontFamily != null) it.copy(fontFamily = displayFontFamily) else it },
        titleLarge = baseline.titleLarge.let { if (displayFontFamily != null) it.copy(fontFamily = displayFontFamily) else it },
        titleMedium = baseline.titleMedium.let { if (displayFontFamily != null) it.copy(fontFamily = displayFontFamily) else it },
        titleSmall = baseline.titleSmall.let { if (displayFontFamily != null) it.copy(fontFamily = displayFontFamily) else it },
        bodyLarge = baseline.bodyLarge.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it },
        bodyMedium = baseline.bodyMedium.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it },
        bodySmall = baseline.bodySmall.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it },
        labelLarge = baseline.labelLarge.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it },
        labelMedium = baseline.labelMedium.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it },
        labelSmall = baseline.labelSmall.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it },
    )
}

@Composable
fun appTypography(): Typography = createTypography()

val AppTypography: Typography
    @Composable
    get() = appTypography()
