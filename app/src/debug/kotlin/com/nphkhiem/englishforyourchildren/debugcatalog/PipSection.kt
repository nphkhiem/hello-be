package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipGuide
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipPose
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/** Human wording for a pose, so raw enum names never reach a content description. */
@Composable
private fun poseLabel(pose: PipPose): String = stringResource(
    when (pose) {
        PipPose.RESTING -> R.string.theme_catalog_pip_pose_resting
        PipPose.GREETING -> R.string.theme_catalog_pip_pose_greeting
        PipPose.POINTING -> R.string.theme_catalog_pip_pose_pointing
        PipPose.MODELING -> R.string.theme_catalog_pip_pose_modeling
        PipPose.CELEBRATING -> R.string.theme_catalog_pip_pose_celebrating
    }
)

/**
 * Every pose side by side, plus one Pip whose pose actually changes.
 *
 * The changing Pip is the point: a static row cannot show a transition, so the reduced-motion
 * substitution would be unreviewable without it. Pressing Next pose with the remote under each
 * motion setting is the only way to see the cross-fade become an instant swap. It also gives the
 * section something focusable, so D-pad navigation reaches it rather than scrolling straight past.
 */
@Composable
internal fun PipSection() {
    var pose by remember { mutableStateOf(PipPose.RESTING) }

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space3)) {
        Text(
            text = stringResource(R.string.theme_catalog_pip_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PipGuide(
                pose = pose,
                contentDescription = stringResource(
                    R.string.theme_catalog_pip_description,
                    poseLabel(pose)
                ),
                modifier = Modifier.size(HelloBeLayout.pipMinSize)
            )
            Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space3)) {
                Text(
                    text = stringResource(R.string.theme_catalog_pip_current, poseLabel(pose)),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.textSecondary
                )
                HelloBeAction(
                    label = stringResource(R.string.theme_catalog_pip_next_pose),
                    onClick = { pose = PipPose.entries[(pose.ordinal + 1) % PipPose.entries.size] },
                    tone = HelloBeActionTone.SECONDARY
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)) {
            PipPose.entries.forEach { entry ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PipGuide(
                        pose = entry,
                        contentDescription = stringResource(
                            R.string.theme_catalog_pip_description,
                            poseLabel(entry)
                        ),
                        modifier = Modifier.size(HelloBeLayout.pipMinSize)
                    )
                    Text(
                        text = poseLabel(entry),
                        style = HelloBeTheme.typography.labelSmall,
                        color = HelloBeTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
