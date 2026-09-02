package com.nphkhiem.englishforyourchildren.feature.learning

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
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import com.nphkhiem.englishforyourchildren.playback.PlaybackFailureCode
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
 * A library of the words a child has already met.
 *
 * Learned-content-only, per the information architecture, and nothing here may change what a child
 * has formally done. Free play is a place to revisit rather than another place to be assessed.
 */
class FreePlayViewModelTest {
    private val curriculum = FakeCurriculumRepository()
    private val progress = FakeProgressRepository()
    private val profiles = FakeProfileRepository()
    private var playback = FakePlaybackController()

    @BeforeEach
    fun useTestDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        curriculum.setCourse(course())
    }

    @AfterEach
    fun restoreDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenAChildWhoHasFinishedNothing_whenFreePlayOpens_thenThereIsNoShelfYet() {
        // What a child sees before their first lesson. The screen draws that as an explanation
        // rather than as a failure, so an empty library is a state and not an error.
        val model = started()

        assertThat(model.state.value.shelves).isEmpty()
    }

    @Test
    fun givenAFinishedLesson_whenFreePlayOpens_thenItsWordsAreOnAShelfNamedForTheUnit() {
        finish(FIRST_LESSON)

        val model = started()

        val shelf = model.state.value.shelves.single()
        assertThat(shelf.name).isEqualTo("My Body")
        assertThat(shelf.objects.map { it.id }).containsExactly(EYES, EARS)
    }

    @Test
    fun givenAWordTaughtByTwoLessons_whenTheShelfIsBuilt_thenItIsOnItOnce() {
        finish(FIRST_LESSON, SECOND_LESSON)

        val model = started()

        assertThat(model.state.value.shelves.single().objects.map { it.id })
            .containsNoDuplicates()
    }

    @Test
    fun givenAShelf_whenItIsChosen_thenItsWordsAreWhatTheChildIsLookingAt() {
        finish(FIRST_LESSON)
        val model = started()

        model.onAction(FreePlayAction.ShelfChosen(UNIT))

        assertThat(model.state.value.openShelf?.id).isEqualTo(UNIT)
    }

    @Test
    fun givenAnOpenShelf_whenTheChildGoesBack_thenTheyAreAmongTheShelvesAgain() {
        finish(FIRST_LESSON)
        val model = started()
        model.onAction(FreePlayAction.ShelfChosen(UNIT))

        model.onAction(FreePlayAction.ShelvesRequested)

        assertThat(model.state.value.openShelf).isNull()
    }

    @Test
    fun givenAWordOnAShelf_whenItIsPressed_thenItsOwnRecordingIsAskedFor() {
        finish(FIRST_LESSON)
        val model = started()

        model.onAction(FreePlayAction.ObjectChosen(EYES))

        assertThat(playback.played.map { it.value }).containsExactly("aud-en-eyes")
    }

    @Test
    fun givenAWordWhoseRecordingIsUnmade_whenItIsPressed_thenTheShelfSaysThereIsNoSound() {
        // Every recording is unmade today, so this is free play as it currently ships. The words
        // stay reachable and the screen says why nothing was heard.
        playback = FakePlaybackController(
            failWith = PlaybackFailureCode.MISSING
        )
        finish(FIRST_LESSON)
        val model = started()

        model.onAction(FreePlayAction.ObjectChosen(EYES))

        assertThat(model.state.value.audioAvailable).isFalse()
        assertThat(model.state.value.speakingObjectId).isNull()
    }

    @Test
    fun givenAChildPlayingWithEveryWord_whenTheyAreDone_thenNothingAboutTheirProgressMoved() {
        // The acceptance criterion, as a property of the shape: this ViewModel is handed the
        // progress repository and calls no write on it, so exploring cannot unlock or score.
        finish(FIRST_LESSON)
        val model = started()

        model.state.value.shelves.single().objects.forEach {
            model.onAction(FreePlayAction.ObjectChosen(it.id))
        }

        assertThat(progress.persisted).isEmpty()
        assertThat(progress.completed).isEmpty()
        assertThat(progress.started).isEmpty()
    }

    private fun finish(vararg lessons: String) {
        progress.setProgress(
            DomainBuilders.profileProgress(
                lessonsCompleted = lessons.map { LessonId(it) }.toSet()
            )
        )
    }

    @Test
    fun givenAShelfTheChildWasLastIn_whenFreePlayOpens_thenItOpensStraightIntoIt() {
        finish(FIRST_LESSON)

        val model = started(preferredShelfId = UNIT)

        assertThat(model.state.value.openShelf?.id).isEqualTo(UNIT)
    }

    @Test
    fun givenARememberedShelfThatIsGone_whenFreePlayOpens_thenTheChildIsAmongTheShelves() {
        // A shelf can go between sessions, and opening into one that is not there would strand a
        // child on a page with nothing on it.
        finish(FIRST_LESSON)

        val model = started(preferredShelfId = "u09-nothing-here")

        assertThat(model.state.value.openShelf).isNull()
    }

    private fun started(preferredShelfId: String? = null): FreePlayViewModel {
        val model = FreePlayViewModel(curriculum, progress, profiles, playback)
        model.start(ProfileId("p1"), preferredShelfId)
        return model
    }

    /** A unit whose lessons teach real words, because free play is about the words. */
    private fun course() = Course(
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
                lessons = listOf(
                    lesson(FIRST_LESSON, 0, listOf(EYES, EARS)),
                    lesson(SECOND_LESSON, 1, listOf(EARS, NOSE))
                )
            )
        )
    )

    private fun lesson(id: String, ordinal: Int, teaches: List<String>) = Lesson(
        id = LessonId(id),
        unitId = UnitId(UNIT),
        ordinal = ordinal,
        teaches = teaches.map { SkillId(it) },
        activities = listOf(
            Activity(
                id = ActivityId("$id-a1"),
                ordinal = 0,
                family = ActivityFamily.LISTEN_AND_CHOOSE,
                content = ActivityContent.ListeningSelection(
                    prompt = "Where are the eyes?",
                    promptAsset = null,
                    choices = teaches.map { choice(it) },
                    correct = SkillId(teaches.first())
                )
            )
        )
    )

    private fun choice(skill: String) = AnswerChoice(
        skillId = SkillId(skill),
        label = skill.removePrefix("word-"),
        image = AssetId("img-${skill.removePrefix("word-")}"),
        audio = AssetId("aud-en-${skill.removePrefix("word-")}")
    )

    private companion object {
        const val UNIT = "u01-my-body"
        const val FIRST_LESSON = "u01-my-body-l1"
        const val SECOND_LESSON = "u01-my-body-l2"
        const val EYES = "word-eyes"
        const val EARS = "word-ears"
        const val NOSE = "word-nose"
    }
}
