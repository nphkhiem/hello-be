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
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
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

    @Test
    fun givenPractice_whenTheOverviewOpens_thenItSuggestsWhatGoesWithTheLatestSession() = overview(
        attempts = listOf(
            metWord(EYES_ACTIVITY, at = NOW),
            metWord(SECOND_LESSON_ACTIVITY, at = NOW + A_MINUTE)
        )
    ) { model ->
        // The latest, not the first. A caregiver reading this after two sessions wants the
        // idea that goes with the one their child just did.
        assertThat(model.state.value.suggestion?.title).isEqualTo(SECOND_IDEA)
    }

    @Test
    fun givenAnEarlierSessionCameLast_whenTheOverviewOpens_thenTheSuggestionFollowsTheClock() =
        overview(
            attempts = listOf(
                metWord(SECOND_LESSON_ACTIVITY, at = NOW),
                metWord(EYES_ACTIVITY, at = NOW + A_MINUTE)
            )
        ) { model ->
            // Storage may hand back attempts in any order, so the answer has to come from the time
            // on them rather than from their position in the list.
            assertThat(model.state.value.suggestion?.title).isEqualTo(FIRST_IDEA)
        }

    @Test
    fun givenAChildWhoHasNeverPractised_whenTheOverviewOpens_thenThereIsNothingToSuggest() =
        overview { model ->
            // The brief's unavailable-content state. A suggestion invented for a child who has done
            // nothing yet would be about no lesson at all.
            assertThat(model.state.value.suggestion).isNull()
        }

    @Test
    fun givenALessonOfferingNothingAwayFromTheScreen_whenItIsPractised_thenNothingIsSuggested() =
        overview(
            course = DomainBuilders.course(
                units = listOf(
                    DomainBuilders.courseUnit(
                        lessons = listOf(
                            DomainBuilders.lesson(
                                activities = listOf(
                                    asking(EYES_ACTIVITY, ordinal = 0, correct = "eyes")
                                )
                            )
                        )
                    )
                )
            ),
            attempts = listOf(metWord(EYES_ACTIVITY))
        ) { model ->
            assertThat(model.state.value.suggestion).isNull()
        }

    @Test
    fun givenWordsThatNeededHelp_whenTheOverviewOpens_thenItNamesTheUnitTheyLiveIn() =
        overview(attempts = listOf(neededHelp(EYES_ACTIVITY))) { model ->
            // The one distinction this summary may draw, followed through: not that the child was
            // wrong, but that these words are worth another go, and here is where they are.
            assertThat(model.state.value.unitToPractise).isEqualTo("My Body")
        }

    @Test
    fun givenNothingWaitingForAnotherGo_whenTheOverviewOpens_thenNoUnitIsNamed() =
        overview(attempts = listOf(metWord(EYES_ACTIVITY))) { model ->
            // A practice suggestion for a child with nothing to practise is a deficit invented to
            // fill a card, which is the one thing this panel may never do.
            assertThat(model.state.value.unitToPractise).isNull()
        }

    @Test
    fun givenTwoUnitsWithWordsWaiting_whenTheOverviewOpens_thenItNamesTheOneWithMore() = overview(
        course = twoUnits(),
        attempts = listOf(
            neededHelp(SECOND_UNIT_EYES),
            neededHelp(SECOND_UNIT_EARS)
        )
    ) { model ->
        assertThat(model.state.value.unitToPractise).isEqualTo(SECOND_THEME)
    }

    /** Two units whose words are all waiting, so which one is named is decided by how many. */
    private fun twoUnits() = DomainBuilders.course(
        units = listOf(
            DomainBuilders.courseUnit(
                lessons = listOf(
                    DomainBuilders.lesson(
                        teaches = listOf(SkillId("word-nose")),
                        activities = listOf(asking(EYES_ACTIVITY, ordinal = 0, correct = "nose"))
                    )
                )
            ),
            DomainBuilders.courseUnit(
                id = UnitId(SECOND_UNIT),
                ordinal = 1,
                theme = SECOND_THEME,
                word = "family",
                lessons = listOf(
                    DomainBuilders.lesson(
                        id = LessonId("$SECOND_UNIT-l1"),
                        unitId = UnitId(SECOND_UNIT),
                        teaches = listOf(SkillId("word-eyes"), SkillId("word-ears")),
                        activities = listOf(
                            asking(SECOND_UNIT_EYES, ordinal = 0, correct = "eyes"),
                            asking(SECOND_UNIT_EARS, ordinal = 1, correct = "ears")
                        )
                    )
                )
            )
        )
    )

    private fun courseWithWords() = DomainBuilders.course(
        units = listOf(
            DomainBuilders.courseUnit(
                lessons = listOf(
                    DomainBuilders.lesson(
                        teaches = listOf(SkillId("word-eyes"), SkillId("word-ears")),
                        activities = listOf(
                            asking(EYES_ACTIVITY, ordinal = 0, correct = "eyes"),
                            asking(EARS_ACTIVITY, ordinal = 1, correct = "ears")
                        ),
                        coPlay = DomainBuilders.coPlayIdea(title = FIRST_IDEA)
                    ),
                    DomainBuilders.lesson(
                        id = LessonId(SECOND_LESSON),
                        ordinal = 1,
                        activities = listOf(
                            asking(SECOND_LESSON_ACTIVITY, ordinal = 0, correct = "nose")
                        ),
                        coPlay = DomainBuilders.coPlayIdea(title = SECOND_IDEA)
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
            choices = listOf(choice("eyes"), choice("ears"), choice("nose")),
            correct = SkillId("word-$correct")
        )
    )

    private fun choice(word: String) = AnswerChoice(
        skillId = SkillId("word-$word"),
        label = word,
        image = AssetId("img-$word"),
        audio = AssetId("aud-en-$word")
    )

    private fun metWord(activity: String = EYES_ACTIVITY, at: Long = NOW) =
        DomainBuilders.activityAttempt(
            activityId = ActivityId(activity),
            activityInstance = ActivityInstanceId("$activity-1"),
            outcome = AttemptOutcome.CORRECT,
            at = EpochMillis(at)
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
        completed: Set<LessonId> = emptySet(),
        course: Course = courseWithWords(),
        body: suspend TestScope.(CaregiverOverviewViewModel) -> Unit
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        // A course whose activity actually names skills. Without content there is nothing for an
        // attempt to have been practice of, and every count would be zero for the wrong reason.
        curriculum.setCourse(course)
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
        const val SECOND_LESSON = "u01-my-body-l2"
        const val SECOND_LESSON_ACTIVITY = "u01-my-body-l2-a1"
        const val SECOND_UNIT = "u02-my-family"
        const val SECOND_THEME = "My Family"
        const val SECOND_UNIT_EYES = "u02-my-family-l1-a1"
        const val SECOND_UNIT_EARS = "u02-my-family-l1-a2"
        const val FIRST_IDEA = "Touch and name"
        const val SECOND_IDEA = "Count together"
        const val NOW = 1_756_000_000_000
        const val A_MINUTE = 60_000L
    }
}
