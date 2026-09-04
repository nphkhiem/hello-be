package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.MainActivity
import com.nphkhiem.englishforyourchildren.data.di.DataProvidersModule
import com.nphkhiem.englishforyourchildren.data.local.HelloBeDatabase
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileDao
import com.nphkhiem.englishforyourchildren.data.progress.ProgressDao
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.feature.caregiver.GateArithmetic
import com.nphkhiem.englishforyourchildren.feature.caregiver.GateChallenges
import com.nphkhiem.englishforyourchildren.feature.caregiver.di.CaregiverModule
import com.nphkhiem.englishforyourchildren.playback.ImageAssetLocator
import com.nphkhiem.englishforyourchildren.playback.MediaAssetLocator
import com.nphkhiem.englishforyourchildren.playback.PlaybackController
import com.nphkhiem.englishforyourchildren.playback.di.PlaybackModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The door, from a child's home screen to a setting that held.
 *
 * The gate's arithmetic is scripted here rather than random, which is the reason `GateChallenges`
 * is injected at all: a journey that had to solve whatever sum a generator produced could not press
 * a known button, and one that read the answer off the screen would be asserting that the screen
 * agrees with itself.
 */
@HiltAndroidTest
@UninstallModules(
    DataProvidersModule::class,
    PlaybackModule::class,
    CaregiverModule::class
)
class AdultGateJourneyTest {

    @Module
    @InstallIn(SingletonComponent::class)
    object KnownChallenge {
        /** One sum, forever, so the right button is a fact this test can name. */
        @Provides
        @Singleton
        fun challenges(): GateChallenges = GateChallenges {
            GateArithmetic(
                left = LEFT,
                right = RIGHT,
                answers = listOf(WRONG_LOW, CORRECT, WRONG_HIGH),
                correctIndex = 1
            )
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object SilentMedia {
        @Provides
        @Singleton
        fun audio(): MediaAssetLocator = TestMedia.silentAudio()

        @Provides
        @Singleton
        fun images(): ImageAssetLocator = TestMedia.noPictures()

        @Provides
        @Singleton
        fun controller(
            @ApplicationContext context: Context,
            locator: MediaAssetLocator
        ): PlaybackController = TestMedia.controller(context, locator)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object Storage {
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): HelloBeDatabase =
            TestStorage.database(context)

        @Provides
        fun profiles(database: HelloBeDatabase): ChildProfileDao = database.childProfileDao()

        @Provides
        fun progress(database: HelloBeDatabase): ProgressDao = database.progressDao()

        @Provides
        @Singleton
        fun assets(@ApplicationContext context: Context): AssetManager = context.assets

        @Provides
        @Singleton
        fun settings(@ApplicationContext context: Context): DataStore<Preferences> =
            TestStorage.settings(context)
    }

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var settings: SettingsRepository

    private val tv by lazy { JourneyDriver(compose) }

    @Before
    fun injectGraph() {
        hilt.inject()
    }

    @Test
    fun givenAGrownUp_whenTheySolveTheGate_thenASettingTheyChangeIsKept() {
        createChild()

        tv.press(tv.string("home_grown_ups"))
        tv.awaitText(CORRECT.toString())
        tv.press(CORRECT.toString())

        // Settings, not the overview: see the comment beside the wiring in `HelloBeNavHost`.
        tv.awaitText(tv.string("settings_captions_title"))
        val before = storedCaptions()
        tv.press(tv.string("settings_captions_title"))

        compose.waitUntil(TIMEOUT_MILLIS) { storedCaptions() != before }
        assertThat(storedCaptions()).isNotEqualTo(before)
    }

    @Test
    fun givenAChildPressingAtIt_whenTheyChooseWithoutMoving_thenTheDoorStaysShut() {
        // Entry focus rests on a wrong answer, so Select without moving is a wrong answer by
        // construction. This is the whole of the protection, walked rather than reasoned about.
        createChild()
        tv.press(tv.string("home_grown_ups"))
        tv.awaitText(CORRECT.toString())

        tv.pressWhateverIsFocused()

        // Still at the gate. A settings row would mean the press opened it.
        tv.awaitText(tv.string("gate_instruction"))
    }

    private fun storedCaptions(): Boolean = runBlocking {
        val read = settings.observeSettings().first()
        (read as? DomainResult.Success)?.value?.captionsEnabled ?: error("settings unreadable")
    }

    private fun createChild() {
        tv.press(AGE)
        tv.press(tv.string("create_submit"))
        tv.awaitText(tv.string("home_start"))
    }

    private companion object {
        const val AGE = "3"
        const val LEFT = 34
        const val RIGHT = 27
        const val CORRECT = LEFT + RIGHT
        const val WRONG_LOW = CORRECT - 2
        const val WRONG_HIGH = CORRECT + 3
        const val TIMEOUT_MILLIS = 5_000L
    }
}
