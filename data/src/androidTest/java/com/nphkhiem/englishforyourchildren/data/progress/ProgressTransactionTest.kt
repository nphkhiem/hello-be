package com.nphkhiem.englishforyourchildren.data.progress

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.data.local.HelloBeDatabase
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressTransactionTest {
    private lateinit var database: HelloBeDatabase
    private lateinit var dao: ProgressDao

    @Before
    fun openDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, HelloBeDatabase::class.java)
            // Foreign keys are off by default in an in-memory database, and cascade delete is one
            // of the things under test here.
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .build()
        dao = database.progressDao()
        runBlocking { database.childProfileDao().insert(profileRow()) }
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun givenAFinishedActivity_whenItIsPersisted_thenTheAttemptAndCheckpointArriveTogether() {
        runBlocking { dao.persistCheckpoint(attempt(), checkpoint(), lessonProgress()) }

        runBlocking {
            assertThat(dao.attemptsFor(PROFILE)).hasSize(1)
            assertThat(dao.checkpointsFor(PROFILE)).hasSize(1)
        }
    }

    @Test
    fun givenTheSameCheckpointTwice_whenItIsRetried_thenTheChildAnsweredOnce() {
        // A retried save must not record a child answering twice. This is what makes the reducer's
        // retry safe, and why the activity instance is the key rather than a generated id.
        runBlocking {
            dao.persistCheckpoint(attempt(), checkpoint(), lessonProgress())
            dao.persistCheckpoint(attempt(), checkpoint(), lessonProgress())
        }

        runBlocking {
            assertThat(dao.attemptsFor(PROFILE)).hasSize(1)
            assertThat(dao.checkpointsFor(PROFILE)).hasSize(1)
        }
    }

    @Test
    fun givenTwoActivitiesInOneLesson_whenBothArePersisted_thenThereIsStillOnePlaceToReturnTo() {
        // One checkpoint per child, lesson and version. The second finished activity replaces the
        // first as where they come back to, rather than adding a second answer to that question.
        runBlocking {
            dao.persistCheckpoint(attempt(), checkpoint(), lessonProgress())
            dao.persistCheckpoint(
                attempt(instance = SECOND_INSTANCE, activity = SECOND_ACTIVITY, ordinal = 1),
                checkpoint(lastActivity = SECOND_ACTIVITY),
                lessonProgress()
            )
        }

        runBlocking {
            assertThat(dao.attemptsFor(PROFILE)).hasSize(2)
            val checkpoints = dao.checkpointsFor(PROFILE)
            assertThat(checkpoints).hasSize(1)
            assertThat(checkpoints.single().lastCompletedActivityId).isEqualTo(SECOND_ACTIVITY)
        }
    }

    @Test
    fun givenAWriteThatCannotComplete_whenItFails_thenNothingFromItIsLeftBehind() {
        // A partial write is the state where a checkpoint says a child finished something no
        // attempt records, and the resume that follows puts them somewhere they have never been.
        val orphan = checkpoint().copy(profileId = "nobody")

        val failed = runCatching {
            runBlocking { dao.persistCheckpoint(attempt(), orphan, lessonProgress()) }
        }

        assertThat(failed.isFailure).isTrue()
        runBlocking { assertThat(dao.attemptsFor(PROFILE)).isEmpty() }
    }

    @Test
    fun givenAStartedSession_whenTheSameOneIsStartedAgain_thenThereIsStillOneSitting() {
        runBlocking {
            dao.upsertSession(session())
            dao.upsertSession(session(current = SECOND_INSTANCE))
        }

        val stored = runBlocking { dao.findSession(SESSION) }
        assertThat(stored?.currentActivityInstanceId).isEqualTo(SECOND_INSTANCE)
    }

    @Test
    fun givenProgress_whenSomeoneIsWatching_thenTheyAreToldAsItArrives() {
        runBlocking { dao.persistCheckpoint(attempt(), checkpoint(), lessonProgress()) }

        val seen = runBlocking { dao.observeCheckpoints(PROFILE).first() }

        assertThat(seen).hasSize(1)
    }

    @Test
    fun givenAChildIsDeleted_whenTheirRowGoes_thenEverythingTheyDidGoesWithIt() {
        // Deleting a profile must not leave attempts and checkpoints behind pointing at a child
        // who is not there.
        runBlocking {
            dao.upsertSession(session())
            dao.persistCheckpoint(attempt(), checkpoint(), lessonProgress())
            database.childProfileDao().delete(PROFILE)
        }

        runBlocking {
            assertThat(dao.attemptsFor(PROFILE)).isEmpty()
            assertThat(dao.checkpointsFor(PROFILE)).isEmpty()
            assertThat(dao.findSession(SESSION)).isNull()
        }
    }

    @Test
    fun givenAChildsProgressIsReset_whenItIsRemoved_thenTheChildRemains() {
        // Reset progress and delete profile are different actions with different confirmations.
        runBlocking {
            dao.upsertSession(session())
            dao.persistCheckpoint(attempt(), checkpoint(), lessonProgress())
            dao.deleteAllFor(PROFILE)
        }

        runBlocking {
            assertThat(dao.attemptsFor(PROFILE)).isEmpty()
            assertThat(database.childProfileDao().findById(PROFILE)).isNotNull()
        }
    }

    @Test
    fun givenWorkInTwoLessons_whenOneIsOpened_thenItResumesItsOwnAndNotTheOther() {
        // The reason a lesson has to ask about itself. A child with work waiting in two lessons
        // must not be dropped into the middle of the wrong one.
        runBlocking {
            dao.upsertCheckpoint(checkpoint(lastActivity = FIRST_ACTIVITY, lessonId = LESSON))
            dao.upsertCheckpoint(
                checkpoint(lastActivity = SECOND_ACTIVITY, lessonId = OTHER_LESSON)
            )
        }

        runBlocking {
            assertThat(dao.openCheckpoint(PROFILE, LESSON, VERSION)?.lastCompletedActivityId)
                .isEqualTo(FIRST_ACTIVITY)
            assertThat(dao.openCheckpoint(PROFILE, OTHER_LESSON, VERSION)?.lastCompletedActivityId)
                .isEqualTo(SECOND_ACTIVITY)
        }
    }

    @Test
    fun givenALessonNobodyHasOpened_whenItIsAskedAbout_thenThereIsNowhereToResume() {
        runBlocking { assertThat(dao.openCheckpoint(PROFILE, LESSON, VERSION)).isNull() }
    }

    @Test
    fun givenWorkUnderAnotherCourseVersion_whenThisOneIsOpened_thenItIsNotCountedAsProgress() {
        // Progress carries the version it was made under, so work done against different content
        // never decides where a child lands in this one.
        runBlocking { dao.upsertCheckpoint(checkpoint(courseVersion = "2025.01")) }

        runBlocking { assertThat(dao.openCheckpoint(PROFILE, LESSON, VERSION)).isNull() }
    }

    private fun profileRow() = ChildProfileEntity(
        id = PROFILE,
        nickname = "Minh",
        ageBand = "FOUR",
        avatarId = "rabbit",
        createdAt = NOW
    )

    private fun session(current: String = FIRST_INSTANCE) = LessonSessionEntity(
        id = SESSION,
        profileId = PROFILE,
        courseVersion = VERSION,
        lessonId = LESSON,
        currentActivityInstanceId = current,
        status = "IN_PROGRESS",
        startedAt = NOW,
        completedAt = null
    )

    private fun attempt(
        instance: String = FIRST_INSTANCE,
        activity: String = FIRST_ACTIVITY,
        ordinal: Int = 0
    ) = ActivityAttemptEntity(
        activityInstanceId = instance,
        profileId = PROFILE,
        sessionId = SESSION,
        activityId = activity,
        ordinal = ordinal,
        outcome = "CORRECT",
        at = NOW
    )

    private fun checkpoint(
        lastActivity: String = FIRST_ACTIVITY,
        lessonId: String = LESSON,
        courseVersion: String = VERSION
    ) = LessonCheckpointEntity(
        profileId = PROFILE,
        lessonId = lessonId,
        courseVersion = courseVersion,
        lastCompletedActivityId = lastActivity,
        sessionId = SESSION,
        updatedAt = NOW
    )

    private fun lessonProgress() = LessonProgressEntity(
        profileId = PROFILE,
        lessonId = LESSON,
        completed = false,
        updatedAt = NOW
    )

    private companion object {
        const val PROFILE = "p1"
        const val SESSION = "s1"
        const val VERSION = "2026.09"
        const val LESSON = "u01-my-body-l1"
        const val FIRST_ACTIVITY = "u01-my-body-l1-a1"
        const val SECOND_ACTIVITY = "u01-my-body-l1-a2"
        const val OTHER_LESSON = "u01-my-body-l2"
        const val FIRST_INSTANCE = "u01-my-body-l1-a1-1"
        const val SECOND_INSTANCE = "u01-my-body-l1-a2-1"
        const val NOW = 1_756_000_000_000
    }
}
