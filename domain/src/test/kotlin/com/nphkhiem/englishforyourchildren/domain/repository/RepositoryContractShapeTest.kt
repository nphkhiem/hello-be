package com.nphkhiem.englishforyourchildren.domain.repository

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.AgeBand
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
import com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome
import com.nphkhiem.englishforyourchildren.domain.model.AvatarId
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.ConfirmedCheckpoint
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.LessonCheckpoint
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileProgress
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * The contracts are interfaces with no behaviour, so what is worth testing is their shape: that a
 * repository can be implemented without reaching for Android, that a read can fail in a way the
 * caller has to handle, and that a write reports what happened rather than returning nothing.
 */
class RepositoryContractShapeTest {
    @Test
    fun givenStorageCannotBeRead_whenProfilesAreObserved_thenTheFailureIsAValue() {
        // The point of the whole result type. An empty list would mean both "no children yet" and
        // "the database will not open", and those send a caregiver to opposite screens: one to
        // create a profile, the other to recovery. See ADR 0008.
        val unreadable = object : FakeProfileRepository() {
            override fun observeProfiles() = flowOf<DomainResult<List<ChildProfile>>>(
                DomainResult.Failure(DomainError.PersistenceUnavailable)
            )
        }

        val read = runBlocking { unreadable.observeProfiles().first() }

        assertThat(read).isInstanceOf(DomainResult.Failure::class.java)
        assertThat((read as DomainResult.Failure).error)
            .isEqualTo(DomainError.PersistenceUnavailable)
    }

    @Test
    fun givenNoProfilesYet_whenStorageIsReadable_thenItIsAnEmptySuccessAndNotAFailure() {
        val empty = FakeProfileRepository()

        val read = runBlocking { empty.observeProfiles().first() }

        assertThat(read).isEqualTo(DomainResult.Success(emptyList<ChildProfile>()))
    }

    @Test
    fun givenAFullTelevision_whenAProfileIsCreated_thenTheLimitIsReportedByTheRepository() {
        // The four-profile cap is a domain rule. It is currently written twice, in :feature:profiles
        // and in :feature:caregiver, and this is the contract that lets it live in one place.
        val full = object : FakeProfileRepository() {
            override suspend fun create(command: CreateProfile) =
                DomainResult.Failure(DomainError.ProfileLimitReached)
        }

        val created = runBlocking { full.create(command = createProfile()) }

        assertThat((created as DomainResult.Failure).error)
            .isEqualTo(DomainError.ProfileLimitReached)
    }

    @Test
    fun givenAWrite_whenItSucceeds_thenItReportsWhatWasWrittenRatherThanNothing() {
        val repository = FakeProfileRepository()

        val created = runBlocking { repository.create(command = createProfile()) }

        assertThat((created as DomainResult.Success).value.nickname).isEqualTo(NICKNAME)
    }

    @Test
    fun givenACheckpoint_whenItIsPersisted_thenTheConfirmationIsWhatComesBack() {
        // Pending save is the vocabulary: nothing may claim progress is saved until storage says it
        // is. A write that returned Unit would leave the caller to assume it, so it returns the
        // confirmation instead.
        val repository = FakeProgressRepository()

        val confirmed = runBlocking { repository.persistCheckpoint(persistCheckpoint()) }

        assertThat(
            (confirmed as DomainResult.Success).value.confirmedAt
        ).isEqualTo(EpochMillis(NOW))
    }

    @Test
    fun givenSettings_whenEverySwitchTheScreenOffersIsListed_thenTheContractCanPersistThemAll() {
        // The built caregiver settings screen offers six: Vietnamese help, caregiver language,
        // captions, reduced motion, high contrast and background music. A setting a caregiver can
        // change with nowhere to persist it is a setting that silently forgets itself.
        val repository = FakeSettingsRepository()

        runBlocking {
            assertThat(repository.updateVietnameseHelp(enabled = true))
                .isEqualTo(DomainResult.Success(Unit))
            assertThat(repository.updateCaregiverLocale(localeTag = "vi"))
                .isEqualTo(DomainResult.Success(Unit))
            assertThat(repository.updateCaptions(enabled = true))
                .isEqualTo(DomainResult.Success(Unit))
            assertThat(repository.updateReducedMotion(enabled = true))
                .isEqualTo(DomainResult.Success(Unit))
            assertThat(repository.updateHighContrast(enabled = true))
                .isEqualTo(DomainResult.Success(Unit))
            assertThat(repository.updateBackgroundMusic(enabled = false))
                .isEqualTo(DomainResult.Success(Unit))
            assertThat(repository.updateSelectedProfile(profileId = null))
                .isEqualTo(DomainResult.Success(Unit))
        }
    }

    @Test
    fun givenTheContracts_whenImplemented_thenNothingAndroidShapedIsNeeded() {
        // The isolation test already proves Android is off the classpath. This proves the contracts
        // are implementable with what is left, which is the part that would break if a Room or
        // Compose type ever leaked into a signature.
        val implementations = listOf(
            FakeProfileRepository(),
            FakeProgressRepository(),
            FakeSettingsRepository()
        )

        assertThat(implementations).hasSize(3)
    }

    private fun createProfile() = CreateProfile(
        nickname = NICKNAME,
        ageBand = AgeBand.THREE,
        avatarId = AvatarId(AVATAR)
    )

    private fun persistCheckpoint() = PersistCheckpoint(
        sessionId = SessionId(SESSION),
        profileId = ProfileId(PROFILE),
        courseVersion = CourseVersion(VERSION),
        lessonId = LessonId(LESSON),
        activityId = ActivityId(ACTIVITY),
        activityInstanceId = ActivityInstanceId(INSTANCE),
        activityOrdinal = 0,
        outcome = AttemptOutcome.CORRECT,
        completedAt = EpochMillis(NOW)
    )

    private open class FakeProfileRepository : ProfileRepository {
        override fun observeProfiles(): Flow<DomainResult<List<ChildProfile>>> =
            flowOf(DomainResult.Success(emptyList()))

        override suspend fun create(command: CreateProfile): DomainResult<ChildProfile> =
            DomainResult.Success(
                ChildProfile(
                    id = ProfileId(PROFILE),
                    nickname = command.nickname,
                    ageBand = command.ageBand,
                    avatarId = command.avatarId
                )
            )

        override suspend fun update(profile: ChildProfile): DomainResult<ChildProfile> =
            DomainResult.Success(profile)

        override suspend fun delete(id: ProfileId): DomainResult<Unit> = DomainResult.Success(Unit)
    }

    private open class FakeProgressRepository : ProgressRepository {
        override fun observeProfileProgress(
            profileId: ProfileId
        ): Flow<DomainResult<ProfileProgress>> = flowOf(
            DomainResult.Success(
                ProfileProgress(
                    profileId = profileId,
                    lessonsCompleted = emptySet(),
                    skills = emptyList(),
                    openCheckpoint = null
                )
            )
        )

        override suspend fun openCheckpoint(
            profileId: ProfileId,
            lessonId: LessonId,
            courseVersion: CourseVersion
        ): DomainResult<LessonCheckpoint?> = DomainResult.Success(null)

        override suspend fun startSession(command: StartSession) =
            DomainResult.Failure(DomainError.LessonNotFound)

        override suspend fun persistCheckpoint(
            command: PersistCheckpoint
        ): DomainResult<ConfirmedCheckpoint> = DomainResult.Success(
            ConfirmedCheckpoint(
                sessionId = command.sessionId,
                lessonId = command.lessonId,
                lastCompletedActivity = command.activityId,
                confirmedAt = command.completedAt
            )
        )

        override suspend fun completeSession(command: CompleteSession) =
            DomainResult.Failure(DomainError.PersistenceUnavailable)

        override suspend fun deleteProfileProgress(profileId: ProfileId) =
            DomainResult.Success(Unit)
    }

    private open class FakeSettingsRepository : SettingsRepository {
        override fun observeSettings(): Flow<DomainResult<AppSettings>> =
            flowOf(DomainResult.Success(AppSettings.DEFAULT))

        override suspend fun updateSelectedProfile(profileId: ProfileId?) =
            DomainResult.Success(Unit)

        override suspend fun updateCaregiverLocale(localeTag: String) = DomainResult.Success(Unit)

        override suspend fun updateVietnameseHelp(enabled: Boolean) = DomainResult.Success(Unit)

        override suspend fun updateCaptions(enabled: Boolean) = DomainResult.Success(Unit)

        override suspend fun updateReducedMotion(enabled: Boolean) = DomainResult.Success(Unit)

        override suspend fun updateHighContrast(enabled: Boolean) = DomainResult.Success(Unit)

        override suspend fun updateBackgroundMusic(enabled: Boolean) = DomainResult.Success(Unit)
    }

    private companion object {
        const val PROFILE = "p1"
        const val SESSION = "s1"
        const val LESSON = "u01-my-body-l1"
        const val ACTIVITY = "u01-my-body-l1-a1"
        const val INSTANCE = "u01-my-body-l1-a1-1"
        const val VERSION = "2026.09"
        const val NICKNAME = "Minh"
        const val AVATAR = "rabbit"
        const val NOW = 1_756_000_000_000
    }
}
