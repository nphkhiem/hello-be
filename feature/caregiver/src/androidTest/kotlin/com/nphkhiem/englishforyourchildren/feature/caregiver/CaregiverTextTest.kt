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

        assertThat(read).isEqualTo(context.resources.getString(R.string.gate_title))
    }

    @Test
    fun givenVietnameseIsChosen_whenATranslatedStringIsRead_thenValuesViIsReallyReached() {
        // Deliberately not pinned to the wording. The Vietnamese is being written and rewritten by
        // the person who speaks it, and a test that named the sentence would break every time they
        // improved it while proving nothing more than this does: that a configuration context
        // really reaches `values-vi` rather than quietly falling back to the default file.
        val english = context.caregiverText(CaregiverLanguage.ENGLISH, R.string.gate_title)

        val vietnamese = context.caregiverText(CaregiverLanguage.VIETNAMESE, R.string.gate_title)

        assertThat(vietnamese).isNotEmpty()
        assertThat(vietnamese).isNotEqualTo(english)
    }

    @Test
    fun givenBothAreChosen_whenATranslatedStringIsRead_thenItCarriesEachLanguageOnce() {
        val english = context.caregiverText(CaregiverLanguage.ENGLISH, R.string.gate_title)
        val vietnamese = context.caregiverText(CaregiverLanguage.VIETNAMESE, R.string.gate_title)

        val both = context.caregiverText(CaregiverLanguage.BOTH, R.string.gate_title)

        assertThat(both).isEqualTo("$english · $vietnamese")
    }

    @Test
    fun givenAStringThatReadsTheSameInEitherLanguage_whenBothAreChosen_thenItIsNotSaidTwice() {
        // A language names itself the same way whoever is reading, so this one is identical in the
        // two files. Joining a sentence to itself would read as a stutter, and the same guard is
        // what let an unfinished values-vi ship earlier in this branch.
        val english =
            context.caregiverText(CaregiverLanguage.ENGLISH, R.string.settings_language_vietnamese)

        val both =
            context.caregiverText(CaregiverLanguage.BOTH, R.string.settings_language_vietnamese)

        assertThat(both).isEqualTo(english)
        assertThat(both).doesNotContain("·")
    }

    @Test
    fun givenTheGateQuestion_whenBothLanguagesAreChosen_thenTheSumIsAskedOnce() {
        // The sum is the question, and arithmetic reads the same to either reader. It used to be
        // "What is %1$d + %2$d?", which the bilingual join turned into the numbers twice over:
        // "What is 19 + 44? · 19 + 44 bằng bao nhiêu?". Written the same way in both files, it is
        // asked once.
        val both = context.caregiverText(CaregiverLanguage.BOTH, R.string.gate_question, 19, 44)

        assertThat(both).isEqualTo("19 + 44 = ?")
        assertThat(both).doesNotContain("·")
    }

    @Test
    fun givenAStringTakingAnArgument_whenItIsRead_thenTheArgumentSurvivesTheLanguageChoice() {
        val read =
            context.caregiverText(CaregiverLanguage.BOTH, R.string.caregiver_rail_title, CHILD)

        assertThat(read).contains(CHILD)
    }

    private companion object {
        const val CHILD = "Bé"
    }
}
