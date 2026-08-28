package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Immutable

/**
 * The one off-screen activity offered after the celebration.
 *
 * A value rather than a flag beside some copy. `stopForNowVisible` on the lesson is a boolean
 * because its wording is fixed; this wording is not, and a boolean with separate strings would
 * permit a prompt that is showing with nothing to say.
 */
@Immutable
data class PlayTogetherActivity(val title: String, val instruction: String)

/** One word a child met in the lesson that just finished. */
@Immutable
data class LearnedWord(val id: String, val label: String)

/**
 * Everything the celebration needs to draw itself.
 *
 * [revealed] is a fact the host sets when the reveal window closes. This screen does not run the
 * three to four second clock, per ADR 0003; `motion.durations.celebrationMax` names that budget
 * for whoever does.
 *
 * [saveConfirmed] is false until Room has the checkpoint. It changes what the page says, because
 * the design brief forbids claiming progress is saved before that.
 *
 * There is deliberately no audio field. Nothing here depends on sound: the headline, the phrase
 * and the words are all on the page and there is no replay control to degrade, so a field for it
 * would be state the composition never reads.
 */
@Immutable
data class CelebrationUiState(
    /**
     * The unit's word, as it appears in the headline: "home" gives "You found 4 home words!".
     *
     * Deliberately not the whole sentence. A host-supplied headline is free to say "four" above
     * five cards, and a page that miscounts the words a child just learned is the same untrue copy
     * the design brief bans elsewhere. The count comes from [words] instead, so it cannot disagree
     * with what is drawn.
     */
    val unitWord: String,
    /** Three to five, per the design brief. The row is built for its maximum. */
    val words: List<LearnedWord>,
    val revealed: Boolean,
    val saveConfirmed: Boolean,
    /**
     * The activity being offered, or null when none is.
     *
     * Null also carries the brief's "repeatedly declined suppression" without this screen knowing
     * anything about counting declines: a suppressed prompt is simply an activity that was never
     * offered.
     */
    val playTogether: PlayTogetherActivity? = null
)

/**
 * What the celebration reports upward.
 *
 * Back reuses whichever of these already means "the safe way out of what is on screen", so no
 * action exists solely to describe a Back press. That is the same reason HB-D07 kept Again and
 * Replay as one action and HB-D09 had Back reuse the safe choice.
 */
sealed interface CelebrationAction {
    data object DoneRequested : CelebrationAction

    /** The caregiver took the activity. Kept apart from declining, which the host counts. */
    data object PlayTogetherAccepted : CelebrationAction

    /** The activity was declined, or Back was pressed while it was being offered. */
    data object MaybeLaterRequested : CelebrationAction
}
