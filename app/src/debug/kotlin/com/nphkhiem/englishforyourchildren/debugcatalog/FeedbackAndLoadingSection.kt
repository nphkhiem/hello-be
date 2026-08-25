package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.ui.tv.component.FeedbackPanel
import com.nphkhiem.englishforyourchildren.ui.tv.component.FeedbackTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryLoading
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The three tones side by side, which is the point: they must look like one family. If a reviewer
 * can tell the tone from the container colour alone, ADR 0002 has been broken.
 */
@Composable
internal fun FeedbackAndLoadingSection() {
    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)) {
        SectionNote(stringResource(R.string.theme_catalog_feedback_label))

        Row(horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)) {
            FeedbackPanel(
                message = stringResource(R.string.theme_catalog_feedback_correct),
                tone = FeedbackTone.CORRECT,
                pipDescription = stringResource(R.string.theme_catalog_feedback_pip_celebrating),
                modifier = Modifier.width(HelloBeLayout.cardThreeColumnSet)
            )
            FeedbackPanel(
                message = stringResource(R.string.theme_catalog_feedback_retry),
                tone = FeedbackTone.SUPPORTIVE_RETRY,
                pipDescription = stringResource(R.string.theme_catalog_feedback_pip_modelling),
                modifier = Modifier.width(HelloBeLayout.cardThreeColumnSet)
            )
            FeedbackPanel(
                message = stringResource(R.string.theme_catalog_feedback_information),
                tone = FeedbackTone.INFORMATION,
                pipDescription = stringResource(R.string.theme_catalog_feedback_pip_pointing),
                modifier = Modifier.width(HelloBeLayout.cardThreeColumnSet)
            )
        }

        SectionNote(stringResource(R.string.theme_catalog_loading_label))

        StoryLoading(
            contentDescription = stringResource(R.string.theme_catalog_loading_message),
            modifier = Modifier.width(HelloBeLayout.cardTwoColumnSet)
        )
    }
}

@Composable
private fun SectionNote(text: String) {
    Text(
        text = text,
        style = HelloBeTheme.typography.labelSmall,
        color = HelloBeTheme.colors.textTertiary
    )
}
