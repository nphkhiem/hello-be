package com.nphkhiem.englishforyourchildren.feature.caregiver

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CaregiverOverviewRulesTest {

    @Test
    fun givenMoreSummariesThanFit_whenTheyAreDrawn_thenOnlyThreeAreKept() {
        val many = (1..7).map { summary("Label $it") }

        assertThat(summariesShown(many)).hasSize(3)
    }

    @Test
    fun givenThreeOrFewerSummaries_whenTheyAreDrawn_thenAllAreKeptInOrder() {
        val two = listOf(summary("Adventures finished"), summary("Words encountered"))

        assertThat(summariesShown(two)).isEqualTo(two)
    }

    @Test
    fun givenALongHistoryOfWords_whenTheyAreDrawn_thenOnlySixAreKept() {
        // The stop condition forbids an infinite history, so the screen is made unable to draw one.
        val many = (1..40).map { "word$it" }

        assertThat(recentWordsShown(many)).hasSize(6)
    }

    @Test
    fun givenALongHistoryOfWords_whenTheyAreDrawn_thenTheMostRecentSurvive() {
        val many = listOf("newest", "second", "third", "fourth", "fifth", "sixth", "oldest")

        assertThat(recentWordsShown(many)).doesNotContain("oldest")
        assertThat(recentWordsShown(many).first()).isEqualTo("newest")
    }

    @Test
    fun givenFewerWordsThanTheBound_whenTheyAreDrawn_thenAllAreKept() {
        val few = listOf("eyes", "hands")

        assertThat(recentWordsShown(few)).isEqualTo(few)
    }

    @Test
    fun givenPractice_whenTheProgressIsRead_thenThereIsSomethingToDescribe() {
        val practiced = OverviewProgress.Practiced(
            summaries = listOf(summary("Adventures finished")),
            recentWords = listOf("eyes")
        )

        assertThat(hasPractice(practiced)).isTrue()
    }

    @Test
    fun givenANewProfileOrAQuietWeek_whenTheProgressIsRead_thenThereIsNothingToDescribe() {
        // Different words for a caregiver, the same absence of a panel.
        assertThat(hasPractice(OverviewProgress.NewProfile)).isFalse()
        assertThat(hasPractice(OverviewProgress.NothingRecent)).isFalse()
    }

    private fun summary(label: String) =
        OverviewSummary(label = label, value = "3", note = "Not a test score")
}
