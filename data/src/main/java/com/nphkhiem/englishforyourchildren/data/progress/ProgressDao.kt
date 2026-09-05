package com.nphkhiem.englishforyourchildren.data.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Reading and writing what a child has done. */
@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: LessonSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttempt(attempt: ActivityAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckpoint(checkpoint: LessonCheckpointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(progress: LessonProgressEntity)

    @Query("SELECT * FROM lesson_session WHERE id = :sessionId")
    suspend fun findSession(sessionId: String): LessonSessionEntity?

    @Query("SELECT * FROM activity_attempt WHERE profileId = :profileId")
    suspend fun attemptsFor(profileId: String): List<ActivityAttemptEntity>

    /**
     * Everything one child has done, as it changes.
     *
     * How well a thing is known is counted from these rather than stored beside them, so the path
     * has to watch them rather than ask once.
     */
    @Query("SELECT * FROM activity_attempt WHERE profileId = :profileId")
    fun observeAttempts(profileId: String): Flow<List<ActivityAttemptEntity>>

    /**
     * Newest first, because "the" open checkpoint is singular and a child is in the latest one.
     *
     * Without an order this returned whatever the table felt like, so which lesson a child was
     * offered to carry on with depended on row layout.
     */
    @Query("SELECT * FROM lesson_checkpoint WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun observeCheckpoints(profileId: String): Flow<List<LessonCheckpointEntity>>

    @Query("SELECT * FROM lesson_progress WHERE profileId = :profileId")
    fun observeLessonProgress(profileId: String): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM skill_progress WHERE profileId = :profileId")
    fun observeSkills(profileId: String): Flow<List<SkillProgressEntity>>

    @Query("SELECT * FROM lesson_checkpoint WHERE profileId = :profileId")
    suspend fun checkpointsFor(profileId: String): List<LessonCheckpointEntity>

    /** Where one child left one lesson. Newest first, because only the latest one is where they are. */
    @Query(
        "SELECT * FROM lesson_checkpoint WHERE profileId = :profileId AND lessonId = :lessonId " +
            "AND courseVersion = :courseVersion ORDER BY updatedAt DESC LIMIT 1"
    )
    suspend fun openCheckpoint(
        profileId: String,
        lessonId: String,
        courseVersion: String
    ): LessonCheckpointEntity?

    /**
     * One finished activity, written down in one go.
     *
     * The attempt, the lesson's progress and the checkpoint move together or not at all. A partial
     * write is the state where a child's checkpoint says they finished something no attempt
     * records, and the resume that follows would put them somewhere they have never been.
     *
     * Every write is keyed so that sending the same checkpoint twice replaces rather than
     * duplicates, which is what makes the reducer's retry safe.
     */
    @Transaction
    suspend fun persistCheckpoint(
        attempt: ActivityAttemptEntity,
        checkpoint: LessonCheckpointEntity,
        lessonProgress: LessonProgressEntity
    ) {
        upsertAttempt(attempt)
        upsertLessonProgress(lessonProgress)
        upsertCheckpoint(checkpoint)
    }

    @Query(
        "DELETE FROM lesson_checkpoint WHERE profileId = :profileId AND lessonId = :lessonId"
    )
    suspend fun deleteCheckpointsFor(profileId: String, lessonId: String)

    @Query("DELETE FROM lesson_session WHERE profileId = :profileId")
    suspend fun deleteSessions(profileId: String)

    @Query("DELETE FROM activity_attempt WHERE profileId = :profileId")
    suspend fun deleteAttempts(profileId: String)

    @Query("DELETE FROM lesson_checkpoint WHERE profileId = :profileId")
    suspend fun deleteCheckpoints(profileId: String)

    @Query("DELETE FROM lesson_progress WHERE profileId = :profileId")
    suspend fun deleteLessonProgress(profileId: String)

    @Query("DELETE FROM skill_progress WHERE profileId = :profileId")
    suspend fun deleteSkills(profileId: String)

    /**
     * A lesson finished, written down in one go.
     *
     * The checkpoint goes with it. A checkpoint is where a child can be put back into a lesson, and
     * once they have finished it there is nowhere to put them back into: a row that outlives its
     * lesson makes the whole profile look permanently half-saved, which is what child home and the
     * caregiver overview were both reporting for ever.
     */
    @Transaction
    suspend fun completeLesson(
        sessionId: String,
        lessonId: String,
        profileId: String,
        completedAt: Long
    ) {
        findSession(sessionId)?.let { session ->
            upsertSession(
                session.copy(
                    status = "COMPLETED",
                    currentActivityInstanceId = null,
                    completedAt = completedAt
                )
            )
        }
        upsertLessonProgress(
            LessonProgressEntity(
                profileId = profileId,
                lessonId = lessonId,
                completed = true,
                updatedAt = completedAt
            )
        )
        deleteCheckpointsFor(profileId, lessonId)
    }

    /**
     * Everything one child has done, removed together.
     *
     * A reset that half worked would leave a child with a checkpoint into a lesson whose attempts
     * are gone, which is worse than either keeping it all or removing it all.
     */
    @Transaction
    suspend fun deleteAllFor(profileId: String) {
        deleteAttempts(profileId)
        deleteCheckpoints(profileId)
        deleteLessonProgress(profileId)
        deleteSkills(profileId)
        deleteSessions(profileId)
    }
}
