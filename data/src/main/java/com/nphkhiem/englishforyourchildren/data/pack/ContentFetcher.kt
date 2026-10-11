package com.nphkhiem.englishforyourchildren.data.pack

import java.io.IOException
import java.io.InputStream

/**
 * Opens one published file, by its path relative to the manifest.
 *
 * Any failure to reach or read it is an [IOException], which the updater treats as transient: the
 * live pack stays as it is and the next check tries again.
 */
fun interface ContentFetcher {
    @Throws(IOException::class)
    fun open(path: String): InputStream
}
