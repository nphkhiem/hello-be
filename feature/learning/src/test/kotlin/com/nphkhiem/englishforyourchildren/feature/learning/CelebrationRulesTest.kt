package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CelebrationRulesTest {

    @Test
    fun givenTheRevealHasNotRun_whenTheWordsAreConsidered_thenTheyAreNotOnThePageYet() {
        assertThat(wordsVisible(revealed = false, reduceMotion = false)).isFalse()
    }

    @Test
    fun givenTheRevealHasRun_whenTheWordsAreConsidered_thenTheyAreOnThePage() {
        assertThat(wordsVisible(revealed = true, reduceMotion = false)).isTrue()
    }

    @Test
    fun givenReducedMotion_whenTheRevealHasNotRun_thenTheWordsAreAlreadyThere() {
        // The static success page: under reduced motion there is no arrival to wait for.
        assertThat(wordsVisible(revealed = false, reduceMotion = true)).isTrue()
    }

    @Test
    fun givenReducedMotion_whenTheRevealHasRun_thenTheWordsStayOnThePage() {
        assertThat(wordsVisible(revealed = true, reduceMotion = true)).isTrue()
    }
}
