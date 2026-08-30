package com.nphkhiem.englishforyourchildren.feature.profiles

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AgeBand
import com.nphkhiem.englishforyourchildren.domain.model.AvatarId
import com.nphkhiem.englishforyourchildren.domain.model.MAX_CHILD_PROFILES
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.testsupport.DomainBuilders
import com.nphkhiem.englishforyourchildren.testsupport.FakeProfileRepository
import com.nphkhiem.englishforyourchildren.testsupport.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProfileViewModelTest {
    // viewModelScope runs on the main dispatcher, which a JVM test does not have. Unconfined so
    // that the collectors started in init have run by the time a test looks at the state.
    @BeforeEach
    fun useTestDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restoreDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenStorageIsStillAnswering_whenTheStateIsFirstRead_thenThePickerSaysItIsLoading() {
        // The picker was built with a loading state in HB-D10 and has shown a fixture ever since.
        // This is the first time there is anything real to wait for.
        //
        // A queuing dispatcher, not the unconfined one the other tests use: unconfined runs the
        // collectors the moment the model is built, so the loading moment exists but cannot be
        // seen. Room is slower than that, and a picker that flashed empty before its children
        // arrived would be a child looking for a face that is not there yet.
        Dispatchers.setMain(StandardTestDispatcher())

        val state = viewModel().picker.value

        assertThat(state.loading).isTrue()
        assertThat(state.profiles).isEmpty()
    }

    @Test
    fun givenChildrenAreStored_whenTheyArrive_thenThePickerShowsThemAndStopsLoading() {
        val profiles = FakeProfileRepository()
        profiles.setProfiles(listOf(DomainBuilders.childProfile(nickname = "Minh")))
        val model = viewModel(profiles)

        val state = runBlocking { model.picker.first { !it.loading } }

        assertThat(state.profiles.map { it.nickname }).containsExactly("Minh")
    }

    @Test
    fun givenStorageCannotBeRead_whenProfilesArrive_thenItIsReportedSeparatelyFromBeingEmpty() {
        // An empty picker invites a caregiver to add a child, and a television that cannot read the
        // children it already has must not do that. The picker itself is not told: the host routes
        // to the caregiver recovery that HB-D22 built for exactly this.
        val profiles = FakeProfileRepository()
        profiles.setReadFailure(DomainError.PersistenceUnavailable)
        val model = viewModel(profiles)

        val unreadable = runBlocking { model.storageUnreadable.first { it } }

        assertThat(unreadable).isTrue()
        assertThat(model.picker.value.profiles).isEmpty()
    }

    @Test
    fun givenAChildIsChosen_whenTheActionArrives_thenTheChoiceIsRemembered() {
        // So the next launch opens on that child rather than asking again.
        val settings = FakeSettingsRepository()
        val model = viewModel(settings = settings)

        runBlocking { model.onPickerAction(ProfileAction.ProfileChosen(profileId = "p1")) }

        val stored = (settings.current as DomainResult.Success).value
        assertThat(stored.selectedProfileId).isEqualTo(ProfileId("p1"))
    }

    @Test
    fun givenADraft_whenItIsSaved_thenAChildIsCreatedFromExactlyWhatWasTyped() {
        val profiles = FakeProfileRepository()
        val model = viewModel(profiles)

        runBlocking {
            model.onCreateAction(
                CreateProfileAction.CreateRequested(
                    draft = ProfileDraft(nickname = "Lan", avatarId = "rabbit", age = 4)
                )
            )
        }

        val created = profiles.created.single()
        assertThat(created.nickname).isEqualTo("Lan")
        assertThat(created.avatarId).isEqualTo(AvatarId("rabbit"))
        assertThat(created.ageBand).isEqualTo(AgeBand.FOUR)
    }

    @Test
    fun givenAChildIsWritten_whenTheWriteReports_thenTheirIdIsSaidRatherThanGuessedAt() {
        // The host opens a home for this id. Working it out by diffing the picker before and after
        // would race the emission that has not arrived yet.
        val profiles = FakeProfileRepository()
        val model = viewModel(profiles)

        runBlocking { model.onCreateAction(CreateProfileAction.CreateRequested(draft())) }

        assertThat(model.lastCreated.value).isEqualTo(
            profiles.created.single().let {
                ProfileId("p1")
            }
        )
    }

    @Test
    fun givenAWriteIsRefused_whenItReports_thenNoChildIsAnnouncedAsCreated() {
        val profiles = FakeProfileRepository()
        profiles.failNext(DomainError.ProfileLimitReached)
        val model = viewModel(profiles)

        runBlocking { model.onCreateAction(CreateProfileAction.CreateRequested(draft())) }

        assertThat(model.lastCreated.value).isNull()
    }

    @Test
    fun givenNoAgeWasChosen_whenTheDraftIsSaved_thenNothingIsWritten() {
        // Age is nullable because "not chosen yet" is a real state, and a child written without one
        // would have an age band this model had to invent.
        val profiles = FakeProfileRepository()
        val model = viewModel(profiles)

        runBlocking {
            model.onCreateAction(
                CreateProfileAction.CreateRequested(
                    draft = ProfileDraft(nickname = "Lan", avatarId = "rabbit", age = null)
                )
            )
        }

        assertThat(profiles.created).isEmpty()
    }

    @Test
    fun givenTheTelevisionIsFull_whenAnotherChildIsSaved_thenTheLimitIsSaidRatherThanSwallowed() {
        val profiles = FakeProfileRepository()
        profiles.failNext(DomainError.ProfileLimitReached)
        val model = viewModel(profiles)

        runBlocking { model.onCreateAction(CreateProfileAction.CreateRequested(draft())) }

        assertThat(model.create.value.capacityReached).isTrue()
        assertThat(model.create.value.saveFailed).isFalse()
    }

    @Test
    fun givenAWriteFails_whenAChildIsSaved_thenTheScreenIsToldItFailedRatherThanNothing() {
        // The create screen has a save-failed state from HB-D11. Leaving it unset would show a
        // caregiver a form that simply did nothing when they pressed the button.
        val profiles = FakeProfileRepository()
        profiles.failNext(DomainError.PersistenceUnavailable)
        val model = viewModel(profiles)

        runBlocking { model.onCreateAction(CreateProfileAction.CreateRequested(draft())) }

        assertThat(model.create.value.saveFailed).isTrue()
        assertThat(model.create.value.capacityReached).isFalse()
    }

    @Test
    fun givenAFailedSave_whenTheCaregiverEditsTheDraftAgain_thenTheFailureIsCleared() {
        // A stale failure sitting under a form the caregiver has since changed would tell them
        // something that is no longer true.
        val profiles = FakeProfileRepository()
        profiles.failNext(DomainError.PersistenceUnavailable)
        val model = viewModel(profiles)
        runBlocking { model.onCreateAction(CreateProfileAction.CreateRequested(draft())) }

        runBlocking { model.onCreateAction(CreateProfileAction.ChangeNameRequested) }

        assertThat(model.create.value.saveFailed).isFalse()
    }

    @Test
    fun givenAFreshForm_whenItIsFirstSeen_thenItAlreadyHasANameToShow() {
        // The screen renders a draft and does not invent one. An empty nickname reached the
        // television and drew the avatar id as the child's name, in letters a foot high.
        val draft = viewModel().create.value.draft

        assertThat(draft.nickname).isNotEmpty()
        assertThat(draft.avatarId).isNotEmpty()
    }

    @Test
    fun givenAnAgeIsPressed_whenTheActionArrives_thenTheDraftCarriesIt() {
        // Without this the create button stays saying "select age, then create" forever, however
        // many times a caregiver presses three.
        val model = viewModel()

        runBlocking { model.onCreateAction(CreateProfileAction.AgeChosen(age = 5)) }

        assertThat(model.create.value.draft.age).isEqualTo(5)
    }

    @Test
    fun givenTheNameIsChanged_whenItIsAskedForAgain_thenItIsADifferentOne() {
        // A remote is a poor keyboard, so the name is chosen by cycling rather than typing.
        val model = viewModel()
        val first = model.create.value.draft.nickname

        runBlocking { model.onCreateAction(CreateProfileAction.ChangeNameRequested) }

        assertThat(model.create.value.draft.nickname).isNotEqualTo(first)
    }

    @Test
    fun givenTheNameIsChangedPastTheEnd_whenItWrapsAround_thenItKeepsWorking() {
        val model = viewModel()

        runBlocking { repeat(20) { model.onCreateAction(CreateProfileAction.ChangeNameRequested) } }

        assertThat(model.create.value.draft.nickname).isNotEmpty()
    }

    @Test
    fun givenTheLimit_whenItIsAsked_thenItIsTheOneTheDomainHolds() {
        assertThat(MAX_CHILD_PROFILES).isEqualTo(4)
    }

    private fun draft() = ProfileDraft(nickname = "Lan", avatarId = "rabbit", age = 4)

    private fun viewModel(
        profiles: FakeProfileRepository = FakeProfileRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository()
    ) = ProfileViewModel(profiles = profiles, settings = settings)
}
