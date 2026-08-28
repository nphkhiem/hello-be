package com.nphkhiem.englishforyourchildren.feature.caregiver

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AdultGateRulesTest {

    @Test
    fun givenTheCorrectAnswerAnywhere_whenFocusIsPlaced_thenItIsNeverOnTheCorrectAnswer() {
        // The whole of the protection, so it is asserted at every position rather than at one.
        ANSWERS.indices.forEach { correct ->
            val challenge = challenge(correctIndex = correct)

            assertThat(gateFocusIndex(challenge)).isNotEqualTo(correct)
        }
    }

    @Test
    fun givenTheCorrectAnswerIsNotFirst_whenFocusIsPlaced_thenItRestsOnTheFirstAnswer() {
        assertThat(gateFocusIndex(challenge(correctIndex = 1))).isEqualTo(0)
        assertThat(gateFocusIndex(challenge(correctIndex = 2))).isEqualTo(0)
    }

    @Test
    fun givenTheCorrectAnswerIsFirst_whenFocusIsPlaced_thenItStepsPastIt() {
        assertThat(gateFocusIndex(challenge(correctIndex = 0))).isEqualTo(1)
    }

    @Test
    fun givenTwoAnswers_whenTheChallengeIsRead_thenItIsStillAGate() {
        val two = GateChallenge(question = QUESTION, answers = listOf("10", "11"), correctIndex = 1)

        assertThat(isChallengeUsable(two)).isTrue()
        assertThat(gateFocusIndex(two)).isEqualTo(0)
    }

    @Test
    fun givenOnlyOneAnswer_whenTheChallengeIsRead_thenItIsNotAGate() {
        // Its only button would be the correct one, so pressing anything at all would open it.
        val one = GateChallenge(question = QUESTION, answers = listOf("11"), correctIndex = 0)

        assertThat(isChallengeUsable(one)).isFalse()
        assertThat(gateFocusIndex(one)).isNull()
    }

    @Test
    fun givenNoAnswers_whenTheChallengeIsRead_thenItIsNotAGate() {
        val none = GateChallenge(question = QUESTION, answers = emptyList(), correctIndex = 0)

        assertThat(isChallengeUsable(none)).isFalse()
        assertThat(gateFocusIndex(none)).isNull()
    }

    @Test
    fun givenACorrectIndexPastTheEnd_whenTheChallengeIsRead_thenItIsNotAGate() {
        // Every answer would be a wrong answer, so the gate could never be opened, and a gate that
        // cannot be opened is a defect rather than a safe default. It fails closed either way.
        val broken = challenge(correctIndex = ANSWERS.size)

        assertThat(isChallengeUsable(broken)).isFalse()
        assertThat(gateFocusIndex(broken)).isNull()
    }

    @Test
    fun givenANegativeCorrectIndex_whenTheChallengeIsRead_thenItIsNotAGate() {
        val broken = challenge(correctIndex = -1)

        assertThat(isChallengeUsable(broken)).isFalse()
        assertThat(gateFocusIndex(broken)).isNull()
    }

    private fun challenge(correctIndex: Int) =
        GateChallenge(question = QUESTION, answers = ANSWERS, correctIndex = correctIndex)

    private companion object {
        const val QUESTION = "What is 7 + 4?"
        val ANSWERS = listOf("10", "11", "12")
    }
}
