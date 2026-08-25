package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipGuide
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipPose
import com.nphkhiem.englishforyourchildren.ui.tv.component.StorybookScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Shows the stage's region structure with each slot outlined: scenery reaching the edge of its
 * container while header, content and support sit inside the safe inset.
 *
 * This demonstrates the relationship, not real overscan. The preview is a box inside a scrolling
 * catalog, so scenery reaches the preview's edge rather than the television's. Genuine overscan
 * behaviour is proved by the instrumented safe-area tests, which measure every slot at 720p,
 * 1080p and 4K densities.
 */
private const val STAGE_PREVIEW_DIVISOR = 2

@Composable
internal fun StageSection() {
    val colors = HelloBeTheme.colors

    Column {
        Text(
            text = stringResource(R.string.theme_catalog_stage_label),
            style = HelloBeTheme.typography.labelSmall,
            color = colors.textTertiary
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight / STAGE_PREVIEW_DIVISOR)
                .background(colors.scenery, HelloBeShapes.medium)
        ) {
            StorybookScaffold(
                scenery = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                HelloBeTheme.focus.guardWidth,
                                colors.accentWarm,
                                HelloBeShapes.medium
                            )
                    )
                },
                header = {
                    StageRegion(
                        label = stringResource(R.string.theme_catalog_stage_header),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                support = {
                    // Pip sits in the support strip, which is what keeps it structurally unable to
                    // cover the focal object above.
                    PipGuide(
                        pose = PipPose.RESTING,
                        contentDescription = stringResource(R.string.theme_catalog_stage_pip),
                        modifier = Modifier.size(HelloBeLayout.pipMinSize)
                    )
                    StageRegion(
                        label = stringResource(R.string.theme_catalog_stage_support),
                        modifier = Modifier.weight(1f)
                    )
                }
            ) {
                StageRegion(
                    label = stringResource(R.string.theme_catalog_stage_content),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun StageRegion(label: String, modifier: Modifier = Modifier) {
    val colors = HelloBeTheme.colors
    Box(
        modifier = modifier
            .background(colors.surfacePrimary, HelloBeShapes.small)
            .border(HelloBeTheme.focus.guardWidth, colors.borderPrimary, HelloBeShapes.small),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = HelloBeTheme.typography.labelSmall,
            color = colors.textSecondary
        )
    }
}
