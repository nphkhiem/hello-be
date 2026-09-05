package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryHeader
import com.nphkhiem.englishforyourchildren.ui.tv.component.StorybookScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The stable shell every caregiver section sits inside.
 *
 * There is no gate anywhere in this tree. The information architecture allows one gate per
 * foreground caregiver session and no repeated challenge between sections, so moving between
 * Overview, Settings and Profiles must not be able to summon one.
 *
 * That session is the host's. It is scoped to app foreground and process lifetime, which is
 * lifecycle, and ADR 0003 keeps lifecycle out of feature modules. What this shell guarantees is the
 * half it can: sections are reached by rail selection alone.
 *
 * It wraps [StorybookScaffold] rather than reimplementing the safe area and the entry focus
 * contract, but passes no scenery. The design brief asks caregiver tools to be visually related to
 * the child experience without imitating it, and inheriting the stage while declining the
 * storybook dressing is what that looks like.
 */
@Composable
fun CaregiverScaffold(
    state: CaregiverShellState,
    onAction: (CaregiverShellAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val sectionFocus = remember { FocusRequester() }

    StorybookScaffold(
        modifier = modifier,
        entryFocus = sectionFocus,
        header = {
            StoryHeader(
                modifier = Modifier.fillMaxWidth(),
                title = caregiverText(R.string.caregiver_area),
                action = {
                    Text(
                        text = state.profileName,
                        style = HelloBeTheme.typography.titleSmall,
                        color = HelloBeTheme.colors.textSecondary
                    )
                }
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.sectionGap)
        ) {
            SectionRail(state = state, onAction = onAction, sectionFocus = sectionFocus)

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                content()
            }
        }
    }
}

/**
 * The rail: exactly three sections, then the way out, kept apart from them.
 *
 * The separation is spatial and required. Returning to child mode ends the caregiver session, and
 * a control that ends a session should not sit in the same run as controls that merely move within
 * one, where a caregiver aiming for Profiles could land on it.
 *
 * Focus restoration between the rail and the panel comes from `helloBeFocusGroup`, which already
 * carries a restorer, rather than being rebuilt here.
 */
@Composable
private fun SectionRail(
    state: CaregiverShellState,
    onAction: (CaregiverShellAction) -> Unit,
    sectionFocus: FocusRequester
) {
    Column(
        modifier = Modifier
            .width(HelloBeLayout.caregiverRailWidth)
            .fillMaxHeight()
            .helloBeFocusGroup(),
        verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)
    ) {
        Text(
            text = caregiverText(R.string.caregiver_rail_title, state.profileName),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        CaregiverSection.entries.forEach { section ->
            HelloBeAction(
                label = caregiverText(sectionLabel(section)),
                onClick = { onAction(CaregiverShellAction.SectionChosen(section)) },
                tone = if (section == state.section) {
                    HelloBeActionTone.SECONDARY
                } else {
                    HelloBeActionTone.QUIET
                },
                minHeight = HelloBeLayout.caregiverControlMinHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (section == state.section) {
                            Modifier.focusRequester(sectionFocus)
                        } else {
                            Modifier
                        }
                    )
            )
        }

        // The spatial separation the information architecture asks for. A spacer that takes the
        // remaining height, so the way out sits at the foot of the rail rather than fourth in a
        // list of four.
        Box(modifier = Modifier.weight(1f))

        HelloBeAction(
            label = caregiverText(R.string.caregiver_return),
            onClick = { onAction(CaregiverShellAction.ReturnToChildRequested) },
            tone = HelloBeActionTone.QUIET,
            minHeight = HelloBeLayout.caregiverControlMinHeight,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun sectionLabel(section: CaregiverSection): Int = when (section) {
    CaregiverSection.OVERVIEW -> R.string.caregiver_overview
    CaregiverSection.SETTINGS -> R.string.caregiver_settings
    CaregiverSection.PROFILES -> R.string.caregiver_profiles
}
