package com.nphkhiem.englishforyourchildren.feature.caregiver

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.jupiter.api.Test

/**
 * What the caregiver area can say in Vietnamese, and what it still cannot.
 *
 * The Vietnamese file is deliberately incomplete: most of it is waiting for a native speaker, and
 * writing it anyway is the mistake this project already refused once over the Vietnamese
 * recordings. Every key is present and the ones nobody has translated say [UNTRANSLATED], which is
 * what a caregiver reading Vietnamese would actually be shown, so the gap is visible rather than
 * hidden behind an English fallback.
 *
 * What must not happen is the gap growing quietly, so the count is pinned here. It only ever goes
 * down: translating one fails this test until the number follows, and a new English string arriving
 * without a Vietnamese decision beside it fails it too.
 */
class CaregiverTranslationTest {
    @Test
    fun givenTheVietnameseFile_whenItsKeysAreRead_thenNoneOfThemIsAStringNobodyAsksFor() {
        // An orphan is a translation of something that no longer exists, which reads as coverage
        // and is not.
        val orphans = translated().keys - english().keys

        assertThat(orphans).isEmpty()
    }

    @Test
    fun givenAnyEnglishString_whenTheVietnameseFileIsRead_thenItHasALineOfItsOwn() {
        // Present but untranslated, rather than absent. An absent key falls back to English, which
        // reads as though somebody decided it needed no translation.
        val missing = english().keys - translated().keys

        assertThat(missing).isEmpty()
    }

    @Test
    fun givenTheStringsNobodyHasTranslated_whenTheyAreCounted_thenTheNumberHasNotGrown() {
        // The ratchet, and deliberately an upper bound rather than an exact count. Somebody working
        // through the backlog should not have to edit this file every time they finish a line; what
        // must not happen is a new English string arriving and quietly joining the gap.
        val waiting = translated().filterValues { it == UNTRANSLATED }.keys

        assertThat(waiting.size).isAtMost(AWAITING_A_NATIVE_SPEAKER)
    }

    @Test
    fun givenAnyVietnameseString_whenItIsRead_thenItSaysSomething() {
        // A blank value is the one state worse than an untranslated one. `TBU` is visible and
        // findable, and an absent key falls back to English, but an empty string shows a caregiver
        // nothing at all and looks like a screen that failed to load.
        val blank = translated().filterValues { it.isBlank() }.keys

        assertThat(blank).isEmpty()
    }

    private fun english(): Map<String, String> = strings(File(VALUES, "strings.xml"))

    private fun translated(): Map<String, String> = strings(File(VALUES_VI, "strings.xml"))

    /** Only real entries: anything inside a comment is a note, not a string the app can resolve. */
    private fun strings(file: File): Map<String, String> {
        val withoutComments = COMMENT.replace(file.readText(), "")
        return ENTRY.findAll(withoutComments).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private companion object {
        val VALUES = File("src/main/res/values")
        val VALUES_VI = File("src/main/res/values-vi")
        val ENTRY = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val COMMENT = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)

        /** What a string says while it is waiting for somebody who speaks the language. */
        const val UNTRANSLATED = "TBU"

        /** An upper bound, not a tally. It comes down as the backlog does. */
        const val AWAITING_A_NATIVE_SPEAKER = 69
    }
}
