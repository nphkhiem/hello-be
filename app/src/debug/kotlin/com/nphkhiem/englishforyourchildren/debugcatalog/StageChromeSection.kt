package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.ui.tv.component.CaptionPanel
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.ProgressTrail
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryHeader
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import java.util.Locale

private const val TOTAL_STEPS = 4

/**
 * The chrome with working controls, so a reviewer can advance the trail and toggle the caption
 * with a remote. Both need to actually change to be reviewable: a still trail cannot show the
 * reduced-motion substitution, and a still caption cannot show that the focal object holds place.
 */
@Composable
internal fun StageChromeSection() {
    // The trail announces from inside a semantics block, which is not a composable scope. The
    // format string is therefore read here, where stringResource re-reads it on configuration
    // change, and only the formatting happens in the lambda.
    val positionFormat = stringResource(R.string.theme_catalog_chrome_position)
    var step by remember { mutableIntStateOf(1) }
    var captionShown by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_chrome_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        StoryHeader(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.theme_catalog_chrome_title),
            contextLabel = stringResource(R.string.theme_catalog_chrome_unit),
            progress = {
                ProgressTrail(
                    totalSteps = TOTAL_STEPS,
                    currentStep = step,
                    describePosition = { current, total ->
                        String.format(Locale.getDefault(), positionFormat, current, total)
                    }
                )
            },
            action = {
                HelloBeAction(
                    label = stringResource(R.string.theme_catalog_chrome_replay),
                    onClick = {},
                    tone = HelloBeActionTone.SECONDARY
                )
            }
        )

        CaptionPanel(
            text = if (captionShown) {
                stringResource(
                    R.string.theme_catalog_chrome_caption
                )
            } else {
                null
            },
            visible = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_chrome_next_step),
                onClick = { step = if (step >= TOTAL_STEPS) 1 else step + 1 },
                tone = HelloBeActionTone.SECONDARY
            )
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_chrome_toggle_caption),
                onClick = { captionShown = !captionShown },
                tone = HelloBeActionTone.SECONDARY
            )
        }
    }
}
