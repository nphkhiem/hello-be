package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import androidx.room.Room
import com.nphkhiem.englishforyourchildren.data.local.HelloBeDatabase
import com.nphkhiem.englishforyourchildren.data.local.MIGRATION_1_2
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileDao
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileEntity
import com.nphkhiem.englishforyourchildren.data.progress.ActivityAttemptEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonCheckpointEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonProgressEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonSessionEntity
import com.nphkhiem.englishforyourchildren.data.progress.ProgressDao
import kotlinx.coroutines.flow.Flow

/**
 * A database on disk that can go away and come back while the graph above it stays put.
 *
 * The repositories are bound `@Singleton` in `DataBindingsModule`, which the journeys do not
 * uninstall, so each one captures its DAO once and keeps it. Closing the database under a live
 * singleton repository throws `attempt to re-open an already-closed object`. So the swap happens
 * below the repository: the DAOs handed to the graph are stable objects that forward to whichever
 * database is open at the moment they are called.
 *
 * Keeping the repositories singleton is not a compromise made to fit the harness. It is half of
 * what the journey proves: a singleton repository must not be the thing remembering where a child
 * had got to.
 */
class ReopeningDatabase(private val context: Context, private val name: String) {

    @Volatile
    private var open: HelloBeDatabase = build()

    val profiles: ChildProfileDao = ForwardingChildProfileDao { open.childProfileDao() }

    val progress: ProgressDao = ForwardingProgressDao { open.progressDao() }

    /** The television going off and coming back on, as far as storage is concerned. */
    fun reopen() {
        open.close()
        open = build()
    }

    fun deleteFile() {
        open.close()
        context.deleteDatabase(name)
    }

    private fun build(): HelloBeDatabase =
        Room.databaseBuilder(context, HelloBeDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2)
            .build()
}

/**
 * Every member forwarded by hand, including the two `@Transaction` ones.
 *
 * Those two have bodies in the interface, so an implementing class inherits them for free. The
 * inherited body is not Room's generated transaction wrapper, so taking that offer would leave
 * `persistCheckpoint` writing three rows without a transaction around them, and quietly drop the
 * atomicity `ProgressTransactionTest` exists to guarantee.
 */
private class ForwardingProgressDao(private val current: () -> ProgressDao) : ProgressDao {
    override suspend fun upsertSession(session: LessonSessionEntity) =
        current().upsertSession(session)

    override suspend fun upsertAttempt(attempt: ActivityAttemptEntity) =
        current().upsertAttempt(attempt)

    override suspend fun upsertCheckpoint(checkpoint: LessonCheckpointEntity) =
        current().upsertCheckpoint(checkpoint)

    override suspend fun upsertLessonProgress(progress: LessonProgressEntity) =
        current().upsertLessonProgress(progress)

    override suspend fun findSession(sessionId: String): LessonSessionEntity? =
        current().findSession(sessionId)

    override suspend fun attemptsFor(profileId: String): List<ActivityAttemptEntity> =
        current().attemptsFor(profileId)

    override fun observeAttempts(profileId: String): Flow<List<ActivityAttemptEntity>> =
        current().observeAttempts(profileId)

    override fun observeCheckpoints(profileId: String): Flow<List<LessonCheckpointEntity>> =
        current().observeCheckpoints(profileId)

    override fun observeLessonProgress(profileId: String): Flow<List<LessonProgressEntity>> =
        current().observeLessonProgress(profileId)

    override fun observeSkills(profileId: String) = current().observeSkills(profileId)

    override suspend fun checkpointsFor(profileId: String): List<LessonCheckpointEntity> =
        current().checkpointsFor(profileId)

    override suspend fun openCheckpoint(
        profileId: String,
        lessonId: String,
        courseVersion: String
    ): LessonCheckpointEntity? = current().openCheckpoint(profileId, lessonId, courseVersion)

    override suspend fun persistCheckpoint(
        attempt: ActivityAttemptEntity,
        checkpoint: LessonCheckpointEntity,
        lessonProgress: LessonProgressEntity
    ) = current().persistCheckpoint(attempt, checkpoint, lessonProgress)

    override suspend fun deleteSessions(profileId: String) = current().deleteSessions(profileId)

    override suspend fun deleteAttempts(profileId: String) = current().deleteAttempts(profileId)

    override suspend fun deleteCheckpoints(profileId: String) =
        current().deleteCheckpoints(profileId)

    override suspend fun deleteCheckpointsFor(profileId: String, lessonId: String) =
        current().deleteCheckpointsFor(profileId, lessonId)

    override suspend fun deleteLessonProgress(profileId: String) =
        current().deleteLessonProgress(profileId)

    override suspend fun deleteSkills(profileId: String) = current().deleteSkills(profileId)

    override suspend fun completeLesson(
        sessionId: String,
        lessonId: String,
        profileId: String,
        completedAt: Long
    ) = current().completeLesson(sessionId, lessonId, profileId, completedAt)

    override suspend fun deleteAllFor(profileId: String) = current().deleteAllFor(profileId)
}

private class ForwardingChildProfileDao(private val current: () -> ChildProfileDao) :
    ChildProfileDao {
    override fun observeAll(): Flow<List<ChildProfileEntity>> = current().observeAll()

    override suspend fun findById(id: String): ChildProfileEntity? = current().findById(id)

    override suspend fun count(): Int = current().count()

    override suspend fun insert(profile: ChildProfileEntity) = current().insert(profile)

    override suspend fun update(profile: ChildProfileEntity) = current().update(profile)

    override suspend fun delete(id: String) = current().delete(id)
}
