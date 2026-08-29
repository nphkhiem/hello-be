package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import org.junit.jupiter.api.Test

class LearningPathRulesTest {

    @Test
    fun givenALessonStillAhead_whenAvailabilityIsRead_thenFocusSkipsIt() {
        val node = node(id = FIRST, progress = LessonProgress.FUTURE)

        assertThat(lessonAvailability(node)).isEqualTo(HelloBeAvailability.DISABLED)
    }

    @Test
    fun givenALessonThatWillNotLoad_whenAvailabilityIsRead_thenItKeepsFocusToSaySo() {
        val node = node(id = FIRST, progress = LessonProgress.AVAILABLE, openable = false)

        assertThat(lessonAvailability(node)).isEqualTo(HelloBeAvailability.UNAVAILABLE)
    }

    @Test
    fun givenALessonStillAheadThatWillNotLoad_whenAvailabilityIsRead_thenBeingLaterWins() {
        // Nothing to announce about a card focus never reaches, so the two rules cannot both apply.
        val node = node(id = FIRST, progress = LessonProgress.FUTURE, openable = false)

        assertThat(lessonAvailability(node)).isEqualTo(HelloBeAvailability.DISABLED)
    }

    @Test
    fun givenAFinishedOrOfferedLesson_whenAvailabilityIsRead_thenItOpens() {
        listOf(
            LessonProgress.COMPLETED,
            LessonProgress.RECOMMENDED,
            LessonProgress.AVAILABLE
        ).forEach { progress ->
            assertThat(lessonAvailability(node(id = FIRST, progress = progress)))
                .isEqualTo(HelloBeAvailability.ENABLED)
        }
    }

    @Test
    fun givenAUnitWithARecommendation_whenTheOpeningLessonIsChosen_thenItIsTheRecommendedOne() {
        val unit = unit(
            node(id = FIRST, progress = LessonProgress.COMPLETED),
            node(id = SECOND, progress = LessonProgress.RECOMMENDED),
            node(id = THIRD, progress = LessonProgress.FUTURE)
        )

        assertThat(recommendedLessonId(unit)).isEqualTo(SECOND)
    }

    @Test
    fun givenARecommendationLaterInTheUnit_whenTheOpeningLessonIsChosen_thenItBeatsEarlierOnes() {
        // The claim is that the recommendation wins, not that the first reachable lesson does.
        val unit = unit(
            node(id = FIRST, progress = LessonProgress.COMPLETED),
            node(id = SECOND, progress = LessonProgress.AVAILABLE),
            node(id = THIRD, progress = LessonProgress.RECOMMENDED)
        )

        assertThat(recommendedLessonId(unit)).isEqualTo(THIRD)
    }

    @Test
    fun givenNoRecommendation_whenTheOpeningLessonIsChosen_thenItIsTheFirstFocusCanReach() {
        val unit = unit(
            node(id = FIRST, progress = LessonProgress.FUTURE),
            node(id = SECOND, progress = LessonProgress.COMPLETED)
        )

        assertThat(recommendedLessonId(unit)).isEqualTo(SECOND)
    }

    @Test
    fun givenNothingReachable_whenTheOpeningLessonIsChosen_thenThereIsNone() {
        val unit = unit(
            node(id = FIRST, progress = LessonProgress.FUTURE),
            node(id = SECOND, progress = LessonProgress.FUTURE)
        )

        assertThat(recommendedLessonId(unit)).isNull()
    }

    @Test
    fun givenNoUnitAtAll_whenRecoveryIsRead_thenTheChildNeedsAWayOut() {
        assertThat(isRecovering(state(unit = null))).isTrue()
    }

    @Test
    fun givenAUnitWithNoLessons_whenRecoveryIsRead_thenTheChildNeedsAWayOut() {
        assertThat(isRecovering(state(unit = unit()))).isTrue()
    }

    @Test
    fun givenAUnitWithLessons_whenRecoveryIsRead_thenThePathIsDrawnNormally() {
        val state = state(unit = unit(node(id = FIRST, progress = LessonProgress.RECOMMENDED)))

        assertThat(isRecovering(state)).isFalse()
    }

    @Test
    fun givenNoUnit_whenEntryFocusIsChosen_thenItGoesToTheWayOut() {
        assertThat(pathFocusTarget(state(unit = null))).isEqualTo(PathFocusTarget.RECOVERY)
    }

    @Test
    fun givenAnOrdinaryUnit_whenEntryFocusIsChosen_thenItGoesToTheRecommendedLesson() {
        val state = state(unit = unit(node(id = FIRST, progress = LessonProgress.RECOMMENDED)))

        assertThat(pathFocusTarget(state)).isEqualTo(PathFocusTarget.RECOMMENDED_LESSON)
    }

    @Test
    fun givenAUnitOfLessonsStillAhead_whenEntryFocusIsChosen_thenItGoesToTheStepper() {
        // Focus has to land somewhere, and the stepper is the one control that still works.
        val state = state(unit = unit(node(id = FIRST, progress = LessonProgress.FUTURE)))

        assertThat(pathFocusTarget(state)).isEqualTo(PathFocusTarget.UNIT_STEPPER)
    }

    private fun node(
        id: String,
        progress: LessonProgress,
        openable: Boolean = true
    ): LessonNodeState = LessonNodeState(
        id = id,
        title = id,
        progress = progress,
        kind = LessonKind.PRACTICE,
        openable = openable
    )

    private fun unit(vararg lessons: LessonNodeState): UnitPageState = UnitPageState(
        unitId = "u1",
        unitNumber = 1,
        unitCount = 12,
        theme = "My Body",
        objective = "Five little adventures",
        lessons = lessons.toList()
    )

    private fun state(unit: UnitPageState?): LearningPathUiState = LearningPathUiState(
        profileName = "Minh",
        profileAvatar = "M",
        unit = unit,
        previousUnit = null,
        nextUnit = null,
        pendingSave = false
    )

    private companion object {
        const val FIRST = "eyes-and-ears"
        const val SECOND = "nose-and-mouth"
        const val THIRD = "hands-and-feet"
    }
}
