package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
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

    @Test
    fun givenACourseOfTwoUnits_whenThePathOpens_thenTheOneAfterIsNamedAsSomewhereToGo() = runTest {
        // A second unit nothing points at is a unit no child can reach. The screen has drawn these
        // controls since HB-D12; until there was more than one unit, nothing ever filled them.
        curriculum.setCourse(twoUnits())

        val model = started()

        assertThat(model.state.value.unit?.unitId).isEqualTo(FIRST_UNIT)
        assertThat(model.state.value.previousUnit).isNull()
        assertThat(model.state.value.nextUnit?.theme).isEqualTo(SECOND_THEME)
    }

    @Test
    fun givenTheChildAsksForTheNextUnit_whenItIsShown_thenTheWayBackIsNamedToo() = runTest {
        curriculum.setCourse(twoUnits())
        val model = started()

        model.showNextUnit()

        assertThat(model.state.value.unit?.theme).isEqualTo(SECOND_THEME)
        assertThat(model.state.value.previousUnit?.unitId).isEqualTo(FIRST_UNIT)
        assertThat(model.state.value.nextUnit).isNull()
    }

    @Test
    fun givenTheChildHasFinishedAUnit_whenThePathOpens_thenItOpensWhereTheyAreUpTo() = runTest {
        // Not always the first unit. A child who has finished My Body and comes back tomorrow
        // should be looking at what is next, not paging their way to it past what they have done.
        curriculum.setCourse(twoUnits())
        progress.setProgress(
            DomainBuilders.profileProgress(
                lessonsCompleted = setOf(LessonId("$LESSON-1"), LessonId("$LESSON-2"))
            )
        )

        val model = started()

        assertThat(model.state.value.unit?.unitId).isEqualTo(SECOND_UNIT)
    }

    @Test
    fun givenTheChildPagedSomewhere_whenTheirProgressChanges_thenTheyAreLeftWhereTheyLooked() =
        runTest {
            // Where a child is looking is their decision, and a write landing underneath them is
            // not a reason to move them somewhere else mid-look.
            curriculum.setCourse(twoUnits())
            val model = started()
            model.showNextUnit()

            progress.setProgress(
                DomainBuilders.profileProgress(lessonsCompleted = setOf(LessonId("$LESSON-1")))
            )

            assertThat(model.state.value.unit?.unitId).isEqualTo(SECOND_UNIT)
        }

    @Test
    fun givenAChildWhoHasFinishedEverything_whenThePathOpens_thenTheyStayWhereTheyEnded() =
        runTest {
            // There is no offer left to follow, and the beginning is the furthest place from what they
            // just did.
            curriculum.setCourse(twoUnits())
            progress.setProgress(
                DomainBuilders.profileProgress(
                    lessonsCompleted = setOf(
                        LessonId("$LESSON-1"),
                        LessonId("$LESSON-2"),
                        LessonId("$SECOND_UNIT-l1")
                    )
                )
            )

            val model = started()

            assertThat(model.state.value.unit?.unitId).isEqualTo(SECOND_UNIT)
        }

    private fun twoUnits() = DomainBuilders.course(
        units = listOf(
            DomainBuilders.courseUnit(
                lessons = (0 until 2).map {
                    DomainBuilders.lesson(id = LessonId("$LESSON-${it + 1}"), ordinal = it)
                }
            ),
            DomainBuilders.courseUnit(
                id = UnitId(SECOND_UNIT),
                ordinal = 1,
                theme = SECOND_THEME,
                word = "family",
                lessons = listOf(
                    DomainBuilders.lesson(
                        id = LessonId("$SECOND_UNIT-l1"),
                        unitId = UnitId(SECOND_UNIT)
                    )
                )
            )
        )
    )

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
        const val FIRST_UNIT = "u01-my-body"
        const val SECOND_UNIT = "u02-my-family"
        const val SECOND_THEME = "My Family"
    }
}
