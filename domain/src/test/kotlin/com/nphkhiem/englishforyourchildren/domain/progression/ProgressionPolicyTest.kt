package com.nphkhiem.englishforyourchildren.domain.progression

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.Activity
import com.nphkhiem.englishforyourchildren.domain.model.ActivityContent
import com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.AnswerChoice
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseId
import com.nphkhiem.englishforyourchildren.domain.model.CourseUnit
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonCheckpoint
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileProgress
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import org.junit.jupiter.api.Test

/**
 * Which lessons a child can reach, as a table.
 *
 * The rule is derived rather than recorded, which is what makes "unlocks the next lesson exactly
 * once" true by construction: there is no unlock event that could fire twice.
 */
class ProgressionPolicyTest {
    private val policy = CourseOrderProgressionPolicy()

    @Test
    fun givenAChildWhoHasDoneNothing_whenThePathIsBuilt_thenOnlyTheFirstLessonIsOffered() {
        val path = policy.build(course(lessons = 3), progress())

        assertThat(path.recommended).isEqualTo(LessonId("$LESSON-1"))
        assertThat(path.standingOf(LessonId("$LESSON-2"))).isEqualTo(LessonStanding.FUTURE)
        assertThat(path.standingOf(LessonId("$LESSON-3"))).isEqualTo(LessonStanding.FUTURE)
    }

    @Test
    fun givenTheFirstLessonIsFinished_whenThePathIsBuilt_thenTheSecondIsTheOneOffered() {
        val path = policy.build(course(lessons = 3), progress(completed = setOf("$LESSON-1")))

        assertThat(path.standingOf(LessonId("$LESSON-1"))).isEqualTo(LessonStanding.COMPLETED)
        assertThat(path.recommended).isEqualTo(LessonId("$LESSON-2"))
        assertThat(path.standingOf(LessonId("$LESSON-3"))).isEqualTo(LessonStanding.FUTURE)
    }

    @Test
    fun givenTheSameLessonFinishedTwice_whenThePathIsBuilt_thenNothingMovesTwice() {
        // The set says finished once however many times a child walked it, which is what makes the
        // "exactly once" criterion something the shape guarantees rather than a counter enforces.
        val once = policy.build(course(lessons = 3), progress(completed = setOf("$LESSON-1")))
        val again = policy.build(course(lessons = 3), progress(completed = setOf("$LESSON-1")))

        assertThat(again.recommended).isEqualTo(once.recommended)
        assertThat(again.standings).isEqualTo(once.standings)
    }

    @Test
    fun givenEveryLessonIsFinished_whenThePathIsBuilt_thenNothingIsRecommended() {
        // Pointing at a lesson a child has already done, as though it were new, is the thing this
        // null exists to avoid.
        val all = (1..3).map { "$LESSON-$it" }.toSet()

        val path = policy.build(course(lessons = 3), progress(completed = all))

        assertThat(path.recommended).isNull()
        assertThat(path.standings.values.toSet()).containsExactly(LessonStanding.COMPLETED)
    }

    @Test
    fun givenALaterLessonWasSomehowFinishedFirst_whenThePathIsBuilt_thenTheGapIsStillOffered() {
        // Content can move under a child's history. A finished lesson two with an unfinished one
        // does not skip them past it, and does not lock anything either.
        val path = policy.build(course(lessons = 3), progress(completed = setOf("$LESSON-2")))

        assertThat(path.recommended).isEqualTo(LessonId("$LESSON-1"))
        assertThat(path.standingOf(LessonId("$LESSON-2"))).isEqualTo(LessonStanding.COMPLETED)
    }

    private fun openCheckpoint(lesson: String) = LessonCheckpoint(
        profileId = ProfileId("p1"),
        courseVersion = CourseVersion("2026.09"),
        lessonId = LessonId(lesson),
        lastCompletedActivity = ActivityId("$lesson-a1"),
        sessionId = SessionId("s1"),
        updatedAt = EpochMillis(1_756_000_000_000)
    )

    private fun progress(completed: Set<String> = emptySet()) = ProfileProgress(
        profileId = ProfileId("p1"),
        lessonsCompleted = completed.map { LessonId(it) }.toSet(),
        attempts = emptyList(),
        openCheckpoint = null
    )

    private fun course(lessons: Int) = Course(
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
                lessons = (0 until lessons).map {
                    Lesson(
                        id = LessonId("$LESSON-${it + 1}"),
                        unitId = UnitId(UNIT),
                        ordinal = it,
                        activities = listOf(question(it + 1))
                    )
                }
            )
        )
    )

    @Test
    fun givenAChildStoppedPartWayThroughALesson_whenThePathIsBuilt_thenThatIsWhatIsOffered() {
        // Where they left off beats where the course would send them. A child who stopped in the
        // middle of lesson two and is pointed at lesson two from the beginning has lost their
        // place, which is the whole thing the checkpoint exists to prevent.
        val stopped = progress(completed = setOf("$LESSON-1"))
            .copy(openCheckpoint = openCheckpoint("$LESSON-2"))

        val path = policy.build(course(lessons = 3), stopped)

        assertThat(path.recommended).isEqualTo(LessonId("$LESSON-2"))
        assertThat(path.standingOf(LessonId("$LESSON-2"))).isEqualTo(LessonStanding.RECOMMENDED)
    }

    @Test
    fun givenACheckpointInALessonSinceFinished_whenThePathIsBuilt_thenTheCourseOrderDecides() {
        // A child can walk a lesson again and finish it, leaving a checkpoint behind in something
        // they have already done. Following it would offer them a lesson they completed and hold
        // the course still.
        val walkedAgain = progress(completed = setOf("$LESSON-1", "$LESSON-2"))
            .copy(openCheckpoint = openCheckpoint("$LESSON-2"))

        val path = policy.build(course(lessons = 3), walkedAgain)

        assertThat(path.recommended).isEqualTo(LessonId("$LESSON-3"))
    }

    @Test
    fun givenACheckpointInALessonTheCourseHasLost_whenThePathIsBuilt_thenTheCourseOrderDecides() {
        // Content moves under a child's history. A checkpoint naming a lesson this course does not
        // have cannot be offered, and offering nothing would be worse than offering the next one.
        val elsewhere = progress().copy(openCheckpoint = openCheckpoint("u09-somewhere-l1"))

        val path = policy.build(course(lessons = 3), elsewhere)

        assertThat(path.recommended).isEqualTo(LessonId("$LESSON-1"))
    }

    @Test
    fun givenEveryLessonFinished_whenThePathIsBuilt_thenThereIsNothingToRecommend() {
        // The completed-course state, which the brief names as one of this screen's five. Null is
        // how it arrives, and it is not the same as a path that would not load.
        val path = policy.build(
            course(lessons = 3),
            progress(completed = setOf("$LESSON-1", "$LESSON-2", "$LESSON-3"))
        )

        assertThat(path.recommended).isNull()
        assertThat(path.standings.values.toSet()).containsExactly(LessonStanding.COMPLETED)
    }

    /** A lesson has to ask something; what it asks does not matter to a standing. */
    private fun question(lesson: Int) = Activity(
        id = ActivityId("$LESSON-$lesson-a1"),
        ordinal = 0,
        family = ActivityFamily.LISTEN_AND_CHOOSE,
        content = ActivityContent.ListeningSelection(
            prompt = "Where are the eyes?",
            promptAsset = null,
            choices = listOf(
                AnswerChoice(
                    skillId = SkillId("word-eyes"),
                    label = "eyes",
                    image = AssetId("img-eyes"),
                    audio = AssetId("aud-en-eyes")
                )
            ),
            correct = SkillId("word-eyes")
        )
    )

    private companion object {
        const val UNIT = "u01-my-body"
        const val LESSON = "u01-my-body-l"
    }
}
