package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.nphkhiem.englishforyourchildren.MainActivity
import com.nphkhiem.englishforyourchildren.data.di.DataProvidersModule
import com.nphkhiem.englishforyourchildren.data.local.HelloBeDatabase
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileDao
import com.nphkhiem.englishforyourchildren.data.progress.ProgressDao
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
import javax.inject.Singleton
import org.junit.Rule
import org.junit.Test

/**
 * Leaving a lesson part way through, and coming back to it.
 *
 * This is the promise the stop-for-now dialog makes out loud: "Pip will remember your last finished
 * activity." A journey is the only place that promise can be checked the way a child meets it,
 * because keeping it takes storage, the reducer and two screens agreeing.
 */
@HiltAndroidTest
@UninstallModules(DataProvidersModule::class, PlaybackModule::class)
class LessonRecoveryJourneyTest {

    /**
     * No recording plays, which is this journey's premise rather than its accident.
     *
     * See [TestMedia]. Unit one ships recordings now, so a journey about missing media has to say
     * so out loud.
     */
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

    private val tv by lazy { JourneyDriver(compose) }

    @Test
    fun givenAChildStopsPartWayThrough_whenTheyComeBack_thenTheyAreWhereTheyLeftOff() {
        openFirstLesson()

        // One question finished, so the next one is where this child now is.
        tv.press(tv.string("lesson_skip"))
        tv.awaitText(SECOND_QUESTION)

        stopForNow()

        // Back through the dominant control, which is where a child who stopped actually returns
        // from. Continue is the acceptance criterion this journey is really about: it has to land
        // on the checkpoint rather than at the top of the lesson.
        tv.press(tv.string("home_continue"))

        // Not "Where are the eyes?". Coming back to the first question is the thing the dialog
        // promises will not happen.
        tv.awaitText(SECOND_QUESTION)
    }

    @Test
    fun givenTheStopQuestionIsOpen_whenTheChildKeepsLearning_thenTheLessonIsStillThere() {
        openFirstLesson()

        pressBack()
        tv.awaitText(tv.string("stop_for_now_title"))
        tv.press(tv.string("stop_for_now_keep"))

        tv.awaitText(FIRST_QUESTION)
    }

    private fun openFirstLesson() {
        tv.press(AGE)
        tv.press(tv.string("create_submit"))
        tv.awaitText(tv.string("home_start"))
        // One press, because that is what the dominant control does now: it opens the lesson the
        // progression policy names rather than the page that lists them.
        tv.press(tv.string("home_start"))
        tv.awaitText(FIRST_QUESTION)
    }

    private fun stopForNow() {
        pressBack()
        tv.awaitText(tv.string("stop_for_now_title"))
        tv.press(tv.string("stop_for_now_stop"))
        // Home, because that is where the lesson was opened from now that one press opens it.
        tv.awaitText(tv.string("home_continue"))
    }

    /** The real Back, through the dispatcher the lesson's BackHandler listens to. */
    private fun pressBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private companion object {
        const val AGE = "3"
        const val FIRST_LESSON = "Lesson 1"
        const val FIRST_QUESTION = "Where are the eyes?"
        const val SECOND_QUESTION = "Where is the nose?"
    }
}
