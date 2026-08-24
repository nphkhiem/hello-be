package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

@Immutable
data class HelloBeMotionDurations(
    val instant: Int = 50,
    val press: Int = 60,
    val focus: Int = 90,
    val fast: Int = 120,
    val feedback: Int = 180,
    val normal: Int = 220,
    val navigation: Int = 260,
    val pageTurn: Int = 360,
    val celebrationMax: Int = 3400
)

object HelloBeMotionEasing {
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val accelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
}

private const val REDUCED_CROSSFADE_MILLIS = 100

@Immutable
data class HelloBeMotion(
    val durations: HelloBeMotionDurations,
    val focusTransitionMillis: Int,
    val navigationTransitionMillis: Int,
    val pageTurnTransitionMillis: Int,
    val progressTrailTransitionMillis: Int,
    val captionTransitionMillis: Int,
    val reduceMotion: Boolean
)

/**
 * Reduced motion, per DESIGN_TOKENS.md "Reduced-motion substitutions": focus feedback becomes
 * an instant surface change, screen and page-turn transitions collapse to a 100ms crossfade,
 * and progress trail segments update immediately rather than animating.
 */
fun helloBeMotion(reduceMotion: Boolean): HelloBeMotion {
    val durations = HelloBeMotionDurations()
    return HelloBeMotion(
        durations = durations,
        focusTransitionMillis = if (reduceMotion) 0 else durations.focus,
        navigationTransitionMillis =
            if (reduceMotion) REDUCED_CROSSFADE_MILLIS else durations.navigation,
        pageTurnTransitionMillis =
            if (reduceMotion) REDUCED_CROSSFADE_MILLIS else durations.pageTurn,
        progressTrailTransitionMillis = if (reduceMotion) 0 else durations.normal,
        captionTransitionMillis = if (reduceMotion) 0 else durations.fast,
        reduceMotion = reduceMotion
    )
}
