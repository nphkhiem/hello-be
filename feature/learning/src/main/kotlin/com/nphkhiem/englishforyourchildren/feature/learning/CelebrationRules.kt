package com.nphkhiem.englishforyourchildren.feature.learning

/**
 * Whether the learned words are on the page yet.
 *
 * Reduced motion shows them before the reveal has run, which is the whole of the design brief's
 * "reduced-motion mode replaces movement with a static success page": under that setting there is
 * no arrival to wait for, so the words are simply there.
 */
internal fun wordsVisible(revealed: Boolean, reduceMotion: Boolean): Boolean =
    revealed || reduceMotion
