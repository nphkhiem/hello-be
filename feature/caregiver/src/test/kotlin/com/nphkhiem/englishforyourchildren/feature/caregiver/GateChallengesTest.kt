package com.nphkhiem.englishforyourchildren.feature.caregiver

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.jupiter.api.Test

/**
 * The arithmetic behind the door.
 *
 * Every other test of the gate rests on the first one here. A source that cannot be made to repeat
 * is a source no test can pin, which is the whole reason this is injected rather than reaching for
 * a global random.
 */
class GateChallengesTest {
    @Test
    fun givenOneSeed_whenTwoSourcesRunFromIt_thenTheyAskExactlyTheSameThings() {
        val one = RandomGateChallenges(Random(SEED))
        val other = RandomGateChallenges(Random(SEED))

        val first = List(RUNS) { one.next() }
        val second = List(RUNS) { other.next() }

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun givenASeed_whenItAsksAgain_thenItDoesNotRepeatTheSameQuestionEveryTime() {
        // A source that returned one question forever would satisfy every other test here and be
        // useless: rotating on a wrong answer would rotate to the same challenge.
        val source = RandomGateChallenges(Random(SEED))

        val asked = List(RUNS) { source.next() }.toSet()

        assertThat(asked.size).isGreaterThan(1)
    }

    @Test
    fun givenAnyChallenge_whenItIsAsked_thenItsAnswerIsTheSumItAsksFor() {
        // A gate whose stated answer is not the answer cannot be opened by getting it right.
        eachChallenge { arithmetic ->
            val correct = arithmetic.answers[arithmetic.correctIndex]

            assertThat(correct).isEqualTo(arithmetic.left + arithmetic.right)
        }
    }

    @Test
    fun givenAnyChallenge_whenItIsAsked_thenItAddsTwoDigitNumbers() {
        eachChallenge { arithmetic ->
            assertThat(arithmetic.left).isAtLeast(TEN)
            assertThat(arithmetic.right).isAtLeast(TEN)
            assertThat(arithmetic.left).isLessThan(HUNDRED)
            assertThat(arithmetic.right).isLessThan(HUNDRED)
        }
    }

    @Test
    fun givenAnyChallenge_whenItIsAsked_thenItOffersThreeAnswersAndPointsInsideThem() {
        // `gateFocusIndex` promises entry focus lands on a wrong answer. It can only promise that
        // while the correct index is really inside the row.
        eachChallenge { arithmetic ->
            assertThat(arithmetic.answers).hasSize(ANSWERS)
            assertThat(arithmetic.correctIndex).isIn(arithmetic.answers.indices.toList())
        }
    }

    @Test
    fun givenAnyChallenge_whenItsWrongAnswersAreRead_thenEachIsANearMiss() {
        // The gate must not be solvable by picking the odd one out. A three year old who cannot add
        // can still spot the number that does not belong, so the wrong answers have to look like
        // plausible answers to the same question.
        eachChallenge { arithmetic ->
            val correct = arithmetic.answers[arithmetic.correctIndex]

            arithmetic.answers.forEachIndexed { index, answer ->
                if (index == arithmetic.correctIndex) return@forEachIndexed
                assertThat(answer).isNotEqualTo(correct)
                assertThat(kotlin.math.abs(answer - correct)).isAtMost(NEAR)
            }
        }
    }

    @Test
    fun givenAnyChallenge_whenItsAnswersAreRead_thenNoTwoOfThemAreTheSameNumber() {
        // Two answers reading the same would make one press both right and wrong at once.
        eachChallenge { arithmetic ->
            assertThat(arithmetic.answers.toSet()).hasSize(arithmetic.answers.size)
        }
    }

    @Test
    fun givenAnyChallenge_whenItsAnswersAreRead_thenNoneOfThemIsBelowZero() {
        // A near miss taken downward off a small sum could go negative, which is not a number a
        // caregiver should be asked to choose between.
        eachChallenge { arithmetic ->
            arithmetic.answers.forEach { assertThat(it).isAtLeast(0) }
        }
    }

    /** Many draws from several seeds, because one draw proves nothing about a random source. */
    private fun eachChallenge(assertion: (GateArithmetic) -> Unit) {
        SEEDS.forEach { seed ->
            val source = RandomGateChallenges(Random(seed))
            repeat(RUNS) { assertion(source.next()) }
        }
    }

    private companion object {
        const val SEED = 20260902L
        const val RUNS = 200
        const val ANSWERS = 3
        const val TEN = 10
        const val HUNDRED = 100

        /** Close enough that the wrong answers are not distinguishable without adding. */
        const val NEAR = 10

        val SEEDS = listOf(1L, 7L, 42L, 20260902L, Long.MAX_VALUE)
    }
}
