package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
import com.nphkhiem.englishforyourchildren.domain.model.AvatarId
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.testsupport.DomainBuilders
import com.nphkhiem.englishforyourchildren.testsupport.FakeProfileRepository
import com.nphkhiem.englishforyourchildren.testsupport.FakeProgressRepository
import com.nphkhiem.englishforyourchildren.testsupport.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * The children this television knows, and which of them it is currently on.
 *
 * Selection is the part with teeth. It is stored, so it can name a child who has since been
 * deleted, and a caregiver acting on a stale selection would be acting on somebody else's profile.
 */
class ProfileManagementViewModelTest {
    private lateinit var profiles: FakeProfileRepository
    private lateinit var progress: FakeProgressRepository
    private lateinit var settings: FakeSettingsRepository

    @AfterEach
    fun releaseMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenTheChildrenOnThisTelevision_whenTheScreenOpens_thenAllOfThemAreListed() =
        management { model ->
            assertThat(model.state.value.profiles.map { it.nickname })
                .containsExactly(FIRST_NAME, SECOND_NAME)
                .inOrder()
        }

    @Test
    fun givenAStoredSelection_whenTheScreenOpens_thenThatIsTheChildItIsAbout() =
        management { model ->
            assertThat(model.state.value.selectedId).isEqualTo(ProfileId(SECOND))
        }

    @Test
    fun givenACaregiverChoosesAnother_whenTheyDo_thenTheTelevisionRemembersIt() =
        management { model ->
            model.onSelect(ProfileId(FIRST))

            assertThat(stored().selectedProfileId).isEqualTo(ProfileId(FIRST))
        }

    @Test
    fun givenStorageRefuses_whenAChildIsChosen_thenItSaysSoRatherThanLookingChanged() =
        management { model ->
            settings.failNext(DomainError.PersistenceUnavailable)

            model.onSelect(ProfileId(FIRST))

            assertThat(model.state.value.persistenceFailed).isTrue()
        }

    @Test
    fun givenASelectionNamingAChildWhoIsGone_whenItIsRead_thenItIsReportedRatherThanQuietlyFixed() =
        management(selected = ProfileId(VANISHED)) { model ->
            // The brief's stale-selection state. This reports what is stored, including a name that
            // is no longer on the list, because `selectedProfile` resolves it where the screen is
            // drawn. Correcting it here would hide from the caregiver that it had ever been wrong,
            // and would write a correction nobody asked for.
            assertThat(model.state.value.profiles.map { it.id.value }).doesNotContain(VANISHED)
            assertThat(model.state.value.selectedId).isEqualTo(ProfileId(VANISHED))
        }

    @Test
    fun givenProfilesCannotBeRead_whenTheScreenOpens_thenItSaysSoRatherThanShowingNoChildren() =
        management { model ->
            profiles.setReadFailure(DomainError.PersistenceUnavailable)

            // An empty list would read as a television with no children on it, which is a very
            // different thing from one that cannot open its own storage.
            assertThat(model.state.value.persistenceFailed).isTrue()
        }

    private fun stored(): AppSettings =
        (settings.current as DomainResult.Success<AppSettings>).value

    @Test
    fun givenEachChildsPractice_whenTheScreenOpens_thenTheirOwnCountIsBesideTheirOwnName() =
        management { model ->
            // One subscription per child rather than one for the television. A single count shared
            // between profiles would tell a caregiver that the child who has done nothing has done
            // as much as the child who has done two.
            assertThat(model.state.value.adventuresFinished[ProfileId(FIRST)]).isEqualTo(2)
        }

    @Test
    fun givenAChildWhoHasFinishedNothing_whenTheScreenOpens_thenTheirCountIsZeroNotMissing() =
        management { model ->
            // Zero is a fact this screen knows; what to do about a zero is the host's decision, and
            // it draws the age alone. A missing count here would be indistinguishable from storage
            // that would not read.
            assertThat(model.state.value.adventuresFinished[ProfileId(SECOND)]).isEqualTo(0)
        }

    private fun management(
        selected: ProfileId? = ProfileId(SECOND),
        body: suspend TestScope.(ProfileManagementViewModel) -> Unit
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        profiles = FakeProfileRepository()
        profiles.setProfiles(
            listOf(
                DomainBuilders.childProfile(id = ProfileId(FIRST), nickname = FIRST_NAME),
                DomainBuilders.childProfile(
                    id = ProfileId(SECOND),
                    nickname = SECOND_NAME,
                    avatarId = AvatarId("bear")
                )
            )
        )
        progress = FakeProgressRepository()
        progress.setProgressFor(
            ProfileId(FIRST),
            DomainBuilders.profileProgress(
                profileId = ProfileId(FIRST),
                lessonsCompleted = setOf(LessonId("u01-my-body-l1"), LessonId("u01-my-body-l2"))
            )
        )
        progress.setProgressFor(
            ProfileId(SECOND),
            DomainBuilders.profileProgress(profileId = ProfileId(SECOND))
        )
        settings = FakeSettingsRepository(AppSettings.DEFAULT.copy(selectedProfileId = selected))

        val store = ViewModelStore()
        val model = ViewModelProvider(
            store,
            viewModelFactory {
                initializer { ProfileManagementViewModel(profiles, progress, settings) }
            }
        )[ProfileManagementViewModel::class.java]

        try {
            body(model)
        } finally {
            store.clear()
        }
    }

    private companion object {
        const val FIRST = "p1"
        const val SECOND = "p2"
        const val VANISHED = "p9"
        const val FIRST_NAME = "Minh"
        const val SECOND_NAME = "Lan"
    }
}
