package com.nphkhiem.englishforyourchildren.domain

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DomainModuleIsolationTest {
    @Test
    fun givenDomainClasspath_whenAndroidApplicationRequested_thenAndroidTypesAreUnavailable() {
        val androidTypeLookup = runCatching { Class.forName("android.app.Application") }

        assertThat(androidTypeLookup.isFailure).isTrue()
    }
}
