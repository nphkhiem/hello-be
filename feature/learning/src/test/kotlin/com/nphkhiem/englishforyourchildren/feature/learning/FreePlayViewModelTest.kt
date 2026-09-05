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

        assertThat(playback.spoken.map { said -> said.map { it.value } })
            .containsExactly(listOf("aud-en-eyes"))
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

    @Test
    fun givenMoreShelvesThanFitOneView_whenTheLibraryOpens_thenOnlyAViewfulIsOffered() {
        // The information architecture caps this at three shelves in one view. Five units on one
        // page is the endless feed the stop condition names, and a child cannot choose between
        // things they cannot take in.
        curriculum.setCourse(courseOfUnits(5))
        progress.setProgress(DomainBuilders.profileProgress(lessonsCompleted = everyFirstLesson(5)))

        val model = started()

        assertThat(model.state.value.shelves).hasSize(3)
        assertThat(model.state.value.previousShelf).isNull()
        assertThat(model.state.value.nextShelf?.id).isEqualTo("u04-unit-4")
    }

    @Test
    fun givenAShelfOffTheEdge_whenTheChildAsksForIt_thenTheViewMovesAndTheWayBackIsNamed() {
        curriculum.setCourse(courseOfUnits(5))
        progress.setProgress(DomainBuilders.profileProgress(lessonsCompleted = everyFirstLesson(5)))
        val model = started()

        model.onAction(FreePlayAction.NextShelvesRequested)

        assertThat(model.state.value.shelves.map { it.id })
            .containsExactly("u04-unit-4", "u05-unit-5")
            .inOrder()
        assertThat(model.state.value.previousShelf?.id).isEqualTo("u03-unit-3")
        assertThat(model.state.value.nextShelf).isNull()
    }

    @Test
    fun givenTheLastViewOfShelves_whenTheChildAsksToGoBack_thenTheFirstViewReturns() {
        curriculum.setCourse(courseOfUnits(5))
        progress.setProgress(DomainBuilders.profileProgress(lessonsCompleted = everyFirstLesson(5)))
        val model = started()
        model.onAction(FreePlayAction.NextShelvesRequested)

        model.onAction(FreePlayAction.PreviousShelvesRequested)

        assertThat(model.state.value.shelves.map { it.id })
            .containsExactly("u01-unit-1", "u02-unit-2", "u03-unit-3")
            .inOrder()
        assertThat(model.state.value.previousShelf).isNull()
    }

    @Test
    fun givenTheChildIsAlreadyAtTheEnd_whenTheyAskForMore_thenNothingMoves() {
        // The controls are only drawn when there is something either side, but a press that
        // arrived anyway must not page off the end of the library.
        curriculum.setCourse(courseOfUnits(5))
        progress.setProgress(DomainBuilders.profileProgress(lessonsCompleted = everyFirstLesson(5)))
        val model = started()
        model.onAction(FreePlayAction.NextShelvesRequested)

        model.onAction(FreePlayAction.NextShelvesRequested)

        assertThat(model.state.value.shelves.map { it.id })
            .containsExactly("u04-unit-4", "u05-unit-5")
            .inOrder()
    }

    @Test
    fun givenAShelfRememberedFromLastTime_whenItIsOffTheFirstView_thenTheViewOpensOnIt() {
        // Opening into a remembered shelf has to bring its page with it, or the shelf a child was
        // last in is on a page they are not looking at.
        curriculum.setCourse(courseOfUnits(5))
        progress.setProgress(DomainBuilders.profileProgress(lessonsCompleted = everyFirstLesson(5)))

        val model = started(preferredShelfId = "u05-unit-5")

        assertThat(model.state.value.openShelf?.id).isEqualTo("u05-unit-5")
        assertThat(model.state.value.shelves.map { it.id }).contains("u05-unit-5")
    }

    @Test
    fun givenAWordIsPlayed_whenTheLibraryIsRedrawn_thenItsShelfIsTheOneMarkedLastPlayed() {
        // Entry focus goes to the last played shelf, which the information architecture asks for
        // and the focus rule already reads. Nothing was ever setting it.
        progress.setProgress(
            DomainBuilders.profileProgress(lessonsCompleted = setOf(LessonId(FIRST_LESSON)))
        )
        val model = started()

        model.onAction(FreePlayAction.ObjectChosen(EYES))

        assertThat(model.state.value.shelves.single().lastPlayed).isTrue()
        assertThat(model.state.value.shelves.single().lastPlayedObjectId).isEqualTo(EYES)
    }

    @Test
    fun givenAChildPlaysWithEveryWord_whenTheirProgressIsRead_thenNothingWasWrittenDown() {
        // The acceptance criterion, held as a fact rather than as a comment. Free play is a place
        // to revisit, and a child pressing every word in the library has still not finished a
        // lesson, unlocked one, or been marked right or wrong about anything.
        progress.setProgress(
            DomainBuilders.profileProgress(lessonsCompleted = setOf(LessonId(FIRST_LESSON)))
        )
        val model = started()

        model.state.value.shelves.flatMap { it.objects }.forEach { word ->
            model.onAction(FreePlayAction.ObjectChosen(word.id))
        }

        assertThat(progress.persisted).isEmpty()
        assertThat(progress.started).isEmpty()
        assertThat(progress.completed).isEmpty()
    }

    /** Units named so their shelf ids sort the way the course does. */
    private fun courseOfUnits(count: Int) = course().copy(
        units = (0 until count).map { index ->
            val unit = "u0${index + 1}-unit-${index + 1}"
            CourseUnit(
                id = UnitId(unit),
                courseId = CourseId("starter"),
                ordinal = index,
                theme = "Unit ${index + 1}",
                word = "thing",
                lessons = listOf(
                    lesson("$unit-l1", 0, listOf(EYES, EARS)).copy(
                        id = LessonId("$unit-l1"),
                        unitId = UnitId(unit)
                    )
                )
            )
        }
    )

    private fun everyFirstLesson(count: Int): Set<LessonId> =
        (0 until count).map { LessonId("u0${it + 1}-unit-${it + 1}-l1") }.toSet()

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
