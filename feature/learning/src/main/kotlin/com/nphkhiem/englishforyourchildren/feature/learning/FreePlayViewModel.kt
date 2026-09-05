package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.AnswerChoice
import com.nphkhiem.englishforyourchildren.domain.model.Answerable
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseUnit
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.playback.PlaybackController
import com.nphkhiem.englishforyourchildren.playback.PlaybackEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The words a child has already met, to press and hear again.
 *
 * Learned-content-only, per `INFORMATION_ARCHITECTURE.md`. A shelf is a unit and holds the words of
 * the lessons the child has finished in it, so something appears on it the first time they complete
 * a lesson rather than the first time a recording exists.
 *
 * That is deliberately not what [com.nphkhiem.englishforyourchildren.domain.progression.tallySkills]
 * counts. The tally answers how well a thing is known and refuses to count a question the child
 * could not hear; this answers what they have been shown. Using the tally here would leave every
 * shelf empty for as long as the course has no audio.
 *
 * **It cannot change what a child has formally done.** The progress repository is read and never
 * written, so "free play cannot accidentally unlock lessons or record scored attempts" holds
 * because there is no call here that could.
 */
@HiltViewModel
class FreePlayViewModel @Inject constructor(
    private val curriculum: CurriculumRepository,
    private val progress: ProgressRepository,
    private val profiles: ProfileRepository,
    private val playback: PlaybackController
) : ViewModel() {

    private val _state = MutableStateFlow(EMPTY)
    val state: StateFlow<FreePlayUiState> = _state.asStateFlow()

    /** Which recording says which word. Built from the course, because a skill id is not a file. */
    private var recordings: Map<SkillId, AssetId> = emptyMap()

    /** Honoured once, when the library first has shelves to honour it with. */
    private var preferred: String? = null

    /** Every shelf the child has earned, of which at most [SHELVES_IN_VIEW] are on screen. */
    private var library: List<Shelf> = emptyList()

    /** Which shelf the current view starts at. A position rather than a page number, so a shelf
     * appearing or going does not shuffle a child onto a different view. */
    private var firstInView: Int = 0

    /** The word most recently pressed, which is where entry focus goes when a child comes back. */
    private var lastPlayedObject: String? = null

    init {
        // Subscribed before anything can be played, for the reason the lesson gives: the event flow
        // keeps no replay, so a listener arriving after the first failure never learns of it.
        viewModelScope.launch {
            playback.events.collect { event ->
                _state.value = when (event) {
                    is PlaybackEvent.Failed ->
                        _state.value.copy(audioAvailable = false, speakingObjectId = null)

                    is PlaybackEvent.Completed -> _state.value.copy(speakingObjectId = null)
                }
            }
        }
    }

    /**
     * [preferredShelfId] opens straight into a shelf a child was last in, where it still exists.
     *
     * Validated rather than trusted, for the reason the state's own comment gives about a
     * remembered object: a shelf can go between sessions, and opening into one that is not there
     * would strand a child on a page with nothing on it.
     */
    fun start(profileId: ProfileId, preferredShelfId: String? = null) {
        preferred = preferredShelfId
        viewModelScope.launch {
            combine(
                curriculum.observeCourse(),
                progress.observeProfileProgress(profileId),
                profiles.observeProfiles()
            ) { course, done, people ->
                Triple(course, done, people)
            }.collect { (course, done, people) ->
                if (course !is DomainResult.Success) return@collect
                recordings = course.value.recordings()

                val finished = (done as? DomainResult.Success)?.value?.lessonsCompleted.orEmpty()
                val child = (people as? DomainResult.Success)
                    ?.value
                    ?.firstOrNull { it.id == profileId }

                _state.value = _state.value.withLibrary(course.value, finished, child)
            }
        }
    }

    fun onAction(action: FreePlayAction) {
        when (action) {
            is FreePlayAction.ShelfChosen ->
                _state.value = _state.value.copy(
                    openShelf = _state.value.shelves.firstOrNull { it.id == action.shelfId }
                )

            FreePlayAction.ShelvesRequested -> _state.value = _state.value.copy(openShelf = null)

            is FreePlayAction.ObjectChosen -> say(action.objectId)

            FreePlayAction.PreviousShelvesRequested -> page(-SHELVES_IN_VIEW)

            FreePlayAction.NextShelvesRequested -> page(SHELVES_IN_VIEW)

            // Leaving and switching profile belong to whoever hosts this screen.
            FreePlayAction.HomeRequested,
            FreePlayAction.SwitchProfileRequested -> Unit
        }
    }

    override fun onCleared() {
        // Free play is the player's owning scope while a child is in it, as a lesson is in its own.
        playback.stop()
    }

    /**
     * A word, said again.
     *
     * The pressed word is marked as speaking before the sound starts, so the press does something a
     * child can see even when the recording turns out not to exist. Whatever happens next, failure
     * or completion, clears it.
     */
    private fun say(objectId: String) {
        val recording = recordings[SkillId(objectId)] ?: return
        lastPlayedObject = objectId
        _state.value = _state.value.copy(speakingObjectId = objectId).withView()
        viewModelScope.launch { playback.play(listOf(recording)) }
    }

    /**
     * Move the view by a whole viewful, and no further than the library goes.
     *
     * Clamped rather than wrapped. A child pressing the same direction twice at the end of the
     * library would otherwise find themselves back at the beginning without having asked to be,
     * and there is no control drawn there to have caused it.
     */
    private fun page(by: Int) {
        val last = ((library.size - 1) / SHELVES_IN_VIEW) * SHELVES_IN_VIEW
        firstInView = (firstInView + by).coerceIn(0, maxOf(last, 0))
        _state.value = _state.value.withView()
    }

    private fun FreePlayUiState.withLibrary(
        course: Course,
        finished: Set<LessonId>,
        child: ChildProfile?
    ): FreePlayUiState {
        library = course.units.mapNotNull { it.toShelf(course, finished) }
        val remembered = preferred?.let { wanted -> library.firstOrNull { it.id == wanted } }
        if (remembered != null) preferred = null

        // A remembered shelf brings its view with it. Opening into a shelf on a page the child is
        // not looking at would put them back on the shelves with the wrong ones on screen.
        val opened =
            remembered ?: openShelf?.let { open -> library.firstOrNull { it.id == open.id } }
        opened?.let { shelf ->
            val at = library.indexOfFirst { it.id == shelf.id }
            if (at >= 0) firstInView = (at / SHELVES_IN_VIEW) * SHELVES_IN_VIEW
        }

        return copy(
            profileName = child?.nickname.orEmpty(),
            profileAvatar = child?.avatarId?.value.orEmpty(),
            // The open shelf is rebuilt from the new list so a word added by a lesson finished
            // elsewhere appears, and a shelf that has gone closes rather than lingering.
            openShelf = opened
        ).withView()
    }

    /**
     * One viewful of shelves, and the names of what lies either side of it.
     *
     * The information architecture caps a view at three: more than that on one page is the endless
     * feed its stop condition names, and a child cannot choose between things they cannot take in.
     * The shelves either side are named rather than counted, because a control that said "more"
     * would tell a child nothing about where it goes.
     */
    private fun FreePlayUiState.withView(): FreePlayUiState {
        if (library.isEmpty()) {
            return copy(shelves = emptyList(), previousShelf = null, nextShelf = null)
        }
        firstInView = firstInView.coerceIn(0, maxOf(library.size - 1, 0))
        val end = minOf(firstInView + SHELVES_IN_VIEW, library.size)

        return copy(
            shelves = library.subList(firstInView, end).map { it.marked() },
            previousShelf = library.getOrNull(firstInView - 1)?.summary(),
            nextShelf = library.getOrNull(end)?.summary()
        )
    }

    /**
     * The shelf holding the last word played, said out loud in the state.
     *
     * `freePlayFocusTarget` has read this since the shelves were drawn and nothing ever wrote it,
     * so entry focus always went to the first shelf. The information architecture asks for the last
     * played one.
     */
    private fun Shelf.marked(): Shelf {
        val holdsIt = objects.any { it.id == lastPlayedObject }
        return copy(
            lastPlayed = holdsIt,
            lastPlayedObjectId = if (holdsIt) lastPlayedObject else null
        )
    }

    private fun Shelf.summary() = ShelfSummary(id = id, name = name)

    /**
     * A unit with nothing finished in it is not an empty shelf; it is not a shelf.
     *
     * The screen draws an empty library as an explanation of what to do next, which is the right
     * thing for a child before their first lesson. A row of empty shelves would not be.
     */
    private fun CourseUnit.toShelf(course: Course, finished: Set<LessonId>): Shelf? {
        val learned = lessons
            .filter { it.id in finished }
            .flatMap { it.teaches }
            .distinct()
            .mapNotNull { skill -> course.wordFor(skill)?.let { it.toLearned() } }

        if (learned.isEmpty()) return null
        return Shelf(id = id.value, name = theme, objects = learned)
    }

    private fun AnswerChoice.toLearned() =
        LearnedObject(id = skillId.value, label = label, image = image.value)

    /**
     * What a word looks and sounds like, taken from the first place the course offers it as a
     * choice. A skill id names a thing to learn; only a choice carries its picture and its sound.
     */
    private fun Course.wordFor(skill: SkillId): AnswerChoice? = choices().firstOrNull {
        it.skillId == skill
    }

    private fun Course.recordings(): Map<SkillId, AssetId> =
        choices().associate { it.skillId to it.audio }

    private fun Course.choices(): List<AnswerChoice> = units
        .flatMap { it.lessons }
        .flatMap { it.activities }
        .flatMap { activity ->
            when (val content = activity.content) {
                is Answerable -> content.choices

                is com.nphkhiem.englishforyourchildren.domain.model.ActivityContent.GuidedRepetition
                -> content.words

                null -> emptyList()
            }
        }

    private companion object {
        /** Up to three story shelves in one view, per `INFORMATION_ARCHITECTURE.md`. */
        const val SHELVES_IN_VIEW = 3

        val EMPTY = FreePlayUiState(
            profileName = "",
            profileAvatar = "",
            shelves = emptyList(),
            previousShelf = null,
            nextShelf = null,
            openShelf = null,
            speakingObjectId = null,
            audioAvailable = true
        )
    }
}
