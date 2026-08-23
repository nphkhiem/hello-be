package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HelloBeColorsTest {
    @Test
    fun givenDayTheme_whenColorsAreRead_thenDaySemanticPaletteIsReturned() {
        val colors = helloBeColors(mode = HelloBeThemeMode.DAY)

        assertThat(colors.canvas).isEqualTo(Color(0xFFF8F1E7))
        assertThat(colors.textPrimary).isEqualTo(Color(0xFF102F4C))
        assertThat(colors.actionPrimary).isEqualTo(Color(0xFF123A5A))
        assertThat(colors.focusRing).isEqualTo(Color(0xFFFFC247))
        assertThat(colors.accentGrowth).isEqualTo(Color(0xFF3F7A60))
    }

    @Test
    fun givenNightTheme_whenColorsAreRead_thenNightSemanticPaletteIsReturned() {
        val colors = helloBeColors(mode = HelloBeThemeMode.NIGHT)

        assertThat(colors.canvas).isEqualTo(Color(0xFF0A2136))
        assertThat(colors.textPrimary).isEqualTo(Color(0xFFFFF5E7))
        assertThat(colors.actionPrimary).isEqualTo(Color(0xFF173F5A))
        assertThat(colors.focusRing).isEqualTo(Color(0xFFFFD56A))
        assertThat(colors.accentGrowth).isEqualTo(Color(0xFF72B68F))
    }

    @Test
    fun givenHighContrastDayTheme_whenColorsAreRead_thenBorderSecondaryMatchesBorderPrimary() {
        val colors = helloBeColors(mode = HelloBeThemeMode.DAY, highContrast = true)

        assertThat(colors.borderSecondary).isEqualTo(colors.borderPrimary)
        assertThat(colors.scrim.alpha).isEqualTo(1f)
    }

    @Test
    fun givenStandardDayTheme_whenColorsAreRead_thenBorderSecondaryIsQuieterThanPrimary() {
        val colors = helloBeColors(mode = HelloBeThemeMode.DAY, highContrast = false)

        assertThat(colors.borderSecondary).isNotEqualTo(colors.borderPrimary)
        assertThat(colors.scrim.alpha).isLessThan(1f)
    }
}
