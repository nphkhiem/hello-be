package com.nphkhiem.englishforyourchildren.feature.caregiver

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * The stop condition, enforced rather than remembered.
 *
 * Profile management shows profiles and `:feature:profiles` already has screens for them, so the
 * pull towards importing one is real. The task forbids it: features talk through shared models and
 * `:ui:tv` primitives, never to each other. A build file can be edited without anyone noticing,
 * so this asserts the classpath itself, the same way `:domain` asserts it has no Android types.
 */
class CaregiverModuleIsolationTest {

    @Test
    fun givenCaregiverClasspath_whenTheProfilesFeatureIsRequested_thenItIsUnavailable() {
        val lookup = runCatching {
            Class.forName(
                "com.nphkhiem.englishforyourchildren.feature.profiles.ProfilePickerUiState"
            )
        }

        assertThat(lookup.isFailure).isTrue()
    }

    @Test
    fun givenCaregiverClasspath_whenTheLearningFeatureIsRequested_thenItIsUnavailable() {
        val lookup = runCatching {
            Class.forName("com.nphkhiem.englishforyourchildren.feature.learning.LessonUiState")
        }

        assertThat(lookup.isFailure).isTrue()
    }
}
