package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class LetterAndSoundRulesTest {

    private val originalLocale: Locale = Locale.getDefault()

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun givenALowercaseLetter_whenThePairIsBuilt_thenBothCasesAppearUppercaseFirst() {
        assertThat(letterPair("a")).isEqualTo("A a")
    }

    @Test
    fun givenAnUppercaseLetter_whenThePairIsBuilt_thenTheResultIsTheSame() {
        // Casing is the activity's guarantee, not something a fixture author has to get right.
        assertThat(letterPair("A")).isEqualTo(letterPair("a"))
    }

    @Test
    fun givenATurkishDefaultLocale_whenThePairIsBuilt_thenTheDottedCapitalNeverAppears() {
        // The default locale turns "i" into "İ" in Turkish. This app already ships a second
        // language, so the conversion is pinned rather than left to whatever the device is set to.
        Locale.setDefault(Locale.forLanguageTag("tr"))

        assertThat(letterPair("i")).isEqualTo("I i")
    }

    @Test
    fun givenATwoLetterSound_whenThePairIsBuilt_thenBothLettersCarryTheirCase() {
        // A digraph is one sound, so it stays one learning object rather than being split.
        assertThat(letterPair("ch")).isEqualTo("CH ch")
    }
}
