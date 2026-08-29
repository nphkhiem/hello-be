package com.nphkhiem.englishforyourchildren.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BackStackTest {

    @Test
    fun givenSomethingBeneath_whenBackIsPressed_thenItGoesBackOne() {
        val stack = listOf(home, HelloBeKey.LearningPath(profileId = minh))

        assertThat(backOutcome(stack)).isEqualTo(BackOutcome.Pop)
    }

    @Test
    fun givenTheRoot_whenBackIsPressed_thenItLeavesTheApp() {
        // Child home, the launch picker and profile create are all root in some launch, and the
        // rule is one rule: root is the destination with nothing beneath it.
        assertThat(backOutcome(listOf(home))).isEqualTo(BackOutcome.ExitApp)
        assertThat(backOutcome(listOf(HelloBeKey.ProfileCreate))).isEqualTo(BackOutcome.ExitApp)
        assertThat(backOutcome(listOf(HelloBeKey.ProfilePicker(ProfilePickerMode.Launch))))
            .isEqualTo(BackOutcome.ExitApp)
    }

    @Test
    fun givenAProfileIsChosen_whenTheStackIsRebuilt_thenChildHomeIsTheOnlyThingOnIt() {
        // Child home becomes root, so Back leaves the app rather than returning to a picker the
        // child has already answered.
        assertThat(afterProfileChosen(minh)).containsExactly(home)
    }

    @Test
    fun givenACelebrationFromTheLearningPath_whenItIsDone_thenItReturnsToThePath() {
        val stack = afterCelebrationDone(minh, LessonReturnTarget.LEARNING_PATH)

        assertThat(stack).containsExactly(home, HelloBeKey.LearningPath(profileId = minh))
            .inOrder()
        assertThat(backOutcome(stack)).isEqualTo(BackOutcome.Pop)
    }

    @Test
    fun givenACelebrationFromChildHome_whenItIsDone_thenItReturnsThere() {
        assertThat(afterCelebrationDone(minh, LessonReturnTarget.CHILD_HOME))
            .containsExactly(home)
    }

    @Test
    fun givenACaregiverSession_whenItCloses_thenNothingCaregiverShapedRemains() {
        val stack = afterCaregiverSessionClosed(minh, ChildReturnTarget.FREE_PLAY)

        assertThat(stack.none { it.isCaregiver() }).isTrue()
        assertThat(stack.last()).isEqualTo(HelloBeKey.FreePlay(profileId = minh))
    }

    @Test
    fun givenACaregiverSessionOpenedFromChildHome_whenItCloses_thenHomeIsRootAgain() {
        assertThat(afterCaregiverSessionClosed(minh, ChildReturnTarget.CHILD_HOME))
            .containsExactly(home)
    }

    @Test
    fun givenNoProfileBehindTheSession_whenItCloses_thenTheLaunchPickerTakesOver() {
        // Reachable after deleting the profile the caregiver entered from.
        val stack = afterCaregiverSessionClosed(profileId = null, ChildReturnTarget.CHILD_HOME)

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
            home,
            HelloBeKey.LearningPath(minh),
            HelloBeKey.FreePlay(minh),
            HelloBeKey.CaregiverGate(minh, ChildReturnTarget.CHILD_HOME),
            HelloBeKey.ProfileCreate
        ).forEach { assertThat(it.isCaregiver()).isFalse() }
    }

    private companion object {
        val minh = ProfileId("minh")
        val home = HelloBeKey.ChildHome(profileId = minh)
    }
}
