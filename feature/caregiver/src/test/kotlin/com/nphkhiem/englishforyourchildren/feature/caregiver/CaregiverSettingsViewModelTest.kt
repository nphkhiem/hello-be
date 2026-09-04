package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
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
 * What a caregiver changes, and whether it held.
 *
 * The rule these all serve: nothing here may claim a change was saved when storage refused it. A
 * caregiver who turns captions on for a child who needs them has to be able to believe the screen.
 */
class CaregiverSettingsViewModelTest {
    private lateinit var repository: FakeSettingsRepository

    @AfterEach
    fun releaseMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenASwitch_whenItIsTurned_thenStorageHasItAndTheScreenShowsIt() = settings { model ->
        model.onToggle(SettingId.CAPTIONS)

        assertThat(stored().captionsEnabled).isFalse()
        assertThat(model.state.value.settings.captionsEnabled).isFalse()
        assertThat(model.state.value.saveStatus).isEqualTo(SettingsSaveStatus.Idle)
    }

    @Test
    fun givenStorageRefuses_whenASwitchIsTurned_thenItSaysSoRatherThanClaimingItSaved() =
        settings { model ->
            repository.failNext(DomainError.PersistenceUnavailable)

            model.onToggle(SettingId.CAPTIONS)

            assertThat(model.state.value.saveStatus).isEqualTo(SettingsSaveStatus.Failed)
            assertThat(stored().captionsEnabled).isTrue()
        }

    @Test
    fun givenAChosenLanguage_whenItIsSet_thenStorageHoldsThatModeAndNotATag() = settings { model ->
        model.onLanguageChosen(CaregiverLanguage.VIETNAMESE)

        assertThat(stored().caregiverLanguage).isEqualTo(CaregiverLanguage.VIETNAMESE)
    }

    @Test
    fun givenTheLanguageRow_whenItIsToggled_thenNothingHappensBecauseItIsNotASwitch() =
        settings { model ->
            // Three modes cannot be turned on and off. Refusing here rather than picking one for
            // the caregiver is what keeps a stray press from changing the language they read.
            model.onToggle(SettingId.CAREGIVER_LANGUAGE)

            assertThat(stored().caregiverLanguage).isEqualTo(AppSettings.DEFAULT.caregiverLanguage)
            assertThat(model.state.value.saveStatus).isEqualTo(SettingsSaveStatus.Idle)
        }

    @Test
    fun givenNothingHasBeenChanged_whenRestoreIsConsidered_thenItIsNotOffered() =
        settings { model ->
            // "Available only when meaningful". An always-live restore button invites a caregiver to
            // press something that does nothing.
            assertThat(model.state.value.canRestoreDefaults).isFalse()
        }

    @Test
    fun givenSomethingHasBeenChanged_whenRestoreIsConsidered_thenItIsOffered() = settings { model ->
        model.onToggle(SettingId.CAPTIONS)

        assertThat(model.state.value.canRestoreDefaults).isTrue()
    }

    @Test
    fun givenChangedSettings_whenDefaultsAreRestored_thenEachGoesBack() = settings { model ->
        model.onToggle(SettingId.CAPTIONS)
        model.onToggle(SettingId.HIGH_CONTRAST)
        model.onLanguageChosen(CaregiverLanguage.ENGLISH)

        model.onRestoreDefaults()

        assertThat(stored().captionsEnabled).isEqualTo(AppSettings.DEFAULT.captionsEnabled)
        assertThat(stored().highContrastEnabled).isEqualTo(AppSettings.DEFAULT.highContrastEnabled)
        assertThat(stored().caregiverLanguage).isEqualTo(AppSettings.DEFAULT.caregiverLanguage)
        assertThat(model.state.value.canRestoreDefaults).isFalse()
    }

    @Test
    fun givenAChildIsSelected_whenDefaultsAreRestored_thenTheyAreStillTheChild() =
        settings(AppSettings.DEFAULT.copy(selectedProfileId = ProfileId(CHILD))) { model ->
            // Restoring settings must not switch the child out. Which profile the television is on
            // is not one of the six things this screen offers, and a caregiver tidying up their
            // captions would not expect to be asked who is learning today.
            model.onToggle(SettingId.CAPTIONS)

            model.onRestoreDefaults()

            assertThat(stored().selectedProfileId).isEqualTo(ProfileId(CHILD))
        }

    private fun stored(): AppSettings =
        (repository.current as DomainResult.Success<AppSettings>).value

    /** A settings screen on a controlled clock, cleared however the body ends. */
    private fun settings(
        initial: AppSettings = AppSettings.DEFAULT,
        body: suspend TestScope.(CaregiverSettingsViewModel) -> Unit
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        repository = FakeSettingsRepository(initial)
        val store = ViewModelStore()
        val model = ViewModelProvider(
            store,
            viewModelFactory { initializer { CaregiverSettingsViewModel(repository) } }
        )[CaregiverSettingsViewModel::class.java]

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
