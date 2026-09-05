package com.nphkhiem.englishforyourchildren.data.progress

import com.nphkhiem.englishforyourchildren.domain.id.IdProvider
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.ConfirmedCheckpoint
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.LessonCheckpoint
import com.nphkhiem.englishforyourchildren.domain.model.LessonCompletion
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.LessonSession
import com.nphkhiem.englishforyourchildren.domain.model.LessonStatus
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileProgress
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.repository.CompleteSession
import com.nphkhiem.englishforyourchildren.domain.repository.PersistCheckpoint
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.repository.StartSession
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.domain.time.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * What each child has done, kept in Room.
 *
 * Every write goes through the DAO's transaction, so an attempt, a lesson's progress and a
 * checkpoint move together. This class does the mapping and nothing clever: the atomicity is the
 * database's, and the rules about when to write are the reducer's.
 */
class RoomProgressRepository @Inject constructor(
    private val dao: ProgressDao,
    private val sessionIds: IdProvider<SessionId>,
    private val timeProvider: TimeProvider
) : ProgressRepository {

    override fun observeProfileProgress(profileId: ProfileId): Flow<DomainResult<ProfileProgress>> =
        combine(
            dao.observeCheckpoints(profileId.value),
            dao.observeLessonProgress(profileId.value),
            dao.observeAttempts(profileId.value)
        ) { checkpoints, lessons, attempts ->
            DomainResult.Success(
                ProfileProgress(
                    profileId = profileId,
                    lessonsCompleted = lessons.filter { it.completed }
                        .map { LessonId(it.lessonId) }
                        .toSet(),
                    // What the child did, not what it adds up to. How well a thing is known is
                    // counted from these by the progression package, which has the course as well.
                    attempts = attempts.map { it.toDomain() },
                    openCheckpoint = checkpoints.firstOrNull()?.toDomain()
                )
            ) as DomainResult<ProfileProgress>
        }.catch { emit(DomainResult.Failure(DomainError.PersistenceUnavailable)) }

    override suspend fun startSession(command: StartSession): DomainResult<LessonSession> =
        attempt {
            val session = LessonSession(
                id = sessionIds.next(),
                profileId = command.profileId,
                courseVersion = command.courseVersion,
                lessonId = command.lessonId,
                // A sitting begins on its first activity, which the caller names when it asks the
                // reducer to start. Until then there is nothing in hand.
                currentActivity = null,
                status = LessonStatus.COMPLETED
            )
            dao.upsertSession(
                LessonSessionEntity(
                    id = session.id.value,
                    profileId = command.profileId.value,
                    courseVersion = command.courseVersion.value,
                    lessonId = command.lessonId.value,
                    currentActivityInstanceId = null,
                    status = LessonStatus.IN_PROGRESS.name,
                    startedAt = timeProvider.now().value,
                    completedAt = null
                )
            )
            DomainResult.Success(session)
        }

    override suspend fun openCheckpoint(
        profileId: ProfileId,
        lessonId: LessonId,
        courseVersion: CourseVersion
    ): DomainResult<LessonCheckpoint?> = attempt {
        DomainResult.Success(
            dao.openCheckpoint(profileId.value, lessonId.value, courseVersion.value)?.toDomain()
        )
    }

    override suspend fun persistCheckpoint(
        command: PersistCheckpoint
    ): DomainResult<ConfirmedCheckpoint> = attempt {
        dao.persistCheckpoint(
            attempt = ActivityAttemptEntity(
                activityInstanceId = command.activityInstanceId.value,
                profileId = command.profileId.value,
                sessionId = command.sessionId.value,
                activityId = command.activityId.value,
                ordinal = command.activityOrdinal,
                outcome = command.outcome.name,
                at = command.completedAt.value
            ),
            checkpoint = LessonCheckpointEntity(
                profileId = command.profileId.value,
                lessonId = command.lessonId.value,
                courseVersion = command.courseVersion.value,
                lastCompletedActivityId = command.activityId.value,
                sessionId = command.sessionId.value,
                updatedAt = command.completedAt.value
            ),
            lessonProgress = LessonProgressEntity(
                profileId = command.profileId.value,
                lessonId = command.lessonId.value,
                completed = false,
                updatedAt = command.completedAt.value
            )
        )
        DomainResult.Success(
            ConfirmedCheckpoint(
                sessionId = command.sessionId,
                lessonId = command.lessonId,
                lastCompletedActivity = command.activityId,
                confirmedAt = timeProvider.now()
            )
        )
    }

    override suspend fun completeSession(command: CompleteSession): DomainResult<LessonCompletion> =
        attempt {
            val session = dao.findSession(command.sessionId.value)
                ?: return DomainResult.Failure(DomainError.PersistenceUnavailable)

            // One transaction, and the checkpoint goes with it: a lesson that is finished has
            // nowhere left to resume into. See `completeLesson`.
            dao.completeLesson(
                sessionId = command.sessionId.value,
                lessonId = session.lessonId,
                profileId = session.profileId,
                completedAt = command.completedAt.value
            )
            DomainResult.Success(
                LessonCompletion(
                    sessionId = command.sessionId,
                    lessonId = LessonId(session.lessonId),
                    // The words a celebration lists back are the ones the content taught, and content
                    // does not carry skills into progress yet.
                    learnedSkills = emptyList(),
                    unitCompleted = false,
                    completedAt = command.completedAt
                )
            )
        }

    override suspend fun deleteProfileProgress(profileId: ProfileId): DomainResult<Unit> = attempt {
        dao.deleteAllFor(profileId.value)
        DomainResult.Success(Unit)
    }

    private fun LessonCheckpointEntity.toDomain() = LessonCheckpoint(
        profileId = ProfileId(profileId),
        courseVersion = CourseVersion(courseVersion),
        lessonId = LessonId(lessonId),
        lastCompletedActivity = lastCompletedActivityId?.let { ActivityId(it) },
        sessionId = SessionId(sessionId),
        updatedAt = com.nphkhiem.englishforyourchildren.domain.model.EpochMillis(updatedAt)
    )

    private inline fun <T> attempt(block: () -> DomainResult<T>): DomainResult<T> =
        runCatching(block).getOrElse { DomainResult.Failure(DomainError.PersistenceUnavailable) }
}
