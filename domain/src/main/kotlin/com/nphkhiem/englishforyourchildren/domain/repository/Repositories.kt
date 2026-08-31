package com.nphkhiem.englishforyourchildren.domain.repository

import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.AgeBand
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
import com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome
import com.nphkhiem.englishforyourchildren.domain.model.Attribution
import com.nphkhiem.englishforyourchildren.domain.model.AvatarId
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.ConfirmedCheckpoint
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonCheckpoint
import com.nphkhiem.englishforyourchildren.domain.model.LessonCompletion
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.LessonSession
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileProgress
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import kotlinx.coroutines.flow.Flow

/**
 * The packaged course.
 *
 * Read-only for the lifetime of an installed version: content arrives with the app and nothing at
 * runtime writes to it.
 */
interface CurriculumRepository {
    fun observeCourse(): Flow<DomainResult<Course>>

    suspend fun getLesson(id: LessonId, version: CourseVersion): DomainResult<Lesson>

    suspend fun getAttributions(): DomainResult<List<Attribution>>
}

/**
 * The children this television knows.
 *
 * The four-profile limit is enforced here and nowhere else. It is a domain rule, and it is
 * currently written twice in the feature modules; this contract is where it belongs, reported as
 * [com.nphkhiem.englishforyourchildren.domain.result.DomainError.ProfileLimitReached].
 */
interface ProfileRepository {
    fun observeProfiles(): Flow<DomainResult<List<ChildProfile>>>

    suspend fun create(command: CreateProfile): DomainResult<ChildProfile>

    suspend fun update(profile: ChildProfile): DomainResult<ChildProfile>

    suspend fun delete(id: ProfileId): DomainResult<Unit>
}

/** What a caregiver typed to make a child, before that child has an identity. */
data class CreateProfile(val nickname: String, val ageBand: AgeBand, val avatarId: AvatarId)

/**
 * What each child has done, and the writes that record it.
 *
 * Every write returns what it wrote rather than nothing, because a caller cannot honestly move a
 * lesson on until storage has confirmed the step behind it.
 */
interface ProgressRepository {
    fun observeProfileProgress(profileId: ProfileId): Flow<DomainResult<ProfileProgress>>

    suspend fun startSession(command: StartSession): DomainResult<LessonSession>

    /**
     * Where a child left this lesson, or null if they have never been in it.
     *
     * A lesson has to ask about itself. [observeProfileProgress] carries a single open checkpoint
     * for the whole profile, which cannot say which lesson it belongs to without the caller
     * checking, and a child may have work waiting in more than one.
     */
    suspend fun openCheckpoint(
        profileId: ProfileId,
        lessonId: LessonId,
        courseVersion: CourseVersion
    ): DomainResult<LessonCheckpoint?>

    suspend fun persistCheckpoint(command: PersistCheckpoint): DomainResult<ConfirmedCheckpoint>

    suspend fun completeSession(command: CompleteSession): DomainResult<LessonCompletion>

    /** Everything one child has done, removed together, or not at all. */
    suspend fun deleteProfileProgress(profileId: ProfileId): DomainResult<Unit>
}

/** Opening a lesson for a child, at the content version they are working through. */
data class StartSession(
    val profileId: ProfileId,
    val lessonId: LessonId,
    val courseVersion: CourseVersion
)

/**
 * One finished activity, written down.
 *
 * A completed activity is the smallest durable checkpoint, so this command carries everything
 * needed to place it: which child, which lesson at which content version, which activity and which
 * of their encounters with it, where it came in the order, and how it went.
 */
data class PersistCheckpoint(
    val sessionId: SessionId,
    val profileId: ProfileId,
    val courseVersion: CourseVersion,
    val lessonId: LessonId,
    val activityId: ActivityId,
    val activityInstanceId: ActivityInstanceId,
    val activityOrdinal: Int,
    val outcome: AttemptOutcome,
    val completedAt: EpochMillis
) {
    init {
        require(activityOrdinal >= 0) { "An activity cannot come before the first one" }
    }
}

/** Closing a lesson that reached its end. */
data class CompleteSession(val sessionId: SessionId, val completedAt: EpochMillis)

/**
 * What a caregiver has chosen, and which child the television is on.
 *
 * One method per switch the settings screen offers, so that a screen and this contract can be read
 * against each other without a mapping table in between.
 */
interface SettingsRepository {
    fun observeSettings(): Flow<DomainResult<AppSettings>>

    suspend fun updateSelectedProfile(profileId: ProfileId?): DomainResult<Unit>

    suspend fun updateCaregiverLocale(localeTag: String): DomainResult<Unit>

    suspend fun updateVietnameseHelp(enabled: Boolean): DomainResult<Unit>

    suspend fun updateCaptions(enabled: Boolean): DomainResult<Unit>

    suspend fun updateReducedMotion(enabled: Boolean): DomainResult<Unit>

    suspend fun updateHighContrast(enabled: Boolean): DomainResult<Unit>

    suspend fun updateBackgroundMusic(enabled: Boolean): DomainResult<Unit>
}
