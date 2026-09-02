package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * Where a picture comes from, to a component that must not know.
 *
 * A suspending function so that reading a file never happens on the frame the card is drawn in, and
 * a `null` result so that "no file has been drawn for this id" is an answer rather than a failure.
 * Turning an id into a path belongs to the one place that owns the packaging convention; this
 * module only draws what comes back.
 */
fun interface PictureSource {
    suspend fun load(id: String): ImageBitmap?
}

/**
 * The picture source in force, which by default supplies none.
 *
 * The default is load-bearing rather than a placeholder. Storybook screens build their state by
 * hand with no dependency graph behind them, so without it every fixture would need a source it has
 * no way to make. Supplying none also puts those screens in the state the shipped build is in,
 * which is the one worth reviewing.
 */
val LocalPictureSource = staticCompositionLocalOf { PictureSource { null } }

/**
 * The picture for [id], or null while it loads and whenever no file has been drawn for it.
 *
 * The two nulls are deliberately the same value. A card with no picture shows its word, and in the
 * moment before a packaged file decodes, its word is still the right thing to show.
 */
@Composable
fun rememberPackagedPicture(id: String?): ImageBitmap? {
    val source = LocalPictureSource.current
    return produceState<ImageBitmap?>(initialValue = null, id, source) {
        value = id?.let { source.load(it) }
    }.value
}

/**
 * A packaged picture, filling its card without being cropped.
 *
 * No content description: the card around it already carries the word as its accessible name, and
 * naming the picture as well would announce the answer twice.
 */
@Composable
fun PackagedPicture(picture: ImageBitmap, modifier: Modifier = Modifier) {
    Image(
        bitmap = picture,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
