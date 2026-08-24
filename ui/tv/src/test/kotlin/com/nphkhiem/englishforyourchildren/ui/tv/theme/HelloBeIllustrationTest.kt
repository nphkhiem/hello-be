package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HelloBeIllustrationTest {
    @Test
    fun givenIllustrationTokens_whenRead_thenTheyMatchTheApprovedValues() {
        assertThat(HelloBeIllustration.strokeReference).isEqualTo(3.dp)
        assertThat(HelloBeIllustration.detailMinimum).isEqualTo(4.dp)
    }
}
