package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HelloBeTypographyTest {
    @Test
    fun givenDefaultTypography_whenDisplayHeroIsRead_thenSizeLineHeightAndWeightMatchTokenSpec() {
        val typography = helloBeTypography()

        assertThat(typography.displayHero.fontSize).isEqualTo(48.sp)
        assertThat(typography.displayHero.lineHeight).isEqualTo(52.sp)
        assertThat(typography.displayHero.fontWeight).isEqualTo(FontWeight(800))
    }

    @Test
    fun givenDefaultTypography_whenLearningGlyphIsRead_thenLargeSingleGlyphSizeIsUsed() {
        val typography = helloBeTypography()

        assertThat(typography.learningGlyph.fontSize).isEqualTo(96.sp)
        assertThat(typography.learningGlyph.lineHeight).isEqualTo(104.sp)
        assertThat(typography.learningGlyph.fontWeight).isEqualTo(FontWeight(800))
    }

    @Test
    fun givenDefaultTypography_whenBodyMediumIsRead_thenCaregiverBodySizeMatchesTokenSpec() {
        val typography = helloBeTypography()

        assertThat(typography.bodyMedium.fontSize).isEqualTo(18.sp)
        assertThat(typography.bodyMedium.lineHeight).isEqualTo(26.sp)
        assertThat(typography.bodyMedium.fontWeight).isEqualTo(FontWeight(450))
    }

    @Test
    fun givenDefaultTypography_whenTrackingIsCompared_thenOnlyDisplayRolesUseTightTracking() {
        val typography = helloBeTypography()

        assertThat(typography.displayHero.letterSpacing).isEqualTo((-0.02).em)
        assertThat(typography.displayLessonPrompt.letterSpacing).isEqualTo((-0.02).em)
        assertThat(typography.bodyMedium.letterSpacing).isEqualTo(0.em)
        assertThat(typography.labelLarge.letterSpacing).isEqualTo(0.em)
    }

    @Test
    fun givenCustomFontFamily_whenTypographyIsBuilt_thenEveryRoleUsesTheProvidedFamily() {
        val customFamily = HelloBeFontFamily.fallback
        val typography = helloBeTypography(fontFamily = customFamily)

        assertThat(typography.displayHero.fontFamily).isEqualTo(customFamily)
        assertThat(typography.caption.fontFamily).isEqualTo(customFamily)
        assertThat(typography.learningGlyph.fontFamily).isEqualTo(customFamily)
    }
}
