package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ActivityScenario
import com.nphkhiem.englishforyourchildren.MainActivity
import com.nphkhiem.englishforyourchildren.data.di.DataProvidersModule
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
import javax.inject.Inject
import javax.inject.Singleton
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * What a television remembers after it has been switched off.
 *
 * M1 claims process death restores the last confirmed checkpoint, and the two words that carry the
 * claim are "last" and "confirmed". Work the app told a child it had not written down must not come
 * back either, or the app is quietly resurrecting progress it disclaimed.
 *
 * True process death is not available here: the instrumentation runner lives in this process and
 * would die with it. What goes instead is everything that could be holding the child's position in
 * memory. The Activity closes, and with it the `ViewModelStore` and the `LessonViewModel` holding
 * the session, because a closed scenario is not a configuration change. The database closes and the
 * same file is opened again.
 *
 * The Hilt component survives on purpose, and that makes this stricter rather than weaker. The
 * repositories are bound `@Singleton`, so one that had cached the child's position would sail
 * through a version of this test where everything was rebuilt, and fails this one.
 *
 * `ActivityScenario.recreate()` is deliberately not used. It hands the new Activity the same
 * `ViewModelStore`, so the ViewModel is never rebuilt and the test passes while proving nothing.
 */
@HiltAndroidTest
@UninstallModules(DataProvidersModule::class, PlaybackModule::class)
class ProcessDeathJourneyTest {

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
        /** On disk, and the same file across the drop. In memory would destroy the thing under test. */
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): ReopeningDatabase =
            ReopeningDatabase(context, DATABASE)

        @Provides
        fun profiles(database: ReopeningDatabase): ChildProfileDao = database.profiles

        /** Wraps the forwarding DAO, which is stable across a reopen, so both survive together. */
        @Provides
        @Singleton
        fun refusing(database: ReopeningDatabase): RefusingProgressDao =
            RefusingProgressDao(database.progress)

        @Provides
        fun progress(refusing: RefusingProgressDao): ProgressDao = refusing

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

    /** Empty, because this test owns its own launches: a rule would hold one Activity throughout. */
    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var storage: ReopeningDatabase

    @Inject lateinit var refusing: RefusingProgressDao

    private val tv by lazy { JourneyDriver(compose) }

    private var television: ActivityScenario<MainActivity>? = null

    @Before
    fun injectStorage() {
        hilt.inject()
    }

    @After
    fun switchOffAndForget() {
        television?.close()
        storage.deleteFile()
    }

    @Test
    fun givenAQuestionWasAnsweredAndSaved_whenTheTelevisionComesBackOn_thenTheChildIsPastIt() {
        switchOn()
        createAChildAndOpenLessonOne()

        // One question finished and written down, so the next one is where this child now is.
        tv.press(tv.string("lesson_skip"))
        tv.awaitText(SECOND_QUESTION)

        switchOff()
        switchOn()
        continueLessonOneFromHome()

        // Off the disk, into a ViewModel that has never seen this child before.
        tv.awaitText(SECOND_QUESTION)
    }

    @Test
    fun givenAnAnswerStorageRefused_whenTheTelevisionComesBackOn_thenItWasNotQuietlyKept() {
        switchOn()
        createAChildAndOpenLessonOne()

        // The first question lands, so coming back to the second is expected either way. It is the
        // second question that must not be treated as finished.
        tv.press(tv.string("lesson_skip"))
        tv.awaitText(SECOND_QUESTION)

        refusing.refuseTheNextWrite()
        tv.press(tv.string("lesson_skip"))
        tv.awaitText(tv.string("lesson_pending_save"))

        switchOff()
        switchOn()
        continueLessonOneFromHome()

        // Still the second question. Nothing on this television ever wrote it down, and a pending
        // checkpoint that came back would be the app resurrecting work it told the child it had not
        // saved.
        tv.awaitText(SECOND_QUESTION)
        compose.onNodeWithText(THIRD_QUESTION).assertDoesNotExist()
    }

    private fun switchOn() {
        television = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    /**
     * Everything that could be remembering, gone.
     *
     * Closing the scenario takes the Activity, its `ViewModelStore` and every ViewModel in it.
     * Reopening the database means the answer that comes back has to come off the file rather than
     * out of a connection that never went away.
     */
    private fun switchOff() {
        television?.close()
        television = null
        storage.reopen()
    }

    private fun createAChildAndOpenLessonOne() {
        // Waited for rather than assumed. The v2 rule queues work on a standard dispatcher instead
        // of running it eagerly, so a launch that has returned is not yet a screen with words on it.
        tv.awaitText(AGE)
        tv.press(AGE)
        tv.press(tv.string("create_submit"))
        openLessonOneFromHome()
    }

    /** With one child stored, the app opens on their home. See `resolveEntry`. */
    private fun openLessonOneFromHome() {
        tv.awaitText(tv.string("home_start"))
        // One press, because that is what the dominant control does now: it opens the lesson the
        // progression policy names rather than the page that lists them.
        tv.press(tv.string("home_start"))
    }

    /**
     * The same control, saying the other thing.
     *
     * A child with a checkpoint behind them is offered Continue rather than a first adventure, so a
     * television coming back on has to be met on those words. That the label changes at all is the
     * returning-learner state, and this journey is the only place that crosses a process death to
     * see it.
     */
    private fun continueLessonOneFromHome() {
        tv.awaitText(tv.string("home_continue"))
        tv.press(tv.string("home_continue"))
    }

    private companion object {
        const val AGE = "3"
        const val DATABASE = "process-death-journey.db"
        const val FIRST_LESSON = "Lesson 1"
        const val SECOND_QUESTION = "Where is the nose?"
        const val THIRD_QUESTION = "Find the ears."
    }
}
