package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.Activity
import com.nphkhiem.englishforyourchildren.domain.model.ActivityContent
import com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.AnswerChoice
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import com.nphkhiem.englishforyourchildren.testsupport.DomainBuilders
import com.nphkhiem.englishforyourchildren.testsupport.FakeCurriculumRepository
import com.nphkhiem.englishforyourchildren.testsupport.FakeProgressRepository
import com.nphkhiem.englishforyourchildren.testsupport.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LessonCelebrationViewModelTest {
    private val curriculum = FakeCurriculumRepository()
    private val progress = FakeProgressRepository(timeProvider = FakeTimeProvider(EpochMillis(NOW)))

    @BeforeEach
    fun useTestDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        curriculum.setLesson(lesson())
    }

    @AfterEach
    fun restoreDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenAFinishedLesson_whenTheCelebrationOpens_thenItNamesTheWordsTheLessonTaught() =
        runTest {
            // Labelled as a child saw them on the cards. "word-eyes" is a key, "eyes" is a word.
            val model = started()

            assertThat(model.state.value.words.map { it.label })
                .containsExactly("eyes", "ears")
                .inOrder()
        }

    @Test
    fun givenTheUnitNamesItsWord_whenTheCelebrationOpens_thenTheHeadlineHasOneToUse() = runTest {
        val model = started()

        assertThat(model.state.value.unitWord).isEqualTo("body")
    }

    @Test
    fun givenTheWriteHasNotLandedYet_whenTheCelebrationOpens_thenNothingClaimsItIsSaved() =
        runTest {
            // The brief's rule, checked rather than assumed: no page says a child's work is stored
            // before storage says so.
            val model = started()

            assertThat(model.state.value.saveConfirmed).isFalse()
        }

    @Test
    fun givenStorageSaysTheLessonIsFinished_whenTheCelebrationOpens_thenTheStorybookSaysSo() =
        runTest {
            progress.setProgress(
                DomainBuilders.profileProgress(lessonsCompleted = setOf(LessonId(LESSON)))
            )

            val model = started()

            assertThat(model.state.value.saveConfirmed).isTrue()
        }

    @Test
    fun givenNoPlayTogetherActivityIsPackaged_whenTheCelebrationOpens_thenNoneIsOffered() =
        runTest {
            // A suppressed prompt is simply one that was never offered, which is what null means.
            val model = started()

            assertThat(model.state.value.playTogether).isNull()
        }

    private suspend fun started(): LessonCelebrationViewModel {
        val model = LessonCelebrationViewModel(curriculum = curriculum, progress = progress)
        model.start(
            profileId = ProfileId(PROFILE),
            lessonId = LessonId(LESSON),
            courseVersion = CourseVersion(VERSION)
        )
        return model
    }

    private fun lesson() = Lesson(
        id = LessonId(LESSON),
        unitId = UnitId(UNIT),
        ordinal = 0,
        teaches = listOf(SkillId("word-eyes"), SkillId("word-ears")),
        activities = listOf(activity())
    )

    private fun activity() = Activity(
        id = ActivityId("$LESSON-a1"),
        ordinal = 0,
        family = ActivityFamily.LISTEN_AND_CHOOSE,
        content = ActivityContent.ListeningSelection(
            prompt = "Where are the eyes?",
            promptAsset = AssetId("aud-en-prompt-where-is"),
            choices = listOf(choice("eyes"), choice("ears")),
            correct = SkillId("word-eyes")
        )
    )

    private fun choice(word: String) = AnswerChoice(
        skillId = SkillId("word-$word"),
        label = word,
        image = AssetId("img-$word"),
        audio = AssetId("aud-en-$word")
    )

    private companion object {
        const val PROFILE = "p1"
        const val UNIT = "u01-my-body"
        const val LESSON = "u01-my-body-l1"
        const val VERSION = "2026.09"
        const val NOW = 1_756_000_000_000
    }
}
