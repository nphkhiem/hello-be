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
import com.nphkhiem.englishforyourchildren.ui.tv.component.ChoiceCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Every HB-D02 interaction state on one screen so focus, press, selection, and availability
 * can be walked with a real remote rather than inspected in code. Disabled entries are
 * included deliberately: D-pad traversal must skip them without leaving a gap in the row.
 */
@Composable
internal fun FocusLabSection() {
    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)) {
        FocusLabRowLabel(stringResource(R.string.theme_catalog_focus_actions_label))
        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_action_continue),
                onClick = {}
            )
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_action_free_play),
                onClick = {},
                tone = HelloBeActionTone.SECONDARY
            )
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_action_delete),
                onClick = {},
                tone = HelloBeActionTone.DESTRUCTIVE
            )
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_action_later),
                onClick = {},
                availability = HelloBeAvailability.UNAVAILABLE,
                stateDescription = stringResource(R.string.theme_catalog_state_later)
            )
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_action_disabled),
                onClick = {},
                availability = HelloBeAvailability.DISABLED
            )
        }

        FocusLabRowLabel(stringResource(R.string.theme_catalog_focus_cards_label))
        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            StoryCard(
                title = stringResource(R.string.theme_catalog_card_current),
                supportingText = stringResource(R.string.theme_catalog_card_supporting),
                onClick = {},
                selected = true,
                stateDescription = stringResource(R.string.theme_catalog_state_selected),
                modifier = Modifier.width(HelloBeTheme.layout.cardFourColumnSet)
            )
            StoryCard(
                title = stringResource(R.string.theme_catalog_card_next),
                supportingText = stringResource(R.string.theme_catalog_card_supporting),
                onClick = {},
                modifier = Modifier.width(HelloBeTheme.layout.cardFourColumnSet)
            )
            StoryCard(
                title = stringResource(R.string.theme_catalog_card_locked),
                onClick = {},
                availability = HelloBeAvailability.UNAVAILABLE,
                stateDescription = stringResource(R.string.theme_catalog_state_later),
                modifier = Modifier.width(HelloBeTheme.layout.cardFourColumnSet)
            )
        }

        FocusLabRowLabel(stringResource(R.string.theme_catalog_focus_choices_label))
        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            ChoiceCard(
                label = stringResource(R.string.theme_catalog_choice_apple),
                onClick = {},
                modifier = Modifier.width(HelloBeTheme.layout.cardFourColumnSet)
            )
            ChoiceCard(
                label = stringResource(R.string.theme_catalog_choice_ball),
                onClick = {},
                feedback = HelloBeChoiceFeedback.CORRECT,
                stateDescription = stringResource(R.string.theme_catalog_state_correct),
                modifier = Modifier.width(HelloBeTheme.layout.cardFourColumnSet)
            )
            ChoiceCard(
                label = stringResource(R.string.theme_catalog_choice_cat),
                onClick = {},
                feedback = HelloBeChoiceFeedback.SUPPORTIVE_RETRY,
                stateDescription = stringResource(R.string.theme_catalog_state_retry),
                modifier = Modifier.width(HelloBeTheme.layout.cardFourColumnSet)
            )
        }
    }
}

@Composable
private fun FocusLabRowLabel(text: String) {
    Text(
        text = text,
        style = HelloBeTheme.typography.labelSmall,
        color = HelloBeTheme.colors.textTertiary
    )
}
