package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.annotation.StringRes

/**
 * Which stop-for-now description a child is told.
 *
 * Keyed off the pending-save flag the lesson already carries, because the brief's "confirmed
 * checkpoint and pending checkpoint use different truthful descriptions" is exactly that flag.
 *
 * The pending wording never claims progress is saved. Telling a child Pip will remember when it
 * might not is the one thing this dialog must not do.
 */
@StringRes
internal fun stopForNowDescription(pendingSave: Boolean): Int = if (pendingSave) {
    R.string.stop_for_now_pending
} else {
    R.string.stop_for_now_saved
}
