package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shape tokens, per DESIGN_TOKENS.md. Shapes do not vary by Day/Night or high contrast. */
object HelloBeShapes {
    val none = RoundedCornerShape(0.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(18.dp)
    val extraLarge = RoundedCornerShape(24.dp)
    val dialog = RoundedCornerShape(28.dp)
    val storyPage =
        RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 30.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp
        )
    val full = CircleShape
}
