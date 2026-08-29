package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.runtime.Immutable

/**
 * One arithmetic question and the answers offered for it.
 *
 * [question] is a finished string rather than two numbers and an operator, because this screen does
 * no arithmetic and writes no grammar. The host composes and rotates it.
 *
 * [correctIndex] is here on purpose, and it is the one place this module knowingly departs from the
 * rule `LessonUiState` sets out, that a screen is never told which answer is right. That rule exists
 * so a child cannot learn a lesson answer from the interface. It does not transfer: the answer to
 * "What is 7 + 4?" is not a secret from anyone who can read the question, and the information
 * architecture is explicit that this is never a password or a secure login. It is a capability
 * check. Knowing which answer is correct is what lets this screen guarantee it never focuses it.
 *
 * The screen still never judges. It reports which answer was pressed and the host decides.
 */
@Immutable
data class GateChallenge(val question: String, val answers: List<String>, val correctIndex: Int)

/**
 * Everything the adult gate needs to draw itself.
 *
 * [previousAnswerWasWrong] is a boolean and not an attempt count. The challenge rotates on every
 * incorrect answer, so a given challenge never collects a second wrong answer and a counter would
 * describe a state that cannot happen.
 */
@Immutable
data class AdultGateUiState(val challenge: GateChallenge, val previousAnswerWasWrong: Boolean)

/** What the adult gate reports upward. */
sealed interface AdultGateAction {
    /**
     * An answer was pressed, named by position rather than by value so that two answers that happen
     * to read the same are still distinguishable. Whether it opens the gate is the host's to decide.
     */
    data class AnswerChosen(val index: Int) : AdultGateAction
}
