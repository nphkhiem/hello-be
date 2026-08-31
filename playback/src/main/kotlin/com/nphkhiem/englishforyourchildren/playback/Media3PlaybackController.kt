package com.nphkhiem.englishforyourchildren.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * Playback on Media3.
 *
 * The player is built on the first recording that actually exists and destroyed by [stop], so the
 * expensive thing has a short life even though this class is a singleton. Today no recording
 * exists at all, which means the common path never builds a player.
 *
 * Everything here runs on the main thread, because Media3 asserts single-threaded access against
 * the application's main looper.
 */
@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val locator: MediaAssetLocator
) : PlaybackController,
    DefaultLifecycleObserver {
    private val emitted = MutableSharedFlow<PlaybackEvent>(
        extraBufferCapacity = EVENT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val events: Flow<PlaybackEvent> = emitted.asSharedFlow()

    private val main = Handler(Looper.getMainLooper())

    private var player: ExoPlayer? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            report { PlaybackEvent.Completed(it) }
        }

        override fun onPlayerError(error: PlaybackException) {
            report { PlaybackEvent.Failed(it, PlaybackFailureCode.UNPLAYABLE) }
        }
    }

    override suspend fun play(assetId: AssetId) = withContext(Dispatchers.Main.immediate) {
        val uri = locator.locate(assetId)
        if (uri == null) {
            // Whatever was sounding is superseded and says nothing on its way out.
            player?.stop()
            emitted.tryEmit(PlaybackEvent.Failed(assetId, PlaybackFailureCode.MISSING))
            return@withContext
        }

        val exo = player ?: buildPlayer().also { player = it }
        // The id travels on the item rather than in a field here, so an event is always read off
        // what the player is actually holding. A recording replaced mid-sentence cannot then be
        // reported under the name of the one that replaced it.
        exo.setMediaItem(MediaItem.Builder().setUri(uri).setMediaId(assetId.value).build())
        exo.prepare()
        exo.play()
    }

    /** Leaving the app silences it at once, before the television has finished changing. */
    override fun onStop(owner: LifecycleOwner) = pause()

    // There is deliberately no onStart. A child coming back to the television must not be met by
    // Pip talking at them unasked, so a paused recording stays paused until somebody presses
    // Select. Anyone tempted to "fix" the missing resume is looking at the feature.

    override fun pause() = onMain { player?.pause() }

    override fun resume() = onMain { player?.play() }

    override fun stop() = onMain {
        player?.release()
        player = null
    }

    /**
     * Emits about whatever the player is holding now, or nothing.
     *
     * Media3 delivers listener callbacks through a queue, so one can arrive after the recording it
     * describes has already been replaced. Re-reading the state at the moment of handling is what
     * makes a stale callback silent instead of a lie.
     */
    private inline fun report(event: (AssetId) -> PlaybackEvent) {
        val exo = player ?: return
        if (exo.playbackState == Player.STATE_ENDED || exo.playerError != null) {
            val id = exo.currentMediaItem?.mediaId ?: return
            emitted.tryEmit(event(AssetId(id)))
        }
    }

    private fun buildPlayer(): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            // A system announcement takes the sound away and gives it back. That is a pause a
            // child can wait through, not a question that failed.
            /* handleAudioFocus = */
            true
        )
        .build()
        .apply { addListener(listener) }

    private inline fun onMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post { block() }
    }

    private companion object {
        /** Enough that a burst of failures is never dropped; small enough to stay a buffer. */
        const val EVENT_BUFFER = 8
    }
}
