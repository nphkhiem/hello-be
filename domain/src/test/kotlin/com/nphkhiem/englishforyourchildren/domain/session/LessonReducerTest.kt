package com.nphkhiem.englishforyourchildren.domain.session

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.Activity
import com.nphkhiem.englishforyourchildren.domain.model.ActivityContent
import com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.AnswerChoice
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import org.junit.jupiter.api.Test

/**
 * The reducer is the whole of a lesson's behaviour with none of its scenery, so this is a
 * transition table rather than a story: for each state and action, what comes out.
 */
class LessonReducerTest {
    private val reducer = LessonReducer()

    @Test
    fun givenAFreshLesson_whenItStarts_thenTheChildIsOnTheFirstActivityAndNothingIsPending() {
        val state = start()

        assertThat(state.activityIndex).isEqualTo(0)
        assertThat(state.phase).isEqualTo(LessonPhase.Asking)
        assertThat(state.saveStatus).isEqualTo(SaveStatus.Saved)
    }

    @Test
    fun givenAnActivity_whenItIsAnsweredCorrectly_thenNothingMovesUntilTheSaveIsConfirmed() {
        // A completed activity is the smallest durable checkpoint. Showing the next question before
        // the last one is written down is how a child loses work they had already done.
        val reduction = reducer.reduce(start(), answer(correct = true))

        assertThat(reduction.state.phase).isEqualTo(LessonPhase.AwaitingCheckpoint)
        assertThat(reduction.state.activityIndex).isEqualTo(0)
        assertThat(reduction.effects.filterIsInstance<LessonEffect.Persist>()).hasSize(1)
    }

    @Test
    fun givenACorrectAnswer_whenItIsPersisted_thenTheCheckpointSaysWhatActuallyHappened() {
        val reduction = reducer.reduce(start(), answer(correct = true))

        val persist = reduction.effects.filterIsInstance<LessonEffect.Persist>().single().command
        assertThat(persist.outcome).isEqualTo(AttemptOutcome.CORRECT)
        assertThat(persist.activityOrdinal).isEqualTo(0)
        assertThat(persist.activityInstanceId).isEqualTo(ActivityInstanceId(FIRST_INSTANCE))
    }

    @Test
    fun givenAWrongAnswer_whenItArrives_thenItIsASupportiveRetryAndTheChildStaysWhereTheyAre() {
        // Never wrong, never a penalty. The child tries the same activity again with more help.
        val reduction = reducer.reduce(start(), answer(correct = false))

        assertThat(reduction.state.phase).isEqualTo(LessonPhase.Asking)
        assertThat(reduction.state.activityIndex).isEqualTo(0)
        assertThat(reduction.state.supportLevel).isEqualTo(1)
        assertThat(reduction.effects).isEmpty()
    }

    @Test
    fun givenRepeatedWrongAnswers_whenTheyArrive_thenHelpEscalatesButNeverPastTheLastRung() {
        var state = start()
        repeat(5) { state = reducer.reduce(state, answer(correct = false)).state }

        assertThat(state.supportLevel).isEqualTo(MAX_SUPPORT)
    }

    @Test
    fun givenTheSameAnswerTwice_whenTheSecondArrives_thenNothingHappensASecondTime() {
        // A child pressing Select twice on a television must not record two attempts, and must not
        // persist the same activity twice.
        val awaiting = reducer.reduce(start(), answer(correct = true)).state

        val again = reducer.reduce(awaiting, answer(correct = true))

        assertThat(again.state).isEqualTo(awaiting)
        assertThat(again.effects).isEmpty()
    }

    @Test
    fun givenAnActionForAnActivityAlreadyLeft_whenItArrives_thenItIsIgnored() {
        // A late press from the previous activity, or a replayed event, must not answer the
        // question the child is looking at now.
        val confirmed = confirmFirst()

        val stale = reducer.reduce(
            confirmed,
            LessonAction.AnswerChosen(
                expectedActivityInstanceId = ActivityInstanceId(FIRST_INSTANCE),
                correct = true
            )
        )

        assertThat(stale.state).isEqualTo(confirmed)
        assertThat(stale.effects).isEmpty()
    }

    @Test
    fun givenAPendingCheckpoint_whenStorageConfirmsIt_thenTheNextActivityIsExposed() {
        val confirmed = confirmFirst()

        assertThat(confirmed.activityIndex).isEqualTo(1)
        assertThat(confirmed.phase).isEqualTo(LessonPhase.Asking)
        assertThat(confirmed.saveStatus).isEqualTo(SaveStatus.Saved)
        assertThat(confirmed.supportLevel).isEqualTo(0)
    }

    @Test
    fun givenAPendingCheckpoint_whenStorageRefuses_thenTheChildIsNotMovedOnByItself() {
        // The failure is not the child's to solve, but nothing may claim the work was saved. They
        // stay where they are until someone chooses to carry on or to try again.
        val awaiting = reducer.reduce(start(), answer(correct = true)).state

        val failed = reducer.reduce(
            awaiting,
            LessonAction.CheckpointFailed(instance(FIRST_INSTANCE))
        )

        assertThat(failed.state.activityIndex).isEqualTo(0)
        assertThat(failed.state.saveStatus).isInstanceOf(SaveStatus.Unsaved::class.java)
    }

    @Test
    fun givenAFailedSave_whenTheChildCarriesOn_thenTheNextActivityIsExposedAndStillSaysUnsaved() {
        // Carrying on is allowed. Pretending it was saved is not, so the pending checkpoint travels
        // with them and the screen keeps saying so.
        val failed = failFirst()

        val carried = reducer.reduce(
            failed,
            LessonAction.ContinueUnsaved(instance(FIRST_INSTANCE), instance(SECOND_INSTANCE))
        )

        assertThat(carried.state.activityIndex).isEqualTo(1)
        assertThat(carried.state.saveStatus).isInstanceOf(SaveStatus.Unsaved::class.java)
    }

    @Test
    fun givenAFailedSave_whenItIsRetried_thenTheSameCheckpointIsSentAgainUnchanged() {
        // The retry must write what actually happened, not what is happening now. Rebuilding the
        // command from current state would record the wrong activity or a later time.
        val failed = failFirst()
        val original = (failed.saveStatus as SaveStatus.Unsaved).pending

        val retried = reducer.reduce(
            failed,
            LessonAction.SaveRetryRequested(instance(FIRST_INSTANCE))
        )

        val resent = retried.effects.filterIsInstance<LessonEffect.Persist>().single().command
        assertThat(resent).isEqualTo(original)
    }

    @Test
    fun givenTheLastActivity_whenItsCheckpointIsConfirmed_thenTheLessonIsFinished() {
        var state = start(activities = 2)
        state = reducer.reduce(state, answer(correct = true)).state
        state =
            reducer.reduce(
                state,
                LessonAction.CheckpointConfirmed(
                    instance(FIRST_INSTANCE),
                    instance(SECOND_INSTANCE)
                )
            ).state
        state = reducer.reduce(state, answer(instance = SECOND_INSTANCE, correct = true)).state

        val done = reducer.reduce(
            state,
            LessonAction.CheckpointConfirmed(instance(SECOND_INSTANCE))
        )

        assertThat(done.state.phase).isEqualTo(LessonPhase.Finished)
        assertThat(done.effects.filterIsInstance<LessonEffect.Complete>()).hasSize(1)
    }

    @Test
    fun givenSoundIsUnavailable_whenItIsReported_thenTheLessonKeepsGoingWithoutIt() {
        // Audio failing never blocks a lesson. The prompt becomes something to read and see rather
        // than something to hear.
        val quiet = reducer.reduce(start(), LessonAction.MediaUnavailable(instance(FIRST_INSTANCE)))

        assertThat(quiet.state.audioAvailable).isFalse()
        assertThat(quiet.state.phase).isEqualTo(LessonPhase.Asking)
    }

    @Test
    fun givenSoundIsUnavailable_whenTheChildAnswers_thenItIsRecordedAsUnscored() {
        // A question the child could not hear is not one they got wrong. The skip costs nothing.
        var state = reducer.reduce(
            start(),
            LessonAction.MediaUnavailable(instance(FIRST_INSTANCE))
        ).state
        state = reducer.reduce(state, answer(correct = false)).state

        assertThat(state.phase).isEqualTo(LessonPhase.AwaitingCheckpoint)
    }

    @Test
    fun givenAPromptWithSound_whenReplayIsAsked_thenItPlaysAgainAndNothingElseChanges() {
        val replayed = reducer.reduce(
            start(withAudio = true),
            LessonAction.PromptReplayRequested(instance(FIRST_INSTANCE))
        )

        assertThat(replayed.effects.filterIsInstance<LessonEffect.Play>()).hasSize(1)
        assertThat(replayed.state.activityIndex).isEqualTo(0)
    }

    @Test
    fun givenAPromptWithNoSoundYet_whenReplayIsAsked_thenNothingIsPlayedAndNothingBreaks() {
        // Every prompt recording is still unmade, so this is the state the app is actually in
        // today. Asking to replay silence must be harmless rather than an error.
        val replayed = reducer.reduce(
            start(),
            LessonAction.PromptReplayRequested(instance(FIRST_INSTANCE))
        )

        assertThat(replayed.effects).isEmpty()
    }

    @Test
    fun givenALesson_whenBackIsPressed_thenNothingIsAbandonedUntilItIsConfirmed() {
        // Back opens the stop-for-now question. Leaving immediately would throw away a checkpoint
        // the child had earned.
        val asked = reducer.reduce(start(), LessonAction.StopRequested(instance(FIRST_INSTANCE)))

        assertThat(asked.state.stopRequested).isTrue()
        assertThat(asked.state.phase).isEqualTo(LessonPhase.Asking)
        assertThat(asked.effects.filterIsInstance<LessonEffect.PausePlayback>()).hasSize(1)
    }

    @Test
    fun givenSoundIsUnavailable_whenTheChildTakesTheSkip_thenItCostsThemNothing() {
        // The fair way past a question that cannot be asked properly. It is written down, because
        // the child was here and did the only thing on offer, and it is written down as unscored.
        val quiet = reducer.reduce(
            start(),
            LessonAction.MediaUnavailable(instance(FIRST_INSTANCE))
        ).state

        val skipped = reducer.reduce(quiet, LessonAction.SkipRequested(instance(FIRST_INSTANCE)))

        assertThat(skipped.state.phase).isEqualTo(LessonPhase.AwaitingCheckpoint)
        assertThat(
            skipped.effects.filterIsInstance<LessonEffect.Persist>().single().command.outcome
        ).isEqualTo(AttemptOutcome.UNSCORED_SKIP)
    }

    @Test
    fun givenSoundIsWorking_whenASkipArrives_thenItIsIgnoredBecauseNoneWasOffered() {
        // The skip only exists while the sound does not, so a press for it at any other moment is
        // for a control this child was never shown. Honouring it would turn a real question into
        // an unscored one.
        val skipped = reducer.reduce(start(), LessonAction.SkipRequested(instance(FIRST_INSTANCE)))

        assertThat(skipped.effects).isEmpty()
        assertThat(skipped.state.phase).isEqualTo(LessonPhase.Asking)
    }

    @Test
    fun givenASkipAlreadyWaitingOnAWrite_whenItIsPressedAgain_thenNothingIsRecordedTwice() {
        val quiet = reducer.reduce(
            start(),
            LessonAction.MediaUnavailable(instance(FIRST_INSTANCE))
        ).state
        val waiting =
            reducer.reduce(quiet, LessonAction.SkipRequested(instance(FIRST_INSTANCE))).state

        val again = reducer.reduce(waiting, LessonAction.SkipRequested(instance(FIRST_INSTANCE)))

        assertThat(again.effects).isEmpty()
    }

    @Test
    fun givenTheStopQuestionIsOpen_whenTheChildKeepsLearning_thenItClosesAndNothingWasLost() {
        // Back asks rather than leaves, so the answer "keep learning" has to be able to put the
        // question away again. Without this the child is left holding a dialog that will not go.
        val asked = reducer.reduce(start(), LessonAction.StopRequested(instance(FIRST_INSTANCE)))

        val stayed = reducer.reduce(
            asked.state,
            LessonAction.KeepLearningRequested(instance(FIRST_INSTANCE))
        )

        assertThat(stayed.state.stopRequested).isFalse()
        assertThat(stayed.state.phase).isEqualTo(LessonPhase.Asking)
        assertThat(stayed.state.activityIndex).isEqualTo(0)
    }

    @Test
    fun givenNobodyAskedToStop_whenKeepLearningArrives_thenNothingChanges() {
        val stayed =
            reducer.reduce(start(), LessonAction.KeepLearningRequested(instance(FIRST_INSTANCE)))

        assertThat(stayed.state.stopRequested).isFalse()
        assertThat(stayed.effects).isEmpty()
    }

    @Test
    fun givenALessonWithRecordings_whenItStarts_thenItsFirstQuestionIsAsked() {
        // A lesson that never says its own question is not a lesson. Starting asks for it, rather
        // than leaving whoever holds the reducer to remember to.
        val begun = started(withAudio = true)

        assertThat(begun.effects).contains(LessonEffect.Play(asset(1)))
    }

    @Test
    fun givenTheFirstActivityIsConfirmed_whenTheSecondAppears_thenItAsksForItsOwnRecording() {
        // The bug this test exists for: the prompt used to be a field set once at the start, so
        // every later question asked for the first question's recording.
        // The first question has to be heard before it can be answered, so getting to the second
        // one now goes through its recording ending. See ADR 0004.
        val heard = reducer.reduce(
            start(withAudio = true),
            LessonAction.PromptFinished(instance(FIRST_INSTANCE), asset(1))
        ).state
        val awaiting = reducer.reduce(heard, answer(correct = true)).state

        val moved = reducer.reduce(
            awaiting,
            LessonAction.CheckpointConfirmed(instance(FIRST_INSTANCE), instance(SECOND_INSTANCE))
        )

        assertThat(moved.effects).contains(LessonEffect.Play(asset(2)))
        assertThat(moved.effects).doesNotContain(LessonEffect.Play(asset(1)))
    }

    @Test
    fun givenTheLessonKnowsItHasNoSound_whenTheNextActivityAppears_thenNothingIsAsked() {
        // Once a lesson has found out it is running silent, asking again for every remaining
        // question is noise. It has already told the child, and the words are on screen.
        val quiet = reducer.reduce(
            start(withAudio = true),
            LessonAction.MediaUnavailable(instance(FIRST_INSTANCE))
        ).state
        val awaiting = reducer.reduce(quiet, answer(correct = true)).state

        val moved = reducer.reduce(
            awaiting,
            LessonAction.CheckpointConfirmed(instance(FIRST_INSTANCE), instance(SECOND_INSTANCE))
        )

        assertThat(moved.effects.filterIsInstance<LessonEffect.Play>()).isEmpty()
    }

    @Test
    fun givenALessonWhoseRecordingsAreAllUnmade_whenItStarts_thenNothingIsAsked() {
        assertThat(started().effects).isEmpty()
    }

    @Test
    fun givenAChildIsComingBack_whenTheLessonOpens_thenItOpensOnTheActivityTheyWereOn() {
        // Coming back to the first question is the thing the stop-for-now dialog promises not to
        // do. The reducer takes where the child is rather than assuming the beginning.
        val resumed =
            started(withAudio = true, activityIndex = 1, currentInstance = SECOND_INSTANCE)

        assertThat(resumed.state.activityIndex).isEqualTo(1)
        assertThat(resumed.state.currentInstance).isEqualTo(instance(SECOND_INSTANCE))
        assertThat(resumed.effects).contains(LessonEffect.Play(asset(2)))
    }

    @Test
    fun givenSayingItWithPip_whenTheChildFinishes_thenItIsPractisedRatherThanSkipped() {
        // The distinction the fourth outcome exists for. A child who worked through a repetition
        // did not skip it, and their history should not say they did.
        val finished = reducer.reduce(
            startSpeaking(),
            LessonAction.RepetitionFinished(instance(FIRST_INSTANCE))
        )

        assertThat(
            finished.effects.filterIsInstance<LessonEffect.Persist>().single().command.outcome
        ).isEqualTo(AttemptOutcome.PRACTISED)
        assertThat(finished.state.phase).isEqualTo(LessonPhase.AwaitingCheckpoint)
    }

    @Test
    fun givenAQuestionToAnswer_whenARepetitionFinishArrives_thenItIsIgnored() {
        // Every other family is answered rather than worked through. A press arriving from the
        // wrong screen must not record a child as having practised something they were asked to
        // choose.
        val ignored = reducer.reduce(
            start(),
            LessonAction.RepetitionFinished(instance(FIRST_INSTANCE))
        )

        assertThat(ignored.effects).isEmpty()
        assertThat(ignored.state.phase).isEqualTo(LessonPhase.Asking)
    }

    @Test
    fun givenALessonWithARecording_whenItStarts_thenItsQuestionIsStillBeingAsked() {
        // The state ADR 0004 has needed since it was approved: a lesson that can say a prompt is
        // sounding. Without it a child can answer over the question.
        val begun = started(withAudio = true)

        assertThat(begun.state.soundingPrompt).isEqualTo(asset(1))
    }

    @Test
    fun givenAQuestionIsStillSounding_whenItsRecordingEnds_thenTheQuestionHasBeenAsked() {
        val sounding = start(withAudio = true)

        val heard = reducer.reduce(
            sounding,
            LessonAction.PromptFinished(instance(FIRST_INSTANCE), asset(1))
        )

        assertThat(heard.state.soundingPrompt).isNull()
    }

    @Test
    fun givenTheQuestionIsStillSounding_whenTheChildAnswers_thenNothingIsRecorded() {
        // The whole of ADR 0004. The screen takes the answers out of the focus order; this is what
        // makes it a guarantee rather than an arrangement of controls.
        val pressed = reducer.reduce(start(withAudio = true), answer(correct = true))

        assertThat(pressed.effects).isEmpty()
        assertThat(pressed.state.phase).isEqualTo(LessonPhase.Asking)
    }

    @Test
    fun givenTheQuestionHasBeenAsked_whenTheChildAnswers_thenItCounts() {
        val heard = reducer.reduce(
            start(withAudio = true),
            LessonAction.PromptFinished(instance(FIRST_INSTANCE), asset(1))
        ).state

        val pressed = reducer.reduce(heard, answer(correct = true))

        assertThat(pressed.state.phase).isEqualTo(LessonPhase.AwaitingCheckpoint)
    }

    @Test
    fun givenPipIsStillAskingForTheWord_whenTheChildSaysTheyHaveFinished_thenNothingIsRecorded() {
        // Say with Pip has no answer, so the rule about answers does not reach it. Without this a
        // child could finish saying a word before Pip had asked for it.
        val finished = reducer.reduce(
            startSpeaking(withAudio = true),
            LessonAction.RepetitionFinished(instance(FIRST_INSTANCE))
        )

        assertThat(finished.effects).isEmpty()
        assertThat(finished.state.phase).isEqualTo(LessonPhase.Asking)
    }

    @Test
    fun givenAQuestionIsSounding_whenADifferentRecordingEnds_thenItIsStillBeingAsked() {
        // A support phrase or a word, not the question. Unit one has four prompt recordings for six
        // activities, so what finished has to be checked rather than assumed.
        val sounding = start(withAudio = true)

        val other = reducer.reduce(
            sounding,
            LessonAction.PromptFinished(instance(FIRST_INSTANCE), AssetId("aud-vi-help-look"))
        )

        assertThat(other.state.soundingPrompt).isEqualTo(asset(1))
    }

    @Test
    fun givenAQuestionIsSounding_whenTheLessonLearnsItHasNoSound_thenNothingIsSoundingAnyMore() {
        // A lesson that has found out it is silent is not sounding anything. Leaving the field set
        // would lock the answers away with no recording left to unlock them.
        val quiet = reducer.reduce(
            start(withAudio = true),
            LessonAction.MediaUnavailable(instance(FIRST_INSTANCE))
        )

        assertThat(quiet.state.soundingPrompt).isNull()
    }

    @Test
    fun givenTheNextQuestionNamesNoRecording_whenItArrives_thenItCanBeAnsweredStraightAway() {
        // A question with nothing to play has nothing to wait for. Moving on cannot carry the last
        // recording forward, because a child cannot answer their way off a question that is still
        // sounding in the first place.
        val heard = reducer.reduce(
            startMixed(),
            LessonAction.PromptFinished(instance(FIRST_INSTANCE), asset(1))
        ).state
        val awaiting = reducer.reduce(heard, answer(correct = true)).state

        val moved = reducer.reduce(
            awaiting,
            LessonAction.CheckpointConfirmed(instance(FIRST_INSTANCE), instance(SECOND_INSTANCE))
        )

        assertThat(moved.state.soundingPrompt).isNull()
        assertThat(reducer.reduce(moved.state, answer(SECOND_INSTANCE, correct = true)).effects)
            .isNotEmpty()
    }

    @Test
    fun givenTheQuestionHasBeenAsked_whenReplayIsPressed_thenItIsBeingAskedAgain() {
        // Replay starts the same recording, so it puts the lesson back in the same state. One
        // meaning for playing a prompt, whoever asked for it.
        val heard = reducer.reduce(
            start(withAudio = true),
            LessonAction.PromptFinished(instance(FIRST_INSTANCE), asset(1))
        ).state

        val again = reducer.reduce(
            heard,
            LessonAction.PromptReplayRequested(instance(FIRST_INSTANCE))
        )

        assertThat(again.state.soundingPrompt).isEqualTo(asset(1))
        assertThat(reducer.reduce(again.state, answer(correct = true)).effects).isEmpty()
    }

    @Test
    fun givenAQuestionIsSounding_whenTheChildLeavesAndComesBack_thenItIsStillBeingAsked() {
        // Back pauses the recording and "keep learning" deliberately never restarts one, so the
        // question stays unasked. Focus is resting on replay, which is one press away.
        val asked =
            reducer.reduce(
                start(withAudio = true),
                LessonAction.StopRequested(instance(FIRST_INSTANCE))
            )
        val stayed = reducer.reduce(
            asked.state,
            LessonAction.KeepLearningRequested(instance(FIRST_INSTANCE))
        )

        assertThat(stayed.state.soundingPrompt).isEqualTo(asset(1))
    }

    private fun start(activities: Int = 3, withAudio: Boolean = false) =
        started(activities, withAudio).state

    private fun started(
        activities: Int = 3,
        withAudio: Boolean = false,
        activityIndex: Int = 0,
        currentInstance: String = FIRST_INSTANCE
    ) = LessonReducer.start(
        sessionId = SessionId(SESSION),
        profileId = ProfileId(PROFILE),
        courseVersion = CourseVersion(VERSION),
        lesson = lesson(activities, withAudio),
        activityIndex = activityIndex,
        currentInstance = ActivityInstanceId(currentInstance),
        startedAt = EpochMillis(NOW)
    )

    private fun confirmFirst(): LessonSessionState {
        val awaiting = reducer.reduce(start(), answer(correct = true)).state
        return reducer.reduce(
            awaiting,
            LessonAction.CheckpointConfirmed(instance(FIRST_INSTANCE), instance(SECOND_INSTANCE))
        ).state
    }

    private fun failFirst(): LessonSessionState {
        val awaiting = reducer.reduce(start(), answer(correct = true)).state
        return reducer.reduce(
            awaiting,
            LessonAction.CheckpointFailed(instance(FIRST_INSTANCE))
        ).state
    }

    private fun answer(instance: String = FIRST_INSTANCE, correct: Boolean) =
        LessonAction.AnswerChosen(
            expectedActivityInstanceId = ActivityInstanceId(instance),
            correct = correct
        )

    private fun instance(value: String) = ActivityInstanceId(value)

    /**
     * Activities that carry their own content, because that is what a real lesson holds and what
     * makes "the second question asks for its own recording" a thing a test can tell apart.
     */
    private fun lesson(activities: Int, withAudio: Boolean = false) = Lesson(
        id = LessonId(LESSON),
        unitId = UnitId(UNIT),
        ordinal = 0,
        activities = (0 until activities).map { index ->
            listening(index, if (withAudio) asset(index + 1) else null)
        }
    )

    private fun startMixed() = LessonReducer.start(
        sessionId = SessionId(SESSION),
        profileId = ProfileId(PROFILE),
        courseVersion = CourseVersion(VERSION),
        lesson = mixedLesson(),
        activityIndex = 0,
        currentInstance = ActivityInstanceId(FIRST_INSTANCE),
        startedAt = EpochMillis(NOW)
    ).state

    /** A lesson whose first question has a recording and whose second does not. */
    private fun mixedLesson() = Lesson(
        id = LessonId(LESSON),
        unitId = UnitId(UNIT),
        ordinal = 0,
        activities = listOf(
            listening(index = 0, asset = asset(1)),
            listening(index = 1, asset = null)
        )
    )

    private fun listening(index: Int, asset: AssetId?) = Activity(
        id = ActivityId("$LESSON-a${index + 1}"),
        ordinal = index,
        family = ActivityFamily.LISTEN_AND_CHOOSE,
        content = ActivityContent.ListeningSelection(
            prompt = "Where are the eyes?",
            promptAsset = asset,
            choices = listOf(choice("eyes"), choice("ears")),
            correct = SkillId("word-eyes")
        )
    )

    private fun speakingLesson(withAudio: Boolean = false) = Lesson(
        id = LessonId(LESSON),
        unitId = UnitId(UNIT),
        ordinal = 0,
        activities = listOf(
            Activity(
                id = ActivityId("$LESSON-a1"),
                ordinal = 0,
                family = ActivityFamily.SAY_WITH_PIP,
                content = ActivityContent.GuidedRepetition(
                    prompt = "Say it with me: eyes.",
                    promptAsset = if (withAudio) asset(1) else null,
                    words = listOf(choice("eyes"))
                )
            ),
            Activity(
                id = ActivityId("$LESSON-a2"),
                ordinal = 1,
                family = ActivityFamily.LISTEN_AND_CHOOSE,
                content = ActivityContent.ListeningSelection(
                    prompt = "Where are the eyes?",
                    promptAsset = null,
                    choices = listOf(choice("eyes"), choice("ears")),
                    correct = SkillId("word-eyes")
                )
            )
        )
    )

    private fun startSpeaking(withAudio: Boolean = false) = LessonReducer.start(
        sessionId = SessionId(SESSION),
        profileId = ProfileId(PROFILE),
        courseVersion = CourseVersion(VERSION),
        lesson = speakingLesson(withAudio),
        activityIndex = 0,
        currentInstance = ActivityInstanceId(FIRST_INSTANCE),
        startedAt = EpochMillis(NOW)
    ).state

    private fun asset(n: Int) = AssetId("aud-en-prompt-a$n")

    private fun choice(word: String) = AnswerChoice(
        skillId = SkillId("word-$word"),
        label = word,
        image = AssetId("img-$word"),
        audio = AssetId("aud-en-$word")
    )

    private companion object {
        const val SESSION = "s1"
        const val PROFILE = "p1"
        const val VERSION = "2026.09"
        const val UNIT = "u01-my-body"
        const val LESSON = "u01-my-body-l1"
        const val FIRST_INSTANCE = "u01-my-body-l1-a1-1"
        const val SECOND_INSTANCE = "u01-my-body-l1-a2-1"
        const val NOW = 1_756_000_000_000
        const val MAX_SUPPORT = 3
    }
}
