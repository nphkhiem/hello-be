package com.nphkhiem.englishforyourchildren.data.profile

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AgeBand
import com.nphkhiem.englishforyourchildren.domain.model.AvatarId
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.MAX_CHILD_PROFILES
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CreateProfile
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.testsupport.FakeTimeProvider
import com.nphkhiem.englishforyourchildren.testsupport.SequentialIdProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class RoomProfileRepositoryTest {
    @Test
    fun givenNoProfiles_whenTheyAreObserved_thenItIsAnEmptySuccessAndNotAFailure() {
        val repository = repository()

        val read = runBlocking { repository.observeProfiles().first() }

        assertThat(read).isEqualTo(DomainResult.Success(emptyList<ChildProfile>()))
    }

    @Test
    fun givenTheDatabaseWillNotAnswer_whenProfilesAreObserved_thenItIsAFailure() {
        // The distinction the whole result type exists for. An empty list here would send a
        // caregiver to create a profile on a television that cannot read the ones they already made.
        val dao = FakeChildProfileDao()
        dao.failReads()

        val read = runBlocking { repository(dao).observeProfiles().first() }

        assertThat(
            (read as DomainResult.Failure).error
        ).isEqualTo(DomainError.PersistenceUnavailable)
    }

    @Test
    fun givenAProfileIsCreated_thenItTakesItsIdAndItsInstantFromWhatItWasGiven() {
        val clock = FakeTimeProvider(EpochMillis(NOW))
        val dao = FakeChildProfileDao()

        val created = runBlocking { repository(dao, clock).create(createProfile()) }

        assertThat((created as DomainResult.Success).value.id).isEqualTo(ProfileId("p1"))
        assertThat(dao.rows.single().createdAt).isEqualTo(NOW)
    }

    @Test
    fun givenFourChildren_whenAFifthIsCreated_thenItIsRefusedAndNothingIsWritten() {
        // Four is a rule about children, not about SQLite, and refusing before writing is what
        // makes it a limit rather than a cleanup.
        val dao = FakeChildProfileDao()
        val repository = repository(dao)
        runBlocking { repeat(MAX_CHILD_PROFILES) { repository.create(createProfile()) } }

        val fifth = runBlocking { repository.create(createProfile()) }

        assertThat((fifth as DomainResult.Failure).error).isEqualTo(DomainError.ProfileLimitReached)
        assertThat(dao.rows).hasSize(MAX_CHILD_PROFILES)
    }

    @Test
    fun givenFourChildrenAndOneLeaves_whenAnotherIsCreated_thenThereIsRoomAgain() {
        val dao = FakeChildProfileDao()
        val repository = repository(dao)
        runBlocking { repeat(MAX_CHILD_PROFILES) { repository.create(createProfile()) } }

        runBlocking { repository.delete(ProfileId("p1")) }
        val replacement = runBlocking { repository.create(createProfile()) }

        assertThat(replacement).isInstanceOf(DomainResult.Success::class.java)
    }

    @Test
    fun givenAProfileThatIsNotThere_whenItIsUpdated_thenItIsReportedRatherThanInserted() {
        // Silently inserting would turn a stale edit into a new child appearing on the picker.
        val dao = FakeChildProfileDao()

        val updated = runBlocking { repository(dao).update(profile(ProfileId("ghost"))) }

        assertThat((updated as DomainResult.Failure).error).isEqualTo(DomainError.ProfileNotFound)
        assertThat(dao.rows).isEmpty()
    }

    @Test
    fun givenAnExistingProfile_whenItIsUpdated_thenTheStoredRowChangesAndItsPlaceDoesNot() {
        // The created instant orders the picker, so an edit must not move a child's face.
        val dao = FakeChildProfileDao()
        val clock = FakeTimeProvider(EpochMillis(NOW))
        val repository = repository(dao, clock)
        runBlocking { repository.create(createProfile(nickname = "Minh")) }
        val originalPlace = dao.rows.single().createdAt

        // The clock has to move, or a restamped instant would equal the original one and this
        // assertion would pass against a repository that reorders the picker on every edit.
        clock.advanceBy(milliseconds = 60_000)
        runBlocking { repository.update(profile(ProfileId("p1"), nickname = "Minh Anh")) }

        assertThat(dao.rows.single().nickname).isEqualTo("Minh Anh")
        assertThat(dao.rows.single().createdAt).isEqualTo(originalPlace)
    }

    @Test
    fun givenAWriteThatFails_whenAProfileIsCreated_thenItIsReportedAndNotSwallowed() {
        val dao = FakeChildProfileDao()
        dao.failWrites()

        val created = runBlocking { repository(dao).create(createProfile()) }

        assertThat((created as DomainResult.Failure).error)
            .isEqualTo(DomainError.PersistenceUnavailable)
    }

    @Test
    fun givenAProfileThatIsNotThere_whenItIsDeleted_thenTheCallerHasWhatTheyAskedFor() {
        // Asking for a child to be gone from a television that never had them is not an error.
        val deleted = runBlocking { repository().delete(ProfileId("ghost")) }

        assertThat(deleted).isEqualTo(DomainResult.Success(Unit))
    }

    private fun repository(
        dao: FakeChildProfileDao = FakeChildProfileDao(),
        clock: FakeTimeProvider = FakeTimeProvider(EpochMillis(NOW))
    ) = RoomProfileRepository(
        dao = dao,
        idProvider = SequentialIdProvider(prefix = "p") { ProfileId(it) },
        timeProvider = clock
    )

    private fun createProfile(nickname: String = "Minh") = CreateProfile(
        nickname = nickname,
        ageBand = AgeBand.THREE,
        avatarId = AvatarId("rabbit")
    )

    private fun profile(id: ProfileId, nickname: String = "Minh") = ChildProfile(
        id = id,
        nickname = nickname,
        ageBand = AgeBand.THREE,
        avatarId = AvatarId("rabbit")
    )

    /**
     * A DAO that keeps rows in a list.
     *
     * Hand written rather than mocked, and deliberately not Room: what is under test here is the
     * repository's policy, and SQLite already has its own instrumented tests from P2-T1.
     */
    private class FakeChildProfileDao : ChildProfileDao {
        private val stored = MutableStateFlow<List<ChildProfileEntity>>(emptyList())
        private var readsFail = false
        private var writesFail = false

        val rows: List<ChildProfileEntity> get() = stored.value

        fun failReads() {
            readsFail = true
        }

        fun failWrites() {
            writesFail = true
        }

        override fun observeAll(): Flow<List<ChildProfileEntity>> = stored.map {
            if (readsFail) throw IllegalStateException("database unavailable") else it
        }

        override suspend fun findById(id: String): ChildProfileEntity? =
            stored.value.firstOrNull { it.id == id }

        override suspend fun count(): Int = stored.value.size

        override suspend fun insert(profile: ChildProfileEntity) {
            if (writesFail) throw IllegalStateException("database unavailable")
            require(stored.value.none { it.id == profile.id }) { "duplicate id" }
            stored.value = stored.value + profile
        }

        override suspend fun update(profile: ChildProfileEntity) {
            if (writesFail) throw IllegalStateException("database unavailable")
            stored.value = stored.value.map { if (it.id == profile.id) profile else it }
        }

        override suspend fun delete(id: String) {
            if (writesFail) throw IllegalStateException("database unavailable")
            stored.value = stored.value.filterNot { it.id == id }
        }
    }

    private companion object {
        const val NOW = 1_756_000_000_000
    }
}
