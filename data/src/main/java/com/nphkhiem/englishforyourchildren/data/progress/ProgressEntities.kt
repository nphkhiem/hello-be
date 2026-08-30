package com.nphkhiem.englishforyourchildren.data.progress

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileEntity

/**
 * One sitting at a lesson.
 *
 * The session id is the key rather than a generated number, so a retried start writes the same row
 * instead of a second sitting.
 */
@Entity(
    tableName = "lesson_session",
    foreignKeys = [
        ForeignKey(
            entity = ChildProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class LessonSessionEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val courseVersion: String,
    val lessonId: String,
    val currentActivityInstanceId: String?,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?
)

/**
 * The last activity a child finished in a lesson, and therefore where they come back to.
 *
 * Keyed by child, lesson and content version together: one place to return to per lesson, and work
 * done under one published version is never counted against another.
 */
@Entity(
    tableName = "lesson_checkpoint",
    primaryKeys = ["profileId", "lessonId", "courseVersion"],
    foreignKeys = [
        ForeignKey(
            entity = ChildProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class LessonCheckpointEntity(
    val profileId: String,
    val lessonId: String,
    val courseVersion: String,
    val lastCompletedActivityId: String?,
    val sessionId: String,
    val updatedAt: Long
)

/**
 * What happened when a child met one activity, once.
 *
 * The activity instance is the key, so writing the same attempt twice replaces it rather than
 * recording a child answering twice. That is what makes a retried save safe.
 */
@Entity(
    tableName = "activity_attempt",
    foreignKeys = [
        ForeignKey(
            entity = ChildProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index("sessionId")]
)
data class ActivityAttemptEntity(
    @PrimaryKey val activityInstanceId: String,
    val profileId: String,
    val sessionId: String,
    val activityId: String,
    val ordinal: Int,
    val outcome: String,
    val at: Long
)

/** Which lessons a child has finished, so the learning path can show where they are. */
@Entity(
    tableName = "lesson_progress",
    primaryKeys = ["profileId", "lessonId"],
    foreignKeys = [
        ForeignKey(
            entity = ChildProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class LessonProgressEntity(
    val profileId: String,
    val lessonId: String,
    val completed: Boolean,
    val updatedAt: Long
)

/**
 * How well one child knows one thing.
 *
 * Written by the content that knows which words an activity teaches, which does not exist yet. The
 * table is here because the migration that adds it should happen once, not twice.
 */
@Entity(
    tableName = "skill_progress",
    primaryKeys = ["profileId", "skillId"],
    foreignKeys = [
        ForeignKey(
            entity = ChildProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class SkillProgressEntity(
    val profileId: String,
    val skillId: String,
    val exposures: Int,
    val supportedSuccesses: Int,
    val reviewNeeded: Boolean,
    val lastPractisedAt: Long?
)
