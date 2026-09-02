package com.nphkhiem.englishforyourchildren.journey

import com.nphkhiem.englishforyourchildren.data.progress.ActivityAttemptEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonCheckpointEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonProgressEntity
import com.nphkhiem.englishforyourchildren.data.progress.ProgressDao
import kotlinx.coroutines.delay

/**
 * Storage that takes its time writing, once, on request.
 *
 * Slow storage is not the thing under test. It is how the moment under test is made to last long
 * enough to press into: a second press arriving while the first is still being written down. On a
 * television that window is a few milliseconds and a child's thumb is faster than it.
 *
 * Like `RefusingProgressDao` it wraps the DAO rather than the repository, so everything above it
 * stays production code.
 */
class DelayingProgressDao(private val real: ProgressDao) : ProgressDao by real {
    @Volatile
    private var delayOnce = false

    fun holdTheNextWrite() {
        delayOnce = true
    }

    override suspend fun persistCheckpoint(
        attempt: ActivityAttemptEntity,
        checkpoint: LessonCheckpointEntity,
        lessonProgress: LessonProgressEntity
    ) {
        if (delayOnce) {
            delayOnce = false
            delay(HELD_MILLIS)
        }
        real.persistCheckpoint(attempt, checkpoint, lessonProgress)
    }

    private companion object {
        /** Long enough that a second press lands inside it, short enough not to pad the suite. */
        const val HELD_MILLIS = 1_500L
    }
}
