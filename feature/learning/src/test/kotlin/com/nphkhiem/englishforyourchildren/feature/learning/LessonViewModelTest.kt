package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.playback.PlaybackFailureCode
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

class LessonViewModelTest {
    private val curriculum = FakeCurriculumRepository()
    private val progress = FakeProgressRepository(timeProvider = FakeTimeProvider(EpochMillis(NOW)))

    /** The lesson's own clock, which is what decides whether a press is an answer. */
    private val clock = FakeTimeProvider(EpochMillis(NOW))

    /** Every recording the course names is unmade, so the shipped answer to any prompt is this. */
    private var playback = FakePlaybackController(failWith = PlaybackFailureCode.MISSING)

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
    fun givenALessonIsOpened_whenItIsStillLoading_thenNothingIsAskedOfTheChildYet() = runTest {
        val model = viewModel()

        assertThat(model.state.value.phase).isEqualTo(LessonPhase.PREPARING)
    }

    @Test
    fun givenALessonThatExists_whenItLoads_thenTheFirstQuestionIsWhatTheContentSays() = runTest {
        val model = started()

        val state = model.state.value
        assertThat(state.prompt).isEqualTo("Where are the eyes?")
        assertThat(state.activityNumber).isEqualTo(1)
        assertThat(state.activityCount).isEqualTo(3)
        assertThat(state.answers.map { it.label }).containsExactly("eyes", "ears")
    }

    @Test
    fun givenAQuestion_whenItIsShown_thenTheScreenIsNeverToldWhichAnswerIsRight() = runTest {
        // The screen cannot give away an answer it does not know. Focus, order and selection are
        // all it has, and none of them may hint.
        val model = started()

        val feedback = model.state.value.answers.map { it.feedback }

        assertThat(feedback.toSet()).containsExactly(HelloBeChoiceFeedbackNeutral)
    }

    @Test
    fun givenALessonThatIsNotThere_whenItIsOpened_thenTheChildIsNotLeftOnAnEmptyStage() = runTest {
        curriculum.failNext(DomainError.LessonNotFound)

        val model = started(lessonId = "u01-my-body-l9")

        assertThat(model.state.value.phase).isEqualTo(LessonPhase.PREPARING)
        assertThat(model.unavailable.value).isTrue()
    }

    @Test
    fun givenNoRecordingExists_whenTheLessonStarts_thenItRunsSilentlyRatherThanWaiting() = runTest {
        // Every prompt recording is unmade, which is the state the app ships in today. It was
        // designed for absent audio, so the lesson runs with the words on screen instead of
        // waiting for a sound that will never come.
        curriculum.setLesson(lesson(withAudio = false))
        val model = started()

        assertThat(model.state.value.audioAvailable).isFalse()
        assertThat(model.state.value.caption).isEqualTo("Where are the eyes?")
    }

    @Test
    fun givenNoRecording_whenAnyAnswerIsChosen_thenItCostsTheChildNothing() = runTest {
        // The consequence of shipping without recordings, stated rather than discovered later: a
        // question nobody could hear is an unscored skip whichever picture is pressed, so a lesson
        // can be walked through but nothing is really being taught yet.
        curriculum.setLesson(lesson(withAudio = false))
        val model = started()

        model.press(LessonUiAction.AnswerChosen("word-ears", activityNumber = 1))

        assertThat(model.state.value.support).isEqualTo(SupportLevel.NONE)
        assertThat(model.state.value.activityNumber).isEqualTo(2)
    }

    @Test
    fun givenACorrectAnswer_whenItIsChosen_thenTheWorkIsWrittenDownBeforeMovingOn() = runTest {
        val model = started()

        model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 1))

        assertThat(progress.persisted).hasSize(1)
        assertThat(model.state.value.activityNumber).isEqualTo(2)
    }

    @Test
    fun givenTheSameAnswerPressedTwice_whenBothArrive_thenTheChildAnsweredOnce() = runTest {
        // A button held a moment too long on a television. The reducer refuses the second, and the
        // serialized queue is what makes sure the second is judged after the first, not beside it.
        val model = started()

        model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 1))
        model.onAction(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 1))

        assertThat(progress.persisted).hasSize(1)
    }

    @Test
    fun givenASecondPressArrivesAfterTheLessonMoved_whenItLands_thenItAnswersNothing() = runTest {
        // The other half of the same gesture, and the one the repeated-input journey found. This
        // press is not stale: the first was written down, the lesson advanced, the screen redrew,
        // and this arrives through the new question's own card carrying its number. Everything
        // else in the app reads that as an answer. It cannot be one, because at that moment the
        // child had been looking at the question for no time at all.
        val model = started()
        model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 1))

        model.onAction(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 2))

        assertThat(progress.persisted).hasSize(1)
        assertThat(model.state.value.activityNumber).isEqualTo(2)
    }

    @Test
    fun givenTheChildHasLookedAtTheNewQuestion_whenTheyAnswerIt_thenItCounts() = runTest {
        // The other side of the same rule, so that refusing a press stays a refusal of one gesture
        // rather than a lesson that has become hard to answer.
        val model = started()
        model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 1))

        model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 2))

        assertThat(progress.persisted).hasSize(2)
        assertThat(model.state.value.activityNumber).isEqualTo(3)
    }

    @Test
    fun givenAWrongAnswer_whenItIsChosen_thenHelpArrivesAndNothingIsWrittenDown() = runTest {
        // Says outright that this needs a recording that plays. A supportive retry only makes
        // sense for a question the child could hear: when the sound is missing the same press is
        // an unscored skip instead, which is the test below.
        val player = FakePlaybackController()
        playback = player
        val model = started()
        // The question has to be heard before it can be answered. See ADR 0004.
        player.finish(PROMPT)

        model.press(LessonUiAction.AnswerChosen("word-ears", activityNumber = 1))

        assertThat(progress.persisted).isEmpty()
        assertThat(model.state.value.support).isEqualTo(SupportLevel.REPEAT)
        assertThat(model.state.value.activityNumber).isEqualTo(1)
    }

    @Test
    fun givenStorageRefusesTheWrite_whenAnAnswerIsChosen_thenTheScreenSaysItIsNotSavedYet() =
        runTest {
            val model = started()
            progress.failNext(DomainError.PersistenceUnavailable)

            model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 1))

            assertThat(model.state.value.pendingSave).isTrue()
            assertThat(model.state.value.activityNumber).isEqualTo(1)
        }

    @Test
    fun givenAFailedWrite_whenTheChildCarriesOn_thenTheyMoveAndItStillSaysNotSaved() = runTest {
        val model = started()
        progress.failNext(DomainError.PersistenceUnavailable)
        model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 1))

        model.press(LessonUiAction.ContinueUnsaved)

        assertThat(model.state.value.activityNumber).isEqualTo(2)
        assertThat(model.state.value.pendingSave).isTrue()
    }

    @Test
    fun givenAFailedWrite_whenItIsRetriedAndSucceeds_thenNothingIsPendingAnyMore() = runTest {
        val model = started()
        progress.failNext(DomainError.PersistenceUnavailable)
        model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 1))

        model.press(LessonUiAction.SaveRetryRequested)

        assertThat(model.state.value.pendingSave).isFalse()
        assertThat(model.state.value.activityNumber).isEqualTo(2)
    }

    @Test
    fun givenALesson_whenBackIsPressed_thenTheStopQuestionIsAskedRatherThanLeaving() = runTest {
        val model = started()

        model.press(LessonUiAction.StopRequested)

        assertThat(model.state.value.stopForNowVisible).isTrue()
    }

    @Test
    fun givenTheLastActivity_whenItIsAnswered_thenTheSessionIsCompleted() = runTest {
        val model = started()

        repeat(3) {
            model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = it + 1))
        }

        assertThat(progress.completed).hasSize(1)
        assertThat(model.state.value.phase).isEqualTo(LessonPhase.COMPLETED)
    }

    @Test
    fun givenALessonIsStarted_whenItBegins_thenASittingIsRecordedForThatChild() = runTest {
        started()

        assertThat(progress.started.single().profileId).isEqualTo(ProfileId(PROFILE))
    }

    @Test
    fun givenAPromptThatNamesARecording_whenTheLessonStarts_thenTheQuestionIsSpoken() = runTest {
        started()

        assertThat(playback.played).containsExactly(AssetId("aud-en-prompt-where-is"))
    }

    @Test
    fun givenTheRecordingWasNeverMade_whenTheLessonStarts_thenItGoesQuietAndShowsTheWords() =
        runTest {
            // The state the app actually ships in. Content names a recording for every prompt and
            // not one of those files exists, so the lesson has to find that out and say so: the
            // question becomes something to read, and the unscored skip becomes reachable.
            val model = started()

            assertThat(model.state.value.audioAvailable).isFalse()
            assertThat(model.state.value.caption).isEqualTo("Where are the eyes?")
        }

    @Test
    fun givenSoundIsUnavailable_whenTheChildTakesTheSkip_thenTheyMoveOnWithoutBeingWrong() =
        runTest {
            val model = started()

            model.press(LessonUiAction.SkipRequested)

            assertThat(model.state.value.activityNumber).isEqualTo(2)
            assertThat(model.state.value.support).isEqualTo(SupportLevel.NONE)
        }

    @Test
    fun givenTheLessonScreenGoesAway_whenTheModelIsCleared_thenThePlayerIsLetGo() {
        val store = ViewModelStore()
        ViewModelProvider(
            store,
            viewModelFactory { initializer { viewModel() } }
        )[LessonViewModel::class.java]

        store.clear()

        assertThat(playback.stopped).isTrue()
    }

    @Test
    fun givenTheStopQuestionIsOpen_whenTheChildKeepsLearning_thenTheLessonIsStillThere() = runTest {
        val model = started()
        model.press(LessonUiAction.StopRequested)
        assertThat(model.state.value.stopForNowVisible).isTrue()

        model.press(LessonUiAction.KeepLearningRequested)

        assertThat(model.state.value.stopForNowVisible).isFalse()
        assertThat(model.state.value.activityNumber).isEqualTo(1)
    }

    @Test
    fun givenTheRecordingsPlay_whenTheChildMovesOn_thenTheNextQuestionIsSpokenToo() = runTest {
        // Every question asks itself, not just the first one.
        val player = FakePlaybackController()
        playback = player
        val model = started()
        player.finish(PROMPT)

        model.press(LessonUiAction.AnswerChosen("word-eyes", activityNumber = 1))

        assertThat(model.state.value.activityNumber).isEqualTo(2)
        assertThat(playback.played).hasSize(2)
    }

    @Test
    fun givenTheLessonHasGoneQuiet_whenTheChildMovesOn_thenItStopsAskingForSound() = runTest {
        // Having found out once that there are no recordings, it does not go asking five more
        // times on the way through the lesson.
        val model = started()

        model.press(LessonUiAction.SkipRequested)

        assertThat(model.state.value.activityNumber).isEqualTo(2)
        assertThat(playback.played).hasSize(1)
    }

    @Test
    fun givenAChildLeftPartWayThrough_whenTheLessonOpensAgain_thenItPicksUpWhereTheyStopped() =
        runTest {
            // What the stop-for-now dialog promises: "Pip will remember your last finished
            // activity." The checkpoint names the last one finished, so the child is on the next.
            progress.setOpenCheckpoint(
                DomainBuilders.lessonCheckpoint(
                    lastCompletedActivity = ActivityId("$LESSON-a1")
                )
            )

            val model = started()

            assertThat(model.state.value.activityNumber).isEqualTo(2)
        }

    @Test
    fun givenACheckpointNamingAnActivityThatIsGone_whenItOpens_thenItStartsAtTheBeginning() =
        runTest {
            // Content can move under a saved checkpoint. Repeating a question costs a child
            // nothing, so this opens the lesson rather than refusing it.
            progress.setOpenCheckpoint(
                DomainBuilders.lessonCheckpoint(lastCompletedActivity = ActivityId("gone-a9"))
            )

            val model = started()

            assertThat(model.state.value.activityNumber).isEqualTo(1)
        }

    @Test
    fun givenStorageCannotSayWhereTheChildWas_whenTheLessonOpens_thenItIsNotQuietlyRestarted() =
        runTest {
            // Silently starting over would throw away work a child had already done and claim
            // nothing was wrong. An adult is asked instead.
            progress.setCheckpointFailure(DomainError.PersistenceUnavailable)

            val model = started()

            assertThat(model.unavailable.value).isTrue()
        }

    @Test
    fun givenSayingItWithPip_whenTheChildAsksToMoveOn_thenTheyDoAndItCountsAsPractised() = runTest {
        // Say with Pip has nothing to answer, so Next is the only way past it. Before the renderer
        // was wired it was a button that did nothing.
        curriculum.setLesson(speakingLesson())
        val model = started()

        model.press(LessonUiAction.RepetitionFinished)

        assertThat(model.state.value.activityNumber).isEqualTo(2)
        assertThat(progress.persisted.single().outcome)
            .isEqualTo(com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome.PRACTISED)
    }

    private fun speakingLesson() = Lesson(
        id = LessonId(LESSON),
        unitId = UnitId(UNIT),
        ordinal = 0,
        activities = listOf(
            Activity(
                id = ActivityId("$LESSON-a1"),
                ordinal = 0,
                family = ActivityFamily.SAY_WITH_PIP,
                content = ActivityContent.GuidedRepetition(
                    prompt = "Say it with me: eyes.",
                    promptAsset = promptAsset(withAudio = false),
                    words = choices()
                )
            ),
            activity(2, ActivityFamily.LISTEN_AND_CHOOSE, withAudio = false)
        )
    )

    @Test
    fun givenContentThatNamesPictures_whenALessonOpens_thenEachAnswerCarriesItsOwn() = runTest {
        // The content has named a picture per choice since the course was packaged. The mapper
        // dropped it, so a pre-reader was shown the word instead.
        val model = started()

        assertThat(model.state.value.answers.map { it.image })
            .containsExactly("img-eyes", "img-ears")
            .inOrder()
    }

    @Test
    fun givenAPictureMatchingQuestion_whenItArrives_thenItsLearningObjectCarriesItsPicture() =
        runTest {
            val model = started()

            model.press(LessonUiAction.SkipRequested)

            assertThat(model.state.value.learningObject?.image).isEqualTo("img-eyes")
        }

    @Test
    fun givenARecordingThatPlays_whenTheLessonOpens_thenTheQuestionIsStillBeingAsked() = runTest {
        // The state ADR 0004 describes, reachable for the first time. Every recording is unmade
        // today, so this is the lesson the packaged audio will produce rather than the one it does.
        playback = FakePlaybackController()

        val model = started()

        assertThat(model.state.value.phase).isEqualTo(LessonPhase.PROMPTING)
    }

    @Test
    fun givenAQuestionIsBeingAsked_whenItsRecordingEnds_thenTheChildMayAnswer() = runTest {
        // PlaybackEvent.Completed has had no consumer since the playback capability was built.
        val player = FakePlaybackController()
        playback = player
        val model = started()

        player.finish(PROMPT)

        assertThat(model.state.value.phase).isEqualTo(LessonPhase.ANSWERING)
    }

    private fun viewModel() = LessonViewModel(
        curriculum = curriculum,
        progress = progress,
        timeProvider = clock,
        playback = playback
    )

    /**
     * A child who has looked at the question, and then presses.
     *
     * Every deliberate press in these tests goes through here, because a lesson refuses one that
     * lands the instant a question appears: see the double press above, which is the same call
     * without this. A fake clock is what makes that a rule these tests can state rather than a
     * race they would have to lose on purpose.
     */
    private suspend fun LessonViewModel.press(action: LessonUiAction) {
        clock.advanceBy(LOOKED_AT_IT_MILLIS)
        onAction(action)
    }

    private suspend fun started(lessonId: String = LESSON): LessonViewModel {
        val model = viewModel()
        model.start(
            profileId = ProfileId(PROFILE),
            lessonId = LessonId(lessonId),
            courseVersion = CourseVersion(VERSION)
        )
        return model
    }

    private fun lesson(withAudio: Boolean = true) = Lesson(
        id = LessonId(LESSON),
        unitId = UnitId(UNIT),
        ordinal = 0,
        activities = listOf(
            activity(1, ActivityFamily.LISTEN_AND_CHOOSE, withAudio),
            activity(2, ActivityFamily.PICTURE_MATCHING, withAudio),
            activity(3, ActivityFamily.REVIEW, withAudio)
        )
    )

    private fun activity(n: Int, family: ActivityFamily, withAudio: Boolean) = Activity(
        id = ActivityId("$LESSON-a$n"),
        ordinal = n - 1,
        family = family,
        content = when (family) {
            ActivityFamily.PICTURE_MATCHING -> ActivityContent.PictureMatching(
                prompt = "Where are the eyes?",
                promptAsset = promptAsset(withAudio),
                choices = choices(),
                correct = SkillId("word-eyes")
            )

            ActivityFamily.REVIEW -> ActivityContent.ReviewQuestion(
                prompt = "Where are the eyes?",
                promptAsset = promptAsset(withAudio),
                choices = choices(),
                correct = SkillId("word-eyes")
            )

            else -> ActivityContent.ListeningSelection(
                prompt = "Where are the eyes?",
                promptAsset = promptAsset(withAudio),
                choices = choices(),
                correct = SkillId("word-eyes")
            )
        }
    )

    private fun promptAsset(withAudio: Boolean) = if (withAudio) PROMPT else null

    private fun choices() = listOf(choice("eyes"), choice("ears"))

    private fun choice(word: String) = AnswerChoice(
        skillId = SkillId("word-$word"),
        label = word,
        image = AssetId("img-$word"),
        audio = AssetId("aud-en-$word")
    )

    private companion object {
        const val PROFILE = "p1"
        const val VERSION = "2026.09"
        const val UNIT = "u01-my-body"
        const val LESSON = "u01-my-body-l1"
        const val NOW = 1_756_000_000_000

        /** Longer than a lesson's own window, which is what makes a press a considered one. */
        const val LOOKED_AT_IT_MILLIS = 400L
        val PROMPT = AssetId("aud-en-prompt-where-is")
        val HelloBeChoiceFeedbackNeutral =
            com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback.NEUTRAL
    }
}
