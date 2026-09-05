package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileProgress
import com.nphkhiem.englishforyourchildren.domain.progression.CourseOrderProgressionPolicy
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
 * Whose home this is, and what one press of the dominant control does.
 *
 * The screen has drawn all four of [HomePrimary] since HB-D05 and the copy for each was written
 * with it. What was missing was anything to choose between them: this read a name and a picture and
 * told a child who had finished nine lessons to start their first adventure.
 *
 * The judgement is not made here. Which lesson comes next is a rule about a child rather than about
 * a screen, so it comes from `:domain`'s progression policy, the same one the learning path uses.
 * Two screens deciding separately what "next" means is how they come to disagree.
 */
@HiltViewModel
class ChildHomeViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val curriculum: CurriculumRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EMPTY)
    val state: StateFlow<ChildHomeUiState> = _state.asStateFlow()

    private val policy = CourseOrderProgressionPolicy()

    fun start(profileId: ProfileId) {
        viewModelScope.launch {
            combine(
                profiles.observeProfiles(),
                progress.observeProfileProgress(profileId),
                curriculum.observeCourse()
            ) { people, done, course ->
                Triple(people, done, course)
            }.collect { (people, done, course) ->
                val child = (people as? DomainResult.Success)
                    ?.value
                    ?.firstOrNull { it.id == profileId }
                    ?: return@collect

                _state.value = _state.value.copy(
                    profileName = child.nickname,
                    profileAvatar = child.avatarId.value,
                    primary = offer(course, done),
                    continueTarget = target(course, done),
                    // The same signal the caregiver overview reads: a checkpoint still open is a
                    // lesson whose ending has not been written down yet.
                    pendingSave = (done as? DomainResult.Success)?.value?.openCheckpoint != null
                )
            }
        }
    }

    /**
     * What the dominant control says.
     *
     * A television that cannot read its own history is not a child who has done nothing, but it is
     * the one answer that cannot be wrong about them: offering a first adventure to a returning
     * child is a smaller mistake than telling them they have finished, or refusing to open.
     */
    private fun offer(
        course: DomainResult<Course>,
        done: DomainResult<ProfileProgress>
    ): HomePrimary {
        val read = readable(course, done) ?: return HomePrimary.StartFirstAdventure
        val (units, practice) = read

        strandedCheckpoint(units, practice)?.let { lost ->
            return HomePrimary.ResumeUnavailable(context = lost)
        }

        val next = policy.build(units, practice).recommended
            ?: return HomePrimary.CourseComplete

        val nothingDoneYet = practice.lessonsCompleted.isEmpty() && practice.openCheckpoint == null
        if (nothingDoneYet) return HomePrimary.StartFirstAdventure

        return HomePrimary.Resume(context = units.describe(next))
    }

    /** The lesson one press opens, which is nothing at all in the two states that offer none. */
    private fun target(
        course: DomainResult<Course>,
        done: DomainResult<ProfileProgress>
    ): LessonId? {
        val read = readable(course, done) ?: return firstLesson(course)
        val (units, practice) = read
        if (strandedCheckpoint(units, practice) != null) return null
        return policy.build(units, practice).recommended
    }

    /**
     * A checkpoint naming a lesson this course does not have.
     *
     * Content moves under a child's history, and the honest answer is to say so rather than to
     * quietly send them somewhere else. Returns the words to say it with, or null when there is
     * nothing wrong.
     */
    private fun strandedCheckpoint(course: Course, practice: ProfileProgress): String? {
        val checkpoint = practice.openCheckpoint ?: return null
        val known = course.units.flatMap { it.lessons }.any { it.id == checkpoint.lessonId }
        return if (known) null else checkpoint.lessonId.value
    }

    private fun readable(
        course: DomainResult<Course>,
        done: DomainResult<ProfileProgress>
    ): Pair<Course, ProfileProgress>? {
        if (course !is DomainResult.Success) return null
        if (done !is DomainResult.Success) return null
        return course.value to done.value
    }

    /** Somewhere to go while storage is unreadable, so the one control still opens something. */
    private fun firstLesson(course: DomainResult<Course>): LessonId? =
        (course as? DomainResult.Success)?.value?.units?.firstOrNull()?.lessons?.firstOrNull()?.id

    /** "My Body, Lesson 2", which is what the approved draft puts under Continue. */
    private fun Course.describe(lessonId: LessonId): String {
        val unit = units.firstOrNull { unit -> unit.lessons.any { it.id == lessonId } }
            ?: return ""
        val lesson: Lesson = unit.lessons.first { it.id == lessonId }
        return "${unit.theme}, Lesson ${lesson.ordinal + 1}"
    }

    private companion object {
        val EMPTY = ChildHomeUiState(
            profileName = "",
            profileAvatar = "",
            greeting = "Let us find words together",
            greetingHint = "Pip has a little adventure ready for you.",
            primary = HomePrimary.StartFirstAdventure,
            continueTarget = null,
            pendingSave = false
        )
    }
}
