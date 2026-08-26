package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability

/** Where a lesson puts focus when it becomes interactive. */
enum class LessonFocusTarget {
    REPLAY,
    FIRST_ANSWER
}

/**
 * Whether a child may reach and choose the answers yet.
 *
 * While the prompt plays the answers leave the focus order entirely rather than sitting there
 * inert, because a press that silently does nothing cannot be interpreted by a child who cannot
 * read an explanation. Once the question is over they stay visible but stop accepting presses, so
 * a second press cannot register a second answer. See ADR 0004.
 */
internal fun answerAvailability(phase: LessonPhase): HelloBeAvailability = when (phase) {
    LessonPhase.PREPARING, LessonPhase.PROMPTING -> HelloBeAvailability.DISABLED
    LessonPhase.ANSWERING -> HelloBeAvailability.ENABLED
    LessonPhase.CORRECT, LessonPhase.COMPLETED -> HelloBeAvailability.UNAVAILABLE
}

/**
 * Where focus belongs when the lesson becomes interactive.
 *
 * Replay until the question has been heard, then the first answer. An activity that arrives with
 * no answers still sends focus to replay, so a malformed activity never leaves a child on a screen
 * with nothing to focus.
 */
internal fun lessonFocusTarget(state: LessonUiState): LessonFocusTarget =
    if (answerAvailability(state.phase) == HelloBeAvailability.ENABLED &&
        state.answers.isNotEmpty()
    ) {
        LessonFocusTarget.FIRST_ANSWER
    } else {
        LessonFocusTarget.REPLAY
    }

/**
 * Whether the unscored skip is offered.
 *
 * Only when audio is unavailable. The brief allows a fair way past a question that would otherwise
 * be unfair, which is not the same as a general way out of anything hard.
 */
internal fun isUnscoredSkipOffered(state: LessonUiState): Boolean = !state.audioAvailable
