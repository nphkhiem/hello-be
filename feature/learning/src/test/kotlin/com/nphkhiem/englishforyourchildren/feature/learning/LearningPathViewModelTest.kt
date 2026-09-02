package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.testsupport.DomainBuilders
import com.nphkhiem.englishforyourchildren.testsupport.FakeCurriculumRepository
import com.nphkhiem.englishforyourchildren.testsupport.FakeProfileRepository
import com.nphkhiem.englishforyourchildren.testsupport.FakeProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * That the path a screen draws is the one the domain decided.
 *
 * The judgement moved out of this ViewModel and into `:domain`, where it has its own table of
 * tests. What is left worth proving here is that it arrives: a screen showing a lesson as finished
 * when nothing says it is would be a worse bug than any of the rules.
 */
class LearningPathViewModelTest {
    private val curriculum = FakeCurriculumRepository()
    private val progress = FakeProgressRepository()
    private val profiles = FakeProfileRepository()

    @BeforeEach
    fun useTestDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        curriculum.setCourse(courseOfThree())
    }

    @AfterEach
    fun restoreDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenAChildWhoHasDoneNothing_whenThePathOpens_thenOnlyTheFirstLessonIsOffered() = runTest {
        val model = started()

        val lessons = model.state.value.unit?.lessons.orEmpty()
        assertThat(lessons.map { it.progress }).containsExactly(
            LessonProgress.RECOMMENDED,
            LessonProgress.FUTURE,
            LessonProgress.FUTURE
        ).inOrder()
    }

    @Test
    fun givenTheFirstLessonIsFinished_whenThePathOpens_thenTheOfferHasMovedOnOne() = runTest {
        progress.setProgress(
            DomainBuilders.profileProgress(lessonsCompleted = setOf(LessonId("$LESSON-1")))
        )

        val model = started()

        assertThat(model.state.value.unit?.lessons.orEmpty().map { it.progress }).containsExactly(
            LessonProgress.COMPLETED,
            LessonProgress.RECOMMENDED,
            LessonProgress.FUTURE
        ).inOrder()
    }

    @Test
    fun givenStorageCannotSayWhatTheChildHasDone_whenThePathOpens_thenNothingIsClaimedFinished() =
        runTest {
            // A television that cannot read its history is not a child who has done nothing, and it
            // is certainly not a child who has finished something. Everything reads as later.
            progress.setReadFailure(DomainError.PersistenceUnavailable)

            val model = started()

            assertThat(model.state.value.unit?.lessons.orEmpty().map { it.progress })
                .containsExactly(
                    LessonProgress.FUTURE,
                    LessonProgress.FUTURE,
                    LessonProgress.FUTURE
                )
        }

    private fun started(): LearningPathViewModel {
        val model = LearningPathViewModel(curriculum, progress, profiles)
        model.start(ProfileId("p1"))
        return model
    }

    private fun courseOfThree() = DomainBuilders.course(
        units = listOf(
            DomainBuilders.courseUnit(
                lessons = (0 until 3).map {
                    DomainBuilders.lesson(id = LessonId("$LESSON-${it + 1}"), ordinal = it)
                }
            )
        )
    )

    private companion object {
        const val LESSON = "u01-my-body-l"
    }
}
