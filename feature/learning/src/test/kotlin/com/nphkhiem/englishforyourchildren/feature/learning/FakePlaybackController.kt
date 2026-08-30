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

    val played = mutableListOf<AssetId>()
    var paused = false
        private set
    var stopped = false
        private set

    override suspend fun play(assetId: AssetId) {
        played += assetId
        failWith?.let { emitted.tryEmit(PlaybackEvent.Failed(assetId, it)) }
    }

    override fun pause() {
        paused = true
    }

    override fun resume() = Unit

    override fun stop() {
        stopped = true
    }
}
