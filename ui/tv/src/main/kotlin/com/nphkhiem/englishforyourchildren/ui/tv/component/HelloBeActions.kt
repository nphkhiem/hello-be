@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The shared child/caregiver action surface behind every Hello Bé button.
 *
 * Built on `androidx.tv.material3.Surface`'s clickable overload because its state model
 * (default, focused, pressed, disabled, each with its own container, content, scale and
 * border) is the same one DESIGN_TOKENS.md specifies. Hand-rolling this is what lost the
 * combined focus/selection state in the theme catalog, so the official primitive is reused
 * wherever its focus semantics match the approved contract.
 *
 * [availability] drives both look and reachability: a disabled action leaves the focus order
 * entirely, an unavailable one stays focusable so its "later" state can be announced.
 */
@Composable
fun HelloBeAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: HelloBeActionTone = HelloBeActionTone.PRIMARY,
    availability: HelloBeAvailability = HelloBeAvailability.ENABLED,
    supportingText: String? = null,
    icon: ImageVector? = null,
    stateDescription: String? = null,
    minHeight: androidx.compose.ui.unit.Dp = HelloBeTheme.layout.childPrimaryActionMinHeight
) {
    val colors = HelloBeTheme.colors
    val focus = HelloBeTheme.focus
    val shape = HelloBeShapes.large
    val interactionSource = remember { MutableInteractionSource() }

    // An unavailable action drops its tone: the state matrix gives "later" a muted surface, so
    // emphasis must not survive unavailability and read as a still-primary action.
    val unavailable = availability == HelloBeAvailability.UNAVAILABLE
    val container =
        when {
            unavailable -> colors.surfaceMuted
            tone == HelloBeActionTone.PRIMARY -> colors.actionPrimary
            tone == HelloBeActionTone.POSITIVE -> colors.actionPrimary
            tone == HelloBeActionTone.SECONDARY -> colors.actionSecondary
            tone == HelloBeActionTone.QUIET -> Color.Transparent
            else -> colors.errorContainer
        }
    val content =
        when {
            unavailable -> colors.textSecondary
            tone == HelloBeActionTone.PRIMARY -> colors.onPrimary
            tone == HelloBeActionTone.POSITIVE -> colors.onPrimary
            tone == HelloBeActionTone.SECONDARY -> colors.onSecondary
            tone == HelloBeActionTone.QUIET -> colors.textPrimary
            else -> colors.errorContent
        }

    Surface(
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
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = container,
                contentColor = content,
                focusedContainerColor = colors.focusFill,
                focusedContentColor = colors.onFocusFill,
                pressedContainerColor = colors.actionPrimaryPressed,
                pressedContentColor = colors.onPrimary,
                disabledContainerColor = colors.surfaceMuted,
                disabledContentColor = colors.textTertiary
            ),
        scale =
            ClickableSurfaceDefaults.scale(
                focusedScale = focus.scaleButton,
                pressedScale = focus.scaleButton * focus.pressScale
            ),
        border =
            ClickableSurfaceDefaults.border(
                border = if (unavailable) HelloBeFocusFrame.unavailable(shape) else Border.None,
                focusedBorder = HelloBeFocusFrame.ring(shape),
                pressedBorder = HelloBeFocusFrame.ring(shape)
            )
    ) {
        ActionContent(
            label = label,
            supportingText = supportingText,
            icon = icon,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ActionContent(
    label: String,
    supportingText: String?,
    icon: ImageVector?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = HelloBeTheme.spacing.cardInternal),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null)
        }
        Column {
            Text(text = label, style = HelloBeTheme.typography.labelLarge)
            if (supportingText != null) {
                Text(text = supportingText, style = HelloBeTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * A square icon-only action. [contentDescription] is required rather than nullable because an
 * icon action has no visible label to fall back on, so omitting it would leave the control
 * unannounced on a TV where a caregiver may be relying on spoken feedback.
 */
@Composable
fun HelloBeIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    availability: HelloBeAvailability = HelloBeAvailability.ENABLED,
    stateDescription: String? = null
) {
    val colors = HelloBeTheme.colors
    val focus = HelloBeTheme.focus
    val shape = HelloBeShapes.full
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = { if (availability.isClickable) onClick() },
        modifier =
            modifier
                .size(HelloBeTheme.layout.caregiverControlMinHeight)
                .helloBeFocusClearance()
                .focusProperties { canFocus = availability.isFocusable }
                .semantics {
                    if (availability == HelloBeAvailability.DISABLED) disabled()
                    stateDescription?.let { this.stateDescription = it }
                },
        enabled = availability.isFocusable,
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor =
                    if (availability == HelloBeAvailability.UNAVAILABLE) {
                        colors.surfaceMuted
                    } else {
                        colors.actionSecondary
                    },
                contentColor =
                    if (availability == HelloBeAvailability.UNAVAILABLE) {
                        colors.textSecondary
                    } else {
                        colors.onSecondary
                    },
                focusedContainerColor = colors.focusFill,
                focusedContentColor = colors.onFocusFill,
                pressedContainerColor = colors.actionPrimaryPressed,
                pressedContentColor = colors.onPrimary,
                disabledContainerColor = colors.surfaceMuted,
                disabledContentColor = colors.textTertiary
            ),
        scale =
            ClickableSurfaceDefaults.scale(
                focusedScale = focus.scaleButton,
                pressedScale = focus.scaleButton * focus.pressScale
            ),
        border =
            ClickableSurfaceDefaults.border(
                border =
                    if (availability == HelloBeAvailability.UNAVAILABLE) {
                        HelloBeFocusFrame.unavailable(shape)
                    } else {
                        Border.None
                    },
                focusedBorder = HelloBeFocusFrame.ring(shape),
                pressedBorder = HelloBeFocusFrame.ring(shape)
            )
    ) {
        Box(modifier = Modifier.align(Alignment.Center)) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.Unspecified
            )
        }
    }
}
