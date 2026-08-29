package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverFixtures
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverOverviewScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverScaffold
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSection
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSettingsAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSettingsScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverShellAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.ProfileManagementAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.ProfileManagementScreen
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeSpacing
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The barrier into the caregiver area.
 *
 * Walk it with one test in hand: press Select without moving focus first, in every state. It must
 * never report the correct answer. The "correct answer first" state is the one that would have gone
 * wrong under a plain "focus the first control" rule.
 *
 * The unusable state is the fail-closed case. Its only answer would be the right one, so it draws
 * no answers at all rather than a gate that opens on a single press.
 */
@Composable
internal fun AdultGateCatalogSection() {
    val states = remember { CaregiverFixtures.gateStates() }
    var index by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<AdultGateAction?>(null) }
    val (stateName, state) = states[index]

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_gate_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_lesson_next_state),
                onClick = { index = (index + 1) % states.size },
                tone = HelloBeActionTone.SECONDARY
            )
            Text(
                text = stringResource(
                    R.string.theme_catalog_lesson_showing,
                    index + 1,
                    states.size,
                    stateName
                ),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textSecondary
            )
            lastAction?.let { action ->
                Text(
                    text = stringResource(
                        R.string.theme_catalog_lesson_last_action,
                        action.toString()
                    ),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.textTertiary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight)
        ) {
            AdultGateScreen(state = state, onAction = { lastAction = it })
        }
    }
}

/** The stable shell. Nothing in it may ask the gate question again. */
@Composable
internal fun CaregiverShellCatalogSection() {
    val states = remember { CaregiverFixtures.shellStates() }
    var index by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<CaregiverShellAction?>(null) }
    val (stateName, state) = states[index]

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_shell_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_lesson_next_state),
                onClick = { index = (index + 1) % states.size },
                tone = HelloBeActionTone.SECONDARY
            )
            Text(
                text = stringResource(
                    R.string.theme_catalog_lesson_showing,
                    index + 1,
                    states.size,
                    stateName
                ),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textSecondary
            )
            lastAction?.let { action ->
                Text(
                    text = stringResource(
                        R.string.theme_catalog_lesson_last_action,
                        action.toString()
                    ),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.textTertiary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight)
        ) {
            CaregiverScaffold(state = state, onAction = { lastAction = it }) {
                // A stand-in for whatever section is open. The sections themselves are HB-D18
                // and later; this task builds the shell they will sit inside.
                Text(
                    text = stateName,
                    style = HelloBeTheme.typography.titleSmall,
                    color = HelloBeTheme.colors.textSecondary
                )
            }
        }
    }
}

/**
 * What a caregiver reads about one child.
 *
 * Shown inside the real shell, because the panel is never seen on its own. Walk it asking whether
 * anything reads as a score, a ranking, or a log: the summaries are bounded to three and the recent
 * words to six by the screen itself, so the two overflowing states are the ones to check.
 */
@Composable
internal fun CaregiverOverviewCatalogSection() {
    val states = remember { CaregiverFixtures.overviewStates() }
    var index by remember { mutableIntStateOf(0) }
    val (stateName, state) = states[index]

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_overview_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_lesson_next_state),
                onClick = { index = (index + 1) % states.size },
                tone = HelloBeActionTone.SECONDARY
            )
            Text(
                text = stringResource(
                    R.string.theme_catalog_lesson_showing,
                    index + 1,
                    states.size,
                    stateName
                ),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textSecondary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight)
        ) {
            CaregiverScaffold(state = CaregiverFixtures.shell(), onAction = {}) {
                CaregiverOverviewScreen(state = state)
            }
        }
    }
}

/**
 * The caregiver's controls.
 *
 * Walk it with the one question the draft asks: can you tell what each row is set to without
 * looking at a colour? Every state is a word on the row and in its state description.
 *
 * Also worth checking that nothing here switches the child's theme. Night mode is not an approved
 * caregiver control in this phase.
 */
@Composable
internal fun CaregiverSettingsCatalogSection() {
    val states = remember { CaregiverFixtures.settingsStates() }
    var index by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<CaregiverSettingsAction?>(null) }
    val (stateName, state) = states[index]

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_settings_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_lesson_next_state),
                onClick = { index = (index + 1) % states.size },
                tone = HelloBeActionTone.SECONDARY
            )
            Text(
                text = stringResource(
                    R.string.theme_catalog_lesson_showing,
                    index + 1,
                    states.size,
                    stateName
                ),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textSecondary
            )
            lastAction?.let { action ->
                Text(
                    text = stringResource(
                        R.string.theme_catalog_lesson_last_action,
                        action.toString()
                    ),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.textTertiary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight)
        ) {
            CaregiverScaffold(
                state = CaregiverFixtures.shell(CaregiverSection.SETTINGS),
                onAction = {}
            ) {
                CaregiverSettingsScreen(state = state, onAction = { lastAction = it })
            }
        }
    }
}

/**
 * Switching, editing, and removing children.
 *
 * Walk it asking whether delete is far enough from everything else. A caregiver reaching for Reset
 * progress must never be one press away from removing the child, which is why the two sit at
 * opposite ends of the pane.
 *
 * Nothing here destroys anything: both destructive rows ask for their confirmations, which are
 * HB-D21's.
 */
@Composable
internal fun ProfileManagementCatalogSection() {
    val states = remember { CaregiverFixtures.profileStates() }
    var index by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<ProfileManagementAction?>(null) }
    val (stateName, state) = states[index]

    Column(verticalArrangement = Arrangement.spacedBy(HelloBeSpacing.space4)) {
        Text(
            text = stringResource(R.string.theme_catalog_profiles_label),
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeSpacing.cardGap)
        ) {
            HelloBeAction(
                label = stringResource(R.string.theme_catalog_lesson_next_state),
                onClick = { index = (index + 1) % states.size },
                tone = HelloBeActionTone.SECONDARY
            )
            Text(
                text = stringResource(
                    R.string.theme_catalog_lesson_showing,
                    index + 1,
                    states.size,
                    stateName
                ),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textSecondary
            )
            lastAction?.let { action ->
                Text(
                    text = stringResource(
                        R.string.theme_catalog_lesson_last_action,
                        action.toString()
                    ),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.textTertiary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeLayout.referenceHeight)
        ) {
            CaregiverScaffold(
                state = CaregiverFixtures.shell(CaregiverSection.PROFILES),
                onAction = {}
            ) {
                ProfileManagementScreen(state = state, onAction = { lastAction = it })
            }
        }
    }
}
