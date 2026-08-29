@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusFrame
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusClearance
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
    modifier: Modifier = Modifier
) {
    val colors = HelloBeTheme.colors
    val focus = HelloBeTheme.focus
    val shape = HelloBeShapes.large
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = HelloBeLayout.caregiverControlMinHeight)
            .helloBeFocusClearance()
            .semantics { stateDescription = value },
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surfacePrimary,
            contentColor = colors.textPrimary,
            focusedContainerColor = colors.focusFill,
            focusedContentColor = colors.onFocusFill,
            pressedContainerColor = colors.actionPrimaryPressed,
            pressedContentColor = colors.onPrimary
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focus.scaleButton),
        border = ClickableSurfaceDefaults.border(
            border = HelloBeFocusFrame.resting(shape),
            focusedBorder = HelloBeFocusFrame.ring(shape),
            pressedBorder = HelloBeFocusFrame.ring(shape)
        )
    ) {
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

            Text(text = value, style = HelloBeTheme.typography.titleSmall)
        }
    }
}
