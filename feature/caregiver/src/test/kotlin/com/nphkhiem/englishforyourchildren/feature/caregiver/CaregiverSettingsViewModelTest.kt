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
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var lessons: FakeLessonUpdates

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

    @Test
    fun givenABuildWithNowhereToGetLessons_whenSettingsOpen_thenThereIsNoLessonsRowAndNoCheck() =
        settings { model ->
            assertThat(model.state.value.lessons).isNull()
            assertThat(lessons.calls).isEmpty()
        }

    @Test
    fun givenLessonsCanBeFetched_whenSettingsOpen_thenItLooksOnceAndShowsWhatItFound() =
        settings(lessonsOffered = true) { model ->
            lessons.state.value = TaskState.Available

            assertThat(lessons.calls).containsExactly(CHECK)
            assertThat(model.state.value.lessons).isEqualTo(TaskState.Available)
        }

    @Test
    fun givenNewLessonsWaiting_whenTheRowIsPressed_thenTheyAreDownloaded() =
        settings(lessonsOffered = true) { model ->
            lessons.state.value = TaskState.Available

            model.onTaskRequested()

            assertThat(lessons.calls).containsExactly(CHECK, DOWNLOAD).inOrder()
        }

    @Test
    fun givenUpToDateOrFailed_whenTheRowIsPressed_thenItLooksAgain() =
        settings(lessonsOffered = true) { model ->
            model.onTaskRequested()
            lessons.state.value = TaskState.Failed(TaskFailure.COULD_NOT_FINISH)
            model.onTaskRequested()

            assertThat(lessons.calls).containsExactly(CHECK, CHECK, CHECK).inOrder()
        }

    @Test
    fun givenADownloadUnderway_whenTheRowIsPressed_thenNothingElseStarts() =
        settings(lessonsOffered = true) { model ->
            lessons.state.value = TaskState.Working(done = 1, total = 3)

            model.onTaskRequested()

            assertThat(lessons.calls).containsExactly(CHECK)
        }

    private fun stored(): AppSettings =
        (repository.current as DomainResult.Success<AppSettings>).value

    /** A settings screen on a controlled clock, cleared however the body ends. */
    private fun settings(
        initial: AppSettings = AppSettings.DEFAULT,
        lessonsOffered: Boolean = false,
        body: suspend TestScope.(CaregiverSettingsViewModel) -> Unit
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        repository = FakeSettingsRepository(initial)
        lessons = FakeLessonUpdates(lessonsOffered)
        val store = ViewModelStore()
        val model = ViewModelProvider(
            store,
            viewModelFactory { initializer { CaregiverSettingsViewModel(repository, lessons) } }
        )[CaregiverSettingsViewModel::class.java]

        try {
            body(model)
        } finally {
            store.clear()
        }
    }

    /** Records what the screen asked for, and reports whatever state a test sets. */
    private class FakeLessonUpdates(override val offered: Boolean) : LessonUpdates {
        override val state = MutableStateFlow<TaskState>(TaskState.UpToDate)
        val calls = mutableListOf<String>()

        override suspend fun check() {
            calls += CHECK
        }

        override suspend fun download() {
            calls += DOWNLOAD
        }
    }

    private companion object {
        const val CHILD = "p1"
        const val CHECK = "check"
        const val DOWNLOAD = "download"
    }
}
