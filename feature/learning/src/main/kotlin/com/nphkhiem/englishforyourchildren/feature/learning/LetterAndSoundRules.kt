package com.nphkhiem.englishforyourchildren.feature.learning

import java.util.Locale

/**
 * The written form of a letter, showing both cases together.
 *
 * The learning object carries the letter; this is its typography. Deriving the pair rather than
 * storing it means a fixture cannot ship "A A" and survive review, and the casing test holds
 * whatever case the letter arrives in.
 *
 * [Locale.ROOT] is pinned rather than using the default: under a Turkish locale the default
 * conversion turns "i" into "İ", and this app already ships a second language.
 */
internal fun letterPair(letter: String): String =
    "${letter.uppercase(Locale.ROOT)} ${letter.lowercase(Locale.ROOT)}"
