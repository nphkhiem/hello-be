package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.testsupport.DomainBuilders
import com.nphkhiem.englishforyourchildren.testsupport.FakeCurriculumRepository
import com.nphkhiem.englishforyourchildren.testsupport.FakeProfileRepository
import com.nphkhiem.englishforyourchildren.testsupport.FakeProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * What a caregiver is told about their child's practice, and what they are deliberately not.
 *
 * The design brief bans ranking language and the information architecture calls this summary
 * privacy minimised. Both come down to one rule that is easy to break by accident: a caregiver sees
 * how much has been met, never what was got wrong. Counting is the whole of the job here.
 */
class CaregiverOverviewViewModelTest {
    private val profiles = FakeProfileRepository()
    private val progress = FakeProgressRepository()
    private val curriculum = FakeCurriculumRepository()

    @AfterEach
    fun releaseMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenAChildWhoHasNeverPractised_whenTheOverviewOpens_thenItSaysSoRatherThanShowingZeroes() =
        overview { model ->
            // "No sessions yet" and "nothing this week" are different things to a caregiver. A row
            // of zeroes would be a third thing, and the wrong one: it reads as a poor result.
            assertThat(model.state.value.counts).isNull()
        }

    @Test
    fun givenPractice_whenTheOverviewOpens_thenItCountsTheWordsThatWereMet() = overview(
        attempts = listOf(metWord(EYES_ACTIVITY), metWord(EARS_ACTIVITY))
    ) { model ->
        assertThat(model.state.value.counts?.wordsMet).isEqualTo(2)
    }

    @Test
    fun givenAFinishedLesson_whenTheOverviewOpens_thenItCountsTheLesson() = overview(
        completed = setOf(DomainBuilders.lesson().id)
    ) { model ->
        assertThat(model.state.value.counts?.lessonsFinished).isEqualTo(1)
    }

    @Test
    fun givenTheSameWordMetTwice_whenItIsCounted_thenItIsStillOneWord() = overview(
        attempts = listOf(metWord(), metWord())
    ) { model ->
        // A count of exposures would say two. A caregiver reading "2 words" would be told the child
        // met something they did not.
        assertThat(model.state.value.counts?.wordsMet).isEqualTo(1)
    }

    @Test
    fun givenTheChildIsNamed_whenTheOverviewOpens_thenItIsTheirName() = overview { model ->
        assertThat(model.state.value.profileName).isEqualTo(DomainBuilders.childProfile().nickname)
    }

    @Test
    fun givenAWordThatNeededHelp_whenItIsCounted_thenTheChildStillMetIt() {
        // A supportive retry is a child in front of a question doing something about it. Counting
        // it as anything less would turn a summary of practice into a record of what went wrong,
        // which is the one thing the design brief forbids this screen to be.
        overview(attempts = listOf(neededHelp(EYES_ACTIVITY))) { model ->
            assertThat(model.state.value.counts?.wordsMet).isEqualTo(1)
        }
    }

    @Test
    fun givenAWordThatNeededHelp_whenItIsCounted_thenItIsNamedAsWorthPractising() {
        // The one distinction this summary is allowed to draw, and it is the actionable one: not
        // that the child was wrong, but that this word is worth coming back to.
        overview(attempts = listOf(neededHelp(EYES_ACTIVITY))) { model ->
            assertThat(model.state.value.counts?.wordsNeedingReview).isEqualTo(1)
        }

        overview(attempts = listOf(metWord(EYES_ACTIVITY))) { model ->
            assertThat(model.state.value.counts?.wordsNeedingReview).isEqualTo(0)
        }
    }

    @Test
    fun givenAWordTheChildWentPast_whenItIsCounted_thenTheyNeverMetIt() {
        // The one outcome that is not a meeting. An unscored skip is offered when the question
        // could not be asked properly, and counting it would tell a caregiver their child met a
        // word they were never able to hear.
        overview(attempts = listOf(wentPast(EYES_ACTIVITY))) { model ->
            assertThat(model.state.value.counts).isNull()
        }
    }

    private fun courseWithWords() = DomainBuilders.course(
        units = listOf(
            DomainBuilders.courseUnit(
                lessons = listOf(
                    DomainBuilders.lesson(
                        activities = listOf(
                            asking(EYES_ACTIVITY, ordinal = 0, correct = "eyes"),
                            asking(EARS_ACTIVITY, ordinal = 1, correct = "ears")
                        )
                    )
                )
            )
        )
    )

    /**
     * One question with a right answer.
     *
     * Only the correct skill counts as met, so two different words need two questions. Two attempts
     * at the same question are the same word met twice, which is what one of these tests is about.
     */
    private fun asking(id: String, ordinal: Int, correct: String) = Activity(
        id = ActivityId(id),
        ordinal = ordinal,
        family = ActivityFamily.LISTEN_AND_CHOOSE,
        content = ActivityContent.ListeningSelection(
            prompt = "Where are the $correct?",
            promptAsset = null,
            choices = listOf(choice("eyes"), choice("ears")),
            correct = SkillId("word-$correct")
        )
    )

    private fun choice(word: String) = AnswerChoice(
        skillId = SkillId("word-$word"),
        label = word,
        image = AssetId("img-$word"),
        audio = AssetId("aud-en-$word")
    )

    private fun metWord(activity: String = EYES_ACTIVITY) = DomainBuilders.activityAttempt(
        activityId = ActivityId(activity),
        activityInstance = ActivityInstanceId("$activity-1"),
        outcome = AttemptOutcome.CORRECT,
        at = EpochMillis(NOW)
    )

    /** A word the child got to with Pip's help, which is still a word they met. */
    private fun neededHelp(activity: String) = DomainBuilders.activityAttempt(
        activityId = ActivityId(activity),
        activityInstance = ActivityInstanceId("$activity-1"),
        outcome = AttemptOutcome.SUPPORTIVE_RETRY,
        at = EpochMillis(NOW)
    )

    /** The fair way past a question that could not be asked. Not a meeting. */
    private fun wentPast(activity: String) = DomainBuilders.activityAttempt(
        activityId = ActivityId(activity),
        activityInstance = ActivityInstanceId("$activity-1"),
        outcome = AttemptOutcome.UNSCORED_SKIP,
        at = EpochMillis(NOW)
    )

    private fun overview(
        attempts: List<ActivityAttempt> = emptyList(),
        completed: Set<com.nphkhiem.englishforyourchildren.domain.model.LessonId> = emptySet(),
        body: suspend TestScope.(CaregiverOverviewViewModel) -> Unit
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        // A course whose activity actually names skills. Without content there is nothing for an
        // attempt to have been practice of, and every count would be zero for the wrong reason.
        curriculum.setCourse(courseWithWords())
        profiles.setProfiles(listOf(DomainBuilders.childProfile()))
        progress.setProgress(
            DomainBuilders.profileProgress(
                profileId = ProfileId(PROFILE),
                attempts = attempts,
                lessonsCompleted = completed
            )
        )
        val store = ViewModelStore()
        val model = ViewModelProvider(
            store,
            viewModelFactory {
                initializer { CaregiverOverviewViewModel(profiles, progress, curriculum) }
            }
        )[CaregiverOverviewViewModel::class.java]
        model.start(ProfileId(PROFILE))

        try {
            body(model)
        } finally {
            store.clear()
        }
    }

    private companion object {
        const val PROFILE = "p1"
        const val EYES_ACTIVITY = "u01-my-body-l1-a1"
        const val EARS_ACTIVITY = "u01-my-body-l1-a2"
        const val NOW = 1_756_000_000_000
    }
}
