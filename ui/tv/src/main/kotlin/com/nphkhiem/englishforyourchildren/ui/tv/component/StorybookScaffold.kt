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
 * [entryFocus] is claimed exactly once, when the stage first appears. The effect is keyed to
 * the composition rather than to the requester, so this holds even if a caller rebuilds
 * its requester: a lesson
 * updating its state mid-activity must never yank focus back from a child who has already
 * moved somewhere else, and passing a freshly constructed requester must not change that.
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
    content: @Composable BoxScope.() -> Unit
) {
    if (entryFocus != null) {
        // Keyed to the composition rather than to the requester, so a caller that rebuilds its
        // FocusRequester cannot cause entry focus to be claimed a second time.
        LaunchedEffect(Unit) {
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
