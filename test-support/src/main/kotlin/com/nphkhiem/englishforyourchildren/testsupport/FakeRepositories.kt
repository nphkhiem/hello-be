package com.nphkhiem.englishforyourchildren.testsupport

import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
import com.nphkhiem.englishforyourchildren.domain.model.Attribution
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
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
import com.nphkhiem.englishforyourchildren.domain.repository.CompleteSession
import com.nphkhiem.englishforyourchildren.domain.repository.CreateProfile
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.PersistCheckpoint
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.repository.StartSession
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one failure a fake has been told to produce next.
 *
 * One-shot rather than sticky, so a test can show that a retry after a failure succeeds. A fake
 * that stayed broken could not express the recovery path, which is the path most worth testing on a
 * product whose recovery family is a designed screen rather than a crash.
 */
private class NextFailure {
    private var queued: DomainError? = null

    fun queue(error: DomainError) {
        queued = error
    }

    /** Returns the queued error once, then forgets it. */
    fun take(): DomainError? = queued.also { queued = null }
}

/**
 * The children this fake television knows.
 *
 * Reads are a [MutableStateFlow], so a collector that arrives after a value was set still sees it.
 * A channel would leave a late collector waiting, which in a test reads as a hang rather than as a
 * failure, and a hanging test is the worst kind to debug.
 */
class FakeProfileRepository : ProfileRepository {
    private val profiles = MutableStateFlow<DomainResult<List<ChildProfile>>>(
        DomainResult.Success(emptyList())
    )
    private val failure = NextFailure()
    private val commands = mutableListOf<CreateProfile>()
    private val deletions = mutableListOf<ProfileId>()

    /** The create commands received, oldest first, as a snapshot that will not change later. */
    val created: List<CreateProfile> get() = commands.toList()

    /** The profiles this fake was asked to delete, as a snapshot. */
    val deleted: List<ProfileId> get() = deletions.toList()

    fun setProfiles(profiles: List<ChildProfile>) {
        this.profiles.value = DomainResult.Success(profiles)
    }

    fun setReadFailure(error: DomainError) {
        profiles.value = DomainResult.Failure(error)
    }

    fun failNext(error: DomainError) = failure.queue(error)

    override fun observeProfiles(): Flow<DomainResult<List<ChildProfile>>> = profiles.asStateFlow()

    override suspend fun create(command: CreateProfile): DomainResult<ChildProfile> {
        commands += command
        failure.take()?.let { return DomainResult.Failure(it) }
        return DomainResult.Success(
            DomainBuilders.childProfile(
                nickname = command.nickname,
                ageBand = command.ageBand,
                avatarId = command.avatarId
            )
        )
    }

    override suspend fun update(profile: ChildProfile): DomainResult<ChildProfile> {
        failure.take()?.let { return DomainResult.Failure(it) }
        return DomainResult.Success(profile)
    }

    override suspend fun delete(id: ProfileId): DomainResult<Unit> {
        deletions += id
        failure.take()?.let { return DomainResult.Failure(it) }
        return DomainResult.Success(Unit)
    }
}

/**
 * What each child has done here.
 *
 * It confirms a checkpoint with the clock it was given rather than with the one in the command, so
 * a test can advance time and see the confirmation move with it.
 */
class FakeProgressRepository(
    private val timeProvider: TimeProvider = FakeTimeProvider(EpochMillis(DEFAULT_NOW))
) : ProgressRepository {
    private val progress = MutableStateFlow<DomainResult<ProfileProgress>>(
        DomainResult.Success(DomainBuilders.profileProgress())
    )
    private val failure = NextFailure()
    private val checkpoints = mutableListOf<PersistCheckpoint>()
    private val starts = mutableListOf<StartSession>()
    private val completions = mutableListOf<CompleteSession>()

    /**
     * Deliberately outside the [failNext] queue. Opening a lesson reads this before anything else,
     * so sharing that queue would let a failure meant for a write be eaten by a read instead.
     */
    private var resumePoint: DomainResult<LessonCheckpoint?> = DomainResult.Success(null)

    /** Every checkpoint this fake was asked to persist, oldest first, as a snapshot. */
    val persisted: List<PersistCheckpoint> get() = checkpoints.toList()

    /** Every session start requested, as a snapshot. */
    val started: List<StartSession> get() = starts.toList()

    /** Every session completion requested, as a snapshot. */
    val completed: List<CompleteSession> get() = completions.toList()

    fun setProgress(value: ProfileProgress) {
        progress.value = DomainResult.Success(value)
    }

    fun setReadFailure(error: DomainError) {
        progress.value = DomainResult.Failure(error)
    }

    fun failNext(error: DomainError) = failure.queue(error)

    override fun observeProfileProgress(profileId: ProfileId): Flow<DomainResult<ProfileProgress>> =
        progress.asStateFlow()

    /** Where a child left a lesson, for a test that wants one resumed. */
    fun setOpenCheckpoint(checkpoint: LessonCheckpoint?) {
        resumePoint = DomainResult.Success(checkpoint)
    }

    fun setCheckpointFailure(error: DomainError) {
        resumePoint = DomainResult.Failure(error)
    }

    override suspend fun openCheckpoint(
        profileId: ProfileId,
        lessonId: LessonId,
        courseVersion: CourseVersion
    ): DomainResult<LessonCheckpoint?> = resumePoint

    override suspend fun startSession(command: StartSession): DomainResult<LessonSession> {
        starts += command
        failure.take()?.let { return DomainResult.Failure(it) }
        return DomainResult.Success(
            DomainBuilders.lessonSession(
                profileId = command.profileId,
                lessonId = command.lessonId,
                courseVersion = command.courseVersion
            )
        )
    }

    override suspend fun persistCheckpoint(
        command: PersistCheckpoint
    ): DomainResult<ConfirmedCheckpoint> {
        checkpoints += command
        failure.take()?.let { return DomainResult.Failure(it) }
        return DomainResult.Success(
            ConfirmedCheckpoint(
                sessionId = command.sessionId,
                lessonId = command.lessonId,
                lastCompletedActivity = command.activityId,
                confirmedAt = timeProvider.now()
            )
        )
    }

    override suspend fun completeSession(command: CompleteSession): DomainResult<LessonCompletion> {
        completions += command
        failure.take()?.let { return DomainResult.Failure(it) }
        return DomainResult.Success(
            LessonCompletion(
                sessionId = command.sessionId,
                lessonId = LessonId(DEFAULT_LESSON),
                learnedSkills = emptyList(),
                unitCompleted = false,
                completedAt = timeProvider.now()
            )
        )
    }

    override suspend fun deleteProfileProgress(profileId: ProfileId): DomainResult<Unit> {
        failure.take()?.let { return DomainResult.Failure(it) }
        return DomainResult.Success(Unit)
    }

    private companion object {
        const val DEFAULT_NOW = 1_756_000_000_000
        const val DEFAULT_LESSON = "u01-my-body-l1"
    }
}

/** The packaged course, as a fake that holds whatever a test put in it. */
class FakeCurriculumRepository : CurriculumRepository {
    private val course = MutableStateFlow<DomainResult<Course>>(
        DomainResult.Success(DomainBuilders.course())
    )
    private val failure = NextFailure()
    private val lessons = mutableMapOf<LessonId, Lesson>()
    private var attributions: List<Attribution> = emptyList()

    fun setCourse(value: Course) {
        course.value = DomainResult.Success(value)
    }

    fun setReadFailure(error: DomainError) {
        course.value = DomainResult.Failure(error)
    }

    fun setLesson(lesson: Lesson) {
        lessons[lesson.id] = lesson
    }

    fun setAttributions(value: List<Attribution>) {
        attributions = value
    }

    fun failNext(error: DomainError) = failure.queue(error)

    override fun observeCourse(): Flow<DomainResult<Course>> = course.asStateFlow()

    override suspend fun getLesson(id: LessonId, version: CourseVersion): DomainResult<Lesson> {
        failure.take()?.let { return DomainResult.Failure(it) }
        val found = lessons[id] ?: return DomainResult.Failure(DomainError.LessonNotFound)
        return DomainResult.Success(found)
    }

    override suspend fun getAttributions(): DomainResult<List<Attribution>> {
        failure.take()?.let { return DomainResult.Failure(it) }
        return DomainResult.Success(attributions)
    }
}

/** What a caregiver has chosen, remembered in memory. */
class FakeSettingsRepository(initial: AppSettings = AppSettings.DEFAULT) : SettingsRepository {
    private val settings = MutableStateFlow<DomainResult<AppSettings>>(
        DomainResult.Success(initial)
    )
    private val failure = NextFailure()

    /** What the settings currently are, for a test that would rather assert than collect. */
    val current: DomainResult<AppSettings> get() = settings.value

    fun setReadFailure(error: DomainError) {
        settings.value = DomainResult.Failure(error)
    }

    fun failNext(error: DomainError) = failure.queue(error)

    override fun observeSettings(): Flow<DomainResult<AppSettings>> = settings.asStateFlow()

    override suspend fun updateSelectedProfile(profileId: ProfileId?): DomainResult<Unit> =
        change { it.copy(selectedProfileId = profileId) }

    override suspend fun updateCaregiverLanguage(language: CaregiverLanguage): DomainResult<Unit> =
        change { it.copy(caregiverLanguage = language) }

    override suspend fun updateVietnameseHelp(enabled: Boolean): DomainResult<Unit> =
        change { it.copy(vietnameseHelpEnabled = enabled) }

    override suspend fun updateCaptions(enabled: Boolean): DomainResult<Unit> =
        change { it.copy(captionsEnabled = enabled) }

    override suspend fun updateReducedMotion(enabled: Boolean): DomainResult<Unit> =
        change { it.copy(reducedMotionEnabled = enabled) }

    override suspend fun updateHighContrast(enabled: Boolean): DomainResult<Unit> =
        change { it.copy(highContrastEnabled = enabled) }

    override suspend fun updateBackgroundMusic(enabled: Boolean): DomainResult<Unit> =
        change { it.copy(backgroundMusicEnabled = enabled) }

    /**
     * Applies a change, unless a failure was queued.
     *
     * A refused write leaves the stored settings exactly as they were, which is what a real store
     * that could not write would do, and what lets a test prove the screen did not move on.
     */
    private fun change(edit: (AppSettings) -> AppSettings): DomainResult<Unit> {
        failure.take()?.let { return DomainResult.Failure(it) }
        val stored = settings.value
        if (stored is DomainResult.Success) {
            settings.value = DomainResult.Success(edit(stored.value))
        }
        return DomainResult.Success(Unit)
    }
}
