package com.nphkhiem.englishforyourchildren.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Which language the caregiver area speaks, read back from what was stored.
 *
 * Three modes rather than a locale tag, because the approved settings screen offers three and one
 * of them is both at once. A field named for a locale holding "both" would be a name that lies.
 */
class CaregiverLanguageTest {

    @Test
    fun givenATelevisionSetToVietnamese_whenItIsReadBack_thenThatIsWhatItSays() {
        assertThat(CaregiverLanguage.from("vi")).isEqualTo(CaregiverLanguage.VIETNAMESE)
    }

    @Test
    fun givenATelevisionSetToEnglish_whenItIsReadBack_thenThatIsWhatItSays() {
        assertThat(CaregiverLanguage.from("en")).isEqualTo(CaregiverLanguage.ENGLISH)
    }

    @Test
    fun givenATelevisionSetToBoth_whenItIsReadBack_thenThatIsWhatItSays() {
        assertThat(CaregiverLanguage.from(CaregiverLanguage.BOTH.stored))
            .isEqualTo(CaregiverLanguage.BOTH)
    }

    @Test
    fun givenNobodyHasChosen_whenItIsReadBack_thenBothLanguagesAreShown() {
        // The mode that cannot strand a caregiver who reads only one of the two.
        assertThat(CaregiverLanguage.from(null)).isEqualTo(CaregiverLanguage.BOTH)
    }

    @Test
    fun givenSomethingNoVersionOfThisAppEverWrote_whenItIsReadBack_thenItFallsBack() {
        // A preference is not a child's work. One that will not read falls back to a usable
        // default, which is the same choice the settings file's corruption handler already made.
        assertThat(CaregiverLanguage.from("kl")).isEqualTo(CaregiverLanguage.BOTH)
    }
}
