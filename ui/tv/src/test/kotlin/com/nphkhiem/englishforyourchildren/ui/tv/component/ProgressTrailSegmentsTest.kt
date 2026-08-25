package com.nphkhiem.englishforyourchildren.ui.tv.component

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ProgressTrailSegmentsTest {

    @Test
    fun givenMidLesson_whenSegmentsAreBuilt_thenEarlierStepsAreCompleteAndLaterAreUpcoming() {
        val segments = progressTrailSegments(totalSteps = 4, currentStep = 2)

        assertThat(segments).containsExactly(
            TrailSegment.COMPLETED,
            TrailSegment.CURRENT,
            TrailSegment.UPCOMING,
            TrailSegment.UPCOMING
        ).inOrder()
    }

    @Test
    fun givenFirstStep_whenSegmentsAreBuilt_thenNothingIsMarkedComplete() {
        val segments = progressTrailSegments(totalSteps = 3, currentStep = 1)

        assertThat(segments.first()).isEqualTo(TrailSegment.CURRENT)
        assertThat(segments).doesNotContain(TrailSegment.COMPLETED)
    }

    @Test
    fun givenLastStep_whenSegmentsAreBuilt_thenOnlyTheFinalSegmentIsCurrent() {
        val segments = progressTrailSegments(totalSteps = 3, currentStep = 3)

        assertThat(segments.last()).isEqualTo(TrailSegment.CURRENT)
        assertThat(segments).doesNotContain(TrailSegment.UPCOMING)
    }

    @Test
    fun givenStepBeyondTheEnd_whenSegmentsAreBuilt_thenItIsClampedRatherThanThrowing() {
        val segments = progressTrailSegments(totalSteps = 3, currentStep = 99)

        assertThat(segments).hasSize(3)
        assertThat(segments.last()).isEqualTo(TrailSegment.CURRENT)
    }

    @Test
    fun givenStepBeforeTheStart_whenSegmentsAreBuilt_thenItIsClampedRatherThanThrowing() {
        val segments = progressTrailSegments(totalSteps = 3, currentStep = 0)

        assertThat(segments).hasSize(3)
        assertThat(segments.first()).isEqualTo(TrailSegment.CURRENT)
    }

    @Test
    fun givenNoSteps_whenSegmentsAreBuilt_thenThereIsNothingToDraw() {
        assertThat(progressTrailSegments(totalSteps = 0, currentStep = 1)).isEmpty()
        assertThat(progressTrailSegments(totalSteps = -5, currentStep = 1)).isEmpty()
    }
}
