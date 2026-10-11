package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.Attribution
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.testsupport.DomainBuilders
import com.nphkhiem.englishforyourchildren.testsupport.FakeProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Which version a lesson opens at once more than one version of the course exists.
 *
 * The course here can read a lesson at exactly the versions it is told it holds, which is the one
 * thing the shared fake does not do and the whole of what these tests are about.
 */
class LessonVersionTest {

    private val progress = FakeProgressRepository()

    @Test
    fun givenNothingPaused_whenALessonOpens_thenItIsTaughtFromTheLiveCourse() {
        val course = CourseHolding(live = NEW, readable = setOf(SHIPPED, NEW))

        assertThat(versionFor(course)).isEqualTo(NEW)
    }

    @Test
    fun givenThisLessonPausedUnderAnOlderReadableVersion_whenItOpens_thenTheChildGoesBackThere() {
        val course = CourseHolding(live = NEW, readable = setOf(SHIPPED, NEW))
        pausedAt(SHIPPED, LESSON)

        assertThat(versionFor(course)).isEqualTo(SHIPPED)
    }

    @Test
    fun givenThePausedVersionIsGone_whenItOpens_thenItStartsAfreshOnTheLiveOne() {
        // The pack it was paused in has been replaced. A lesson that works beats a place that
        // cannot be read.
        val course = CourseHolding(live = AFTER, readable = setOf(SHIPPED, AFTER))
        pausedAt(NEW, LESSON)

        assertThat(versionFor(course)).isEqualTo(AFTER)
    }

    @Test
    fun givenADifferentLessonPaused_whenThisOneOpens_thenItIsTaughtFromTheLiveCourse() {
        val course = CourseHolding(live = NEW, readable = setOf(SHIPPED, NEW))
        pausedAt(SHIPPED, LessonId("u01-my-body-l2"))

        assertThat(versionFor(course)).isEqualTo(NEW)
    }

    @Test
    fun givenNoCourseCanBeRead_whenALessonOpens_thenThereIsNoVersionToOpenAt() {
        assertThat(versionFor(UnreadableCourse)).isNull()
    }

    private fun versionFor(course: CurriculumRepository): CourseVersion? =
        runBlocking { lessonVersionFor(CHILD, LESSON, course, progress) }

    private fun pausedAt(version: CourseVersion, lesson: LessonId) {
        progress.setProgress(
            DomainBuilders.profileProgress(
                profileId = CHILD,
                openCheckpoint = DomainBuilders.lessonCheckpoint(
                    profileId = CHILD,
                    courseVersion = version,
                    lessonId = lesson
                )
            )
        )
    }

    /** A course that can read lessons at [readable] versions and teaches from [live]. */
    private class CourseHolding(
        private val live: CourseVersion,
        private val readable: Set<CourseVersion>
    ) : CurriculumRepository {
        override fun observeCourse(): Flow<DomainResult<Course>> =
            flowOf(DomainResult.Success(DomainBuilders.course(version = live)))

        override suspend fun getLesson(id: LessonId, version: CourseVersion): DomainResult<Lesson> =
            if (version in readable) {
                DomainResult.Success(DomainBuilders.lesson(id = id))
            } else {
                DomainResult.Failure(DomainError.LessonNotFound)
            }

        override suspend fun getAttributions(): DomainResult<List<Attribution>> =
            DomainResult.Success(emptyList())
    }

    private object UnreadableCourse : CurriculumRepository {
        override fun observeCourse(): Flow<DomainResult<Course>> =
            flowOf(DomainResult.Failure(DomainError.InvalidContent))

        override suspend fun getLesson(id: LessonId, version: CourseVersion): DomainResult<Lesson> =
            DomainResult.Failure(DomainError.InvalidContent)

        override suspend fun getAttributions(): DomainResult<List<Attribution>> =
            DomainResult.Failure(DomainError.InvalidContent)
    }

    private companion object {
        val CHILD = ProfileId("p1")
        val LESSON = LessonId("u01-my-body-l1")
        val SHIPPED = CourseVersion("2026.09")
        val NEW = CourseVersion("2026.10")
        val AFTER = CourseVersion("2026.11")
    }
}
