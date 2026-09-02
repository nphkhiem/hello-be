package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.playback.PlaybackController
import com.nphkhiem.englishforyourchildren.playback.PlaybackEvent
import com.nphkhiem.englishforyourchildren.playback.PlaybackFailureCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * A player that does what it is told and reports it at once.
 *
 * [failWith] reports from inside [play], on purpose: that is the moment a real missing recording is
 * discovered, and it is the moment a listener that subscribed too late would miss.
 *
 * It lives here rather than in `:test-support`, which is a plain Kotlin module and cannot see an
 * Android library.
 */
class FakePlaybackController(private val failWith: PlaybackFailureCode? = null) :
    PlaybackController {
    private val emitted = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 8)

    override val events: Flow<PlaybackEvent> = emitted

    /**
     * One entry per thing said, each holding the clips it was said in.
     *
     * A list of lists rather than a flat one, because asking a question plays two recordings and a
     * flat list could not tell that apart from asking twice.
     */
    val spoken = mutableListOf<List<AssetId>>()
    var paused = false
        private set
    var stopped = false
        private set

    override suspend fun play(assets: List<AssetId>) {
        spoken += assets
        // The first one, matching the real controller: it looks for every part before playing any
        // of them, so the first one it cannot find is the one it reports.
        failWith?.let { emitted.tryEmit(PlaybackEvent.Failed(assets.first(), it)) }
    }

    /** A recording reaching its end, which is the one thing [failWith] can never produce. */
    fun finish(assetId: AssetId) {
        emitted.tryEmit(PlaybackEvent.Completed(assetId))
    }

    override fun pause() {
        paused = true
    }

    override fun resume() = Unit

    override fun stop() {
        stopped = true
    }
}
