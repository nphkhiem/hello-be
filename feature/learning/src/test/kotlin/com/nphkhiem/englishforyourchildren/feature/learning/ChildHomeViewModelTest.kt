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
 * What the dominant slot on child home offers, and where it goes.
 *
 * The screen has drawn all four of these since HB-D05 and the copy for each has been written since;
 * what was missing was anything to decide between them. Until now this ViewModel read a name and a
 * picture and said "Start an adventure" to a child who had finished nine lessons.
 *
 * The brief asks for one Select press to the next useful activity, so the offer has to name the
 * lesson it means rather than pointing at the path and letting the child find it again.
 */
class ChildHomeViewModelTest {
    private val profiles = FakeProfileRepository()
    private val progress = FakeProgressRepository()
    private val curriculum = FakeCurriculumRepository()

    @BeforeEach
    fun useTestDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        profiles.setProfiles(listOf(DomainBuilders.childProfile(id = ProfileId(PROFILE))))
        curriculum.setCourse(courseOfThree())
    }

    @AfterEach
    fun restoreDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenAChildWhoHasDoneNothing_whenHomeOpens_thenItOffersTheirFirstAdventure() = runTest {
        val model = started()

        assertThat(model.state.value.primary).isEqualTo(HomePrimary.StartFirstAdventure)
        assertThat(model.state.value.continueTarget).isEqualTo(LessonId("$LESSON-1"))
    }

    @Test
    fun givenAChildPartWayThrough_whenHomeOpens_thenItOffersToCarryOnAndSaysWhere() = runTest {
        progress.setProgress(
            DomainBuilders.profileProgress(lessonsCompleted = setOf(LessonId("$LESSON-1")))
        )

        val model = started()

        assertThat(
            model.state.value.primary
        ).isEqualTo(HomePrimary.Resume(context = "My Body, Lesson 2"))
        assertThat(model.state.value.continueTarget).isEqualTo(LessonId("$LESSON-2"))
    }

    @Test
    fun givenAChildWhoHasFinishedEverything_whenHomeOpens_thenFreePlayIsWhatIsLeft() = runTest {
        progress.setProgress(
            DomainBuilders.profileProgress(
                lessonsCompleted = setOf(
                    LessonId("$LESSON-1"),
                    LessonId("$LESSON-2"),
                    LessonId("$LESSON-3")
                )
            )
        )

        val model = started()

        assertThat(model.state.value.primary).isEqualTo(HomePrimary.CourseComplete)
        assertThat(model.state.value.continueTarget).isNull()
    }

    @Test
    fun givenStorageCannotSayWhatTheChildHasDone_whenHomeOpens_thenNothingIsClaimedFinished() =
        runTest {
            // A television that cannot read its history is not a child who has finished. Offering
            // the first adventure is the answer that cannot be wrong about them.
            progress.setReadFailure(DomainError.PersistenceUnavailable)

            val model = started()

            assertThat(model.state.value.primary).isEqualTo(HomePrimary.StartFirstAdventure)
        }

    @Test
    fun givenACheckpointInALessonTheCourseHasLost_whenHomeOpens_thenItSaysSoRatherThanHiding() =
        runTest {
            // Content moves under a child's history. The screen has copy for this and a rule that
            // makes the control unpressable; what it needed was somebody to notice.
            progress.setProgress(
                DomainBuilders.profileProgress(
                    lessonsCompleted = setOf(LessonId("$LESSON-1")),
                    openCheckpoint = DomainBuilders.lessonCheckpoint(
                        lessonId = LessonId("u09-somewhere-l1")
                    )
                )
            )

            val model = started()

            assertThat(
                model.state.value.primary
            ).isInstanceOf(HomePrimary.ResumeUnavailable::class.java)
            assertThat(model.state.value.continueTarget).isNull()
        }

    @Test
    fun givenAWriteStillLanding_whenHomeOpens_thenItSaysProgressIsPending() = runTest {
        progress.setProgress(
            DomainBuilders.profileProgress(
                lessonsCompleted = setOf(LessonId("$LESSON-1")),
                openCheckpoint = DomainBuilders.lessonCheckpoint(lessonId = LessonId("$LESSON-2"))
            )
        )

        val model = started()

        assertThat(model.state.value.pendingSave).isTrue()
    }

    private fun started(): ChildHomeViewModel {
        val model = ChildHomeViewModel(profiles, progress, curriculum)
        model.start(ProfileId(PROFILE))
        return model
    }

    private fun courseOfThree() = DomainBuilders.course(
        units = listOf(
            DomainBuilders.courseUnit(
                lessons = (0 until 3).map {
                    DomainBuilders.lesson(
                        id = LessonId("$LESSON-${it + 1}"),
                        unitId = UnitId("u01-my-body"),
                        ordinal = it
                    )
                }
            )
        )
    )

    private companion object {
        const val PROFILE = "p1"
        const val LESSON = "u01-my-body-l"
    }
}
