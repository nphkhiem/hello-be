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

            // Paging, leaving and switching profile belong to whoever hosts this screen. A library
            // of one unit has nothing either side of it, and the rule for choosing which three
            // shelves to show needs more than one unit to be about anything.
            FreePlayAction.PreviousShelvesRequested,
            FreePlayAction.NextShelvesRequested,
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
        _state.value = _state.value.copy(speakingObjectId = objectId)
        viewModelScope.launch { playback.play(recording) }
    }

    private fun FreePlayUiState.withLibrary(
        course: Course,
        finished: Set<LessonId>,
        child: ChildProfile?
    ): FreePlayUiState {
        val shelves = course.units.mapNotNull { it.toShelf(course, finished) }
        val remembered = preferred?.let { wanted -> shelves.firstOrNull { it.id == wanted } }
        if (remembered != null) preferred = null

        return copy(
            profileName = child?.nickname.orEmpty(),
            profileAvatar = child?.avatarId?.value.orEmpty(),
            shelves = shelves,
            // The open shelf is rebuilt from the new list so a word added by a lesson finished
            // elsewhere appears, and a shelf that has gone closes rather than lingering.
            openShelf = remembered
                ?: openShelf?.let { open -> shelves.firstOrNull { it.id == open.id } }
        )
    }

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
