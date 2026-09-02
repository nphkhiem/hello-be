package com.nphkhiem.englishforyourchildren.domain.progression

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.Activity
import com.nphkhiem.englishforyourchildren.domain.model.ActivityAttempt
import com.nphkhiem.englishforyourchildren.domain.model.ActivityContent
import com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.AnswerChoice
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseId
import com.nphkhiem.englishforyourchildren.domain.model.CourseUnit
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import org.junit.jupiter.api.Test

/**
 * How much of a thing a child knows, counted from what they actually did.
 *
 * A table rather than a story: each outcome against what it should leave behind. Nothing here is
 * stored, so a count can never drift from the attempts it summarises.
 */
class SkillTallyTest {

    @Test
    fun givenAQuestionAnsweredFirstTime_whenItIsCounted_thenTheSkillWasMetAndKnown() {
        val skills = tally(attempt(EYES_ACTIVITY, AttemptOutcome.CORRECT, at = 1))

        val eyes = skills.getValue(SkillId(EYES))
        assertThat(eyes.exposures).isEqualTo(1)
        assertThat(eyes.supportedSuccesses).isEqualTo(1)
        assertThat(eyes.reviewNeeded).isFalse()
    }

    @Test
    fun givenAChildNeededPipsHelp_whenItIsCounted_thenTheyGotThereAndItWantsReview() {
        // A supportive retry is a child getting there with help, so it is a success. That they
        // needed the help is what the review flag is for, and it is the only thing carrying it.
        val skills = tally(attempt(EYES_ACTIVITY, AttemptOutcome.SUPPORTIVE_RETRY, at = 1))

        val eyes = skills.getValue(SkillId(EYES))
        assertThat(eyes.supportedSuccesses).isEqualTo(1)
        assertThat(eyes.reviewNeeded).isTrue()
    }

    @Test
    fun givenHelpWasNeededAndThenNotNeeded_whenItIsCounted_thenItNoLongerWantsReview() {
        // Where a child is now, rather than everything they have ever found hard.
        val skills = tally(
            attempt(EYES_ACTIVITY, AttemptOutcome.SUPPORTIVE_RETRY, at = 1),
            attempt(EYES_ACTIVITY, AttemptOutcome.CORRECT, at = 2)
        )

        assertThat(skills.getValue(SkillId(EYES)).reviewNeeded).isFalse()
    }

    @Test
    fun givenTheQuestionCouldNotBeAsked_whenItIsCounted_thenTheChildNeverMetIt() {
        // The recording would not play, so the child was never actually asked. Counting it as a
        // meeting they failed would put the app's missing audio into their record as difficulty.
        val skills = tally(attempt(EYES_ACTIVITY, AttemptOutcome.UNSCORED_SKIP, at = 1))

        assertThat(skills).doesNotContainKey(SkillId(EYES))
    }

    @Test
    fun givenAWordSaidWithPip_whenItIsCounted_thenEveryWordInItWasPractised() {
        val skills = tally(attempt(SPEAKING_ACTIVITY, AttemptOutcome.PRACTISED, at = 1))

        assertThat(skills.keys).containsExactly(SkillId(EYES), SkillId(EARS))
        assertThat(skills.getValue(SkillId(EARS)).supportedSuccesses).isEqualTo(1)
    }

    @Test
    fun givenAnAttemptForContentTheCourseNoLongerHas_whenItIsCounted_thenItIsPassedOver() {
        // Content moves under a child's history. An attempt naming an activity that is gone is not
        // a reason to refuse to count the rest of what they did.
        val skills = tally(attempt(ActivityId("u01-my-body-l1-a9"), AttemptOutcome.CORRECT, at = 1))

        assertThat(skills).isEmpty()
    }

    private fun tally(vararg attempts: ActivityAttempt) = tallySkills(course(), attempts.toList())

    private fun attempt(activityId: ActivityId, outcome: AttemptOutcome, at: Long) =
        ActivityAttempt(
            sessionId = SessionId("s1"),
            activityId = activityId,
            activityInstance = ActivityInstanceId("${activityId.value}-1"),
            ordinal = 0,
            outcome = outcome,
            at = EpochMillis(at)
        )

    private fun course() = Course(
        id = CourseId("starter"),
        version = CourseVersion("2026.09"),
        schemaVersion = 2,
        supportedLocales = setOf("en"),
        units = listOf(
            CourseUnit(
                id = UnitId(UNIT),
                courseId = CourseId("starter"),
                ordinal = 0,
                theme = "My Body",
                word = "body",
                lessons = listOf(
                    Lesson(
                        id = LessonId(LESSON),
                        unitId = UnitId(UNIT),
                        ordinal = 0,
                        teaches = listOf(SkillId(EYES), SkillId(EARS)),
                        activities = listOf(
                            Activity(
                                id = EYES_ACTIVITY,
                                ordinal = 0,
                                family = ActivityFamily.LISTEN_AND_CHOOSE,
                                content = ActivityContent.ListeningSelection(
                                    prompt = "Where are the eyes?",
                                    promptAsset = null,
                                    choices = listOf(choice(EYES), choice(EARS)),
                                    correct = SkillId(EYES)
                                )
                            ),
                            Activity(
                                id = SPEAKING_ACTIVITY,
                                ordinal = 1,
                                family = ActivityFamily.SAY_WITH_PIP,
                                content = ActivityContent.GuidedRepetition(
                                    prompt = "Say it with me.",
                                    promptAsset = null,
                                    words = listOf(choice(EYES), choice(EARS))
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    private fun choice(skill: String) = AnswerChoice(
        skillId = SkillId(skill),
        label = skill.removePrefix("word-"),
        image = AssetId("img-${skill.removePrefix("word-")}"),
        audio = AssetId("aud-en-${skill.removePrefix("word-")}")
    )

    private companion object {
        const val UNIT = "u01-my-body"
        const val LESSON = "u01-my-body-l1"
        const val EYES = "word-eyes"
        const val EARS = "word-ears"
        val EYES_ACTIVITY = ActivityId("$LESSON-a1")
        val SPEAKING_ACTIVITY = ActivityId("$LESSON-a2")
    }
}
