package com.nphkhiem.englishforyourchildren.playback

import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import kotlinx.coroutines.flow.Flow

/**
 * Playing one recording at a time.
 *
 * Deliberately the whole of it. There is no scoring here, no progression, no idea of an activity
 * or a child: this plays a sound and says what happened, and a lesson decides what that is worth.
 *
 * [events] carries no replay, so a listener that subscribes after a recording has already failed
 * hears nothing. Subscribe before the first [play].
 */
interface PlaybackController {
    val events: Flow<PlaybackEvent>

    /** Starts [assetId], replacing whatever was playing. Returns once playback has begun. */
    suspend fun play(assetId: AssetId)

    fun pause()

    /** Resumes a paused recording. Only ever called because somebody asked for it. */
    fun resume()

    /** Stops and releases the player. */
    fun stop()
}
