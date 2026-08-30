package com.nphkhiem.englishforyourchildren.domain.session

import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
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

            is LessonAction.PromptReplayRequested -> replay(state)

            is LessonAction.MediaUnavailable -> unchanged(state.copy(audioAvailable = false))

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

        val checkpoint = PersistCheckpoint(
            sessionId = state.sessionId,
            profileId = state.profileId,
            courseVersion = state.courseVersion,
            lessonId = state.lesson.id,
            activityId = state.currentActivity.id,
            activityInstanceId = state.currentInstance,
            activityOrdinal = state.currentActivity.ordinal,
            outcome = outcome,
            completedAt = action.at
        )
        return LessonReduction(
            state = state.copy(phase = LessonPhase.AwaitingCheckpoint),
            effects = listOf(LessonEffect.Persist(checkpoint))
        )
    }

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
        return unchanged(
            state.copy(
                activityIndex = state.activityIndex + 1,
                currentInstance = next,
                phase = LessonPhase.Asking,
                saveStatus = SaveStatus.Saved,
                supportLevel = 0
            )
        )
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
        return unchanged(
            state.copy(
                activityIndex = state.activityIndex + 1,
                currentInstance = next,
                phase = LessonPhase.Asking,
                supportLevel = 0
            )
        )
    }

    private fun replay(state: LessonSessionState): LessonReduction {
        val asset = state.promptAsset ?: return unchanged(state)
        return LessonReduction(state = state, effects = listOf(LessonEffect.Play(asset)))
    }

    private fun unchanged(state: LessonSessionState) =
        LessonReduction(state = state, effects = emptyList())

    companion object {
        /** A lesson at its first activity, with nothing behind it and nothing pending. */
        fun start(
            sessionId: SessionId,
            profileId: ProfileId,
            courseVersion: CourseVersion,
            lesson: Lesson,
            firstInstance: ActivityInstanceId,
            promptAsset: AssetId?,
            startedAt: EpochMillis
        ) = LessonSessionState(
            sessionId = sessionId,
            profileId = profileId,
            courseVersion = courseVersion,
            lesson = lesson,
            activityIndex = 0,
            currentInstance = firstInstance,
            phase = LessonPhase.Asking,
            saveStatus = SaveStatus.Saved,
            supportLevel = 0,
            audioAvailable = true,
            stopRequested = false,
            promptAsset = promptAsset
        ).also { require(startedAt.value >= 0) { "A lesson cannot start before the epoch" } }
    }
}
