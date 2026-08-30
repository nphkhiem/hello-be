package com.nphkhiem.englishforyourchildren.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileDao
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileEntity
import com.nphkhiem.englishforyourchildren.data.progress.ActivityAttemptEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonCheckpointEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonProgressEntity
import com.nphkhiem.englishforyourchildren.data.progress.LessonSessionEntity
import com.nphkhiem.englishforyourchildren.data.progress.ProgressDao
import com.nphkhiem.englishforyourchildren.data.progress.SkillProgressEntity

/**
 * Everything Hello Bé keeps on this television.
 *
 * Version 1 held profiles. Version 2 adds what those children have done, reached by a hand written
 * migration rather than by throwing the old database away.
 *
 * There is deliberately no `fallbackToDestructiveMigration` here or at any call site. A database
 * that will not open, or one whose shape has moved on, must surface as a failure the caller
 * handles, and the app already turns that into the caregiver recovery where a reset sits behind the
 * adult gate. A child's progress is only ever deleted by an adult who was told what would happen.
 */
@Database(
    entities = [
        ChildProfileEntity::class,
        LessonSessionEntity::class,
        LessonCheckpointEntity::class,
        ActivityAttemptEntity::class,
        LessonProgressEntity::class,
        SkillProgressEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class HelloBeDatabase : RoomDatabase() {
    abstract fun childProfileDao(): ChildProfileDao

    abstract fun progressDao(): ProgressDao

    companion object {
        /** The file this database lives in. Named once, so nothing has to guess it later. */
        const val NAME = "hello-be.db"
    }
}
