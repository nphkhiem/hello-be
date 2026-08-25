package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.ui.unit.dp

/**
 * Illustration tokens, per DESIGN_TOKENS.md. Shapes drawn from these stay legible when the same
 * dp layout is rasterised at 720p, 1080p and 4K, which is why the stroke has a reference width
 * and detail below a minimum size is dropped rather than drawn and lost.
 */
object HelloBeIllustration {
    /** Silhouette stroke at the mdpi reference, keeping outlines visible when downscaled. */
    val strokeReference = 3.dp

    /** Detail smaller than this is nonessential and is removed rather than drawn. */
    val detailMinimum = 4.dp
}
