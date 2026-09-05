package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseUnit
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import com.nphkhiem.englishforyourchildren.domain.progression.LearningPath
import com.nphkhiem.englishforyourchildren.domain.progression.LessonStanding
import com.nphkhiem.englishforyourchildren.domain.progression.ObserveLearningPathUseCase
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The course as one child's path through it.
 *
 * No judgement is made here any more. Which lessons are open, which one is the offer and what wants
 * reviewing are rules about a child rather than about a screen, so they live in `:domain` where
 * they can be written down as a table and checked without a television. What is left is the part a
 * screen is actually for: turning that into words and shapes.
 */
@HiltViewModel
class LearningPathViewModel @Inject constructor(
    private val curriculum: CurriculumRepository,
    private val progress: ProgressRepository,
    private val profiles: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EMPTY)
    val state: StateFlow<LearningPathUiState> = _state.asStateFlow()

    /**
     * Which unit the child is looking at, or null while that is still the course's decision.
     *
     * Once they have paged somewhere it is theirs, and a write landing underneath them does not
     * move them: a path that jumped to the next unit the moment a lesson was recorded would take
     * the screen away from a child who was reading it.
     */
    private val chosenUnit = MutableStateFlow<UnitId?>(null)

    private val _unavailable = MutableStateFlow(false)
    val unavailable: StateFlow<Boolean> = _unavailable.asStateFlow()

    private val learningPath = ObserveLearningPathUseCase(curriculum, progress)

    fun start(profileId: ProfileId) {
        viewModelScope.launch {
            combine(
                curriculum.observeCourse(),
                learningPath(profileId),
                profiles.observeProfiles(),
                chosenUnit
            ) { course, path, people, chosen ->
                Reading(course, path, people, chosen)
            }.collect { (course, path, people, chosen) ->
                if (course !is DomainResult.Success) {
                    _unavailable.value = true
                    return@collect
                }
                val child = (people as? DomainResult.Success)
                    ?.value
                    ?.firstOrNull { it.id == profileId }
                _state.value = map(
                    course = course.value,
                    path = (path as? DomainResult.Success)?.value,
                    child = child,
                    chosen = chosen
                )
            }
        }
    }

    /** The child asks for the unit on either side; there is nothing to ask for at the ends. */
    fun showPreviousUnit() {
        _state.value.previousUnit?.let { chosenUnit.value = UnitId(it.unitId) }
    }

    fun showNextUnit() {
        _state.value.nextUnit?.let { chosenUnit.value = UnitId(it.unitId) }
    }

    private fun map(
        course: Course,
        path: LearningPath?,
        child: ChildProfile?,
        chosen: UnitId?
    ): LearningPathUiState {
        val index = course.units.indexOfShown(chosen, path)
        val unit = course.units.getOrNull(index) ?: return EMPTY
        return LearningPathUiState(
            profileName = child?.nickname.orEmpty(),
            profileAvatar = child?.avatarId?.value.orEmpty(),
            unit = unit.toPage(course.units.size, path),
            previousUnit = course.units.getOrNull(index - 1)?.toSummary(),
            nextUnit = course.units.getOrNull(index + 1)?.toSummary(),
            pendingSave = false
        )
    }

    /**
     * Where the child paged to, or where they are up to, or the beginning.
     *
     * The offer is what decides when nobody has paged anywhere, so a child who finished a unit
     * yesterday opens on the next one rather than on a page of finished lessons they have to walk
     * past. A chosen unit the course no longer has falls back the same way, because content can
     * move under a child and a screen that showed nothing would be the worse answer.
     *
     * A child who has finished everything has no offer to follow, and sending them back to unit one
     * would take them furthest from what they just did. They stay at the end. A path that will not
     * read at all is a different thing and still opens at the beginning.
     */
    private fun List<CourseUnit>.indexOfShown(chosen: UnitId?, path: LearningPath?): Int {
        indexOfFirst { it.id == chosen }.let { if (it >= 0) return it }
        indexOfFirst { unit -> unit.lessons.any { it.id == path?.recommended } }
            .let { if (it >= 0) return it }
        if (path != null && path.recommended == null) return lastIndex
        return 0
    }

    private fun CourseUnit.toSummary() =
        UnitSummary(unitId = id.value, unitNumber = ordinal + 1, theme = theme)

    /** Four things read together, which `combine` cannot express as a pair. */
    private data class Reading(
        val course: DomainResult<Course>,
        val path: DomainResult<LearningPath>,
        val people: DomainResult<List<ChildProfile>>,
        val chosen: UnitId?
    )

    private fun CourseUnit.toPage(unitCount: Int, path: LearningPath?) = UnitPageState(
        unitId = id.value,
        unitNumber = ordinal + 1,
        unitCount = unitCount,
        theme = theme,
        objective = "",
        lessons = lessons.map { it.toNode(path) }
    )

    /**
     * Storage that cannot say what a child has done shows the path as untouched.
     *
     * Everything is later and nothing is offered, which is honest: with no history to read, this
     * screen cannot say a lesson is finished and must not guess that one is.
     */
    private fun Lesson.toNode(path: LearningPath?) = LessonNodeState(
        id = id.value,
        title = title(),
        progress = when (path?.standingOf(id)) {
            LessonStanding.COMPLETED -> LessonProgress.COMPLETED
            LessonStanding.RECOMMENDED -> LessonProgress.RECOMMENDED
            LessonStanding.FUTURE, null -> LessonProgress.FUTURE
        },
        kind = if (activities.all { it.family == ActivityFamily.REVIEW }) {
            LessonKind.REVIEW
        } else {
            LessonKind.PRACTICE
        },
        openable = activities.isNotEmpty()
    )

    /** A lesson's name comes from the words it teaches until the content carries a title. */
    private fun Lesson.title() = "Lesson ${ordinal + 1}"

    private companion object {
        /**
         * Before the course has been read.
         *
         * [LearningPathUiState.loading] rather than a null unit alone, because a null unit also
         * means the recovery case and a child must not be shown that while nothing is wrong yet.
         */
        val EMPTY = LearningPathUiState(
            profileName = "",
            profileAvatar = "",
            unit = null,
            previousUnit = null,
            nextUnit = null,
            pendingSave = false,
            loading = true
        )
    }
}
