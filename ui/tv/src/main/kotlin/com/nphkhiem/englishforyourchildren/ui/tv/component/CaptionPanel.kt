package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

private const val CAPTION_MAX_LINES = 2

/**
 * The spoken instruction shown as text, for caregivers and for children starting to read.
 *
 * Two requirements pull against each other: a caption must never move the focal object, yet a
 * caption with nothing to say must not clutter a calm stage. They are reconciled by separating
 * the space from what is drawn in it.
 *
 * [visible] is the caregiver's setting. Captions off renders nothing and occupies no space at all.
 *
 * While captions are on, the panel holds a fixed height of [CAPTION_MAX_LINES] lines. It is fixed
 * rather than a minimum on purpose: a floor would still grow for a line that wraps, moving the
 * object a child is looking at. A rare long line is truncated instead, because captions support
 * the spoken audio rather than replacing it, and losing the tail of one line costs less than
 * moving the focal object.
 *
 * Between lines the space stays reserved but nothing is painted, so a child sees the stage rather
 * than an empty panel.
 */
@Composable
fun CaptionPanel(text: String?, visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return

    val motion = HelloBeTheme.motion
    val colors = HelloBeTheme.colors
    val hasLine = !text.isNullOrBlank()
    val opacity by animateFloatAsState(
        targetValue = if (hasLine) 1f else 0f,
        animationSpec = tween(durationMillis = motion.captionTransitionMillis),
        label = "captionOpacity"
    )

    Box(
        modifier = modifier.height(HelloBeLayout.captionReservedHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(opacity)
                .background(
                    if (hasLine) colors.surfaceRaised else Color.Transparent,
                    HelloBeShapes.medium
                )
                .padding(HelloBeTheme.spacing.space4),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.orEmpty(),
                style = HelloBeTheme.typography.bodyMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = CAPTION_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
