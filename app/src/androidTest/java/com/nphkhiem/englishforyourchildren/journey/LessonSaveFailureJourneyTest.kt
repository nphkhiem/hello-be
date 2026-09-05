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
import javax.inject.Inject
import javax.inject.Singleton
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * What a child sees when storage refuses to write down what they just did.
 *
 * The refusal happens at the database, so everything above it is production code the whole way up.
 */
@HiltAndroidTest
@UninstallModules(DataProvidersModule::class, PlaybackModule::class)
class LessonSaveFailureJourneyTest {

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
        @Singleton
        fun refusing(database: HelloBeDatabase): RefusingProgressDao =
            RefusingProgressDao(database.progressDao())

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

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var storage: RefusingProgressDao

    private val tv by lazy { JourneyDriver(compose) }

    @Before
    fun injectGraph() {
        hilt.inject()
    }

    @Test
    fun givenStorageRefusesTheWrite_whenAChildAnswers_thenNothingClaimsItWasSaved() {
        openFirstLesson()
        storage.refuseTheNextWrite()

        tv.press(tv.string("lesson_skip"))

        // The product's own word for it. Nothing moves on and nothing pretends it was written.
        tv.awaitText(tv.string("lesson_pending_save"))
        tv.awaitText(FIRST_QUESTION)
    }

    @Test
    fun givenARefusedWrite_whenTheChildPressesAgain_thenTheWorkLandsAndTheyMoveOn() {
        // There is no retry control and no carry-on control on this screen, so pressing again is
        // the only way a child has out of a refused write. It does work, and this pins that it
        // does, because until those controls exist it is the whole recovery.
        openFirstLesson()
        storage.refuseTheNextWrite()
        tv.press(tv.string("lesson_skip"))
        tv.awaitText(tv.string("lesson_pending_save"))

        tv.press(tv.string("lesson_skip"))

        tv.awaitText(SECOND_QUESTION)
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

    private companion object {
        const val AGE = "3"
        const val FIRST_LESSON = "Lesson 1"
        const val FIRST_QUESTION = "Where are the eyes?"
        const val SECOND_QUESTION = "Where is the nose?"
    }
}
