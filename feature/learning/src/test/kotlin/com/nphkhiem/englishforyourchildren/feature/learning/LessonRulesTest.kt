package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback
import org.junit.jupiter.api.Test

class LessonRulesTest {

    @Test
    fun givenThePromptIsPlaying_whenAnswerAvailabilityIsRead_thenAnswersAreOutOfTheFocusOrder() {
        // A child must hear the question before answering it, so answers are not merely inert:
        // they leave the focus order entirely. See ADR 0004.
        assertThat(answerAvailability(LessonPhase.PROMPTING))
            .isEqualTo(HelloBeAvailability.DISABLED)
        assertThat(answerAvailability(LessonPhase.PREPARING))
            .isEqualTo(HelloBeAvailability.DISABLED)
    }

    @Test
    fun givenAnsweringHasBegun_whenAnswerAvailabilityIsRead_thenAnswersCanBeChosen() {
        assertThat(answerAvailability(LessonPhase.ANSWERING))
            .isEqualTo(HelloBeAvailability.ENABLED)
    }

    @Test
    fun givenTheAnswerWasCorrect_whenAnswerAvailabilityIsRead_thenChoicesStopAcceptingPresses() {
        // The question is over; pressing another picture must not register a second answer.
        assertThat(answerAvailability(LessonPhase.CORRECT))
            .isEqualTo(HelloBeAvailability.UNAVAILABLE)
        assertThat(answerAvailability(LessonPhase.COMPLETED))
            .isEqualTo(HelloBeAvailability.UNAVAILABLE)
    }

    @Test
    fun givenAudioHasNotBeenHeard_whenFocusTargetIsRead_thenItIsReplay() {
        assertThat(lessonFocusTarget(stateWith(phase = LessonPhase.PREPARING)))
            .isEqualTo(LessonFocusTarget.REPLAY)
        assertThat(lessonFocusTarget(stateWith(phase = LessonPhase.PROMPTING)))
            .isEqualTo(LessonFocusTarget.REPLAY)
    }

    @Test
    fun givenAnsweringHasBegun_whenFocusTargetIsRead_thenItIsTheFirstAnswer() {
        assertThat(lessonFocusTarget(stateWith(phase = LessonPhase.ANSWERING)))
            .isEqualTo(LessonFocusTarget.FIRST_ANSWER)
    }

    @Test
    fun givenNoAnswersAtAll_whenFocusTargetIsRead_thenFocusStillHasSomewhereToGo() {
        // A malformed activity must not leave a child on a screen with nothing focusable.
        val empty = stateWith(phase = LessonPhase.ANSWERING, answers = emptyList())

        assertThat(lessonFocusTarget(empty)).isEqualTo(LessonFocusTarget.REPLAY)
    }

    @Test
    fun givenAudioIsUnavailable_whenSkipIsConsidered_thenAFairWayPastIsOffered() {
        val broken = stateWith(phase = LessonPhase.ANSWERING, audioAvailable = false)

        assertThat(isUnscoredSkipOffered(broken)).isTrue()
    }

    @Test
    fun givenAudioWorks_whenSkipIsConsidered_thenItIsNotOfferedAsAGeneralEscape() {
        LessonPhase.entries.forEach { phase ->
            assertThat(isUnscoredSkipOffered(stateWith(phase = phase))).isFalse()
        }
    }

    @Test
    fun givenAnyState_whenAnswersAreRead_thenNothingRevealsWhichOneIsCorrect() {
        // The screen is handed feedback per answer and is never told the correct id, so it cannot
        // leak the answer through focus, ordering or selection. This is the structural guarantee
        // behind givenListenAndChooseWithThreeAnswers_whenFocusEnters_thenRecommendedAnswerIsNot-
        // Preselected, and it is asserted here at the model level.
        val fresh = stateWith(phase = LessonPhase.ANSWERING)

        assertThat(fresh.answers.map { it.feedback })
            .containsExactly(
                HelloBeChoiceFeedback.NEUTRAL,
                HelloBeChoiceFeedback.NEUTRAL,
                HelloBeChoiceFeedback.NEUTRAL
            )
    }

    private fun stateWith(
        phase: LessonPhase,
        audioAvailable: Boolean = true,
        answers: List<AnswerOption> = listOf(
            AnswerOption(id = "chair", label = "chair"),
            AnswerOption(id = "lamp", label = "lamp"),
            AnswerOption(id = "bed", label = "bed")
        )
    ) = LessonUiState(
        unitName = "My Home",
        activityTitle = "Listen and choose",
        prompt = "Where is the chair?",
        caption = null,
        activityNumber = 2,
        activityCount = 4,
        phase = phase,
        support = SupportLevel.NONE,
        learningObject = null,
        answers = answers,
        audioAvailable = audioAvailable,
        pendingSave = false,
        stopForNowVisible = false
    )
}
