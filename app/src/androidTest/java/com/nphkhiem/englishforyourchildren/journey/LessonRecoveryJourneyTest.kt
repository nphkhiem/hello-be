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
@UninstallModules(DataProvidersModule::class)
class LessonRecoveryJourneyTest {

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

        tv.press(FIRST_LESSON)

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
        tv.press(tv.string("home_start"))
        tv.awaitText(FIRST_LESSON)
        tv.press(FIRST_LESSON)
        tv.awaitText(FIRST_QUESTION)
    }

    private fun stopForNow() {
        pressBack()
        tv.awaitText(tv.string("stop_for_now_title"))
        tv.press(tv.string("stop_for_now_stop"))
        tv.awaitText(FIRST_LESSON)
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
