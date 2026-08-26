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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The large image-first answer surface used by every lesson activity.
 *
 * [feedback] is deliberately independent of [selected]: a child's chosen answer stays selected
 * while the surface separately reports correct or supportive-retry. Supportive retry keeps the
 * card focusable and never uses the error palette, per the "Supportive retry" row of the state
 * matrix - it is a slower second invitation, not a failure.
 *
 * [stateDescription] carries the spoken meaning so feedback never depends on color alone.
 *
 * [labelVisible] withholds the drawn word while keeping it as the card's accessible name, for
 * activities whose prompt already names the target in text: captioning the answers there would
 * make the question solvable by reading instead of by looking. A screen reader still receives the
 * word, because for that child the picture is not available at all.
 *
 * Like [StoryCard], the card sizes to its content and takes its width from the grid tokens.
 */
@Composable
fun ChoiceCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    feedback: HelloBeChoiceFeedback = HelloBeChoiceFeedback.NEUTRAL,
    availability: HelloBeAvailability = HelloBeAvailability.ENABLED,
    stateDescription: String? = null,
    labelVisible: Boolean = true,
    minHeight: Dp = HelloBeTheme.layout.childChoiceMinHeight,
    illustration: (@Composable () -> Unit)? = null
) {
    val colors = HelloBeTheme.colors
    val focus = HelloBeTheme.focus
    val shape = HelloBeShapes.extraLarge
    val interactionSource = remember { MutableInteractionSource() }

    val unavailable = availability == HelloBeAvailability.UNAVAILABLE
    val restingContainer =
        if (unavailable) {
            colors.surfaceMuted
        } else {
            when (feedback) {
                HelloBeChoiceFeedback.NEUTRAL -> colors.surfacePrimary
                HelloBeChoiceFeedback.CORRECT -> colors.successContainer
                HelloBeChoiceFeedback.SUPPORTIVE_RETRY -> colors.supportiveRetryContainer
            }
        }
    val restingContent =
        if (unavailable) {
            colors.textSecondary
        } else {
            when (feedback) {
                HelloBeChoiceFeedback.NEUTRAL -> colors.textPrimary
                HelloBeChoiceFeedback.CORRECT -> colors.successContent
                HelloBeChoiceFeedback.SUPPORTIVE_RETRY -> colors.supportiveRetryContent
            }
        }
    val restingBorder =
        when {
            unavailable -> HelloBeFocusFrame.unavailable(shape)
            feedback == HelloBeChoiceFeedback.CORRECT -> HelloBeFocusFrame.correct(shape)
            else -> Border.None
        }

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
                    // Only when the word is not drawn. The Text below is already the accessible
                    // name, so setting both would announce the answer twice.
                    if (!labelVisible) contentDescription = label
                },
        enabled = availability.isFocusable,
        interactionSource = interactionSource,
        shape = SelectableSurfaceDefaults.shape(shape = shape),
        colors =
            SelectableSurfaceDefaults.colors(
                containerColor = restingContainer,
                contentColor = restingContent,
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
                border = restingBorder,
                focusedBorder = HelloBeFocusFrame.ring(shape),
                pressedBorder = HelloBeFocusFrame.ring(shape),
                selectedBorder = HelloBeFocusFrame.selection(shape),
                focusedSelectedBorder = HelloBeFocusFrame.ring(shape),
                pressedSelectedBorder = HelloBeFocusFrame.ring(shape)
            )
    ) {
        Column(
            modifier = Modifier.padding(HelloBeTheme.spacing.cardInternal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)
        ) {
            illustration?.invoke()
            if (labelVisible) {
                Text(text = label, style = HelloBeTheme.typography.titleMedium)
            }
        }
    }
}
