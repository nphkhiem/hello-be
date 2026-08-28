package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ChildHomeRulesTest {

    @Test
    fun givenACheckpoint_whenSlotsAreDecided_thenContinueIsDominant() {
        val layout = homeDestinations(HomePrimary.Resume(context = "My Home"))

        assertThat(layout.dominant).isEqualTo(HomeTarget.CONTINUE)
        assertThat(layout.secondaries)
            .containsExactly(HomeTarget.LEARNING_PATH, HomeTarget.FREE_PLAY).inOrder()
    }

    @Test
    fun givenNoCheckpointYet_whenSlotsAreDecided_thenTheDominantSlotIsStillLearning() {
        val layout = homeDestinations(HomePrimary.StartFirstAdventure)

        assertThat(layout.dominant).isEqualTo(HomeTarget.CONTINUE)
    }

    @Test
    fun givenTheCourseIsFinished_whenSlotsAreDecided_thenFreePlayIsPromoted() {
        val layout = homeDestinations(HomePrimary.CourseComplete)

        assertThat(layout.dominant).isEqualTo(HomeTarget.FREE_PLAY)
    }

    @Test
    fun givenTheCourseIsFinished_whenSlotsAreDecided_thenFreePlayAppearsExactlyOnce() {
        // Promoting it without dropping it would offer the same destination twice.
        val layout = homeDestinations(HomePrimary.CourseComplete)

        val everywhere = layout.secondaries + layout.dominant

        assertThat(everywhere.count { it == HomeTarget.FREE_PLAY }).isEqualTo(1)
        assertThat(layout.secondaries).containsExactly(HomeTarget.LEARNING_PATH)
    }

    @Test
    fun givenTheCheckpointWillNotOpen_whenAvailabilityIsRead_thenTheDominantActionIsClosed() {
        assertThat(isDominantAvailable(HomePrimary.ResumeUnavailable(context = "My Home")))
            .isFalse()
    }

    @Test
    fun givenEveryOtherPrimary_whenAvailabilityIsRead_thenTheDominantActionIsOpen() {
        listOf(
            HomePrimary.Resume(context = "My Home"),
            HomePrimary.StartFirstAdventure,
            HomePrimary.CourseComplete
        ).forEach { primary ->
            assertThat(isDominantAvailable(primary)).isTrue()
        }
    }
}
