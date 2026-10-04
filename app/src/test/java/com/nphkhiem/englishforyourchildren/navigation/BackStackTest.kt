package com.nphkhiem.englishforyourchildren.navigation

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import org.junit.jupiter.api.Test

class BackStackTest {

    @Test
    fun givenSomethingBeneath_whenBackIsPressed_thenItGoesBackOne() {
        val stack = listOf(root, HelloBeKey.FreePlay(profileId = minh))

        assertThat(backOutcome(stack)).isEqualTo(BackOutcome.Pop)
    }

    @Test
    fun givenTheRoot_whenBackIsPressed_thenItLeavesTheApp() {
        // The learning path, the launch picker and profile create are all root in some launch,
        // and the rule is one rule: root is the destination with nothing beneath it.
        assertThat(backOutcome(listOf(root))).isEqualTo(BackOutcome.ExitApp)
        assertThat(backOutcome(listOf(HelloBeKey.ProfileCreate))).isEqualTo(BackOutcome.ExitApp)
        assertThat(backOutcome(listOf(HelloBeKey.ProfilePicker(ProfilePickerMode.Launch))))
            .isEqualTo(BackOutcome.ExitApp)
    }

    @Test
    fun givenAProfileIsChosen_whenTheStackIsRebuilt_thenTheLearningPathIsTheOnlyThingOnIt() {
        // The path becomes root, so Back leaves the app rather than returning to a picker the
        // child has already answered.
        assertThat(afterProfileChosen(minh)).containsExactly(root)
    }

    @Test
    fun givenACelebration_whenItIsDone_thenItReturnsToThePathAsRoot() {
        // Every lesson is reached from the path and the path is the root, so completion lands on a
        // single-entry stack. Back from there leaves the app, which is what root means.
        val stack = afterCelebrationDone(minh)

        assertThat(stack).containsExactly(root)
        assertThat(backOutcome(stack)).isEqualTo(BackOutcome.ExitApp)
    }

    @Test
    fun givenACaregiverSession_whenItCloses_thenNothingCaregiverShapedRemains() {
        val stack = afterCaregiverSessionClosed(minh, ChildReturnTarget.FREE_PLAY)

        assertThat(stack.none { it.isCaregiver() }).isTrue()
        assertThat(stack.last()).isEqualTo(HelloBeKey.FreePlay(profileId = minh))
    }

    @Test
    fun givenACaregiverSessionOpenedFromThePath_whenItCloses_thenThePathIsRootAgain() {
        assertThat(afterCaregiverSessionClosed(minh, ChildReturnTarget.LEARNING_PATH))
            .containsExactly(root)
    }

    @Test
    fun givenNoProfileBehindTheSession_whenItCloses_thenTheLaunchPickerTakesOver() {
        // Reachable after deleting the profile the caregiver entered from.
        val stack = afterCaregiverSessionClosed(profileId = null, ChildReturnTarget.LEARNING_PATH)

        assertThat(stack)
            .containsExactly(HelloBeKey.ProfilePicker(mode = ProfilePickerMode.Launch))
    }

    @Test
    fun givenEveryCaregiverDestination_whenItIsClassified_thenItIsBehindTheGate() {
        listOf(
            HelloBeKey.CaregiverDashboard(minh),
            HelloBeKey.CaregiverSettings(minh),
            HelloBeKey.ProfileManagement(minh),
            HelloBeKey.DeleteProfileConfirmation(minh),
            HelloBeKey.ResetProgressConfirmation(minh)
        ).forEach { assertThat(it.isCaregiver()).isTrue() }
    }

    @Test
    fun givenChildDestinationsAndTheGate_whenClassified_thenNoneAreBehindTheGate() {
        // The gate itself is not behind the gate: closing a session must not strand a caregiver
        // on the challenge that opens one.
        listOf(
            root,
            HelloBeKey.FreePlay(minh),
            HelloBeKey.CaregiverGate(minh, ChildReturnTarget.LEARNING_PATH),
            HelloBeKey.ProfileCreate
        ).forEach { assertThat(it.isCaregiver()).isFalse() }
    }

    private companion object {
        val minh = ProfileId("minh")
        val root = HelloBeKey.LearningPath(profileId = minh)
    }
}
