package com.nphkhiem.englishforyourchildren.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 knew children. Version 2 knows what they have done.
 *
 * Written by hand rather than left to a destructive fallback, because the fallback deletes a
 * child's progress to make a schema fit. Nothing here touches `child_profile`: the profiles that
 * existed before this migration are the same rows afterwards, and there is a test that says so.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lesson_session` (
                `id` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `courseVersion` TEXT NOT NULL,
                `lessonId` TEXT NOT NULL,
                `currentActivityInstanceId` TEXT,
                `status` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `completedAt` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`profileId`) REFERENCES `child_profile`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_lesson_session_profileId` ON `lesson_session` (`profileId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lesson_checkpoint` (
                `profileId` TEXT NOT NULL,
                `lessonId` TEXT NOT NULL,
                `courseVersion` TEXT NOT NULL,
                `lastCompletedActivityId` TEXT,
                `sessionId` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`profileId`, `lessonId`, `courseVersion`),
                FOREIGN KEY(`profileId`) REFERENCES `child_profile`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_lesson_checkpoint_profileId` ON `lesson_checkpoint` (`profileId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `activity_attempt` (
                `activityInstanceId` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `activityId` TEXT NOT NULL,
                `ordinal` INTEGER NOT NULL,
                `outcome` TEXT NOT NULL,
                `at` INTEGER NOT NULL,
                PRIMARY KEY(`activityInstanceId`),
                FOREIGN KEY(`profileId`) REFERENCES `child_profile`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_activity_attempt_profileId` ON `activity_attempt` (`profileId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_activity_attempt_sessionId` ON `activity_attempt` (`sessionId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lesson_progress` (
                `profileId` TEXT NOT NULL,
                `lessonId` TEXT NOT NULL,
                `completed` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`profileId`, `lessonId`),
                FOREIGN KEY(`profileId`) REFERENCES `child_profile`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_lesson_progress_profileId` ON `lesson_progress` (`profileId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `skill_progress` (
                `profileId` TEXT NOT NULL,
                `skillId` TEXT NOT NULL,
                `exposures` INTEGER NOT NULL,
                `supportedSuccesses` INTEGER NOT NULL,
                `reviewNeeded` INTEGER NOT NULL,
                `lastPractisedAt` INTEGER,
                PRIMARY KEY(`profileId`, `skillId`),
                FOREIGN KEY(`profileId`) REFERENCES `child_profile`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_skill_progress_profileId` ON `skill_progress` (`profileId`)"
        )
    }
}
