package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

private const val DEFAULT_PLACEHOLDERS = 3

/**
 * The calm wait while a stage is preparing.
 *
 * Nothing here shimmers, pulses or sweeps. That is the rule rather than a simplification, since
 * the motion tokens forbid looping decorative animation, and it means there is nothing to
 * substitute under reduced motion: this behaves identically at either setting. Pip is drawn in a
 * constant pose, so the cross-fade inside [PipGuide] never has a target to animate towards.
 *
 * Pip waits with the child, and the whole treatment is announced so a waiting screen is not silent
 * to a screen reader. Pip itself is decorative here because the panel announces as a whole.
 *
 * Takes its width from the caller, like every other component, and shares that width between Pip
 * and the placeholder column.
 */
@Composable
fun StoryLoading(
    contentDescription: String,
    modifier: Modifier = Modifier,
    placeholderCount: Int = DEFAULT_PLACEHOLDERS,
    pip: @Composable () -> Unit = {
        PipGuide(
            pose = PipPose.RESTING,
            contentDescription = null,
            modifier = Modifier.size(HelloBeLayout.pipMinSize)
        )
    }
) {
    val colors = HelloBeTheme.colors

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = contentDescription
            this.liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pip()

        Column(
            // Weighted so the blocks share the width the caller gave the row. Without this they
            // would measure to zero under an unbounded constraint.
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)
        ) {
            repeat(placeholderCount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HelloBeLayout.loadingPlaceholderHeight)
                        .background(colors.surfaceMuted, HelloBeShapes.medium)
                )
            }
        }
    }
}
