package com.nphkhiem.englishforyourchildren.domain.session

import com.nphkhiem.englishforyourchildren.domain.model.ActivityContent
import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.repository.CompleteSession
import com.nphkhiem.englishforyourchildren.domain.repository.PersistCheckpoint

/**
 * Everything a lesson does, as a function.
 *
 * No clock, no database, no player and no randomness: a state and an action go in, a state and a
 * list of things to do come out. Every rule a child meets is therefore checkable by writing down a
 * table, which is what its test is.
 *
 * The two rules worth stating outright, because everything else follows from them:
 *
 * A completed activity is the smallest durable checkpoint, so answering does not move the child on.
 * It asks for a write and waits. Only a confirmed write exposes the next question, because showing
 * one before the last is written down is how a child loses work they had already done.
 *
 * A refused write does not move them either, but it does not trap them. They may carry on, and the
 * checkpoint travels with them unsaved so that nothing on screen claims otherwise.
 */
class LessonReducer {

    fun reduce(state: LessonSessionState, action: LessonAction): LessonReduction {
        // A press meant for an activity the child has left, or a duplicate of one already handled,
        // is not the press this lesson is waiting for.
        if (action.expectedActivityInstanceId != state.currentInstance) return unchanged(state)
        if (state.phase == LessonPhase.Finished) return unchanged(state)

        return when (action) {
            is LessonAction.AnswerChosen -> answer(state, action)

            is LessonAction.CheckpointConfirmed -> confirmed(state, action)

            is LessonAction.CheckpointFailed -> failed(state)

            is LessonAction.SaveRetryRequested -> retry(state)

            is LessonAction.ContinueUnsaved -> continueUnsaved(state, action)

            is LessonAction.PromptReplayRequested -> ask(state)

            is LessonAction.PromptFinished -> promptFinished(state, action)

            is LessonAction.MediaUnavailable ->
                // A lesson that has found out it is silent is not sounding anything. Leaving it set
                // would lock the answers away with no recording left to open them.
                unchanged(state.copy(audioAvailable = false, soundingPrompt = null))

            is LessonAction.RepetitionFinished -> finishRepetition(state, action)

            is LessonAction.SkipRequested -> skip(state, action)

            is LessonAction.KeepLearningRequested ->
                unchanged(state.copy(stopRequested = false))

            is LessonAction.StopRequested -> LessonReduction(
                state = state.copy(stopRequested = true),
                effects = listOf(LessonEffect.PausePlayback)
            )
        }
    }

    private fun answer(
        state: LessonSessionState,
        action: LessonAction.AnswerChosen
    ): LessonReduction {
        // Already answered and waiting for the write. A second press must not record a second
        // attempt, which on a television is one button held a moment too long.
        if (state.phase == LessonPhase.AwaitingCheckpoint) return unchanged(state)

        // The question is still being asked. See ADR 0004: the screen has already taken the
        // answers out of the focus order, and this is what makes that a guarantee rather than an
        // arrangement of controls.
        if (state.soundingPrompt != null) return unchanged(state)

        if (!action.correct && state.audioAvailable) {
            // Never wrong, never a penalty: the same activity again with more help.
            return unchanged(
                state.copy(
                    supportLevel = minOf(
                        state.supportLevel + 1,
                        LessonSessionState.MAX_SUPPORT_LEVEL
                    )
                )
            )
        }

        // A question the child could not hear is not one they got wrong, so it is recorded as an
        // unscored skip and costs them nothing.
        val outcome = when {
            !state.audioAvailable -> AttemptOutcome.UNSCORED_SKIP
            state.supportLevel > 0 -> AttemptOutcome.SUPPORTIVE_RETRY
            else -> AttemptOutcome.CORRECT
        }

        return record(state, outcome, action.at)
    }

    /**
     * The unscored skip.
     *
     * Refused while the sound works, because the control is only ever offered while it does not. A
     * press that arrives at any other moment is for something this child was never shown, and
     * honouring it would turn a question they could hear into one that counted for nothing.
     */
    private fun skip(
        state: LessonSessionState,
        action: LessonAction.SkipRequested
    ): LessonReduction {
        if (state.audioAvailable) return unchanged(state)
        if (state.phase == LessonPhase.AwaitingCheckpoint) return unchanged(state)

        return record(state, AttemptOutcome.UNSCORED_SKIP, action.at)
    }

    /**
     * Saying it with Pip, finished.
     *
     * Refused on anything else, because every other family is answered rather than worked through,
     * and a press arriving from the wrong screen must not record a child as having practised
     * something they were being asked to choose.
     */
    private fun finishRepetition(
        state: LessonSessionState,
        action: LessonAction.RepetitionFinished
    ): LessonReduction {
        if (state.phase == LessonPhase.AwaitingCheckpoint) return unchanged(state)
        if (state.currentActivity.content !is ActivityContent.GuidedRepetition) {
            return unchanged(state)
        }

        // Pip has not finished asking. This family has no answer, so ADR 0004's rule about answers
        // does not reach it, and without this a child could finish saying a word unprompted.
        if (state.soundingPrompt != null) return unchanged(state)

        return record(state, AttemptOutcome.PRACTISED, action.at)
    }

    /** Ask for the write, and wait. Nothing moves the child on until storage confirms it. */
    private fun record(
        state: LessonSessionState,
        outcome: AttemptOutcome,
        at: EpochMillis
    ): LessonReduction = LessonReduction(
        state = state.copy(phase = LessonPhase.AwaitingCheckpoint),
        effects = listOf(
            LessonEffect.Persist(
                PersistCheckpoint(
                    sessionId = state.sessionId,
                    profileId = state.profileId,
                    courseVersion = state.courseVersion,
                    lessonId = state.lesson.id,
                    activityId = state.currentActivity.id,
                    activityInstanceId = state.currentInstance,
                    activityOrdinal = state.currentActivity.ordinal,
                    outcome = outcome,
                    completedAt = at
                )
            )
        )
    )

    private fun confirmed(
        state: LessonSessionState,
        action: LessonAction.CheckpointConfirmed
    ): LessonReduction {
        if (state.phase != LessonPhase.AwaitingCheckpoint) return unchanged(state)

        if (state.isLastActivity) {
            return LessonReduction(
                state = state.copy(phase = LessonPhase.Finished, saveStatus = SaveStatus.Saved),
                effects = listOf(
                    LessonEffect.Complete(
                        CompleteSession(sessionId = state.sessionId, completedAt = action.at)
                    )
                )
            )
        }

        val next = action.nextInstance ?: return unchanged(state)
        val moved = state.copy(
            activityIndex = state.activityIndex + 1,
            currentInstance = next,
            phase = LessonPhase.Asking,
            saveStatus = SaveStatus.Saved,
            supportLevel = 0
        )
        return ask(moved)
    }

    private fun failed(state: LessonSessionState): LessonReduction {
        if (state.phase != LessonPhase.AwaitingCheckpoint) return unchanged(state)

        // The checkpoint that did not land is kept whole, so a retry sends exactly what happened
        // rather than something rebuilt from a later moment.
        val pending = PersistCheckpoint(
            sessionId = state.sessionId,
            profileId = state.profileId,
            courseVersion = state.courseVersion,
            lessonId = state.lesson.id,
            activityId = state.currentActivity.id,
            activityInstanceId = state.currentInstance,
            activityOrdinal = state.currentActivity.ordinal,
            outcome = AttemptOutcome.CORRECT,
            completedAt = EpochMillis(0)
        )
        val existing = (state.saveStatus as? SaveStatus.Unsaved)?.pending
        return unchanged(
            state.copy(
                phase = LessonPhase.Asking,
                saveStatus = SaveStatus.Unsaved(existing ?: pending)
            )
        )
    }

    private fun retry(state: LessonSessionState): LessonReduction {
        val unsaved = state.saveStatus as? SaveStatus.Unsaved ?: return unchanged(state)
        return LessonReduction(
            state = state.copy(phase = LessonPhase.AwaitingCheckpoint),
            effects = listOf(LessonEffect.Persist(unsaved.pending))
        )
    }

    private fun continueUnsaved(
        state: LessonSessionState,
        action: LessonAction.ContinueUnsaved
    ): LessonReduction {
        if (state.saveStatus !is SaveStatus.Unsaved) return unchanged(state)
        if (state.isLastActivity) return unchanged(state.copy(phase = LessonPhase.Finished))

        val next = action.nextInstance ?: return unchanged(state)
        // The pending checkpoint travels with them. Clearing it here is what would turn carrying on
        // into a quiet claim that the work was saved.
        val moved = state.copy(
            activityIndex = state.activityIndex + 1,
            currentInstance = next,
            phase = LessonPhase.Asking,
            supportLevel = 0
        )
        return ask(moved)
    }

    /**
     * The question has been asked in full.
     *
     * Only what the reducer actually started can end it. A recording the lesson never asked for,
     * or the previous question's, leaves the state alone rather than opening the answers early.
     */
    private fun promptFinished(
        state: LessonSessionState,
        action: LessonAction.PromptFinished
    ): LessonReduction {
        if (action.assetId != state.soundingPrompt) return unchanged(state)
        return unchanged(state.copy(soundingPrompt = null))
    }

    private fun unchanged(state: LessonSessionState) =
        LessonReduction(state = state, effects = emptyList())

    companion object {
        /**
         * A lesson opened where the child actually is, with nothing pending.
         *
         * [activityIndex] is a resumed position rather than always zero, because a child who
         * stopped part way through was promised they could come back to it.
         */
        fun start(
            sessionId: SessionId,
            profileId: ProfileId,
            courseVersion: CourseVersion,
            lesson: Lesson,
            activityIndex: Int,
            currentInstance: ActivityInstanceId,
            startedAt: EpochMillis
        ): LessonReduction = LessonSessionState(
            sessionId = sessionId,
            profileId = profileId,
            courseVersion = courseVersion,
            lesson = lesson,
            activityIndex = activityIndex,
            currentInstance = currentInstance,
            phase = LessonPhase.Asking,
            saveStatus = SaveStatus.Saved,
            supportLevel = 0,
            audioAvailable = true,
            soundingPrompt = null,
            stopRequested = false
        )
            .also { require(startedAt.value >= 0) { "A lesson cannot start before the epoch" } }
            .let { ask(it) }

        /**
         * The question speaks itself, and the lesson remembers that it is speaking.
         *
         * The one way a prompt starts, whether the lesson asked on arrival or the child pressed
         * replay. Both put the lesson in the same state, so there is one meaning for playing a
         * prompt rather than two that can drift apart. See ADR 0004.
         *
         * Nothing is asked for once a lesson has found out it is running silent: it has already
         * told the child so, the words are on screen, and asking again for every remaining question
         * would only produce the same failure five more times.
         */
        private fun ask(state: LessonSessionState): LessonReduction {
            if (!state.audioAvailable) return LessonReduction(state, emptyList())
            val spoken = state.spokenPrompt
            if (spoken.isEmpty()) return LessonReduction(state, emptyList())
            return LessonReduction(
                // The last clip, not the first. A question is asked when the thing it is about has
                // been said, so the stem finishing means nothing and the word finishing is the
                // moment the answers may be reached.
                state = state.copy(soundingPrompt = spoken.last()),
                effects = listOf(LessonEffect.Play(spoken))
            )
        }
    }
}
