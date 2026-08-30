package com.nphkhiem.englishforyourchildren.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HelloBeDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun givenAChildStoredUnderVersionOne_whenTheDatabaseMoves_thenTheyAreStillThere() {
        // The whole reason this migration is written by hand. A destructive fallback would satisfy
        // the schema by deleting the child, and nobody would be told.
        helper.createDatabase(NAME, 1).use { v1 ->
            v1.execSQL(
                "INSERT INTO child_profile (id, nickname, ageBand, avatarId, createdAt) " +
                    "VALUES ('p1', 'Minh', 'FOUR', 'rabbit', 1756000000000)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(NAME, 2, true, MIGRATION_1_2)

        migrated.query("SELECT nickname, ageBand FROM child_profile WHERE id = 'p1'").use {
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.getString(0)).isEqualTo("Minh")
            assertThat(it.getString(1)).isEqualTo("FOUR")
        }
    }

    @Test
    fun givenTheMigration_whenItRuns_thenTheProgressTablesExistAndAreEmpty() {
        helper.createDatabase(NAME, 1).close()

        val migrated = helper.runMigrationsAndValidate(NAME, 2, true, MIGRATION_1_2)

        for (table in PROGRESS_TABLES) {
            migrated.query("SELECT COUNT(*) FROM $table").use {
                assertThat(it.moveToFirst()).isTrue()
                assertThat(it.getInt(0)).isEqualTo(0)
            }
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
        val PROGRESS_TABLES = listOf(
            "lesson_session",
            "lesson_checkpoint",
            "activity_attempt",
            "lesson_progress",
            "skill_progress"
        )
    }
}
