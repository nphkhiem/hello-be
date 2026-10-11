package com.nphkhiem.englishforyourchildren.content

import com.nphkhiem.englishforyourchildren.BuildConfig
import com.nphkhiem.englishforyourchildren.data.pack.ContentUpdater
import com.nphkhiem.englishforyourchildren.data.pack.ContentUpdaterFactory
import com.nphkhiem.englishforyourchildren.feature.caregiver.LessonUpdates
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Whether this build can get new lessons, and through what.
 *
 * Both halves of the configuration are needed: an address to fetch from and a key to check what
 * arrives. Half a configuration offers nothing, because a download that could not be verified must
 * never be possible.
 */
@Module
@InstallIn(SingletonComponent::class)
object LessonUpdatesModule {
    /** One updater for the whole app, or none: the caregiver's row and the prefetch share it. */
    @Provides
    @Singleton
    fun provideContentUpdates(factory: ContentUpdaterFactory): ContentUpdates =
        if (BuildConfig.CONTENT_BASE_URL.isBlank() || BuildConfig.CONTENT_PUBLIC_KEY.isBlank()) {
            ContentUpdates(updater = null)
        } else {
            ContentUpdates(
                factory.create(BuildConfig.CONTENT_BASE_URL, BuildConfig.CONTENT_PUBLIC_KEY)
            )
        }

    @Provides
    @Singleton
    fun provideLessonUpdates(updates: ContentUpdates): LessonUpdates =
        updates.updater?.let(::PackedLessonUpdates) ?: UnofferedLessonUpdates

    @Provides
    @Singleton
    fun provideBackgroundLessons(updates: ContentUpdates): BackgroundLessons =
        BackgroundLessons(updates.updater)
}

/** The app's one content updater, or null when this build has nowhere to get lessons from. */
class ContentUpdates(val updater: ContentUpdater?)
