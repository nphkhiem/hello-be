@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * One topic per card - unit, lesson, profile, word, or free-play - sharing a single focus
 * grammar so a child learns the interaction once and it holds everywhere.
 *
 * Uses the selectable `Surface` overload rather than the clickable one because a story card
 * can be both current (selected) and focused at the same time; that combination is its own
 * state in the token matrix, not a case of one overriding the other.
 *
 * The card sizes to its content. Callers set width from the 12-column grid tokens (for
 * example `layout.cardThreeColumnSet`) rather than the card filling its parent, so a row of
 * cards divides the grid instead of the first card consuming it.
 *
 * [titleMinLines] holds the title block to a fixed number of lines so a row of cards shares one
 * baseline. Without it a row mixes one and two line titles and every line below them steps up and
 * down, which is what the learning path's lesson row did until it was measured.
 *
 * [centerContent] is for a card holding a single mark rather than a block. It centres the text
 * inside each line as well as centring the block on the card: without that, a title that wraps is
 * still left aligned within its own box, so the second line hangs off the start of the first
 * instead of sitting under its middle. The default reads as a
 * block, an optional picture then a title then a supporting line, and top left is right for that:
 * it is what a profile card and a unit card want. One glyph sitting in the corner of a large card
 * is not, which is what an age choice looked like before this existed.
 */
@Composable
fun StoryCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    availability: HelloBeAvailability = HelloBeAvailability.ENABLED,
    supportingText: String? = null,
    stateDescription: String? = null,
    titleMinLines: Int = 1,
    centerContent: Boolean = false,
    minHeight: Dp = HelloBeTheme.layout.childChoiceMinHeight,
    illustration: (@Composable () -> Unit)? = null
) {
    val colors = HelloBeTheme.colors
    val focus = HelloBeTheme.focus
    val shape = HelloBeShapes.extraLarge
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        selected = selected,
        onClick = { if (availability.isClickable) onClick() },
        modifier =
            modifier
                .defaultMinSize(minHeight = minHeight)
                .helloBeFocusClearance()
                .focusProperties { canFocus = availability.isFocusable }
                .semantics {
                    if (availability == HelloBeAvailability.DISABLED) disabled()
                    stateDescription?.let { this.stateDescription = it }
                },
        enabled = availability.isFocusable,
        interactionSource = interactionSource,
        shape = SelectableSurfaceDefaults.shape(shape = shape),
        colors =
            SelectableSurfaceDefaults.colors(
                containerColor =
                    if (availability == HelloBeAvailability.UNAVAILABLE) {
                        colors.surfaceMuted
                    } else {
                        colors.surfacePrimary
                    },
                contentColor =
                    if (availability == HelloBeAvailability.UNAVAILABLE) {
                        colors.textSecondary
                    } else {
                        colors.textPrimary
                    },
                focusedContainerColor = colors.focusFill,
                focusedContentColor = colors.onFocusFill,
                pressedContainerColor = colors.actionPrimaryPressed,
                pressedContentColor = colors.onPrimary,
                selectedContainerColor = colors.actionSelected,
                selectedContentColor = colors.onSelected,
                focusedSelectedContainerColor = colors.focusFill,
                focusedSelectedContentColor = colors.onFocusFill,
                pressedSelectedContainerColor = colors.actionPrimaryPressed,
                pressedSelectedContentColor = colors.onPrimary,
                disabledContainerColor = colors.surfaceMuted,
                disabledContentColor = colors.textTertiary
            ),
        scale =
            SelectableSurfaceDefaults.scale(
                focusedScale = focus.scaleCard,
                pressedScale = focus.scaleCard * focus.pressScale,
                focusedSelectedScale = focus.scaleCard,
                pressedSelectedScale = focus.scaleCard * focus.pressScale
            ),
        border =
            SelectableSurfaceDefaults.border(
                border =
                    if (availability == HelloBeAvailability.UNAVAILABLE) {
                        HelloBeFocusFrame.unavailable(shape)
                    } else {
                        Border.None
                    },
                focusedBorder = HelloBeFocusFrame.ring(shape),
                pressedBorder = HelloBeFocusFrame.ring(shape),
                selectedBorder = HelloBeFocusFrame.selection(shape),
                focusedSelectedBorder = HelloBeFocusFrame.ring(shape),
                pressedSelectedBorder = HelloBeFocusFrame.ring(shape)
            )
    ) {
        Column(
            // Aligned rather than filled: an interactive Surface hosts its content in a nested box
            // that does not propagate minimum constraints, so this column wraps while the box
            // around it is already the full card.
            modifier = Modifier
                .then(if (centerContent) Modifier.align(Alignment.Center) else Modifier)
                .padding(HelloBeTheme.spacing.cardInternal),
            horizontalAlignment = if (centerContent) {
                Alignment.CenterHorizontally
            } else {
                Alignment.Start
            },
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)
        ) {
            illustration?.invoke()
            Text(
                text = title,
                style = HelloBeTheme.typography.titleMedium,
                minLines = titleMinLines,
                textAlign = if (centerContent) TextAlign.Center else null,
                modifier = Modifier.semantics { heading() }
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = HelloBeTheme.typography.bodyMedium,
                    textAlign = if (centerContent) TextAlign.Center else null
                )
            }
        }
    }
}
