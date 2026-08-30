package com.nphkhiem.englishforyourchildren.playback.di

import com.nphkhiem.englishforyourchildren.playback.Media3PlaybackController
import com.nphkhiem.englishforyourchildren.playback.MediaAssetLocator
import com.nphkhiem.englishforyourchildren.playback.PackagedAssetLocator
import com.nphkhiem.englishforyourchildren.playback.PlaybackController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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

    @Binds
    abstract fun bindMediaAssetLocator(implementation: PackagedAssetLocator): MediaAssetLocator
}
