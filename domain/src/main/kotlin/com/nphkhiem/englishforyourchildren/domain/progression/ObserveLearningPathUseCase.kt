package com.nphkhiem.englishforyourchildren.domain.progression

import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * One child's way through the course, kept up to date.
 *
 * A plain constructor rather than an injected one. `:domain` carries no dependency-injection
 * annotations at all, which is what keeps it a module a test can use without a graph, so whoever
 * needs one builds it from the repositories they already hold.
 *
 * Storage that cannot say what a child has done is not the same as a child who has done nothing,
 * and the difference matters here: the first must not quietly present a course as untouched. A
 * course that will not load is a failure, because there is no path without one.
 */
class ObserveLearningPathUseCase(
    private val curriculum: CurriculumRepository,
    private val progress: ProgressRepository,
    private val policy: ProgressionPolicy = CourseOrderProgressionPolicy()
) {
    operator fun invoke(profileId: ProfileId): Flow<DomainResult<LearningPath>> = combine(
        curriculum.observeCourse(),
        progress.observeProfileProgress(profileId)
    ) { course, done ->
        when {
            course !is DomainResult.Success -> DomainResult.Failure(DomainError.InvalidContent)

            done !is DomainResult.Success ->
                DomainResult.Failure(DomainError.PersistenceUnavailable)

            else -> DomainResult.Success(policy.build(course.value, done.value))
        }
    }
}
