package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
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
 * The two things a caregiver cannot undo.
 *
 * Deleting removes a child from this television. Resetting keeps the child and restarts what they
 * have learned. They are not degrees of the same act, and the rule that matters most here is what
 * each one leaves behind.
 */
class CaregiverConfirmationViewModelTest {
    private lateinit var profiles: FakeProfileRepository
    private lateinit var progress: FakeProgressRepository
    private lateinit var settings: FakeSettingsRepository

    @AfterEach
    fun releaseMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenADeletion_whenItIsConfirmed_thenTheChildAndTheirProgressBothGo() =
        confirming(CaregiverConfirmationKind.DELETE_PROFILE) { model ->
            model.onConfirm()

            assertThat(profiles.deleted).containsExactly(ProfileId(CHILD))
            assertThat(progress.deleted).containsExactly(ProfileId(CHILD))
        }

    @Test
    fun givenAReset_whenItIsConfirmed_thenTheProgressGoesAndTheChildStays() =
        confirming(CaregiverConfirmationKind.RESET_PROGRESS) { model ->
            // The whole difference between the two. A reset that removed the profile would be a
            // deletion a caregiver did not ask for.
            model.onConfirm()

            assertThat(progress.deleted).containsExactly(ProfileId(CHILD))
            assertThat(profiles.deleted).isEmpty()
        }

    @Test
    fun givenADeletionOfTheSelectedChild_whenItIsConfirmed_thenNothingIsStillSelectingThem() =
        confirming(CaregiverConfirmationKind.DELETE_PROFILE) { model ->
            // A preference naming a deleted child is what the brief calls the stale selection, and
            // this is the one place it can be prevented rather than resolved afterwards.
            model.onConfirm()

            assertThat(stored().selectedProfileId).isNull()
        }

    @Test
    fun givenAResetOfTheSelectedChild_whenItIsConfirmed_thenTheyAreStillTheChildOnThisTelevision() =
        confirming(CaregiverConfirmationKind.RESET_PROGRESS) { model ->
            model.onConfirm()

            assertThat(stored().selectedProfileId).isEqualTo(ProfileId(CHILD))
        }

    @Test
    fun givenTheWorkIsUnderway_whenItIsConfirmedAgain_thenItHappensOnce() =
        confirming(CaregiverConfirmationKind.DELETE_PROFILE) { model ->
            // A television remote sends two presses when a button is held a moment too long, and
            // this is the one screen where doing the thing twice cannot be taken back.
            model.onConfirm()
            model.onConfirm()

            assertThat(profiles.deleted).hasSize(1)
        }

    @Test
    fun givenStorageRefuses_whenItIsConfirmed_thenItSaysNothingChanged() =
        confirming(CaregiverConfirmationKind.DELETE_PROFILE) { model ->
            profiles.failNext(DomainError.PersistenceUnavailable)

            model.onConfirm()

            assertThat(model.state.value.phase).isEqualTo(CaregiverConfirmationPhase.FAILED)
            assertThat(model.state.value.done).isFalse()
        }

    @Test
    fun givenItFailed_whenItIsRetried_thenItIsTriedAgainRatherThanRefused() =
        confirming(CaregiverConfirmationKind.DELETE_PROFILE) { model ->
            profiles.failNext(DomainError.PersistenceUnavailable)
            model.onConfirm()

            model.onRetry()

            // The fake records attempts rather than successes, so two here is the refused one and
            // the one that worked. What says it worked is the state.
            assertThat(profiles.deleted).hasSize(2)
            assertThat(model.state.value.done).isTrue()
        }

    private fun stored(): AppSettings =
        (settings.current as DomainResult.Success<AppSettings>).value

    private fun confirming(
        kind: CaregiverConfirmationKind,
        body: suspend TestScope.(CaregiverConfirmationViewModel) -> Unit
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        profiles = FakeProfileRepository()
        profiles.setProfiles(listOf(DomainBuilders.childProfile(id = ProfileId(CHILD))))
        progress = FakeProgressRepository()
        settings = FakeSettingsRepository(
            AppSettings.DEFAULT.copy(selectedProfileId = ProfileId(CHILD))
        )

        val store = ViewModelStore()
        val model = ViewModelProvider(
            store,
            viewModelFactory {
                initializer { CaregiverConfirmationViewModel(profiles, progress, settings) }
            }
        )[CaregiverConfirmationViewModel::class.java]
        model.start(kind, ProfileId(CHILD))

        try {
            body(model)
        } finally {
            store.clear()
        }
    }

    private companion object {
        const val CHILD = "p1"
    }
}
