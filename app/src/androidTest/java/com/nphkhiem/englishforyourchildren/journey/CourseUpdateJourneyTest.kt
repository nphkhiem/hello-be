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
 * A newer course arriving on a television a child already uses.
 *
 * The pack is real: the starter course read out of this app's own assets, written to where
 * downloads land at a newer version, with two questions reworded so the screen says which course
 * is teaching. It is put there directly rather than downloaded, because what is under test is when
 * it goes live and what a child then sees. The download is tested on its own in `:data`.
 */
@HiltAndroidTest
@UninstallModules(DataProvidersModule::class)
class CourseUpdateJourneyTest {

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

    /** Empty, because the pack has to be on disk before the television is switched on. */
    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val tv by lazy { JourneyDriver(compose) }

    private val packs get() = TestPacks.folder()

    private var television: ActivityScenario<MainActivity>? = null

    @Before
    fun startFromTheSeed() {
        hilt.inject()
        // Another journey must never inherit a pack, and this one must never inherit theirs.
        packs.deleteRecursively()
    }

    @After
    fun leaveNoPackBehind() {
        television?.close()
        packs.deleteRecursively()
    }

    @Test
    fun givenANewerCourseWaitingOnDisk_whenTheChildReachesTheirPath_thenTheyAreTaughtFromIt() {
        TestPacks.stage(packs)
        switchOn()

        openLessonOne()

        tv.awaitText(PACK_FIRST)
        compose.onNodeWithText(SEED_FIRST).assertDoesNotExist()
    }

    @Test
    fun givenANewerCourseArrivingMidLesson_whenTheLessonGoesOn_thenItIsNotChangedUnderThem() {
        switchOn()
        openLessonOne()
        tv.awaitText(SEED_FIRST)

        TestPacks.stage(packs)
        tv.press(FIRST_ANSWER)

        // The second question as the shipped course asks it. A pack may go live only once no
        // lesson is open, and a session never mixes two versions.
        tv.awaitText(SEED_SECOND)
        compose.onNodeWithText(PACK_SECOND).assertDoesNotExist()
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
        const val SEED_FIRST = TestPacks.SEED_FIRST
        const val PACK_FIRST = TestPacks.PACK_FIRST
        const val SEED_SECOND = TestPacks.SEED_SECOND
        const val PACK_SECOND = TestPacks.PACK_SECOND
    }
}
