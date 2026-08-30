package com.nphkhiem.englishforyourchildren.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IdentifiersTest {
    @Test
    fun givenABlankValue_whenAnyIdentifierIsCreated_thenItIsRejected() {
        // Every identifier is listed here on purpose. A new one that forgets its invariant is a
        // new one that is missing from this list, which is easier to notice in a test than in a
        // value class that looks like all the others.
        for ((name, create) in stringIdentifiers) {
            for (blank in BLANKS) {
                assertThrows<IllegalArgumentException>("$name accepted \"$blank\"") {
                    create(blank)
                }
            }
        }
    }

    @Test
    fun givenAValue_whenAnyIdentifierIsCreated_thenItKeepsItExactly() {
        // An identifier is opaque: it does not trim, case-fold or otherwise improve what it holds,
        // because two ids that differ only by whitespace are two different rows somewhere.
        for ((name, create) in stringIdentifiers) {
            assertThat(create(VALUE).toString()).contains(VALUE)
            assertThat(name).isNotEmpty()
        }
    }

    @Test
    fun givenANegativeInstant_whenEpochMillisIsCreated_thenItIsRejected() {
        assertThrows<IllegalArgumentException> { EpochMillis(-1) }
    }

    @Test
    fun givenTheEpochItself_whenEpochMillisIsCreated_thenItIsAccepted() {
        // Zero is a real instant, not a missing one. A model that wants "no time yet" says so with
        // null rather than with a sentinel.
        assertThat(EpochMillis(0).value).isEqualTo(0)
    }

    private companion object {
        const val VALUE = "my-home-lesson-1"
        val BLANKS = listOf("", " ", "\t", "\n", "   ")

        val stringIdentifiers: List<Pair<String, (String) -> Any>> = listOf(
            "ProfileId" to { value: String -> ProfileId(value) },
            "AvatarId" to { value: String -> AvatarId(value) },
            "CourseId" to { value: String -> CourseId(value) },
            "CourseVersion" to { value: String -> CourseVersion(value) },
            "UnitId" to { value: String -> UnitId(value) },
            "LessonId" to { value: String -> LessonId(value) },
            "ActivityId" to { value: String -> ActivityId(value) },
            "ActivityInstanceId" to { value: String -> ActivityInstanceId(value) },
            "SessionId" to { value: String -> SessionId(value) },
            "SkillId" to { value: String -> SkillId(value) },
            "ShelfId" to { value: String -> ShelfId(value) }
        )
    }
}
