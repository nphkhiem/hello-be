package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The gate as it stands right now.
 *
 * Not [AdultGateUiState], which needs a written question this cannot supply: the sentence is
 * composed where there are string resources and a caregiver language, and this holds the sum.
 *
 * [opened] is state rather than a one-shot event so that a host reading it late still learns the
 * door is open. Nothing here navigates; it says what happened and the host acts.
 */
data class AdultGateState(
    val arithmetic: GateArithmetic,
    val previousAnswerWasWrong: Boolean,
    val opened: Boolean
)

/**
 * A door only a grown-up opens.
 *
 * The protection is not secrecy. `AdultGateUiState` says outright that the correct answer is no
 * secret from anyone who can read the question, and `INFORMATION_ARCHITECTURE.md` refuses to call
 * this a password or a login. It is a capability check: doing two-digit addition is the thing a
 * three year old cannot do and their caregiver can.
 *
 * Three rules make that hold, and each one is here rather than in a screen because a screen that
 * could be rebuilt differently would be a screen that could rebuild the gate open:
 *
 * - Entry focus rests on a wrong answer, which `AdultGateRules.gateFocusIndex` decides.
 * - Every wrong answer rotates the challenge, so walking the row and pressing each in turn gains
 *   nothing: the next question puts the correct answer somewhere else.
 * - A question nobody has answered for half a minute is replaced, so a challenge cannot be left on
 *   screen for a child to work at.
 */
@HiltViewModel
class AdultGateViewModel @Inject constructor(private val challenges: GateChallenges) : ViewModel() {
    private val _state = MutableStateFlow(
        AdultGateState(
            arithmetic = challenges.next(),
            previousAnswerWasWrong = false,
            opened = false
        )
    )

    val state: StateFlow<AdultGateState> = _state.asStateFlow()

    private var staleness: Job? = null

    init {
        waitForItToGoStale()
    }

    fun onAction(action: AdultGateAction) {
        if (_state.value.opened) return

        when (action) {
            is AdultGateAction.AnswerChosen -> answer(action.index)
        }
    }

    private fun answer(index: Int) {
        if (index == _state.value.arithmetic.correctIndex) {
            // Nothing rotates behind an open door, so the waiting stops here rather than carrying
            // on against a question nobody is looking at.
            staleness?.cancel()
            _state.value = _state.value.copy(opened = true)
            return
        }

        _state.value = AdultGateState(
            arithmetic = challenges.next(),
            previousAnswerWasWrong = true,
            opened = false
        )
        waitForItToGoStale()
    }

    /**
     * Half a minute untouched and the question is replaced, with nobody sent anywhere.
     *
     * Ejecting an adult who paused to think would be the gate punishing hesitation, and the pause
     * before "what is 34 plus 27" is exactly the pause this is meant to allow. The wrong-answer
     * notice goes with the question it belonged to: carrying it onto a fresh one would tell a
     * caregiver they had failed at something they had not been asked yet.
     *
     * Restarted by answering, because answering is touching it.
     */
    private fun waitForItToGoStale() {
        staleness?.cancel()
        staleness = viewModelScope.launch {
            while (true) {
                delay(UNTOUCHED_MILLIS)
                _state.value = AdultGateState(
                    arithmetic = challenges.next(),
                    previousAnswerWasWrong = false,
                    opened = false
                )
            }
        }
    }

    private companion object {
        /** Long enough to think about a sum, short enough that a question is never left standing. */
        const val UNTOUCHED_MILLIS = 30_000L
    }
}
