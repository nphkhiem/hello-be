package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class HelloBeThemeMode {
    DAY,
    NIGHT
}

@Immutable
data class HelloBeColors(
    val canvas: Color,
    val scenery: Color,
    val surfacePrimary: Color,
    val surfaceRaised: Color,
    val surfaceSoft: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textInverse: Color,
    val actionPrimary: Color,
    val actionPrimaryFocused: Color,
    val actionPrimaryPressed: Color,
    val actionSecondary: Color,
    val actionSelected: Color,
    val actionSelectedBorder: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onSelected: Color,
    val borderPrimary: Color,
    val borderSecondary: Color,
    val focusRing: Color,
    val focusGuard: Color,
    val scrim: Color,
    val accentPip: Color,
    val accentWarm: Color,
    val accentGrowth: Color,
    val accentCalm: Color,
    val accentStory: Color,
    val successContent: Color,
    val successContainer: Color,
    val warningContent: Color,
    val warningContainer: Color,
    val errorContent: Color,
    val errorContainer: Color,
    val infoContent: Color,
    val infoContainer: Color,
    val supportiveRetryContent: Color,
    val supportiveRetryContainer: Color
)

private val dayColors =
    HelloBeColors(
        canvas = Color(0xFFF8F1E7),
        scenery = Color(0xFFEFE2CA),
        surfacePrimary = Color(0xFFFFFCF5),
        surfaceRaised = Color(0xFFFFFFFF),
        surfaceSoft = Color(0xFFE7EFE9),
        surfaceMuted = Color(0xFFEAE6DD),
        textPrimary = Color(0xFF102F4C),
        textSecondary = Color(0xFF4C6472),
        textTertiary = Color(0xFF66747C),
        textInverse = Color(0xFFFFF9F0),
        actionPrimary = Color(0xFF123A5A),
        actionPrimaryFocused = Color(0xFF102F4C),
        actionPrimaryPressed = Color(0xFF0B263E),
        actionSecondary = Color(0xFFE7EFE9),
        actionSelected = Color(0xFF2F6187),
        actionSelectedBorder = Color(0xFF7FA8C6),
        onPrimary = Color(0xFFFFF9F0),
        onSecondary = Color(0xFF102F4C),
        onSelected = Color(0xFFFFF9F0),
        borderPrimary = Color(0xFF7A8C94),
        borderSecondary = Color(0xFFD4C8AD),
        focusRing = Color(0xFFFFC247),
        focusGuard = Color(0xFF102F4C),
        scrim = Color(red = 0x08, green = 0x2A, blue = 0x4A, alpha = 0xB8),
        accentPip = Color(0xFF61B7CA),
        accentWarm = Color(0xFFD97352),
        accentGrowth = Color(0xFF3F7A60),
        accentCalm = Color(0xFF8D7BB8),
        accentStory = Color(0xFFB75E77),
        successContent = Color(0xFF23513B),
        successContainer = Color(0xFFDDEEE4),
        warningContent = Color(0xFF68470C),
        warningContainer = Color(0xFFFFF0CF),
        errorContent = Color(0xFF7B342B),
        errorContainer = Color(0xFFF9E2DD),
        infoContent = Color(0xFF27546D),
        infoContainer = Color(0xFFDEEBF2),
        supportiveRetryContent = Color(0xFF102F4C),
        supportiveRetryContainer = Color(0xFFFFF0D9)
    )

private val nightColors =
    HelloBeColors(
        canvas = Color(0xFF0A2136),
        scenery = Color(0xFF102B43),
        surfacePrimary = Color(0xFF15354D),
        surfaceRaised = Color(0xFF1B405A),
        surfaceSoft = Color(0xFF173F3F),
        surfaceMuted = Color(0xFF263A48),
        textPrimary = Color(0xFFFFF5E7),
        textSecondary = Color(0xFFD6E2E5),
        textTertiary = Color(0xFFAEBFC4),
        textInverse = Color(0xFF102F4C),
        actionPrimary = Color(0xFF173F5A),
        actionPrimaryFocused = Color(0xFF102F4C),
        actionPrimaryPressed = Color(0xFF071A2B),
        actionSecondary = Color(0xFF254A52),
        actionSelected = Color(0xFFA9C8DE),
        actionSelectedBorder = Color(0xFF4A7EA3),
        onPrimary = Color(0xFFFFF5E7),
        onSecondary = Color(0xFFFFF5E7),
        onSelected = Color(0xFF102F4C),
        borderPrimary = Color(0xFFAFC3C8),
        borderSecondary = Color(0xFF3B5668),
        focusRing = Color(0xFFFFD56A),
        focusGuard = Color(0xFF061624),
        scrim = Color(red = 0x04, green = 0x11, blue = 0x1D, alpha = 0xC7),
        accentPip = Color(0xFF7CCDDD),
        accentWarm = Color(0xFFF08A69),
        accentGrowth = Color(0xFF72B68F),
        accentCalm = Color(0xFFB7A3E0),
        accentStory = Color(0xFFE58AA2),
        successContent = Color(0xFFBCE8CD),
        successContainer = Color(0xFF1D4A36),
        warningContent = Color(0xFFFFE0A0),
        warningContainer = Color(0xFF563D17),
        errorContent = Color(0xFFFFD0C7),
        errorContainer = Color(0xFF5A2D29),
        infoContent = Color(0xFFC5E7F4),
        infoContainer = Color(0xFF1D465C),
        supportiveRetryContent = Color(0xFFFFF5E7),
        supportiveRetryContainer = Color(0xFF4A3B20)
    )

/**
 * High contrast is a semantic override layer applied on top of the active Day or Night
 * palette: it strengthens the quiet secondary border to the same weight as the primary
 * structural border and removes scrim transparency, per DESIGN_TOKENS.md "High contrast".
 */
private fun HelloBeColors.asHighContrast(): HelloBeColors = copy(
    borderSecondary = borderPrimary,
    scrim = scrim.copy(alpha = 1f)
)

fun helloBeColors(mode: HelloBeThemeMode, highContrast: Boolean = false): HelloBeColors {
    val base =
        when (mode) {
            HelloBeThemeMode.DAY -> dayColors
            HelloBeThemeMode.NIGHT -> nightColors
        }
    return if (highContrast) base.asHighContrast() else base
}
