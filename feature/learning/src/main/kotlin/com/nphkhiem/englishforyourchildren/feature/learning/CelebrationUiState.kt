package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Immutable

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
    val saveConfirmed: Boolean
)

/**
 * What the celebration reports upward.
 *
 * One action, because Back and Done mean the same thing here. Two actions that must always be
 * handled identically is the trap HB-D07 avoided for Again and Replay, and HB-D09 avoided again
 * when Back reused the safe choice.
 */
sealed interface CelebrationAction {
    data object DoneRequested : CelebrationAction
}
