package com.nphkhiem.englishforyourchildren

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.playback.ImageAssetLocator
import com.nphkhiem.englishforyourchildren.ui.tv.component.PictureSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The picture for an id, from the live downloaded pack or the APK, decoded off the frame it is
 * drawn in.
 *
 * Every way this can fail returns null, because a card with no picture already knows what to do and
 * none of these is worth ending a child's lesson over: an id that is not one, a file nobody has
 * drawn, and a file that will not decode all mean the same thing to the card asking.
 */
class PackagedPictureSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val images: ImageAssetLocator
) : PictureSource {
    override suspend fun load(id: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val assetId = runCatching { AssetId(id) }.getOrNull() ?: return@withContext null
        // The locator answers with the asset URI it built, and decoding needs the path back out of
        // it. Deriving it here rather than rebuilding the convention keeps that convention in the
        // one place that owns it.
        val uri = images.locate(assetId) ?: return@withContext null

        runCatching {
            // A downloaded picture is a file; a packaged one is an asset, and its path comes back
            // out of the URI the locator built rather than being rebuilt here.
            val stream = if (uri.scheme == FILE) {
                File(requireNotNull(uri.path)).inputStream()
            } else {
                context.assets.open(requireNotNull(uri.path).trimStart('/'))
            }
            stream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
    }
}

private const val FILE = "file"
