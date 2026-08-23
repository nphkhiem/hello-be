package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HelloBeElevationTest {
    @Test
    fun givenDayElevation_whenMediumLevelIsRead_thenOffsetBlurAndColorMatchTokenSpec() {
        val elevation = helloBeElevation(mode = HelloBeThemeMode.DAY)

        assertThat(elevation.medium.offsetY).isEqualTo(10.dp)
        assertThat(elevation.medium.blurRadius).isEqualTo(24.dp)
        assertThat(
            elevation.medium.color
        ).isEqualTo(Color(red = 0x10, green = 0x2F, blue = 0x4C, alpha = 0x24))
    }

    @Test
    fun givenNightElevation_whenMediumLevelIsRead_thenOffsetBlurAndColorMatchTokenSpec() {
        val elevation = helloBeElevation(mode = HelloBeThemeMode.NIGHT)

        assertThat(elevation.medium.offsetY).isEqualTo(10.dp)
        assertThat(elevation.medium.blurRadius).isEqualTo(28.dp)
        assertThat(
            elevation.medium.color
        ).isEqualTo(Color(red = 0x00, green = 0x00, blue = 0x00, alpha = 0x59))
    }

    @Test
    fun givenDayElevation_whenFocusGlowIsRead_thenItUsesTheGoldFocusAccent() {
        val elevation = helloBeElevation(mode = HelloBeThemeMode.DAY)

        assertThat(elevation.focusGlow.blurRadius).isEqualTo(24.dp)
        assertThat(
            elevation.focusGlow.color
        ).isEqualTo(Color(red = 0xFF, green = 0xC2, blue = 0x47, alpha = 0x52))
    }
}
