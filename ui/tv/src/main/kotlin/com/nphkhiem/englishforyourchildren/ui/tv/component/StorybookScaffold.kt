package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The shared stage every Hello Bé screen sits inside.
 *
 * [scenery] is the only slot allowed outside the safe area, because decoration may bleed to
 * the screen edge while nothing meaningful may. Every other slot is a child of a container
 * already inset by `layout.safeHorizontal` and `layout.safeVertical`, so essential content
 * cannot reach overscan even if a screen author forgets to ask for the inset. That is the
 * point: the safe area is structural here rather than a modifier someone has to remember.
 *
 * [support] is the predictable strip along the bottom where Pip and the caption live, which is
 * how Pip is prevented from covering the focal object rather than merely being asked not to.
 *
 * [entryFocus] is claimed exactly once, and [entryFocusReady] says when there is something real to
 * claim it on. A screen that loads its content says so, because claiming focus on a spinner spends
 * the one claim on a view that is about to be thrown away: when the real content replaces it, the
 * focused node is destroyed and focus falls back to whatever is left, which on these screens is the
 * header. That is how a child opening the learning path ended up focused on the profile switcher,
 * one press away from being thrown out of their own lesson.
 *
 * Claimed once still means once. A caller that rebuilds its requester, or whose state changes mid
 * activity, does not get a second claim: a lesson must never yank focus back from a child who has
 * already moved somewhere else. See ADR 0004.
 *
 * A failure to claim entry focus is deliberately not swallowed. Silently continuing would
 * leave a stage with nothing focused, which is the exact defect this parameter exists to
 * prevent, so it must surface rather than degrade quietly.
 */
@Composable
fun StorybookScaffold(
    modifier: Modifier = Modifier,
    scenery: (@Composable BoxScope.() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    support: (@Composable RowScope.() -> Unit)? = null,
    entryFocus: FocusRequester? = null,
    entryFocusReady: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    if (entryFocus != null) {
        // Remembered rather than keyed to the composition, because the claim now waits for content
        // and the wait is what a key alone cannot express. A caller that rebuilds its
        // FocusRequester still cannot cause entry focus to be claimed a second time.
        var claimed by remember { mutableStateOf(false) }
        var waited by remember { mutableStateOf(false) }

        LaunchedEffect(entryFocusReady) {
            if (claimed) return@LaunchedEffect
            if (!entryFocusReady) {
                waited = true
                return@LaunchedEffect
            }
            // A screen that was ready all along claims in its first composition, exactly as before.
            // Only content that arrived late needs a frame to be laid out first, and spending that
            // frame on every screen would be enough for a child to focus something and have it
            // taken back: the press they had already aimed would land somewhere else.
            if (waited) withFrameNanos { }
            claimed = true
            entryFocus.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        scenery?.invoke(this)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = HelloBeLayout.safeHorizontal,
                    vertical = HelloBeLayout.safeVertical
                ),
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
        ) {
            header?.invoke()

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                content()
            }

            if (support != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
                    verticalAlignment = Alignment.CenterVertically,
                    content = support
                )
            }
        }
    }
}
