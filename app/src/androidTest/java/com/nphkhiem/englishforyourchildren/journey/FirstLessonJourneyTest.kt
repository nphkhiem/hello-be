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
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
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
 * A child from nothing to a finished lesson, through the real graph.
 *
 * Only storage is replaced. Content is read from the packaged bundle, so this walks the lesson that
 * actually ships, and playback is the real controller, which finds no recording and reports so
 * exactly as it does on a television.
 */
@HiltAndroidTest
@UninstallModules(DataProvidersModule::class)
class FirstLessonJourneyTest {

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

    @Inject lateinit var profiles: ProfileRepository

    @Inject lateinit var progress: ProgressRepository

    private val tv by lazy { JourneyDriver(compose) }

    @Before
    fun injectGraph() {
        hilt.inject()
    }

    @Test
    fun givenATelevisionNobodyHasUsed_whenItOpens_thenItAsksForAChildRatherThanInventingOne() {
        tv.awaitText(tv.string("create_question"))
    }

    @Test
    fun givenANewChild_whenTheyWalkTheFirstLesson_thenTheWorkIsWrittenDown() {
        // The whole of M1 press by press: a television with nothing on it, a child made, a lesson
        // opened from the packaged course, and every question answered.
        createChild()

        tv.press(tv.string("home_start"))
        tv.awaitText(FIRST_LESSON)
        tv.press(FIRST_LESSON)

        // No recording exists for any prompt, so every question offers the unscored skip and that
        // is the fair way through. This is the "missing media, fair skip" journey.
        repeat(ACTIVITIES) {
            tv.awaitText(tv.string("lesson_skip"))
            tv.press(tv.string("lesson_skip"))
        }

        // The storybook page at the end, on the words this lesson is for. The wait covers the
        // reveal budget, which is a little over three seconds.
        tv.awaitText(tv.string("celebration_done"))
        TAUGHT_WORDS.forEach { tv.awaitText(it) }

        // And the same thing again as a fact rather than a picture: no screen can fake this.
        compose.waitUntil(TIMEOUT_MILLIS) { completedLessons().contains(LessonId(FIRST_LESSON_ID)) }
        assertThat(completedLessons()).contains(LessonId(FIRST_LESSON_ID))
    }

    private fun createChild() {
        tv.press(AGE)
        tv.press(tv.string("create_submit"))
        tv.awaitText(tv.string("home_start"))
    }

    private fun completedLessons(): Set<LessonId> = runBlocking {
        val stored = profiles.observeProfiles().first()
        val child = (stored as? DomainResult.Success)?.value?.singleOrNull()
            ?: return@runBlocking emptySet()
        val progressed = progress.observeProfileProgress(child.id).first()
        (progressed as? DomainResult.Success)?.value?.lessonsCompleted.orEmpty()
    }

    private companion object {
        const val AGE = "3"
        const val FIRST_LESSON = "Lesson 1"
        const val FIRST_LESSON_ID = "u01-my-body-l1"

        /** What the shipped first lesson declares it teaches. */
        val TAUGHT_WORDS = listOf("eyes", "ears", "nose", "mouth")

        /** The approved spine: listen, listen, picture, letter, say with Pip, review. */
        const val ACTIVITIES = 6
        const val TIMEOUT_MILLIS = 10_000L
    }
}
