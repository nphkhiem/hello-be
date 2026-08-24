package com.nphkhiem.englishforyourchildren.ui.tv.component

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class FeedbackToneTest {

    @Test
    fun givenCorrectTone_whenPoseIsChosen_thenPipCelebratesTheEffort() {
        assertThat(FeedbackTone.CORRECT.pose).isEqualTo(PipPose.CELEBRATING)
    }

    @Test
    fun givenSupportiveRetry_whenPoseIsChosen_thenPipModelsAgainRatherThanReacting() {
        assertThat(FeedbackTone.SUPPORTIVE_RETRY.pose).isEqualTo(PipPose.MODELING)
    }

    @Test
    fun givenInformation_whenPoseIsChosen_thenPipSimplyGuides() {
        assertThat(FeedbackTone.INFORMATION.pose).isEqualTo(PipPose.POINTING)
    }

    @Test
    fun givenEveryTone_whenPosesAreChosen_thenNoneOfThemIsADisappointedReaction() {
        val poses = FeedbackTone.entries.map { it.pose }

        // Pip never scolds or looks disappointed, so no tone may resolve to a resting slump or
        // any pose that could read as a reaction to the child getting it wrong.
        assertThat(poses).doesNotContain(PipPose.RESTING)
        assertThat(poses).containsNoDuplicates()
    }
}
