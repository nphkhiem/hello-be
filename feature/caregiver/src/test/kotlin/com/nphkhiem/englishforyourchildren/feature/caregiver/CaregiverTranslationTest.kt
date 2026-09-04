package com.nphkhiem.englishforyourchildren.feature.caregiver

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.jupiter.api.Test

/**
 * What the caregiver area can say in Vietnamese, and what it still cannot.
 *
 * The Vietnamese file is deliberately incomplete: most of it is waiting for a native speaker, and
 * writing it anyway is the mistake this project already refused once over the Vietnamese
 * recordings. What must not happen is the gap growing quietly. So the file lists the keys it has no
 * translation for, and this holds that list to the truth in both directions: a new English string
 * nobody has thought about fails here, and so does a translation somebody wrote without crossing
 * the key off.
 */
class CaregiverTranslationTest {
    @Test
    fun givenTheVietnameseFile_whenItsKeysAreRead_thenNoneOfThemIsAStringNobodyAsksFor() {
        // An orphan is a translation of something that no longer exists, which reads as coverage
        // and is not.
        val orphans = translated() - english().keys

        assertThat(orphans).isEmpty()
    }

    @Test
    fun givenTheVietnameseFile_whenItSaysWhatIsMissing_thenThatIsExactlyWhatIsMissing() {
        val untranslated = english().keys - translated()

        assertThat(listedAsUntranslated()).isEqualTo(untranslated)
    }

    @Test
    fun givenACaregiverReadingVietnamese_whenAStringHasNoTranslation_thenEnglishStandsInForIt() {
        // Android resolves an untranslated key to the default file, so the gap shows as English
        // rather than as nothing. That is the whole reason shipping the gap is tolerable, and it
        // stops being true the moment a key exists only in the Vietnamese file.
        assertThat(translated() - english().keys).isEmpty()
        assertThat(english()).isNotEmpty()
    }

    private fun english(): Map<String, String> = strings(File(VALUES, "strings.xml"))

    private fun translated(): Set<String> = strings(File(VALUES_VI, "strings.xml")).keys

    /** Only real entries: anything inside a comment is a note, not a string the app can resolve. */
    private fun strings(file: File): Map<String, String> {
        val withoutComments = COMMENT.replace(file.readText(), "")
        return ENTRY.findAll(withoutComments).associate { it.groupValues[1] to it.groupValues[2] }
    }

    /** The keys the Vietnamese file admits it has no translation for. */
    private fun listedAsUntranslated(): Set<String> {
        val text = File(VALUES_VI, "strings.xml").readText()
        return COMMENT.findAll(text)
            .flatMap { LISTED.findAll(it.value) }
            .map { it.groupValues[1] }
            .toSet()
    }

    private companion object {
        val VALUES = File("src/main/res/values")
        val VALUES_VI = File("src/main/res/values-vi")
        val ENTRY = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val COMMENT = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)

        /** A line in the waiting list: a key, a colon, and the English it stands for. */
        val LISTED = Regex("""^\s{8}([a-z0-9_]+): """, RegexOption.MULTILINE)
    }
}
