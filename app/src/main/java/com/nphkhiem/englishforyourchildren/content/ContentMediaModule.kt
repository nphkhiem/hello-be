package com.nphkhiem.englishforyourchildren.content

import com.nphkhiem.englishforyourchildren.data.pack.ContentPackStore
import com.nphkhiem.englishforyourchildren.playback.DownloadedMedia
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Tells playback where the live pack's pictures and recordings are.
 *
 * Here because only the app sees both sides: playback finds media, and the data layer knows which
 * pack is live. Neither may depend on the other.
 */
@Module
@InstallIn(SingletonComponent::class)
object ContentMediaModule {
    @Provides
    fun provideDownloadedMedia(packs: ContentPackStore): DownloadedMedia =
        DownloadedMedia(packs::liveDirectory)
}
