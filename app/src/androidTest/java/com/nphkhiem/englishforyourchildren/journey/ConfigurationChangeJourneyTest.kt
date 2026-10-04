package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * A television changing underneath a lesson.
 *
 * A gamepad paired, an HDMI switch to another resolution, a caregiver who went to Settings for
 * bigger text and came back: each is a configuration change, and by default each one rebuilt the
 * Activity. The host's stack is held in composition rather than saved, deliberately, so a rebuild
 * dropped the child out of their lesson onto the learning path. Their answers were safe; their
 * place was not.
 *
 * Driven by a real change to the system font scale rather than `ActivityScenario.recreate()`,
 * which rebuilds the Activity whatever it has declared it can handle and so could only ever show
 * the problem, never its absence.
 */
@HiltAndroidTest
@UninstallModules(DataProvidersModule::class)
class ConfigurationChangeJourneyTest {

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

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    /** The device's own setting, put back afterwards. Null when it had never been set. */
    private var originalFontScale: String? = null

    @Before
    fun injectGraph() {
        hilt.inject()
        originalFontScale =
            shell("settings get system font_scale").trim().takeUnless { it == "null" }
    }

    @After
    fun restoreFontScale() {
        shell(
            originalFontScale?.let { "settings put system font_scale $it" }
                ?: "settings delete system font_scale"
        )
    }

    @Test
    fun givenAChildMidLesson_whenTheTelevisionChangesConfiguration_thenTheyAreStillInIt() {
        tv.awaitText(AGE)
        tv.press(AGE)
        tv.press(tv.string("create_submit"))
        tv.awaitText(tv.string("path_lesson_start"))
        tv.press(FIRST_LESSON)
        tv.press(FIRST_ANSWER)
        tv.awaitText(SECOND_QUESTION)
        val before = currentActivity()

        changeFontScale()

        // The same Activity, and the same question. A rebuilt one starts from the entry
        // destination, which for a child with a profile is the learning path.
        assertThat(currentActivity()).isSameInstanceAs(before)
        tv.awaitText(SECOND_QUESTION)
        compose.onNodeWithText(FIRST_LESSON).assertDoesNotExist()
    }

    private fun changeFontScale() {
        val target = if (originalFontScale?.toFloatOrNull() ==
            LARGER_TEXT
        ) {
            LARGEST_TEXT
        } else {
            LARGER_TEXT
        }
        shell("settings put system font_scale $target")
        compose.waitUntil(CONFIGURATION_TIMEOUT_MS) {
            instrumentation.targetContext.resources.configuration.fontScale == target
        }
        // A relaunch, when there is one, is posted to the main thread after the change lands.
        instrumentation.waitForIdleSync()
        compose.waitForIdle()
    }

    private fun currentActivity(): MainActivity {
        lateinit var activity: MainActivity
        compose.activityRule.scenario.onActivity { activity = it }
        return activity
    }

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).bufferedReader().readText()
        }

    private companion object {
        const val AGE = "3"
        const val FIRST_LESSON = "Lesson 1"
        const val FIRST_ANSWER = "eyes"
        const val SECOND_QUESTION = "Where is the nose?"
        const val LARGER_TEXT = 1.15f
        const val LARGEST_TEXT = 1.3f
        const val CONFIGURATION_TIMEOUT_MS = 10_000L
    }
}
