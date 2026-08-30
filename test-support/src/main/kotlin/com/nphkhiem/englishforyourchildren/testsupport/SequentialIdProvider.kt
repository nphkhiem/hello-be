package com.nphkhiem.englishforyourchildren.testsupport

import com.nphkhiem.englishforyourchildren.domain.id.IdProvider

/**
 * Identities a test can name: the first is `s1`, the second `s2`, and so on.
 *
 * Each provider counts on its own, so sessions and profiles taken in the same test do not interfere
 * and neither has to be read in the order the other was used.
 */
class SequentialIdProvider<T>(private val prefix: String, private val wrap: (String) -> T) :
    IdProvider<T> {
    private var taken = 0

    override fun next(): T {
        taken += 1
        return wrap("$prefix$taken")
    }
}
