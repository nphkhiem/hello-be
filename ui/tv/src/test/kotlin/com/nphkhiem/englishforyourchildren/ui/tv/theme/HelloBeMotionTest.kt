package com.nphkhiem.englishforyourchildren.ui.tv.theme

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HelloBeMotionTest {
    @Test
    fun givenStandardMotion_whenBaseDurationsAreRead_thenTheyMatchTokenSpec() {
        val motion = helloBeMotion(reduceMotion = false)

        assertThat(motion.durations.focus).isEqualTo(90)
        assertThat(motion.durations.navigation).isEqualTo(260)
        assertThat(motion.durations.pageTurn).isEqualTo(360)
        assertThat(motion.durations.celebrationMax).isEqualTo(3400)
    }

    @Test
    fun givenStandardMotion_whenUsageTransitionsAreRead_thenTheyUseTheNamedDuration() {
        val motion = helloBeMotion(reduceMotion = false)

        assertThat(motion.focusTransitionMillis).isEqualTo(motion.durations.focus)
        assertThat(motion.navigationTransitionMillis).isEqualTo(motion.durations.navigation)
        assertThat(motion.pageTurnTransitionMillis).isEqualTo(motion.durations.pageTurn)
    }

    @Test
    fun givenReducedMotion_whenFocusTransitionIsRead_thenSurfaceUpdatesInstantly() {
        val motion = helloBeMotion(reduceMotion = true)

        assertThat(motion.focusTransitionMillis).isEqualTo(0)
    }

    @Test
    fun givenReducedMotion_whenScreenTransitionsAreRead_thenOnlyAShortCrossfadeApplies() {
        val motion = helloBeMotion(reduceMotion = true)

        assertThat(motion.navigationTransitionMillis).isEqualTo(100)
        assertThat(motion.pageTurnTransitionMillis).isEqualTo(100)
    }

    @Test
    fun givenReducedMotion_whenProgressTrailTransitionIsRead_thenItUpdatesImmediately() {
        val standard = helloBeMotion(reduceMotion = false)
        val reduced = helloBeMotion(reduceMotion = true)

        assertThat(standard.progressTrailTransitionMillis).isEqualTo(standard.durations.normal)
        assertThat(reduced.progressTrailTransitionMillis).isEqualTo(0)
    }

    @Test
    fun givenMotionEasing_whenTransformedAtCurveEndpoints_thenItStartsAtZeroAndEndsAtOne() {
        assertThat(HelloBeMotionEasing.standard.transform(0f)).isEqualTo(0f)
        assertThat(HelloBeMotionEasing.standard.transform(1f)).isEqualTo(1f)
        assertThat(HelloBeMotionEasing.decelerate.transform(0f)).isEqualTo(0f)
        assertThat(HelloBeMotionEasing.accelerate.transform(1f)).isEqualTo(1f)
    }
}
