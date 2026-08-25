package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Widths and scales for the state-indication treatments in DESIGN_TOKENS.md: the focus
 * ring and guard, and the selection outline a selected-but-not-focused control keeps so
 * selection never depends on fill alone. Both widths grow under high contrast.
 */
@Immutable
data class HelloBeFocus(
    val ringWidth: Dp,
    val guardWidth: Dp,
    val ringGap: Dp,
    val selectionWidth: Dp,
    val scaleButton: Float,
    val scaleCard: Float,
    val scaleLargeCard: Float,
    val pressScale: Float,
    val clearance: Dp
)

fun helloBeFocus(highContrast: Boolean): HelloBeFocus = HelloBeFocus(
    ringWidth = if (highContrast) 6.dp else 4.dp,
    guardWidth = 2.dp,
    selectionWidth = if (highContrast) 5.dp else 3.dp,
    ringGap = 4.dp,
    scaleButton = 1.04f,
    scaleCard = 1.05f,
    scaleLargeCard = 1.025f,
    pressScale = 0.98f,
    clearance = 12.dp
)
