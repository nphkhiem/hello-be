package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * `type.family.primary` should be Nunito Sans (DESIGN_TOKENS.md), but no licensed font asset
 * is bundled in this session. [primary] falls back to the platform sans-serif so every role
 * still renders correctly; swapping in the real Nunito Sans font resource later requires
 * changing only this one declaration.
 */
object HelloBeFontFamily {
    val primary: FontFamily = FontFamily.Default
    val fallback: FontFamily = FontFamily.SansSerif
    val mono: FontFamily = FontFamily.Monospace
}

@Immutable
data class HelloBeTypography(
    val displayHero: TextStyle,
    val displayLessonPrompt: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val caption: TextStyle,
    val learningGlyph: TextStyle
)

private val tightTracking = (-0.02).em
private val normalTracking = 0.em

fun helloBeTypography(fontFamily: FontFamily = HelloBeFontFamily.primary): HelloBeTypography {
    fun role(size: Int, lineHeight: Int, weight: Int, letterSpacing: TextUnit = normalTracking) =
        TextStyle(
            fontFamily = fontFamily,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            fontWeight = FontWeight(weight),
            letterSpacing = letterSpacing
        )

    return HelloBeTypography(
        displayHero = role(48, 52, 800, tightTracking),
        displayLessonPrompt = role(40, 44, 800, tightTracking),
        headlineLarge = role(36, 40, 800),
        headlineMedium = role(32, 36, 750),
        titleLarge = role(28, 34, 700),
        titleMedium = role(22, 28, 700),
        titleSmall = role(20, 26, 700),
        bodyLarge = role(20, 29, 500),
        bodyMedium = role(18, 26, 450),
        bodySmall = role(16, 23, 450),
        labelLarge = role(18, 22, 700),
        labelMedium = role(16, 20, 700),
        labelSmall = role(14, 18, 700),
        caption = role(20, 28, 600),
        learningGlyph = role(96, 104, 800)
    )
}
