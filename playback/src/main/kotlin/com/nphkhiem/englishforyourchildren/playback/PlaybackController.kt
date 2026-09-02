package com.nphkhiem.englishforyourchildren.playback

import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import kotlinx.coroutines.flow.Flow

/**
 * Playing recordings, one thing at a time.
 *
 * Deliberately the whole of it. There is no scoring here, no progression, no idea of an activity
 * or a child: this plays a sound and says what happened, and a lesson decides what that is worth.
 *
 * [events] carries no replay, so a listener that subscribes after a recording has already failed
 * hears nothing. Subscribe before the first [play].
 */
interface PlaybackController {
    val events: Flow<PlaybackEvent>

    /**
     * Starts [assets] in order as one thing, replacing whatever was playing.
     *
     * One call rather than several because a spoken question is a stem and then the word it is
     * about, and nothing between them should be able to interleave. Only the last one reports
     * [PlaybackEvent.Completed]: the parts have no separate meaning, and a listener that heard the
     * stem end would think the question had been asked.
     *
     * Every asset must be there. If any one of them is missing this reports the missing one and
     * plays nothing, because half a question is worse than a silent one.
     *
     * Returns once playback has begun.
     */
    suspend fun play(assets: List<AssetId>)

    fun pause()

    /** Resumes a paused recording. Only ever called because somebody asked for it. */
    fun resume()

    /** Stops and releases the player. */
    fun stop()
}
