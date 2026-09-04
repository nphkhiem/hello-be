package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import org.junit.Test

/**
 * What a caregiver actually reads.
 *
 * Instrumented because this is about resource resolution, which is the part that cannot be
 * reasoned about from a string table: whether `values-vi` is really reached, and what Android hands
 * back for a key that has no translation yet.
 */
class CaregiverTextTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenEnglishIsChosen_whenATranslatedStringIsRead_thenOnlyEnglishComesBack() {
        val read = context.caregiverText(CaregiverLanguage.ENGLISH, R.string.gate_title)

        assertThat(read).isEqualTo(ENGLISH_TITLE)
    }

    @Test
    fun givenVietnameseIsChosen_whenATranslatedStringIsRead_thenOnlyVietnameseComesBack() {
        // Proves `values-vi` is genuinely reached. A configuration context that silently fell back
        // would return the English here and look like a passing test of nothing.
        val read = context.caregiverText(CaregiverLanguage.VIETNAMESE, R.string.gate_title)

        assertThat(read).isEqualTo(VIETNAMESE_TITLE)
    }

    @Test
    fun givenBothAreChosen_whenATranslatedStringIsRead_thenItCarriesEachLanguageOnce() {
        val read = context.caregiverText(CaregiverLanguage.BOTH, R.string.gate_title)

        assertThat(read).isEqualTo("$ENGLISH_TITLE · $VIETNAMESE_TITLE")
    }

    @Test
    fun givenBothAreChosen_whenAnUntranslatedStringIsRead_thenEnglishStandsAloneRatherThanTwice() {
        // The thing that makes an unfinished `values-vi` safe to ship. Most keys have no Vietnamese
        // yet, Android hands back the English for them, and joining a sentence to itself would read
        // as a stutter rather than as two languages.
        val english = context.caregiverText(CaregiverLanguage.ENGLISH, R.string.caregiver_overview)

        val both = context.caregiverText(CaregiverLanguage.BOTH, R.string.caregiver_overview)

        assertThat(both).isEqualTo(english)
        assertThat(both).doesNotContain("·")
    }

    @Test
    fun givenAStringTakingAnArgument_whenItIsRead_thenTheArgumentSurvivesTheLanguageChoice() {
        val read = context.caregiverText(
            CaregiverLanguage.ENGLISH,
            R.string.caregiver_rail_title,
            arrayOf(CHILD)
        )

        assertThat(read).contains(CHILD)
    }

    private companion object {
        const val ENGLISH_TITLE = "Grown-ups only"
        const val VIETNAMESE_TITLE = "Dành cho người lớn"
        const val CHILD = "Bé"
    }
}
