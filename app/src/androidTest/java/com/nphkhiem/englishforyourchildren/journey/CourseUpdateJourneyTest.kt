package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
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
import java.io.File
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

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val packs get() = File(context.filesDir, "packs")

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
        stagePack()
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

        stagePack()
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

    /** The shipped course, as the next published version, written where a finished download lands. */
    private fun stagePack() {
        val pack = File(packs, PACK_VERSION)
        listOf(COURSE, ATTRIBUTIONS).forEach { copyAsset(it, pack) }
        context.assets.list(UNITS).orEmpty().forEach { copyAsset("$UNITS/$it", pack) }

        File(pack, COURSE).apply {
            writeText(readText().replace(SEED_VERSION, "\"$PACK_VERSION\""))
        }
        File(pack, "$UNITS/u01-my-body.json").apply {
            writeText(readText().replace(SEED_FIRST, PACK_FIRST).replace(SEED_SECOND, PACK_SECOND))
        }
    }

    private fun copyAsset(path: String, pack: File) {
        val target = File(pack, path).apply { parentFile?.mkdirs() }
        context.assets.open(path).use { input -> target.outputStream().use { input.copyTo(it) } }
    }

    private companion object {
        const val AGE = "3"
        const val FIRST_LESSON = "Lesson 1"
        const val FIRST_ANSWER = "eyes"
        const val COURSE = "curriculum/course.json"
        const val UNITS = "curriculum/units"
        const val ATTRIBUTIONS = "attribution/attributions.json"
        const val SEED_VERSION = "\"2026.09\""
        const val PACK_VERSION = "2026.10"
        const val SEED_FIRST = "Where are the eyes?"
        const val PACK_FIRST = "Where are your eyes?"
        const val SEED_SECOND = "Where is the nose?"
        const val PACK_SECOND = "Where is your nose?"
    }
}
