package com.nphkhiem.englishforyourchildren.navigation

import androidx.lifecycle.ViewModel
import com.nphkhiem.englishforyourchildren.content.BackgroundLessons
import com.nphkhiem.englishforyourchildren.data.pack.PackActivation
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.feature.learning.lessonVersionFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Which course a child is taught from, and when a newer one may take its place.
 *
 * The host's, because it is the one place that knows whether a lesson is open: a new course goes
 * live only from the learning path, never under a child mid-lesson, and lessons fetch the next one
 * quietly while they run.
 */
@HiltViewModel
class CourseViewModel @Inject constructor(
    private val curriculum: CurriculumRepository,
    private val progress: ProgressRepository,
    private val activation: PackActivation,
    private val background: BackgroundLessons
) : ViewModel() {

    suspend fun lessonVersion(profileId: ProfileId, lessonId: LessonId): CourseVersion? =
        lessonVersionFor(profileId, lessonId, curriculum, progress)

    suspend fun liveVersion(): CourseVersion? =
        (curriculum.observeCourse().first() as? DomainResult.Success)?.value?.version

    /** Called with no lesson open. Whatever was waiting goes live, and the path redraws from it. */
    suspend fun switchToNewestCourse() {
        activation.activateNewest()
    }

    /** Called as a lesson starts, while the child has something to do. */
    fun prefetch() = background.prefetch()
}
