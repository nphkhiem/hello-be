package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.annotation.StringRes
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipPose

/**
 * Where a lesson puts focus when it becomes interactive.
 *
 * [CONTENT] was called FIRST_ANSWER while every family had answers. Say with Pip has controls and
 * no answers, so the old name described something that is no longer always there.
 */
enum class LessonFocusTarget {
    REPLAY,
    CONTENT
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

    // Say with Pip has no answers to enable. Any later family that reaches this phase carrying
    // choices should have to think about it rather than inherit whatever this returns.
    LessonPhase.RESPONDING -> HelloBeAvailability.DISABLED

    LessonPhase.CORRECT, LessonPhase.COMPLETED -> HelloBeAvailability.UNAVAILABLE
}

/**
 * Where focus belongs when the lesson becomes interactive.
 *
 * Replay until the question has been heard, then the activity's own entry control. An activity
 * that arrives with no answers still sends focus to replay, so a malformed activity never leaves a
 * child on a screen with nothing to focus.
 *
 * Say with Pip is the exception that needs naming: it reaches [LessonPhase.RESPONDING] with two
 * controls and an empty answer list, so the answer count cannot decide it. That phase is reachable
 * only in that family, which always has its controls by then, so the branch cannot strand focus.
 */
internal fun lessonFocusTarget(state: LessonUiState): LessonFocusTarget = when {
    state.phase == LessonPhase.RESPONDING -> LessonFocusTarget.CONTENT

    answerAvailability(state.phase) == HelloBeAvailability.ENABLED &&
        state.answers.isNotEmpty() -> LessonFocusTarget.CONTENT

    else -> LessonFocusTarget.REPLAY
}

/**
 * Whether the unscored skip is offered.
 *
 * Only when audio is unavailable. The brief allows a fair way past a question that would otherwise
 * be unfair, which is not the same as a general way out of anything hard.
 */
internal fun isUnscoredSkipOffered(state: LessonUiState): Boolean = !state.audioAvailable

/**
 * How Pip stands for the current moment.
 *
 * The pose follows the rung of the support ladder the state carries, so Pip visibly does more as
 * a child struggles, and celebrates when they get there. Pip never has a pose for disappointment.
 */
internal fun pipPoseFor(phase: LessonPhase, support: SupportLevel): PipPose = when {
    phase == LessonPhase.CORRECT || phase == LessonPhase.COMPLETED -> PipPose.CELEBRATING
    support == SupportLevel.SLOWER || support == SupportLevel.VIETNAMESE -> PipPose.MODELING
    support == SupportLevel.REPEAT -> PipPose.POINTING
    phase == LessonPhase.PROMPTING -> PipPose.GREETING
    else -> PipPose.RESTING
}

/**
 * What a child using a screen reader hears in place of seeing Pip's pose, so the same information
 * reaches them.
 */
@StringRes
internal fun pipDescriptionFor(phase: LessonPhase, support: SupportLevel): Int = when {
    phase == LessonPhase.CORRECT || phase == LessonPhase.COMPLETED ->
        R.string.lesson_pip_celebrating

    support == SupportLevel.VIETNAMESE -> R.string.lesson_pip_helping

    support == SupportLevel.SLOWER -> R.string.lesson_pip_demonstrating

    support == SupportLevel.REPEAT -> R.string.lesson_pip_repeating

    phase == LessonPhase.PROMPTING -> R.string.lesson_pip_asking

    else -> R.string.lesson_pip_waiting
}
