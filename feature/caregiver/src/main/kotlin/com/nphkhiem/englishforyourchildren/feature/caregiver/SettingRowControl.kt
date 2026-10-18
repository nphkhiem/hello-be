@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusFrame
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusClearance
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberHelloBeInteractionSource
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * One row of the settings list: what it is on the left, where it stands on the right.
 *
 * A row rather than a card, because the approved draft draws a compact list and a stack of cards
 * fits two and a half settings on a television. The value sits at the trailing edge as its own
 * text, which is the draft's own arrangement and the reason this is not a [StoryCard]: that card
 * has no trailing slot, and appending the value to the subtitle with a separator read as part of
 * the explanation rather than as the state.
 *
 * [value] is a word, never a colour and never a shape alone. The draft is explicit that state stays
 * visible "in text and semantics", so it is drawn and also carried as the row's state description.
 *
 * The focus grammar is the shared one: the yellow fill with its black ring, the same clearance, the
 * same shape family. A caregiver surface may be denser than a child's, never differently behaved.
 */
@Composable
internal fun SettingRowControl(
    title: String,
    consequence: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * [HelloBeAvailability.UNAVAILABLE] keeps the row in the focus order, readable and announced,
     * and stops it being pressed. A row that dropped out of focus while it worked would move the
     * caregiver's place under them.
     */
    availability: HelloBeAvailability = HelloBeAvailability.ENABLED,
    /**
     * Whether a change to [value] is announced even when the row does not hold focus. For a row that
     * runs something, such as a download a caregiver may have walked away from; a switch changes
     * only when pressed, while it is focused, and announcing it twice would be noise.
     */
    announcesChanges: Boolean = false
) {
    val unavailable = availability == HelloBeAvailability.UNAVAILABLE
    val colors = HelloBeTheme.colors
    val focus = HelloBeTheme.focus
    val shape = HelloBeShapes.large
    val interactionSource = rememberHelloBeInteractionSource()

    Surface(
        onClick = { if (!unavailable) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = HelloBeLayout.caregiverControlMinHeight)
            .helloBeFocusClearance()
            .semantics {
                stateDescription = value
                if (announcesChanges) liveRegion = LiveRegionMode.Polite
            },
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (unavailable) colors.surfaceMuted else colors.surfacePrimary,
            contentColor = if (unavailable) colors.textSecondary else colors.textPrimary,
            focusedContainerColor = colors.focusFill,
            focusedContentColor = colors.onFocusFill,
            pressedContainerColor = colors.actionPrimaryPressed,
            pressedContentColor = colors.onPrimary
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focus.scaleButton),
        border = ClickableSurfaceDefaults.border(
            border = if (unavailable) {
                HelloBeFocusFrame.unavailable(shape)
            } else {
                HelloBeFocusFrame.resting(shape)
            },
            focusedBorder = HelloBeFocusFrame.ring(shape),
            pressedBorder = HelloBeFocusFrame.ring(shape)
        )
    ) {
        BoxWithConstraints {
            val valueMaxWidth = maxWidth * VALUE_MAX_FRACTION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = HelloBeTheme.spacing.space5,
                        vertical = HelloBeTheme.spacing.space3
                    ),
                horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space1)
                ) {
                    Text(text = title, style = HelloBeTheme.typography.titleSmall)
                    Text(text = consequence, style = HelloBeTheme.typography.bodyMedium)
                }

                // Capped and allowed to wrap. A short value takes only the width it needs; a long one,
                // such as a download's state in two languages at the largest text size, used to stay
                // on one line and squeeze the title beside it into a column of single words.
                Text(
                    text = value,
                    style = HelloBeTheme.typography.titleSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = valueMaxWidth)
                )
            }
        }
    }
}

/** The most of a row its value may take, so the title and its consequence keep the rest. */
private const val VALUE_MAX_FRACTION = 0.4f
