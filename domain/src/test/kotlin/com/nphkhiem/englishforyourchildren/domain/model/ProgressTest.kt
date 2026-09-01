package com.nphkhiem.englishforyourchildren.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProgressTest {
    @Test
    fun givenNoCurrentActivity_whenASessionIsInProgress_thenItIsRejected() {
        // A session in progress is a child sitting in front of something. If it cannot say what,
        // then nothing can resume it and the status is decoration.
        assertThrows<IllegalArgumentException> {
            session(status = LessonStatus.IN_PROGRESS, current = null)
        }
    }

    @Test
    fun givenACurrentActivity_whenASessionIsCompleted_thenItIsRejected() {
        // Finished means finished. A completed session still pointing at an activity is the state
        // that lets a lesson be counted and then resumed into, which would count it twice.
        assertThrows<IllegalArgumentException> {
            session(status = LessonStatus.COMPLETED, current = ActivityInstanceId(INSTANCE))
        }
    }

    @Test
    fun givenAnActivityInHand_whenASessionIsInProgress_thenItIsAccepted() {
        val open =
            session(status = LessonStatus.IN_PROGRESS, current = ActivityInstanceId(INSTANCE))

        assertThat(open.currentActivity).isEqualTo(ActivityInstanceId(INSTANCE))
    }

    @Test
    fun givenNothingInHand_whenASessionIsCompleted_thenItIsAccepted() {
        val done = session(status = LessonStatus.COMPLETED, current = null)

        assertThat(done.currentActivity).isNull()
    }

    @Test
    fun givenALessonNotStarted_whenACheckpointIsWritten_thenItMayNameNoActivity() {
        // A checkpoint written before the first activity is confirmed is how a lesson that was
        // opened and left says so. Null is the honest value, not a sentinel activity.
        val fresh = checkpoint(lastCompleted = null)

        assertThat(fresh.lastCompletedActivity).isNull()
    }

    @Test
    fun givenAnAttempt_whenItIsRecorded_thenItCarriesNoRecordingAndNoFreeText() {
        // The no-microphone rule and the no-child-analytics rule are both held by this shape. There
        // is nowhere here to put audio, a transcript, or anything a child said or typed.
        val attempt = attempt(outcome = AttemptOutcome.CORRECT)

        assertThat(attempt.outcome).isEqualTo(AttemptOutcome.CORRECT)
        assertThat(attempt.ordinal).isEqualTo(0)
    }

    @Test
    fun givenTheApprovedOutcomes_whenListed_thenNoneOfThemIsAFailure() {
        // Supportive retry, never wrong. Unscored skip, never a penalty. Practised, which says a
        // child worked through a guided repetition rather than went past it. The vocabulary in
        // CONTEXT.md is the vocabulary here, so a scoring word cannot be introduced by accident.
        assertThat(AttemptOutcome.entries).containsExactly(
            AttemptOutcome.CORRECT,
            AttemptOutcome.SUPPORTIVE_RETRY,
            AttemptOutcome.UNSCORED_SKIP,
            AttemptOutcome.PRACTISED
        )
    }

    @Test
    fun givenANegativeOrdinal_whenAnAttemptIsRecorded_thenItIsRejected() {
        assertThrows<IllegalArgumentException> { attempt(ordinal = -1) }
    }

    @Test
    fun givenMoreSuccessesThanExposures_whenSkillProgressIsRead_thenItIsRejected() {
        // A child cannot have succeeded at a word more often than they have met it. If that arrives
        // from storage it is a counting bug, and it should stop here rather than be shown to a
        // caregiver as progress.
        assertThrows<IllegalArgumentException> { skill(exposures = 2, successes = 3) }
    }

    @Test
    fun givenNegativeCounts_whenSkillProgressIsRead_thenItIsRejected() {
        assertThrows<IllegalArgumentException> { skill(exposures = -1, successes = 0) }
        assertThrows<IllegalArgumentException> { skill(exposures = 0, successes = -1) }
    }

    @Test
    fun givenAWordNeverPractised_whenSkillProgressIsRead_thenItHasNoLastPractisedTime() {
        val untouched = skill(exposures = 0, successes = 0, lastPractised = null)

        assertThat(untouched.lastPractisedAt).isNull()
    }

    private fun session(status: LessonStatus, current: ActivityInstanceId?) = LessonSession(
        id = SessionId(SESSION),
        profileId = ProfileId(PROFILE),
        courseVersion = CourseVersion(VERSION),
        lessonId = LessonId(LESSON),
        currentActivity = current,
        status = status
    )

    private fun checkpoint(lastCompleted: ActivityId?) = LessonCheckpoint(
        profileId = ProfileId(PROFILE),
        courseVersion = CourseVersion(VERSION),
        lessonId = LessonId(LESSON),
        lastCompletedActivity = lastCompleted,
        sessionId = SessionId(SESSION),
        updatedAt = EpochMillis(NOW)
    )

    private fun attempt(outcome: AttemptOutcome = AttemptOutcome.CORRECT, ordinal: Int = 0) =
        ActivityAttempt(
            sessionId = SessionId(SESSION),
            activityInstance = ActivityInstanceId(INSTANCE),
            ordinal = ordinal,
            outcome = outcome,
            at = EpochMillis(NOW)
        )

    private fun skill(
        exposures: Int,
        successes: Int,
        lastPractised: EpochMillis? = EpochMillis(NOW)
    ) = SkillProgress(
        skillId = SkillId(SKILL),
        exposures = exposures,
        supportedSuccesses = successes,
        reviewNeeded = false,
        lastPractisedAt = lastPractised
    )

    private companion object {
        const val PROFILE = "p1"
        const val SESSION = "s1"
        const val LESSON = "l1"
        const val INSTANCE = "l1-a1-1"
        const val SKILL = "word-chair"
        const val VERSION = "2026.08"
        const val NOW = 1_756_000_000_000
    }
}
