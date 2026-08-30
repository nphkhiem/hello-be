package com.nphkhiem.englishforyourchildren.testsupport

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.SessionId
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class TestSupportTest {
    @Test
    fun givenAFakeClock_whenTimeIsReadTwice_thenItHasNotMoved() {
        // The whole point. A test that reads the real clock twice can fail once a year on a leap
        // second and nobody will ever reproduce it.
        val clock = FakeTimeProvider(initial = EpochMillis(NOW))

        assertThat(clock.now()).isEqualTo(clock.now())
        assertThat(clock.now()).isEqualTo(EpochMillis(NOW))
    }

    @Test
    fun givenAFakeClock_whenItIsAdvanced_thenTimePassesByExactlyThatMuch() {
        val clock = FakeTimeProvider(initial = EpochMillis(NOW))

        clock.advanceBy(milliseconds = 1_500)

        assertThat(clock.now()).isEqualTo(EpochMillis(NOW + 1_500))
    }

    @Test
    fun givenAFakeClock_whenItIsWoundBackwards_thenItRefuses() {
        // Time not going backwards is an invariant of EpochMillis's users, not just of this fake.
        // A checkpoint written before the one it replaces would be a resume that loses progress.
        val clock = FakeTimeProvider(initial = EpochMillis(NOW))

        val wound = runCatching { clock.advanceBy(milliseconds = -1) }

        assertThat(wound.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun givenAnIdProvider_whenIdsAreTaken_thenTheyAreSequentialAndPredictable() {
        val sessions = SequentialIdProvider(prefix = "s") { SessionId(it) }

        assertThat(sessions.next()).isEqualTo(SessionId("s1"))
        assertThat(sessions.next()).isEqualTo(SessionId("s2"))
        assertThat(sessions.next()).isEqualTo(SessionId("s3"))
    }

    @Test
    fun givenTwoIdProviders_whenBothAreUsed_thenNeitherAffectsTheOther() {
        val sessions = SequentialIdProvider(prefix = "s") { SessionId(it) }
        val profiles = SequentialIdProvider(prefix = "p") { ProfileId(it) }

        sessions.next()
        sessions.next()

        assertThat(profiles.next()).isEqualTo(ProfileId("p1"))
    }

    @Test
    fun givenAFakeRepository_whenNothingIsArranged_thenItSucceedsWithNothingInIt() {
        // Empty success is the default because it is the state a test most often does not care
        // about. A fake that failed by default would make every test arrange the boring case.
        val profiles = FakeProfileRepository()

        val read = runBlocking { profiles.observeProfiles().first() }

        assertThat(read).isEqualTo(DomainResult.Success(emptyList<Nothing>()))
    }

    @Test
    fun givenAFakeRepository_whenOneFailureIsQueued_thenOnlyTheNextCallFails() {
        // One-shot, so a test can prove that a retry after a failure succeeds. A fake that stayed
        // broken could not express the recovery path, which is the path most worth testing.
        val profiles = FakeProfileRepository()
        profiles.failNext(DomainError.PersistenceUnavailable)

        val first = runBlocking { profiles.create(builders.createProfile()) }
        val second = runBlocking { profiles.create(builders.createProfile()) }

        assertThat((first as DomainResult.Failure).error)
            .isEqualTo(DomainError.PersistenceUnavailable)
        assertThat(second).isInstanceOf(DomainResult.Success::class.java)
    }

    @Test
    fun givenAFakeRepository_whenCommandsArrive_thenItRecordsThemInOrder() {
        val profiles = FakeProfileRepository()

        runBlocking {
            profiles.create(builders.createProfile(nickname = "Minh"))
            profiles.create(builders.createProfile(nickname = "Lan"))
        }

        assertThat(profiles.created.map { it.nickname }).containsExactly("Minh", "Lan").inOrder()
    }

    @Test
    fun givenARecordedCommandList_whenTheFakeIsUsedAgain_thenTheSnapshotDoesNotChange() {
        // Handing back the live list would let a caller's earlier assertion quietly become true
        // later, which is the kind of test that passes and proves nothing.
        val profiles = FakeProfileRepository()
        runBlocking { profiles.create(builders.createProfile()) }

        val snapshot = profiles.created
        runBlocking { profiles.create(builders.createProfile()) }

        assertThat(snapshot).hasSize(1)
    }

    @Test
    fun givenAFakeRepository_whenItIsGivenProfiles_thenEveryCollectorSeesTheCurrentValue() {
        // Replayable: a collector that arrives after the value was set still sees it. A plain
        // channel would leave a late collector waiting forever, which reads as a hung test.
        val profiles = FakeProfileRepository()
        val minh = builders.childProfile(nickname = "Minh")

        profiles.setProfiles(listOf(minh))

        val late = runBlocking { profiles.observeProfiles().first() }
        assertThat((late as DomainResult.Success).value).containsExactly(minh)
    }

    @Test
    fun givenAProgressFake_whenACheckpointIsPersisted_thenItConfirmsWithTheClockItWasGiven() {
        val clock = FakeTimeProvider(initial = EpochMillis(NOW))
        val progress = FakeProgressRepository(timeProvider = clock)

        clock.advanceBy(milliseconds = 2_000)
        val confirmed = runBlocking { progress.persistCheckpoint(builders.persistCheckpoint()) }

        assertThat((confirmed as DomainResult.Success).value.confirmedAt)
            .isEqualTo(EpochMillis(NOW + 2_000))
    }

    @Test
    fun givenTheBuilders_whenNothingIsOverridden_thenWhatComesOutIsValid() {
        // Every builder default has to satisfy the model's own invariants, or the builders become
        // a second place where a model's rules are written down and can drift.
        val lesson = builders.lesson()

        assertThat(lesson.activities).isNotEmpty()
        assertThat(builders.childProfile().nickname).isNotEmpty()
        assertThat(builders.course().units).isNotEmpty()
        assertThat(builders.skillProgress().supportedSuccesses)
            .isAtMost(builders.skillProgress().exposures)
    }

    @Test
    fun givenABuilder_whenOneThingIsOverridden_thenOnlyThatThingChanges() {
        val quiet = builders.appSettings(captionsEnabled = false)

        assertThat(quiet.captionsEnabled).isFalse()
        assertThat(quiet.vietnameseHelpEnabled)
            .isEqualTo(builders.appSettings().vietnameseHelpEnabled)
    }

    @Test
    fun givenAnAttemptBuilder_whenAnOutcomeIsAsked_thenItIsTheOneRequested() {
        val retried = builders.activityAttempt(outcome = AttemptOutcome.SUPPORTIVE_RETRY)

        assertThat(retried.outcome).isEqualTo(AttemptOutcome.SUPPORTIVE_RETRY)
    }

    private val builders = DomainBuilders

    private companion object {
        const val NOW = 1_756_000_000_000
    }
}
