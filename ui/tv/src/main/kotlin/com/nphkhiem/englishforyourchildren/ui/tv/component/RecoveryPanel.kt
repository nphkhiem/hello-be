package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * What a screen shows instead of itself when it cannot be what it was.
 *
 * Structurally generic on purpose: it knows a heading, a calm explanation, one safe action and an
 * optional second, and nothing about why any of them are there. Every reason, every word and every
 * mapping from reason to action lives in the feature that owns the failure.
 *
 * [safeAction] takes entry focus, always. The approved recovery rules open with "recovery never
 * opens with a destructive action focused", so the focused slot is the safe one by construction
 * rather than by each caller remembering.
 *
 * [technicalDetail] is the seam the child surfaces must not reach through. It is nullable here
 * because this component is shared, but no child recovery model carries a field that could fill
 * it: diagnostic codes belong behind the adult gate, and the way to hold that is for the child's
 * type to have nowhere to put one.
 */
@Composable
fun RecoveryPanel(
    title: String,
    message: String,
    safeAction: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    secondaryAction: (@Composable (Modifier) -> Unit)? = null,
    technicalDetail: String? = null,
    illustration: (@Composable () -> Unit)? = null
) {
    val safeFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        safeFocus.requestFocus()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = HelloBeLayout.dialogMaxWidth),
            shape = HelloBeShapes.dialog,
            colors = SurfaceDefaults.colors(
                containerColor = HelloBeTheme.colors.surfaceRaised,
                contentColor = HelloBeTheme.colors.textPrimary
            ),
            border = HelloBeFocusFrame.resting(HelloBeShapes.dialog)
        ) {
            Column(
                modifier = Modifier.padding(HelloBeTheme.spacing.sectionGap),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
            ) {
                illustration?.invoke()

                if (kicker != null) {
                    Text(
                        text = kicker,
                        style = HelloBeTheme.typography.labelSmall,
                        color = HelloBeTheme.colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = title,
                    style = HelloBeTheme.typography.headlineMedium,
                    color = HelloBeTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = HelloBeTheme.typography.bodyLarge,
                    color = HelloBeTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.helloBeFocusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
                ) {
                    safeAction(Modifier.focusRequester(safeFocus))
                    secondaryAction?.invoke(Modifier)
                }

                if (technicalDetail != null) {
                    // Last, smallest, and quietest. A caregiver reading a code is looking for it;
                    // it must never compete with the sentence that says what to do next.
                    Text(
                        text = technicalDetail,
                        style = HelloBeTheme.typography.labelSmall,
                        color = HelloBeTheme.colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
