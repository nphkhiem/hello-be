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
    fun givenBothLanguagesAreWanted_whenContentIsRead_thenBothAreThereAndSoIsTheMiddleDot() {
        // The same join the approved caregiver strings used when they carried both languages
        // themselves, so an authored suggestion and the heading above it read as one thing.
        assertThat(CaregiverLanguage.BOTH.read("Touch and name", "Chạm và gọi tên"))
            .isEqualTo("Touch and name · Chạm và gọi tên")
    }

    @Test
    fun givenOneLanguageIsWanted_whenContentIsRead_thenOnlyThatOneIsThere() {
        assertThat(CaregiverLanguage.ENGLISH.read("Touch and name", "Chạm và gọi tên"))
            .isEqualTo("Touch and name")
        assertThat(CaregiverLanguage.VIETNAMESE.read("Touch and name", "Chạm và gọi tên"))
            .isEqualTo("Chạm và gọi tên")
    }

    @Test
    fun givenContentNobodyHasTranslated_whenVietnameseIsWanted_thenTheEnglishStandsAlone() {
        // Words a caregiver may not read beat an empty line. This is what makes it safe to package
        // a unit before a translator has seen it.
        assertThat(CaregiverLanguage.VIETNAMESE.read("Touch and name", "")).isEqualTo(
            "Touch and name"
        )
    }

    @Test
    fun givenContentNobodyHasTranslated_whenBothAreWanted_thenNothingIsSaidTwice() {
        // Joining a sentence to itself reads as a stutter rather than as bilingual.
        assertThat(CaregiverLanguage.BOTH.read("Touch and name", "")).isEqualTo("Touch and name")
        assertThat(CaregiverLanguage.BOTH.read("Touch and name", "Touch and name"))
            .isEqualTo("Touch and name")
    }

    @Test
    fun givenSomethingNoVersionOfThisAppEverWrote_whenItIsReadBack_thenItFallsBack() {
        // A preference is not a child's work. One that will not read falls back to a usable
        // default, which is the same choice the settings file's corruption handler already made.
        assertThat(CaregiverLanguage.from("kl")).isEqualTo(CaregiverLanguage.BOTH)
    }
}
