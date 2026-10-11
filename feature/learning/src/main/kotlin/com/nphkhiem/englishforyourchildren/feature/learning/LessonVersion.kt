package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import kotlinx.coroutines.flow.first

/**
 * Which published version of the course a lesson opens at for this child.
 *
 * The live one, which is what a new session should always be taught from. Except when this child
 * stopped partway through this very lesson under an earlier version that can still be read: then
 * they go back to where they were, at the version they were in, because a session pins its version
 * and the place they left only exists in that one. Their finished lessons do not depend on this;
 * those are counted by lesson, whatever version they were finished in.
 *
 * Null when even the live course cannot be read, which the lesson already knows how to report.
 */
suspend fun lessonVersionFor(
    profileId: ProfileId,
    lessonId: LessonId,
    curriculum: CurriculumRepository,
    progress: ProgressRepository
): CourseVersion? {
    val live = (curriculum.observeCourse().first() as? DomainResult.Success)?.value?.version
        ?: return null
    val pausedHere = (progress.observeProfileProgress(profileId).first() as? DomainResult.Success)
        ?.value
        ?.openCheckpoint
        ?.takeIf { it.lessonId == lessonId && it.courseVersion != live }
        ?: return live
    return pausedHere.courseVersion
        .takeIf { curriculum.getLesson(lessonId, it) is DomainResult.Success }
        ?: live
}
