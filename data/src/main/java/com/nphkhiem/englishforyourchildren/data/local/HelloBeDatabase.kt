package com.nphkhiem.englishforyourchildren.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileDao
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileEntity

/**
 * Everything Hello Bé keeps on this television.
 *
 * Version 1 holds profiles. Progress arrives in version 2, which is why the schema is exported and
 * committed: a migration cannot be checked against a shape nobody kept.
 *
 * There is deliberately no `fallbackToDestructiveMigration` here or at any call site. A database
 * that will not open, or one whose shape has moved on, must surface as a failure the caller
 * handles, and the app already turns that into the caregiver recovery where a reset sits behind the
 * adult gate. A child's progress is only ever deleted by an adult who was told what would happen.
 */
@Database(entities = [ChildProfileEntity::class], version = 1, exportSchema = true)
abstract class HelloBeDatabase : RoomDatabase() {
    abstract fun childProfileDao(): ChildProfileDao

    companion object {
        /** The file this database lives in. Named once, so nothing has to guess it later. */
        const val NAME = "hello-be.db"
    }
}
