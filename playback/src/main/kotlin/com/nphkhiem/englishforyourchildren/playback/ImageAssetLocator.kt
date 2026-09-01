package com.nphkhiem.englishforyourchildren.playback

import android.net.Uri
import com.nphkhiem.englishforyourchildren.domain.model.AssetId

/**
 * Where the picture for an [AssetId] lives, if it lives anywhere.
 *
 * Separate from [MediaAssetLocator] rather than one locator told which kind to fetch, because
 * `CONTENT_ID_REGISTRY.md` rule 3 says an id is opaque to code. Nothing here reads the string to
 * discover what kind of thing it names: the caller wants a picture, and asks something that only
 * ever returns pictures.
 *
 * Every illustration this course refers to is still undrawn, so the common case by far is `null`.
 * A card with no picture shows its word instead, which is why finding out has to be cheap and
 * synchronous.
 */
fun interface ImageAssetLocator {
    /** The picture, or null when no file has been made yet. */
    fun locate(assetId: AssetId): Uri?
}
