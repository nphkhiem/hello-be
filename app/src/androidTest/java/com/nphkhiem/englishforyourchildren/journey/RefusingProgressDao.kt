package com.nphkhiem.englishforyourchildren.journey

import com.nphkhiem.englishforyourchildren.data.progress.ActivityAttemptEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonCheckpointEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonProgressEntity
import com.nphkhiem.englishforyourchildren.data.progress.ProgressDao
import java.io.IOException

/**
 * Storage that refuses to write, once, on request.
 *
 * It fails at the database rather than at the repository, so everything above it is the production
 * code: `RoomProgressRepository` turns the exception into a typed failure, the reducer decides what
 * a refused write means, and the screen says "Not saved yet" on its own. A fake repository would
 * have skipped all three of the things worth testing.
 */
class RefusingProgressDao(private val real: ProgressDao) : ProgressDao by real {
    @Volatile
    private var refuseOnce = false

    fun refuseTheNextWrite() {
        refuseOnce = true
    }

    override suspend fun persistCheckpoint(
        attempt: ActivityAttemptEntity,
        checkpoint: LessonCheckpointEntity,
        lessonProgress: LessonProgressEntity
    ) {
        if (refuseOnce) {
            refuseOnce = false
            throw IOException("storage refused this write")
        }
        real.persistCheckpoint(attempt, checkpoint, lessonProgress)
    }
}
