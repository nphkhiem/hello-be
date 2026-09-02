package com.nphkhiem.englishforyourchildren.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreSettingsRepositoryTest {
    private lateinit var file: File
    private lateinit var scope: CoroutineScope
    private lateinit var repository: DataStoreSettingsRepository

    @Before
    fun createStore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.cacheDir, "settings-test-${System.nanoTime()}.preferences_pb")
        scope = newScope()
        repository = DataStoreSettingsRepository(openStore(scope))
    }

    @After
    fun deleteStore() {
        scope.cancel()
        file.delete()
    }

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * DataStore refuses two live instances on one file, which is why the app provides it as a
     * singleton. A test that wants to reopen the file has to let go of the first one, and cancelling
     * its scope is how.
     */
    private fun openStore(
        scope: CoroutineScope,
        corruptionHandler:
        androidx.datastore.core.handlers.ReplaceFileCorruptionHandler<Preferences>? = null
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = corruptionHandler,
        scope = scope,
        produceFile = { file }
    )

    @Test
    fun givenNothingHasBeenChosen_whenSettingsAreRead_thenTheAccessibleDefaultsAreWhatComeBack() {
        // Captions and Vietnamese help start on. The cost of them being on for a child who does not
        // need them is small; the cost of being off for one who does is a lesson they cannot follow.
        val read = runBlocking { repository.observeSettings().first() }

        val settings = (read as DomainResult.Success).value
        assertThat(settings.captionsEnabled).isTrue()
        assertThat(settings.vietnameseHelpEnabled).isTrue()
        assertThat(settings.reducedMotionEnabled).isFalse()
        assertThat(settings.highContrastEnabled).isFalse()
    }

    @Test
    fun givenACaregiverChangesASetting_whenItIsReadBack_thenTheChoiceIsThere() {
        runBlocking {
            repository.updateCaptions(enabled = false)
            repository.updateHighContrast(enabled = true)
        }

        val settings = runBlocking { repository.observeSettings().first() }
            .let { (it as DomainResult.Success).value }

        assertThat(settings.captionsEnabled).isFalse()
        assertThat(settings.highContrastEnabled).isTrue()
    }

    @Test
    fun givenSettingsWereChosen_whenTheStoreIsOpenedAgain_thenTheySurvived() {
        // The point of writing them down at all. A caregiver should not have to set captions again
        // every time the television is switched off.
        runBlocking { repository.updateReducedMotion(enabled = true) }

        scope.cancel()
        val restarted = newScope()
        val reopened = DataStoreSettingsRepository(openStore(restarted))
        val settings = runBlocking { reopened.observeSettings().first() }
            .let { (it as DomainResult.Success).value }

        assertThat(settings.reducedMotionEnabled).isTrue()
    }

    @Test
    fun givenOneSettingWasNeverWritten_whenSettingsAreRead_thenTheOthersAreStillWhatWasChosen() {
        // Each absent value falls back on its own, not to a whole default object, so a store
        // written before a setting existed keeps every choice a caregiver did make.
        runBlocking { repository.updateCaregiverLanguage(localeTag = "en") }

        val settings = runBlocking { repository.observeSettings().first() }
            .let { (it as DomainResult.Success).value }

        assertThat(settings.caregiverLanguage).isEqualTo(CaregiverLanguage.ENGLISH)
        assertThat(settings.captionsEnabled).isEqualTo(AppSettings.DEFAULT.captionsEnabled)
    }

    @Test
    fun givenAProfileWasSelected_whenItIsClearedAgain_thenNothingIsSelected() {
        runBlocking {
            repository.updateSelectedProfile(ProfileId("p1"))
            repository.updateSelectedProfile(null)
        }

        val settings = runBlocking { repository.observeSettings().first() }
            .let { (it as DomainResult.Success).value }

        assertThat(settings.selectedProfileId).isNull()
    }

    @Test
    fun givenTheStoreIsCorrupt_whenSettingsAreRead_thenTheAccessibleDefaultsComeBack() {
        // A file that will not parse is replaced rather than reported, because settings are
        // recoverable preferences and the replacement lands on the side that keeps a child's
        // support switched on. An unreadable disk is a different case and is reported.
        scope.cancel()
        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        val restarted = newScope()
        val corrupted = DataStoreSettingsRepository(
            openStore(
                restarted,
                corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler {
                    androidx.datastore.preferences.core.emptyPreferences()
                }
            )
        )

        val settings = runBlocking { corrupted.observeSettings().first() }
            .let { (it as DomainResult.Success).value }

        assertThat(settings).isEqualTo(AppSettings.DEFAULT)
    }
}
