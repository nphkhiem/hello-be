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
import com.nphkhiem.englishforyourchildren.domain.model.ProfileProgress
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
 * Which lesson is recommended is the only judgement here: the first one they have not finished. It
 * is a rule about a child's progress rather than about content, which is why it lives with the
 * progress it reads rather than in the packaged course.
 */
@HiltViewModel
class LearningPathViewModel @Inject constructor(
    private val curriculum: CurriculumRepository,
    private val progress: ProgressRepository,
    private val profiles: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EMPTY)
    val state: StateFlow<LearningPathUiState> = _state.asStateFlow()

    private val _unavailable = MutableStateFlow(false)
    val unavailable: StateFlow<Boolean> = _unavailable.asStateFlow()

    fun start(profileId: ProfileId) {
        viewModelScope.launch {
            combine(
                curriculum.observeCourse(),
                progress.observeProfileProgress(profileId),
                profiles.observeProfiles()
            ) { course, done, people ->
                Triple(course, done, people)
            }.collect { (course, done, people) ->
                if (course !is DomainResult.Success) {
                    _unavailable.value = true
                    return@collect
                }
                val child = (people as? DomainResult.Success)
                    ?.value
                    ?.firstOrNull { it.id == profileId }
                _state.value = map(
                    course = course.value,
                    progress = (done as? DomainResult.Success)?.value,
                    child = child
                )
            }
        }
    }

    private fun map(
        course: Course,
        progress: ProfileProgress?,
        child: ChildProfile?
    ): LearningPathUiState {
        val unit = course.units.firstOrNull() ?: return EMPTY
        val finished = progress?.lessonsCompleted.orEmpty()
        return LearningPathUiState(
            profileName = child?.nickname.orEmpty(),
            profileAvatar = child?.avatarId?.value.orEmpty(),
            unit = unit.toPage(course.units.size, finished),
            previousUnit = null,
            nextUnit = null,
            pendingSave = false
        )
    }

    private fun CourseUnit.toPage(unitCount: Int, finished: Set<LessonId>) = UnitPageState(
        unitId = id.value,
        unitNumber = ordinal + 1,
        unitCount = unitCount,
        theme = theme,
        objective = "",
        lessons = lessons.map { lesson ->
            lesson.toNode(finished, recommendedOrdinal = recommendedOrdinal(finished))
        }
    )

    /**
     * The first lesson not yet finished.
     *
     * Null once a unit is complete, which is what stops the path pointing at a lesson a child has
     * already done as though it were new.
     */
    private fun CourseUnit.recommendedOrdinal(finished: Set<LessonId>): Int? =
        lessons.firstOrNull { it.id !in finished }?.ordinal

    private fun Lesson.toNode(finished: Set<LessonId>, recommendedOrdinal: Int?) = LessonNodeState(
        id = id.value,
        title = title(),
        progress = when {
            id in finished -> LessonProgress.COMPLETED

            ordinal == recommendedOrdinal -> LessonProgress.RECOMMENDED

            // Everything past the recommended one is later. A path a child can wander into the
            // middle of is a path with no shape to it.
            recommendedOrdinal != null && ordinal > recommendedOrdinal -> LessonProgress.FUTURE

            else -> LessonProgress.AVAILABLE
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
        val EMPTY = LearningPathUiState(
            profileName = "",
            profileAvatar = "",
            unit = null,
            previousUnit = null,
            nextUnit = null,
            pendingSave = false
        )
    }
}
