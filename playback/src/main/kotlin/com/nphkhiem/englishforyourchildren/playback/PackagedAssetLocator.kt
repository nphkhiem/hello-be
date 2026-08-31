package com.nphkhiem.englishforyourchildren.playback

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

/**
 * Recordings packaged in the APK, under one directory and one container.
 *
 * A directory per kind rather than a flat one: images and recordings share a single id namespace,
 * so a flat layout would have to read `aud-` or `img-` off the front of an id to choose an
 * extension, and the registry forbids that. Here the kind comes from which locator was asked.
 */
class PackagedAssetLocator @Inject constructor(
    @param:ApplicationContext private val context: Context
) : MediaAssetLocator {
    override fun locate(assetId: AssetId): Uri? {
        val path = "$DIRECTORY/${assetId.value}.$EXTENSION"
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

    private companion object {
        const val DIRECTORY = "media/audio"
        const val EXTENSION = "m4a"
    }
}
