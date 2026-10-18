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
    /** In order of preference. The first that exists wins, a pack's before the APK's. */
    private val extensions: List<String>
) {
    fun locate(assetId: AssetId): Uri? {
        val paths = extensions.map { "$directory/${assetId.value}.$it" }
        val inPack = downloaded.directory()?.let { pack ->
            paths.map { File(pack, it) }.firstOrNull { it.isFile }
        }
        return inPack?.let(Uri::fromFile)
            ?: paths.firstOrNull(::exists)?.let { "asset:///$it".toUri() }
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
            // Recordings made for the course are m4a. A word fetched from Wikimedia Commons keeps the
            // container its author uploaded, usually Ogg, so its file is the licensed work itself
            // rather than a converted copy; Media3 plays all three.
            val files = PackagedAssetLocator(context, downloaded, "media/audio", AUDIO)
            return MediaAssetLocator { files.locate(it) }
        }

        fun forImages(context: Context, downloaded: DownloadedMedia): ImageAssetLocator {
            val files = PackagedAssetLocator(context, downloaded, "media/image", listOf("webp"))
            return ImageAssetLocator { files.locate(it) }
        }

        private val AUDIO = listOf("m4a", "ogg", "mp3")
    }
}
