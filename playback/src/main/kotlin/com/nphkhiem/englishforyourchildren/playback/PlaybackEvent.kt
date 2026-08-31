package com.nphkhiem.englishforyourchildren.playback

import com.nphkhiem.englishforyourchildren.domain.model.AssetId

/** Why a recording did not play. */
enum class PlaybackFailureCode {
    /** No file has been made for this id yet. Today this is every recording the course names. */
    MISSING,

    /** A file exists and the player refused it. */
    UNPLAYABLE
}

/**
 * What playback reports, and nothing more.
 *
 * These are facts about a sound, not about a lesson: there is no outcome here, no score and no
 * notion of a question. Whoever listens decides what any of it means.
 */
sealed interface PlaybackEvent {
    /** The recording reached its end. Nobody is asked to do anything about it. */
    data class Completed(val assetId: AssetId) : PlaybackEvent

    data class Failed(val assetId: AssetId, val code: PlaybackFailureCode) : PlaybackEvent
}
