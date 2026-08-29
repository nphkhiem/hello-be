package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The caregiver's controls, as one shallow list.
 *
 * Every row names the setting, what it does to the child's experience, and where it currently
 * stands, and the last of those is written in words. The approved draft is explicit that state
 * stays visible "in text and semantics instead of relying on toggle color", which is the same rule
 * this product applies to lesson feedback.
 *
 * A choice row expands in place. Pushing a destination for a two-option list would make settings
 * deeper than the information architecture allows, and deeper than a remote deserves.
 *
 * There is deliberately no theme control. `DESIGN_TOKENS.md` keeps night mode out of this phase
 * until either a system-following policy or a caregiver theme control is separately approved, and
 * the task's stop condition names an unapproved child-facing theme switch as the thing not to add.
 *
 * This list scrolls, and that is safe here because every row takes focus: moving down the list
 * brings the next row into view. The overview panel next door does not scroll for the opposite
 * reason.
 */
@Composable
fun CaregiverSettingsScreen(
    state: CaregiverSettingsUiState,
    onAction: (CaregiverSettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstRowFocus = remember { FocusRequester() }
    val firstRowId = state.rows.firstOrNull()?.id

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .helloBeFocusGroup(),
        verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space4)
    ) {
        Head(state = state)

        groupsWithRows(state.rows).forEach { group ->
            Text(
                text = stringResource(groupHeading(group)),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textTertiary
            )

            rowsIn(state.rows, group).forEach { row ->
                SettingRowCard(
                    row = row,
                    expanded = isExpanded(state, row),
                    onAction = onAction,
                    modifier = if (row.id == firstRowId) {
                        Modifier.focusRequester(firstRowFocus)
                    } else {
                        Modifier
                    }
                )
            }
        }

        if (canOfferRestore(state)) {
            HelloBeAction(
                label = stringResource(R.string.settings_restore),
                onClick = { onAction(CaregiverSettingsAction.RestoreDefaultsRequested) },
                tone = HelloBeActionTone.QUIET,
                minHeight = HelloBeLayout.caregiverControlMinHeight
            )
        }
    }
}

@Composable
private fun Head(state: CaregiverSettingsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space2)) {
        Text(
            text = stringResource(R.string.settings_title),
            style = HelloBeTheme.typography.headlineLarge,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = stringResource(R.string.settings_intro),
            style = HelloBeTheme.typography.bodyMedium,
            color = HelloBeTheme.colors.textSecondary
        )

        when (state.saveStatus) {
            SettingsSaveStatus.Idle -> Unit

            SettingsSaveStatus.Saving -> Text(
                text = stringResource(R.string.settings_saving),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textTertiary
            )

            // Said plainly. A caregiver who changed something has to know whether it held.
            SettingsSaveStatus.Failed -> Text(
                text = stringResource(R.string.settings_save_failed),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.errorContent
            )
        }
    }
}

/**
 * One setting.
 *
 * Built on [StoryCard], which already carries a title, a supporting line, availability and a state
 * description. The value goes in the state description as well as on the face of the row, so a
 * screen reader hears where the setting stands rather than only what it is called.
 */
@Composable
private fun SettingRowCard(
    row: SettingRow,
    expanded: Boolean,
    onAction: (CaregiverSettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val valueText = settingValueText(row.value)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space2)
    ) {
        SettingRowControl(
            title = row.title,
            consequence = row.consequence,
            value = valueText,
            onClick = {
                onAction(
                    when (row.value) {
                        is SettingValue.Toggle -> CaregiverSettingsAction.SettingToggled(row.id)
                        is SettingValue.Choice -> CaregiverSettingsAction.SettingExpanded(row.id)
                    }
                )
            },
            modifier = modifier
        )

        if (expanded && row.value is SettingValue.Choice) {
            Row(
                modifier = Modifier.fillMaxWidth().helloBeFocusGroup(),
                horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.value.options.forEach { option ->
                    HelloBeAction(
                        label = option,
                        onClick = {
                            onAction(
                                CaregiverSettingsAction.SettingChoiceChosen(row.id, option)
                            )
                        },
                        tone = if (option == row.value.current) {
                            HelloBeActionTone.SECONDARY
                        } else {
                            HelloBeActionTone.QUIET
                        },
                        minHeight = HelloBeLayout.caregiverControlMinHeight
                    )
                }
            }
        }
    }
}

/** The setting's state, in words. Never a colour, never a shape, always something readable. */
@Composable
private fun settingValueText(value: SettingValue): String = when (value) {
    is SettingValue.Toggle -> if (value.on) {
        stringResource(R.string.settings_on)
    } else {
        stringResource(R.string.settings_off)
    }

    is SettingValue.Choice -> value.current
}

private fun groupHeading(group: SettingGroup): Int = when (group) {
    SettingGroup.LANGUAGE -> R.string.settings_group_language
    SettingGroup.ACCESSIBILITY -> R.string.settings_group_accessibility
    SettingGroup.AUDIO -> R.string.settings_group_audio
}
