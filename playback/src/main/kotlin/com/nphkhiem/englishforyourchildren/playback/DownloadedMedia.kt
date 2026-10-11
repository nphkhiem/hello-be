package com.nphkhiem.englishforyourchildren.playback

import java.io.File

/**
 * The folder of the downloaded course a child is being taught, or null when that course is the one
 * that shipped in the APK.
 *
 * Asked on every lookup and answered from memory, because a lookup is synchronous and has to be
 * cheap. Supplied by the app, since knowing which pack is live is the data layer's business and this
 * module stays clear of it.
 */
fun interface DownloadedMedia {
    fun directory(): File?
}
