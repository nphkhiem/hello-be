package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.MainActivity
import com.nphkhiem.englishforyourchildren.data.di.DataProvidersModule
import com.nphkhiem.englishforyourchildren.data.local.HelloBeDatabase
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileDao
import com.nphkhiem.englishforyourchildren.data.progress.ProgressDao
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
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
 * A button held a moment too long, which on a television is not an edge case but a Tuesday.
 *
 * The reducer and the ViewModel both already refuse a second answer while the first is being
 * written down, and both are tested. What was never tested is the same thing through the real focus
 * and key pipeline, which is where a duplicate actually comes from.
 *
 * The two halves of that gesture land on either side of the write, and which side is a matter of
 * milliseconds, so both are asserted here. The second is the case this test first found and that
 * was fixed afterwards: a press arriving *after* the lesson has moved on used to be accepted, and
 * answered a question the child never saw.
 */
@HiltAndroidTest
@UninstallModules(DataProvidersModule::class)
class RepeatedInputJourneyTest {

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
        @Singleton
        fun holding(database: HelloBeDatabase): DelayingProgressDao =
            DelayingProgressDao(database.progressDao())

        @Provides
        fun progress(holding: DelayingProgressDao): ProgressDao = holding

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

    /** Read directly, because no repository exposes attempts and this test is about the rows. */
    @Inject lateinit var attempts: ProgressDao

    @Inject lateinit var storage: DelayingProgressDao

    private val tv by lazy { JourneyDriver(compose) }

    @Before
    fun injectGraph() {
        hilt.inject()
    }

    @Test
    fun givenTwoPressesLandBeforeTheWriteDoes_whenItLands_thenOnlyOneAnswerWasRecorded() {
        openFirstLesson()

        // The window this is about lasts a few milliseconds on a television, so it is held open
        // deliberately. Both presses then land while the first answer is still being written down,
        // which is the moment the reducer refuses a second one.
        storage.holdTheNextWrite()

        tv.doublePress(FIRST_ANSWER)

        // One question forward, and one row. As far as this child's history is concerned they
        // pressed once, because that is how many questions they were asked.
        tv.awaitText(SECOND_QUESTION)
        assertThat(recordedAttempts().map { it.activityId })
            .containsExactly("$FIRST_LESSON_ID-a1")
    }

    @Test
    fun givenTwoPressesLandEitherSideOfTheWrite_whenItLands_thenOnlyOneAnswerWasRecorded() {
        // The same gesture with nothing held open, which is what a television actually does: the
        // first press is written down and the lesson moves on, and the second arrives through the
        // new question's own card. Whether it lands before or after the advance is a race, and the
        // child pressed once either way, so the row count is the same either way.
        openFirstLesson()

        tv.doublePress(FIRST_ANSWER)

        tv.awaitText(SECOND_QUESTION)
        assertThat(recordedAttempts().map { it.activityId })
            .containsExactly("$FIRST_LESSON_ID-a1")
    }

    private fun openFirstLesson() {
        // The v2 rule queues work on a standard dispatcher rather than running it eagerly, so a
        // launched Activity is not yet a screen with words on it.
        tv.awaitText(AGE)
        tv.press(AGE)
        tv.press(tv.string("create_submit"))
        tv.awaitText(tv.string("home_start"))
        // One press, because that is what the dominant control does now: it opens the lesson the
        // progression policy names rather than the page that lists them.
        tv.press(tv.string("home_start"))
        tv.awaitText(FIRST_ANSWER)
    }

    private fun recordedAttempts() = runBlocking {
        val stored = profiles.observeProfiles().first()
        check(stored is DomainResult.Success) { "the child this test made should be readable" }
        attempts.attemptsFor(stored.value.single().id.value)
    }

    private companion object {
        const val AGE = "3"
        const val FIRST_LESSON = "Lesson 1"
        const val FIRST_LESSON_ID = "u01-my-body-l1"
        const val FIRST_ANSWER = "eyes"
        const val SECOND_QUESTION = "Where is the nose?"
    }
}
