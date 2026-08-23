package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class HelloBeElevationLevel(val offsetY: Dp, val blurRadius: Dp, val color: Color)

@Immutable
data class HelloBeElevation(
    val none: HelloBeElevationLevel,
    val low: HelloBeElevationLevel,
    val medium: HelloBeElevationLevel,
    val high: HelloBeElevationLevel,
    val focusGlow: HelloBeElevationLevel
)

private val noElevation =
    HelloBeElevationLevel(offsetY = 0.dp, blurRadius = 0.dp, color = Color.Transparent)

private val dayElevation =
    HelloBeElevation(
        none = noElevation,
        low =
            HelloBeElevationLevel(
                offsetY = 4.dp,
                blurRadius = 10.dp,
                color = Color(red = 0x10, green = 0x2F, blue = 0x4C, alpha = 0x14)
            ),
        medium =
            HelloBeElevationLevel(
                offsetY = 10.dp,
                blurRadius = 24.dp,
                color = Color(red = 0x10, green = 0x2F, blue = 0x4C, alpha = 0x24)
            ),
        high =
            HelloBeElevationLevel(
                offsetY = 20.dp,
                blurRadius = 56.dp,
                color = Color(red = 0x08, green = 0x2A, blue = 0x4A, alpha = 0x45)
            ),
        focusGlow =
            HelloBeElevationLevel(
                offsetY = 0.dp,
                blurRadius = 24.dp,
                color = Color(red = 0xFF, green = 0xC2, blue = 0x47, alpha = 0x52)
            )
    )

private val nightElevation =
    HelloBeElevation(
        none = noElevation,
        low =
            HelloBeElevationLevel(
                offsetY = 4.dp,
                blurRadius = 12.dp,
                color = Color(red = 0x00, green = 0x00, blue = 0x00, alpha = 0x42)
            ),
        medium =
            HelloBeElevationLevel(
                offsetY = 10.dp,
                blurRadius = 28.dp,
                color = Color(red = 0x00, green = 0x00, blue = 0x00, alpha = 0x59)
            ),
        high =
            HelloBeElevationLevel(
                offsetY = 20.dp,
                blurRadius = 60.dp,
                color = Color(red = 0x00, green = 0x00, blue = 0x00, alpha = 0x80)
            ),
        focusGlow =
            HelloBeElevationLevel(
                offsetY = 0.dp,
                blurRadius = 28.dp,
                color = Color(red = 0xFF, green = 0xD5, blue = 0x6A, alpha = 0x66)
            )
    )

fun helloBeElevation(mode: HelloBeThemeMode): HelloBeElevation = when (mode) {
    HelloBeThemeMode.DAY -> dayElevation
    HelloBeThemeMode.NIGHT -> nightElevation
}
