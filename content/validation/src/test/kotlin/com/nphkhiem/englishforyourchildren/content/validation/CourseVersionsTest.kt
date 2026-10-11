package com.nphkhiem.englishforyourchildren.content.validation

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import org.junit.jupiter.api.Test

class CourseVersionsTest {

    @Test
    fun givenDottedVersions_whenCompared_thenNumbersCountNotCharacters() {
        assertThat(CourseVersion("2026.10").isNewerThan(CourseVersion("2026.09"))).isTrue()
        assertThat(CourseVersion("1.10.0").isNewerThan(CourseVersion("1.9.0"))).isTrue()
        assertThat(CourseVersion("1.1").isNewerThan(CourseVersion("1.1.0"))).isFalse()
        assertThat(CourseVersion("2026.09").isNewerThan(CourseVersion("2026.09"))).isFalse()
        assertThat(CourseVersion("next").isNewerThan(CourseVersion("2026.09"))).isFalse()
    }
}
