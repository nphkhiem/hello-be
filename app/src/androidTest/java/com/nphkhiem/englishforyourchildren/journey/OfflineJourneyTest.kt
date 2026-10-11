package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.MainActivity
import com.nphkhiem.englishforyourchildren.content.BackgroundLessons
import com.nphkhiem.englishforyourchildren.content.ContentUpdates
import com.nphkhiem.englishforyourchildren.content.LessonUpdatesModule
import com.nphkhiem.englishforyourchildren.content.PackedLessonUpdates
import com.nphkhiem.englishforyourchildren.data.di.DataProvidersModule
import com.nphkhiem.englishforyourchildren.data.local.HelloBeDatabase
import com.nphkhiem.englishforyourchildren.data.pack.ContentFetcher
import com.nphkhiem.englishforyourchildren.data.pack.ContentUpdaterFactory
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileDao
import com.nphkhiem.englishforyourchildren.data.progress.ProgressDao
import com.nphkhiem.englishforyourchildren.feature.caregiver.LessonUpdates
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * A television configured to fetch lessons, on a network that never answers.
 *
 * The build under test has a lesson server, so the fetching code really runs: every request goes
 * to a server that holds the connection open and never replies, which is the worst case, slower to
 * fail than having no network at all. Nothing a child does may wait for it.
 */
@HiltAndroidTest
@UninstallModules(DataProvidersModule::class, LessonUpdatesModule::class)
class OfflineJourneyTest {

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

    /** The app's own lesson wiring, pointed at [SilentServer] instead of a real host. */
    @Module
    @InstallIn(SingletonComponent::class)
    object Configured {
        @Provides
        @Singleton
        fun updates(factory: ContentUpdaterFactory): ContentUpdates =
            ContentUpdates(factory.create(SilentServer, ANY_KEY))

        @Provides
        @Singleton
        fun lessonUpdates(updates: ContentUpdates): LessonUpdates =
            PackedLessonUpdates(requireNotNull(updates.updater))

        @Provides
        @Singleton
        fun background(updates: ContentUpdates): BackgroundLessons =
            BackgroundLessons(updates.updater)
    }

    /** Takes every request and never answers it, until the test is over. */
    object SilentServer : ContentFetcher {
        val asked = CountDownLatch(1)
        var hangUp = CountDownLatch(1)

        override fun open(path: String): InputStream {
            asked.countDown()
            hangUp.await()
            throw IOException("never answered")
        }
    }

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val tv by lazy { JourneyDriver(compose) }

    private val packs get() = TestPacks.folder()

    private var television: ActivityScenario<MainActivity>? = null

    @Before
    fun startOffline() {
        hilt.inject()
        packs.deleteRecursively()
        SilentServer.hangUp = CountDownLatch(1)
    }

    @After
    fun releaseTheServer() {
        // Lets the requests still held open finish, so no thread outlives the test.
        SilentServer.hangUp.countDown()
        television?.close()
        packs.deleteRecursively()
    }

    // Bounded, because the failure this guards against is a lesson that waits forever: it has to
    // fail the suite, not hang it.
    @Test(timeout = LESSON_BUDGET_MILLIS)
    fun givenANetworkThatNeverAnswers_whenAChildDoesTheirFirstLesson_thenNothingWaitsForIt() {
        switchOn()
        openLessonOne()

        tv.awaitText(TestPacks.SEED_FIRST)
        tv.press(FIRST_ANSWER)
        tv.awaitText(TestPacks.SEED_SECOND)

        // And the fetch really was under way the whole time, held open behind the lesson.
        assertThat(SilentServer.asked.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun givenNewerLessonsAlreadyHere_whenTheTelevisionComesBackOnOffline_thenTheyStillTeach() {
        TestPacks.stage(packs)
        switchOn()
        tv.awaitText(AGE)
        tv.press(AGE)
        tv.press(tv.string("create_submit"))
        tv.awaitText(FIRST_LESSON)

        television?.close()
        switchOn()
        tv.awaitText(FIRST_LESSON)
        tv.press(FIRST_LESSON)

        tv.awaitText(TestPacks.PACK_FIRST)
        compose.onNodeWithText(TestPacks.SEED_FIRST).assertDoesNotExist()
    }

    private fun switchOn() {
        television = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    private fun openLessonOne() {
        tv.awaitText(AGE)
        tv.press(AGE)
        tv.press(tv.string("create_submit"))
        tv.awaitText(FIRST_LESSON)
        tv.press(FIRST_LESSON)
    }

    private companion object {
        const val AGE = "3"
        const val FIRST_LESSON = "Lesson 1"
        const val FIRST_ANSWER = "eyes"
        const val WAIT_SECONDS = 10L
        const val LESSON_BUDGET_MILLIS = 60_000L

        /** Any 32 bytes: nothing ever arrives to be verified against it. */
        const val ANY_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    }
}
