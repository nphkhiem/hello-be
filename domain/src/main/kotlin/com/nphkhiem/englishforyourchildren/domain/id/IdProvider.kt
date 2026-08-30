package com.nphkhiem.englishforyourchildren.domain.id

/**
 * Where a new identity comes from.
 *
 * Sessions and activity instances are made at runtime rather than authored, so something has to
 * mint them. Taking that from a provider rather than from a random source is what lets a test say
 * which session it is talking about.
 */
fun interface IdProvider<out T> {
    fun next(): T
}
