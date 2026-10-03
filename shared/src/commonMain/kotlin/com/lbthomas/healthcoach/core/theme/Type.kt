package com.lbthomas.healthcoach.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
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

private fun scaleStyle(style: TextStyle, deltaSp: Int): TextStyle {
    if (deltaSp == 0) return style
    val newFontSize = if (style.fontSize.isSpecified && style.fontSize.value > 0) {
        (style.fontSize.value + deltaSp).coerceAtLeast(6f).sp
    } else {
        style.fontSize
    }
    val newLineHeight = if (style.lineHeight.isSpecified && style.lineHeight.value > 0) {
        (style.lineHeight.value + deltaSp).coerceAtLeast(8f).sp
    } else {
        style.lineHeight
    }
    return style.copy(fontSize = newFontSize, lineHeight = newLineHeight)
}

@Composable
fun createTypography(
    displayFontFamily: FontFamily? = defaultDisplayFontFamily(),
    bodyFontFamily: FontFamily? = defaultBodyFontFamily(),
    bodyDeltaSp: Int = 0,
    labelDeltaSp: Int = 0,
    baseline: Typography = Typography()
): Typography {
    val bodyLargeBase = baseline.bodyLarge.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it }
    val bodyMediumBase = baseline.bodyMedium.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it }
    val bodySmallBase = baseline.bodySmall.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it }
    val labelLargeBase = baseline.labelLarge.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it }
    val labelMediumBase = baseline.labelMedium.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it }
    val labelSmallBase = baseline.labelSmall.let { if (bodyFontFamily != null) it.copy(fontFamily = bodyFontFamily) else it }

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
        bodyLarge = scaleStyle(bodyLargeBase, bodyDeltaSp),
        bodyMedium = scaleStyle(bodyMediumBase, bodyDeltaSp),
        bodySmall = scaleStyle(bodySmallBase, bodyDeltaSp),
        labelLarge = scaleStyle(labelLargeBase, labelDeltaSp),
        labelMedium = scaleStyle(labelMediumBase, labelDeltaSp),
        labelSmall = scaleStyle(labelSmallBase, labelDeltaSp),
    )
}

@Composable
fun appTypography(
    bodyDeltaSp: Int = 0,
    labelDeltaSp: Int = 0
): Typography = createTypography(
    bodyDeltaSp = bodyDeltaSp,
    labelDeltaSp = labelDeltaSp
)

val AppTypography: Typography
    @Composable
    get() = appTypography()
