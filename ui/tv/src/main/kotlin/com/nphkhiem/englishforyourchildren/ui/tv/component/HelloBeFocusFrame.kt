@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The one focus treatment every Hello Bé control shares.
 *
 * DESIGN_TOKENS.md requires that focus is never communicated by color alone, so a focused
 * control combines a gold ring, a contrast guard behind it, a focused container fill, and a
 * restrained scale. This object owns the ring and guard halves; container fill and scale come
 * from each component's token-driven colors and scale.
 *
 * The guard is what makes the ring legible on both themes: focus gold on the Day canvas is
 * only `1.43:1`, so it is never drawn alone.
 */
object HelloBeFocusFrame {

    /** The gold ring drawn on a focused control. */
    @Composable
    @ReadOnlyComposable
    fun ring(shape: Shape): Border = Border(
        border = BorderStroke(HelloBeTheme.focus.ringWidth, HelloBeTheme.colors.focusRing),
        shape = shape
    )

    /** The dark guard drawn just outside [ring] so the gold never sits directly on the canvas. */
    @Composable
    @ReadOnlyComposable
    fun guard(shape: Shape): Border = Border(
        border = BorderStroke(HelloBeTheme.focus.guardWidth, HelloBeTheme.colors.focusGuard),
        shape = shape
    )

    /** The outline a selected-but-not-focused control keeps, so selection is never fill-only. */
    @Composable
    @ReadOnlyComposable
    fun selection(shape: Shape): Border = Border(
        border =
            BorderStroke(
                HelloBeTheme.focus.selectionWidth,
                HelloBeTheme.colors.actionSelectedBorder
            ),
        shape = shape
    )

    /**
     * The growth outline on a confirmed-correct surface. This is the one place `accent.growth`
     * belongs on an interactive control: here green really does mean "correct", which is why
     * selection was moved off this token and onto `action.selected`.
     */
    @Composable
    @ReadOnlyComposable
    fun correct(shape: Shape): Border = Border(
        border =
            BorderStroke(
                HelloBeTheme.focus.selectionWidth,
                HelloBeTheme.colors.accentGrowth
            ),
        shape = shape
    )

    /**
     * The secondary border a surface carries at rest, per the Default row of the component state
     * matrix.
     *
     * Draws the same thing as [unavailable] today and is deliberately a separate function anyway.
     * "This is the object of the lesson" and "this is not for you right now" are different
     * statements, and collapsing them into one call would let a later change to either meaning
     * silently alter the other.
     */
    @Composable
    @ReadOnlyComposable
    fun resting(shape: Shape): Border = Border(
        border = BorderStroke(
            HelloBeTheme.focus.guardWidth,
            HelloBeTheme.colors.borderSecondary
        ),
        shape = shape
    )

    /** The quiet border that marks an unavailable "later" surface without a lock metaphor. */
    @Composable
    @ReadOnlyComposable
    fun unavailable(shape: Shape): Border = Border(
        border = BorderStroke(
            HelloBeTheme.focus.guardWidth,
            HelloBeTheme.colors.borderSecondary
        ),
        shape = shape
    )
}

/**
 * Reserves `focus.clearance` on every side so a control's ring and focus scale never collide
 * with its neighbour. Apply to each focusable child in a row or grid rather than to the
 * container, so the clearance travels with the control.
 */
@Composable
fun Modifier.helloBeFocusClearance(): Modifier = padding(HelloBeTheme.focus.clearance)

/**
 * Marks a row or grid of controls as one focus group that remembers its place.
 *
 * Without this, leaving a group and coming back drops focus on whichever child is nearest rather
 * than the one the child left from, so moving down and back up silently loses their position. On
 * a remote that means hunting for the thing they were about to choose.
 *
 * Apply to the container. The children keep their own `helloBeFocusClearance`.
 */
fun Modifier.helloBeFocusGroup(): Modifier = this.focusRestorer().focusGroup()
