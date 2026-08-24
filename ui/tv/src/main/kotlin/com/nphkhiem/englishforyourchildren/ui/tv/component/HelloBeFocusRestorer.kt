package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

/**
 * The one place focus is handed back after an overlay closes.
 *
 * This is deliberately a plain class rather than a composable so it can be hoisted above a
 * dialog and passed across that boundary. [StorybookScaffold] owns entry focus, but a dialog
 * opens over a stage rather than inside one, so restoration could not live in the scaffold
 * without every dialog reimplementing it. See ADR 0001.
 *
 * Attach [returnTarget] to the control that should regain focus, then call [restore] when the
 * overlay closes.
 */
@Stable
class HelloBeFocusRestorer internal constructor() {

    /** Attach this to the control that should hold focus again once the overlay closes. */
    val returnTarget: FocusRequester = FocusRequester()

    /**
     * Returns focus to [returnTarget].
     *
     * Compose throws when a requester was never attached, which happens legitimately when the
     * screen behind a dialog has changed while the dialog was open. A child should see the
     * dialog close, not a crash, so that case is swallowed.
     */
    fun restore() {
        runCatching { returnTarget.requestFocus() }
    }
}

/** Remembers a [HelloBeFocusRestorer] for the lifetime of the calling composable. */
@Composable
fun rememberHelloBeFocusRestorer(): HelloBeFocusRestorer = remember { HelloBeFocusRestorer() }
