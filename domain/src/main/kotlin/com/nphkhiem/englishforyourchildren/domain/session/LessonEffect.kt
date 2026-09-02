package com.nphkhiem.englishforyourchildren.domain.session

import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.repository.CompleteSession
import com.nphkhiem.englishforyourchildren.domain.repository.PersistCheckpoint

/**
 * What the world outside is asked to do.
 *
 * The reducer never writes, plays or waits. It says what should happen and something else does it,
 * which is what lets every rule in here be tested without a database or a speaker.
 */
sealed interface LessonEffect {
    data class Persist(val command: PersistCheckpoint) : LessonEffect

    /**
     * Say these, in this order, as one thing.
     *
     * A question is a stem and then what it is about, so playing it is one instruction rather than
     * two that something else has to keep in step. The last one is what the lesson listens for the
     * end of. See `spokenPrompt`.
     */
    data class Play(val assets: List<AssetId>) : LessonEffect {
        init {
            require(assets.isNotEmpty()) { "Playing nothing is silence, not an effect" }
        }
    }

    data object PausePlayback : LessonEffect

    data class Complete(val command: CompleteSession) : LessonEffect
}

/** A new state, and what to do about it. */
data class LessonReduction(val state: LessonSessionState, val effects: List<LessonEffect>)
