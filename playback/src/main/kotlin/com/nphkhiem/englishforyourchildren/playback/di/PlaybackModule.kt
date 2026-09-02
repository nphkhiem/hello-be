package com.nphkhiem.englishforyourchildren.playback.di

import android.content.Context
import com.nphkhiem.englishforyourchildren.playback.ImageAssetLocator
import com.nphkhiem.englishforyourchildren.playback.Media3PlaybackController
import com.nphkhiem.englishforyourchildren.playback.MediaAssetLocator
import com.nphkhiem.englishforyourchildren.playback.PackagedAssetLocator
import com.nphkhiem.englishforyourchildren.playback.PlaybackController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Which implementation plays a recording, and which one finds it.
 *
 * The graph lives here rather than in `:app`, for the reason storage gives in `DataProvidersModule`:
 * a module assembles itself and `:app` only installs it. Note that nothing here provides an
 * `AssetManager`, because storage already provides one into the same component.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {
    @Binds
    @Singleton
    abstract fun bindPlaybackController(
        implementation: Media3PlaybackController
    ): PlaybackController

    companion object {
        // Provided rather than bound, because one class serves both kinds and which kind an
        // instance is comes from the factory that made it, never from the id it is handed.
        @Provides
        @Singleton
        fun provideMediaAssetLocator(@ApplicationContext context: Context): MediaAssetLocator =
            PackagedAssetLocator.forAudio(context)

        @Provides
        @Singleton
        fun provideImageAssetLocator(@ApplicationContext context: Context): ImageAssetLocator =
            PackagedAssetLocator.forImages(context)
    }
}
