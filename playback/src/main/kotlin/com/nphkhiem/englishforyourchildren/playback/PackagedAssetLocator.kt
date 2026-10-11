package com.nphkhiem.englishforyourchildren.playback

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import java.io.File
import java.io.IOException

/**
 * Media for the course a child is being taught: a downloaded pack's own file when it has one, else
 * the one packaged in the APK, one directory and one container per kind either way.
 *
 * The pack is asked first because a pack may bring a recording or picture the APK never had, or a
 * corrected one. An id the pack does not carry falls through to the APK, which is always there.
 * A lesson resumed under an older version still hears the live pack's file for the same id: a
 * published id never changes meaning, so that is the same word, at worst better recorded.
 *
 * A directory per kind rather than a flat one: images and recordings share a single id namespace,
 * so a flat layout would have to read `aud-` or `img-` off the front of an id to choose an
 * extension, and the registry forbids that. Here the kind comes from which locator was asked, which
 * is why the constructor is private and the two kinds are named factories rather than arguments a
 * caller chooses.
 */
class PackagedAssetLocator private constructor(
    private val context: Context,
    private val downloaded: DownloadedMedia,
    private val directory: String,
    private val extension: String
) {
    fun locate(assetId: AssetId): Uri? {
        val path = "$directory/${assetId.value}.$extension"
        val inPack = downloaded.directory()?.let { File(it, path) }?.takeIf { it.isFile }
        return when {
            inPack != null -> Uri.fromFile(inPack)
            exists(path) -> "asset:///$path".toUri()
            else -> null
        }
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
        fun forAudio(context: Context, downloaded: DownloadedMedia): MediaAssetLocator {
            val files = PackagedAssetLocator(context, downloaded, "media/audio", "m4a")
            return MediaAssetLocator { files.locate(it) }
        }

        fun forImages(context: Context, downloaded: DownloadedMedia): ImageAssetLocator {
            val files = PackagedAssetLocator(context, downloaded, "media/image", "webp")
            return ImageAssetLocator { files.locate(it) }
        }
    }
}
