package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HelloBeFocusTest {
    @Test
    fun givenStandardContrast_whenFocusTokensAreRead_thenRingWidthIsFourDp() {
        val focus = helloBeFocus(highContrast = false)

        assertThat(focus.ringWidth).isEqualTo(4.dp)
        assertThat(focus.guardWidth).isEqualTo(2.dp)
        assertThat(focus.selectionWidth).isEqualTo(3.dp)
        assertThat(focus.clearance).isEqualTo(12.dp)
    }

    @Test
    fun givenHighContrast_whenFocusTokensAreRead_thenRingWidthIsSixDp() {
        val focus = helloBeFocus(highContrast = true)

        assertThat(focus.ringWidth).isEqualTo(6.dp)
        assertThat(focus.selectionWidth).isEqualTo(5.dp)
    }

    @Test
    fun givenFocusTokens_whenScaleValuesAreRead_thenTheyMatchTheApprovedFocusScales() {
        val focus = helloBeFocus(highContrast = false)

        assertThat(focus.scaleButton).isEqualTo(1.04f)
        assertThat(focus.scaleCard).isEqualTo(1.05f)
        assertThat(focus.scaleLargeCard).isEqualTo(1.025f)
        assertThat(focus.pressScale).isEqualTo(0.98f)
    }
}
