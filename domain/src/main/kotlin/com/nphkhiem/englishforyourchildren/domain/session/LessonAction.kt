package com.nphkhiem.englishforyourchildren.domain.session

import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis

/**
 * Something that happened, addressed to one encounter with one activity.
 *
 * Every action names the activity instance it was meant for. A press that arrives after the child
 * has moved on, or a duplicate of one already handled, is not the same as the press the lesson is
 * waiting for, and saying so in the type is what makes ignoring it impossible to forget.
 */
sealed interface LessonAction {
    val expectedActivityInstanceId: ActivityInstanceId

    /** The child pressed an answer. Whether it was right is decided before this reaches here. */
    data class AnswerChosen(
        override val expectedActivityInstanceId: ActivityInstanceId,
        val correct: Boolean,
        val at: EpochMillis = EpochMillis(0)
    ) : LessonAction

    /** Storage wrote the checkpoint down. [nextInstance] is the encounter that follows. */
    data class CheckpointConfirmed(
        override val expectedActivityInstanceId: ActivityInstanceId,
        val nextInstance: ActivityInstanceId? = null,
        val at: EpochMillis = EpochMillis(0)
    ) : LessonAction

    /** Storage refused. The work stands, unsaved, and the child is told the truth about it. */
    data class CheckpointFailed(override val expectedActivityInstanceId: ActivityInstanceId) :
        LessonAction

    /** Try that write again, with the checkpoint that did not land. */
    data class SaveRetryRequested(override val expectedActivityInstanceId: ActivityInstanceId) :
        LessonAction

    /** Carry on without it. Allowed, as long as nothing claims it was saved. */
    data class ContinueUnsaved(
        override val expectedActivityInstanceId: ActivityInstanceId,
        val nextInstance: ActivityInstanceId? = null
    ) : LessonAction

    /** Play the prompt again. */
    data class PromptReplayRequested(override val expectedActivityInstanceId: ActivityInstanceId) :
        LessonAction

    /** The sound will not play. The lesson goes on without it. */
    data class MediaUnavailable(override val expectedActivityInstanceId: ActivityInstanceId) :
        LessonAction

    /**
     * The child took the fair way past a question that could not be asked properly.
     *
     * Only ever offered while the sound will not play, which is what makes it unscored: nobody
     * skipped a question they had actually heard.
     */
    data class SkipRequested(
        override val expectedActivityInstanceId: ActivityInstanceId,
        val at: EpochMillis = EpochMillis(0)
    ) : LessonAction

    /**
     * The child chose to stay, so the stop question goes away.
     *
     * The sound is deliberately not started again. "Keep learning" means do not end the lesson, not
     * play that again, and a recording restarting on its own is the thing this app refuses to do
     * anywhere else. Replay is right there when a child wants it.
     */
    data class KeepLearningRequested(override val expectedActivityInstanceId: ActivityInstanceId) :
        LessonAction

    /** Back was pressed. Nothing is abandoned until an adult or a child confirms it. */
    data class StopRequested(override val expectedActivityInstanceId: ActivityInstanceId) :
        LessonAction
}
