package com.nphkhiem.englishforyourchildren.feature.caregiver

import kotlin.random.Random

/**
 * One sum, and the answers offered for it, with no words in it.
 *
 * Numbers rather than a finished question, because [GateChallenge.question] says the host composes
 * the sentence. That is not a detail: a question written here would be written in one language, and
 * the caregiver area is bilingual. Arithmetic is the same in both.
 *
 * [answers] are numbers for the same reason, and because "is this answer a near miss" is a question
 * about numbers that a test should not have to parse strings to ask.
 */
data class GateArithmetic(
    val left: Int,
    val right: Int,
    val answers: List<Int>,
    val correctIndex: Int
)

/**
 * Where the gate's arithmetic comes from.
 *
 * Injected, following [com.nphkhiem.englishforyourchildren.domain.time.TimeProvider] and
 * `IdProvider`, so that a gate cannot pick its own numbers from a global source and become
 * something no test can pin down. A gate whose challenges cannot be repeated is a gate whose
 * behaviour cannot be asserted, and this one carries the only thing standing between a three year
 * old and their caregiver's settings.
 */
fun interface GateChallenges {
    fun next(): GateArithmetic
}

/**
 * Two-digit addition, with wrong answers that look like right ones.
 *
 * The wrong answers are near misses on purpose. A child who cannot add can still pick the number
 * that does not belong, so a challenge offering 43, 41 and 7 would be a gate with a door in it.
 * Every answer here is within [NEAR] of the truth, which is close enough that telling them apart
 * means doing the sum.
 *
 * Both numbers are kept under [HALF_HUNDRED] so their sum stays two digits, which keeps the near
 * misses two digits too. A row reading 98, 101, 96 gives the answer away by its width.
 */
class RandomGateChallenges(private val random: Random) : GateChallenges {
    override fun next(): GateArithmetic {
        val left = random.nextInt(TEN, HALF_HUNDRED)
        val right = random.nextInt(TEN, HALF_HUNDRED)
        val correct = left + right

        val wrong = mutableSetOf<Int>()
        while (wrong.size < ANSWERS - 1) {
            val miss = correct + nudge()
            // Never the truth by accident, and never below zero: a caregiver choosing between
            // negative numbers would be a stranger question than the one being asked.
            if (miss != correct && miss >= 0) wrong += miss
        }

        val answers = (wrong + correct).shuffled(random)
        return GateArithmetic(
            left = left,
            right = right,
            answers = answers,
            correctIndex = answers.indexOf(correct)
        )
    }

    /** A step away from the answer, never nothing and never far. */
    private fun nudge(): Int {
        val size = random.nextInt(1, NEAR + 1)
        return if (random.nextBoolean()) size else -size
    }

    private companion object {
        const val TEN = 10
        const val HALF_HUNDRED = 50
        const val ANSWERS = 3

        /** Close enough that the wrong answers are not distinguishable without adding. */
        const val NEAR = 10
    }
}
