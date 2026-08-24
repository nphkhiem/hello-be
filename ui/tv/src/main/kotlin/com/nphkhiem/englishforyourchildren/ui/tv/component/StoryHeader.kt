package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The quiet strip at the top of a stage: where the child is, what they are doing, and at most one
 * action such as replay.
 *
 * Deliberately shallow. Navigation chrome is reduced on a lesson stage so the learning object,
 * instruction and answers stay primary, which is why there is room for exactly one action here.
 *
 * The title is marked as a heading so assistive technology can jump to it, and the header takes
 * its width from the caller rather than filling on its own.
 */
@Composable
fun StoryHeader(
    title: String,
    modifier: Modifier = Modifier,
    contextLabel: String? = null,
    progress: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    val colors = HelloBeTheme.colors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space2)
        ) {
            if (contextLabel != null) {
                Text(
                    text = contextLabel,
                    style = HelloBeTheme.typography.labelSmall,
                    color = colors.textTertiary
                )
            }
            Text(
                text = title,
                style = HelloBeTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
        }

        // Title column and action column carry equal weight, so progress sits on the header's
        // centre line rather than drifting with the length of the title.
        progress?.invoke()

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            action?.invoke()
        }
    }
}
