package com.nphkhiem.englishforyourchildren.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CurriculumTest {
    @Test
    fun givenNoActivities_whenALessonIsCreated_thenItIsRejected() {
        // A lesson with nothing in it would put a child on a stage with nothing to do, and the
        // learning path would offer it as though it were a lesson.
        assertThrows<IllegalArgumentException> { lesson(activities = emptyList()) }
    }

    @Test
    fun givenTwoActivitiesSharingAnId_whenALessonIsCreated_thenItIsRejected() {
        // A checkpoint names the last activity a child finished. Two activities with one id would
        // make that name ambiguous, and resuming would be a guess.
        val repeated = listOf(activity("a", 0), activity("a", 1))

        assertThrows<IllegalArgumentException> { lesson(activities = repeated) }
    }

    @Test
    fun givenActivitiesOutOfOrder_whenALessonIsCreated_thenItIsRejected() {
        val gap = listOf(activity("a", 0), activity("b", 2))

        assertThrows<IllegalArgumentException> { lesson(activities = gap) }
    }

    @Test
    fun givenActivitiesListedOutOfSequence_whenALessonIsCreated_thenItIsRejected() {
        // Position in the list is the order a child meets them, so a correct set in the wrong
        // sequence is still wrong. Sorting it here would hide a content mistake instead of
        // reporting it.
        val shuffled = listOf(activity("b", 1), activity("a", 0))

        assertThrows<IllegalArgumentException> { lesson(activities = shuffled) }
    }

    @Test
    fun givenOrderedActivities_whenALessonIsCreated_thenItKeepsThem() {
        val ordered = listOf(activity("a", 0), activity("b", 1), activity("c", 2))

        assertThat(lesson(activities = ordered).activities).hasSize(3)
    }

    @Test
    fun givenNoLessons_whenAUnitIsCreated_thenItIsRejected() {
        assertThrows<IllegalArgumentException> { unit(lessons = emptyList()) }
    }

    @Test
    fun givenLessonsOutOfOrder_whenAUnitIsCreated_thenItIsRejected() {
        val gap = listOf(lesson(ordinal = 0), lesson(id = "l2", ordinal = 2))

        assertThrows<IllegalArgumentException> { unit(lessons = gap) }
    }

    @Test
    fun givenNoUnits_whenACourseIsCreated_thenItIsRejected() {
        assertThrows<IllegalArgumentException> { course(units = emptyList()) }
    }

    @Test
    fun givenNoSupportedLocale_whenACourseIsCreated_thenItIsRejected() {
        // A course nobody can be spoken to in is a course that cannot be taught. English-led still
        // means at least one locale is present.
        assertThrows<IllegalArgumentException> { course(locales = emptySet()) }
    }

    @Test
    fun givenAnActivity_whenItIsRead_thenItNamesItsFamilyAndNothingItAsks() {
        // Structure now, payload later. What a listen-and-choose actually asks belongs to the
        // curriculum blueprint, which does not exist yet, so this model deliberately cannot carry
        // a prompt, a choice or an asset. See the design note for P1-T2.
        val listen = activity("a", 0, ActivityFamily.LISTEN_AND_CHOOSE)

        assertThat(listen.family).isEqualTo(ActivityFamily.LISTEN_AND_CHOOSE)
    }

    @Test
    fun givenTheBuiltActivityScreens_whenFamiliesAreListed_thenThereIsOnePerScreen() {
        // Five families, five built screens. A sixth would be a product decision and a new screen,
        // so it fails here before it reaches a lesson.
        assertThat(ActivityFamily.entries).containsExactly(
            ActivityFamily.LISTEN_AND_CHOOSE,
            ActivityFamily.PICTURE_MATCHING,
            ActivityFamily.LETTER_AND_SOUND,
            ActivityFamily.SAY_WITH_PIP,
            ActivityFamily.REVIEW
        )
    }

    private fun activity(
        id: String,
        ordinal: Int,
        family: ActivityFamily = ActivityFamily.LISTEN_AND_CHOOSE
    ) = Activity(id = ActivityId(id), ordinal = ordinal, family = family)

    private fun lesson(
        id: String = LESSON,
        ordinal: Int = 0,
        activities: List<Activity> = listOf(activity("a", 0))
    ) = Lesson(
        id = LessonId(id),
        unitId = UnitId(UNIT),
        ordinal = ordinal,
        activities = activities
    )

    private fun unit(lessons: List<Lesson> = listOf(lesson())) = CourseUnit(
        id = UnitId(UNIT),
        courseId = CourseId(COURSE),
        ordinal = 0,
        theme = THEME,
        lessons = lessons
    )

    private fun course(
        units: List<CourseUnit> = listOf(unit()),
        locales: Set<String> = setOf("en")
    ) = Course(
        id = CourseId(COURSE),
        version = CourseVersion(VERSION),
        schemaVersion = 1,
        supportedLocales = locales,
        units = units
    )

    private companion object {
        const val COURSE = "starter"
        const val VERSION = "2026.08"
        const val UNIT = "my-home"
        const val LESSON = "l1"
        const val THEME = "My Home"
    }
}
