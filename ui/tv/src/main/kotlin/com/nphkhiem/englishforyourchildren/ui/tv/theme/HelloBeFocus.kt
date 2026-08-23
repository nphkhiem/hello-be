package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class HelloBeFocus(
    val ringWidth: Dp,
    val guardWidth: Dp,
    val ringGap: Dp,
    val scaleButton: Float,
    val scaleCard: Float,
    val scaleLargeCard: Float,
    val pressScale: Float,
    val clearance: Dp
)

fun helloBeFocus(highContrast: Boolean): HelloBeFocus = HelloBeFocus(
    ringWidth = if (highContrast) 8.dp else 6.dp,
    guardWidth = 2.dp,
    ringGap = 0.dp,
    scaleButton = 1.04f,
    scaleCard = 1.05f,
    scaleLargeCard = 1.025f,
    pressScale = 0.98f,
    clearance = 12.dp
)
