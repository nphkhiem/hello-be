package com.nphkhiem.englishforyourchildren.playback

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import java.io.IOException

/**
 * Media packaged in the APK, one directory and one container per kind.
 *
 * A directory per kind rather than a flat one: images and recordings share a single id namespace,
 * so a flat layout would have to read `aud-` or `img-` off the front of an id to choose an
 * extension, and the registry forbids that. Here the kind comes from which locator was asked, which
 * is why the constructor is private and the two kinds are named factories rather than arguments a
 * caller chooses.
 */
class PackagedAssetLocator private constructor(
    private val context: Context,
    private val directory: String,
    private val extension: String
) {
    fun locate(assetId: AssetId): Uri? {
        val path = "$directory/${assetId.value}.$extension"
        return if (exists(path)) "asset:///$path".toUri() else null
    }

    /**
     * Opening the file is the only honest test that it is there. `AssetManager.list` would need a
     * directory walk per lookup and still says nothing about a file it cannot open.
     */
    private fun exists(path: String): Boolean = try {
        context.assets.open(path).close()
        true
    } catch (_: IOException) {
        false
    }

    companion object {
        fun forAudio(context: Context): MediaAssetLocator {
            val files = PackagedAssetLocator(context, "media/audio", "m4a")
            return MediaAssetLocator { files.locate(it) }
        }

        fun forImages(context: Context): ImageAssetLocator {
            val files = PackagedAssetLocator(context, "media/image", "webp")
            return ImageAssetLocator { files.locate(it) }
        }
    }
}
