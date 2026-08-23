package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalHelloBeColors =
    staticCompositionLocalOf<HelloBeColors> {
        error("HelloBeTheme not applied: no HelloBeColors provided")
    }
private val LocalHelloBeTypography =
    staticCompositionLocalOf<HelloBeTypography> {
        error("HelloBeTheme not applied: no HelloBeTypography provided")
    }
private val LocalHelloBeElevation =
    staticCompositionLocalOf<HelloBeElevation> {
        error("HelloBeTheme not applied: no HelloBeElevation provided")
    }
private val LocalHelloBeFocus =
    staticCompositionLocalOf<HelloBeFocus> {
        error("HelloBeTheme not applied: no HelloBeFocus provided")
    }
private val LocalHelloBeMotion =
    staticCompositionLocalOf<HelloBeMotion> {
        error("HelloBeTheme not applied: no HelloBeMotion provided")
    }

/**
 * Root Storybook Stage theme. Feature code must read every visual constant through
 * [HelloBeTheme] rather than hard-coding colors, dp values, text styles, shapes, or
 * motion durations, per DESIGN_TOKENS.md.
 */
@Composable
fun HelloBeTheme(
    themeMode: HelloBeThemeMode = HelloBeThemeMode.DAY,
    highContrast: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = remember(themeMode, highContrast) { helloBeColors(themeMode, highContrast) }
    val typography = remember { helloBeTypography() }
    val elevation = remember(themeMode) { helloBeElevation(themeMode) }
    val focus = remember(highContrast) { helloBeFocus(highContrast) }
    val motion = remember(reduceMotion) { helloBeMotion(reduceMotion) }

    CompositionLocalProvider(
        LocalHelloBeColors provides colors,
        LocalHelloBeTypography provides typography,
        LocalHelloBeElevation provides elevation,
        LocalHelloBeFocus provides focus,
        LocalHelloBeMotion provides motion,
        content = content
    )
}

object HelloBeTheme {
    val colors: HelloBeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHelloBeColors.current

    val typography: HelloBeTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalHelloBeTypography.current

    val elevation: HelloBeElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalHelloBeElevation.current

    val focus: HelloBeFocus
        @Composable
        @ReadOnlyComposable
        get() = LocalHelloBeFocus.current

    val motion: HelloBeMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalHelloBeMotion.current

    val spacing: HelloBeSpacing
        get() = HelloBeSpacing

    val shapes: HelloBeShapes
        get() = HelloBeShapes

    val layout: HelloBeLayout
        get() = HelloBeLayout
}
