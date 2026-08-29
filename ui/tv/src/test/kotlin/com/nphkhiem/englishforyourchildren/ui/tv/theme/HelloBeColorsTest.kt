package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.jupiter.api.Test

class HelloBeColorsTest {
    @Test
    fun givenDayTheme_whenColorsAreRead_thenDaySemanticPaletteIsReturned() {
        val colors = helloBeColors(mode = HelloBeThemeMode.DAY)

        assertThat(colors.canvas).isEqualTo(Color(0xFFF8F1E7))
        assertThat(colors.textPrimary).isEqualTo(Color(0xFF102F4C))
        assertThat(colors.actionPrimary).isEqualTo(Color(0xFF123A5A))
        assertThat(colors.focusRing).isEqualTo(Color(0xFF000000))
        assertThat(colors.accentGrowth).isEqualTo(Color(0xFF3F7A60))
    }

    @Test
    fun givenNightTheme_whenColorsAreRead_thenNightSemanticPaletteIsReturned() {
        val colors = helloBeColors(mode = HelloBeThemeMode.NIGHT)

        assertThat(colors.canvas).isEqualTo(Color(0xFF0A2136))
        assertThat(colors.textPrimary).isEqualTo(Color(0xFFFFF5E7))
        assertThat(colors.actionPrimary).isEqualTo(Color(0xFF173F5A))
        assertThat(colors.focusRing).isEqualTo(Color(0xFF000000))
        assertThat(colors.accentGrowth).isEqualTo(Color(0xFF72B68F))
    }

    @Test
    fun givenDayTheme_whenSelectionColorsAreRead_thenSelectionUsesTheActionFamilyNotGrowth() {
        val colors = helloBeColors(mode = HelloBeThemeMode.DAY)

        assertThat(colors.actionSelected).isEqualTo(Color(0xFF2F6187))
        assertThat(colors.onSelected).isEqualTo(Color(0xFFFFF9F0))
        assertThat(colors.actionSelectedBorder).isEqualTo(Color(0xFF7FA8C6))
        assertThat(colors.actionSelected).isNotEqualTo(colors.accentGrowth)
    }

    @Test
    fun givenNightTheme_whenSelectionColorsAreRead_thenSelectionUsesTheActionFamilyNotGrowth() {
        val colors = helloBeColors(mode = HelloBeThemeMode.NIGHT)

        assertThat(colors.actionSelected).isEqualTo(Color(0xFFA9C8DE))
        assertThat(colors.onSelected).isEqualTo(Color(0xFF102F4C))
        assertThat(colors.actionSelectedBorder).isEqualTo(Color(0xFF4A7EA3))
        assertThat(colors.actionSelected).isNotEqualTo(colors.accentGrowth)
    }

    @Test
    fun givenEitherTheme_whenSelectionLabelContrastIsMeasured_thenItPassesAaText() {
        for (mode in HelloBeThemeMode.entries) {
            val colors = helloBeColors(mode = mode)

            assertThat(contrastRatio(colors.actionSelected, colors.onSelected)).isGreaterThan(4.5)
        }
    }

    @Test
    fun givenEitherTheme_whenSelectedAndFocusedFillsAreCompared_thenTheyAreDistinguishable() {
        for (mode in HelloBeThemeMode.entries) {
            val colors = helloBeColors(mode = mode)

            assertThat(colors.actionSelected).isNotEqualTo(colors.actionPrimaryFocused)
            assertThat(contrastRatio(colors.actionSelected, colors.actionSecondary))
                .isGreaterThan(3.0)
            assertThat(contrastRatio(colors.actionSelected, colors.actionPrimaryFocused))
                .isGreaterThan(2.0)
        }
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

    @Test
    fun givenAnyPalette_whenQuietTextSitsOnAnySurface_thenItPassesAaText() {
        // The recorded ratios only ever measured tertiary text on the primary surface, so the
        // pairing that actually ships, a 14sp bold label on the canvas, went unmeasured and sat
        // at 4.30. Fourteen bold is not WCAG large text, so every quiet role is checked here
        // against every surface it can be drawn on.
        for (mode in HelloBeThemeMode.entries) {
            for (highContrast in listOf(false, true)) {
                val colors = helloBeColors(mode = mode, highContrast = highContrast)

                for (surface in colors.surfaces()) {
                    assertThat(contrastRatio(colors.textSecondary, surface)).isGreaterThan(4.5)
                    assertThat(contrastRatio(colors.textTertiary, surface)).isGreaterThan(4.5)
                }
            }
        }
    }

    @Test
    fun givenHighContrast_whenQuietTextIsRead_thenItIsStrongerAndTheThreeLevelsRemain() {
        for (mode in HelloBeThemeMode.entries) {
            val standard = helloBeColors(mode = mode)
            val strengthened = helloBeColors(mode = mode, highContrast = true)

            assertThat(contrastRatio(strengthened.textSecondary, strengthened.canvas))
                .isGreaterThan(contrastRatio(standard.textSecondary, standard.canvas))
            assertThat(contrastRatio(strengthened.textTertiary, strengthened.canvas))
                .isGreaterThan(contrastRatio(standard.textTertiary, standard.canvas))

            assertThat(strengthened.textPrimary).isNotEqualTo(strengthened.textSecondary)
            assertThat(strengthened.textSecondary).isNotEqualTo(strengthened.textTertiary)
        }
    }

    /** Every surface a label can be drawn on, decorative scenery included. */
    private fun HelloBeColors.surfaces(): List<Color> =
        listOf(canvas, scenery, surfacePrimary, surfaceRaised, surfaceSoft, surfaceMuted)

    /** WCAG 2.1 relative luminance, used to keep the palette's contrast claims executable. */
    private fun relativeLuminance(color: Color): Double {
        fun channel(value: Float): Double {
            val srgb = value.toDouble()
            return if (srgb <= 0.03928) srgb / 12.92 else ((srgb + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val a = relativeLuminance(first)
        val b = relativeLuminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    @Test
    fun givenEitherTheme_whenFocusIsRendered_thenTheStrokeDefinesTheFillAndTheLabelStaysReadable() {
        for (mode in HelloBeThemeMode.entries) {
            val colors = helloBeColors(mode = mode)

            // The stroke has to carve the fill out from its surroundings...
            assertThat(contrastRatio(colors.focusRing, colors.focusFill)).isGreaterThan(3.0)
            // ...and the label on that fill is text, so it takes the text bar.
            assertThat(contrastRatio(colors.focusFill, colors.onFocusFill)).isGreaterThan(4.5)
        }
    }

    @Test
    fun givenEitherTheme_whenFocusSitsOnTheCanvas_thenAtLeastOneHalfOfItCarriesTheContrast() {
        // Neither half clears 3:1 on both canvases alone: the yellow fill is only 1.43 against
        // Day's cream, and the black stroke is only 1.28 against Night's navy. The treatment
        // works because on each theme the other half carries it, so assert exactly that.
        for (mode in HelloBeThemeMode.entries) {
            val colors = helloBeColors(mode = mode)

            val fillAgainstCanvas = contrastRatio(colors.focusFill, colors.canvas)
            val strokeAgainstCanvas = contrastRatio(colors.focusRing, colors.canvas)

            assertThat(maxOf(fillAgainstCanvas, strokeAgainstCanvas)).isGreaterThan(3.0)
        }
    }
}
