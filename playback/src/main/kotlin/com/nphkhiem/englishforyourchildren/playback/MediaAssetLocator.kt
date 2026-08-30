package com.nphkhiem.englishforyourchildren.playback

import android.net.Uri
import com.nphkhiem.englishforyourchildren.domain.model.AssetId

/**
 * Where the recording for an [AssetId] lives, if it lives anywhere.
 *
 * This exists so that a missing file is an answer rather than an accident. Every recording this
 * course refers to is still unmade, so the common case by far is `null`, and finding that out has
 * to be cheap, synchronous and free of any player.
 *
 * It is also the one place that knows how an id becomes a path. `CONTENT_ID_REGISTRY.md` rule 3
 * says an id is opaque to code, so nothing here reads the string to discover what kind of thing it
 * names: the caller wants audio, and asks something that only ever returns audio.
 */
fun interface MediaAssetLocator {
    /** The recording, or null when no file has been made yet. */
    fun locate(assetId: AssetId): Uri?
}
