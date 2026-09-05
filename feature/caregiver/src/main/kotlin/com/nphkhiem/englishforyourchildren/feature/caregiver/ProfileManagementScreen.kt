package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Where a caregiver switches, edits, or removes the children on this television.
 *
 * Master and detail, as the approved draft draws it: choosing a profile on the left is separate
 * from acting on it on the right, so switching cannot be confused with editing and neither can be
 * confused with deleting.
 *
 * Nothing here destroys anything. Reset and delete ask for their confirmations, which are HB-D21's,
 * and this screen only reports that they were asked for.
 *
 * It carries its own profile type and its own copy of the four-profile cap. `:feature:profiles`
 * has both already, and the two feature modules must not depend on each other, so this is
 * duplication chosen over coupling. A test asserts the classpath stays clean.
 */
@Composable
fun ProfileManagementScreen(
    state: ProfileManagementUiState,
    onAction: (ProfileManagementAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = selectedProfile(state)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space4)
    ) {
        Head(state = state, onAction = onAction)

        if (selected == null) {
            EmptyLibrary()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
            ) {
                ProfileList(
                    state = state,
                    selected = selected,
                    onAction = onAction,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                ProfileDetail(
                    profile = selected,
                    onAction = onAction,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun Head(state: ProfileManagementUiState, onAction: (ProfileManagementAction) -> Unit) {
    val roomForMore = canAddProfile(state)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space2)
        ) {
            Text(
                text = caregiverText(R.string.profiles_title),
                style = HelloBeTheme.typography.headlineLarge,
                color = HelloBeTheme.colors.textPrimary
            )
            Text(
                text = caregiverText(R.string.profiles_local),
                style = HelloBeTheme.typography.bodyMedium,
                color = HelloBeTheme.colors.textSecondary
            )
            Text(
                // Capacity in words rather than a bar. The draft puts it in the header chrome;
                // it sits here instead so the shell stays one shape for every section.
                text = caregiverText(
                    R.string.profiles_capacity,
                    capacityUsed(state),
                    MAX_PROFILES
                ),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textTertiary
            )
            if (state.persistenceFailed) {
                Text(
                    text = caregiverText(R.string.profiles_persistence_failed),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.errorContent
                )
            }
        }

        HelloBeAction(
            label = caregiverText(R.string.profiles_add),
            onClick = { onAction(ProfileManagementAction.AddProfileRequested) },
            tone = HelloBeActionTone.SECONDARY,
            // Focusable at the limit so the reason can be read, never clickable. The same rule
            // the profile picker applies to its own full state, reached independently.
            availability = if (roomForMore) {
                HelloBeAvailability.ENABLED
            } else {
                HelloBeAvailability.UNAVAILABLE
            },
            stateDescription = if (roomForMore) {
                null
            } else {
                caregiverText(R.string.profiles_at_limit)
            },
            minHeight = HelloBeLayout.caregiverControlMinHeight
        )
    }
}

@Composable
private fun ProfileList(
    state: ProfileManagementUiState,
    selected: ManagedProfile,
    onAction: (ProfileManagementAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.helloBeFocusGroup(),
        verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)
    ) {
        state.profiles.forEach { profile ->
            // Selection reads on the subtitle, as the draft writes it, not as a trailing word.
            // Put in the value slot it competed with the child's details for width and wrapped
            // "Age 3 · 6 adventures finished" onto four lines.
            SettingRowControl(
                title = profile.name,
                consequence = if (profile.id == selected.id) {
                    caregiverText(R.string.profiles_selected)
                } else {
                    profile.detail
                },
                value = profile.avatar,
                onClick = { onAction(ProfileManagementAction.ProfileSelected(profile.id)) }
            )
        }
    }
}

/**
 * What can be done to the selected child.
 *
 * Delete sits apart from the rest, which the information architecture asks for in as many words:
 * "visually and spatially separated". A caregiver reaching for Reset progress must not be one
 * press away from removing the child entirely.
 */
@Composable
private fun ProfileDetail(
    profile: ManagedProfile,
    onAction: (ProfileManagementAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        // Scrollable, because every label here carries two languages and wraps to two lines, and
        // the pane was measured against a single-language draft. Without this the last control is
        // pushed past the foot of the pane and clipped, and the last control is delete.
        //
        // A scroll is safe here for the reason the overview gives for refusing one: every row in
        // this pane takes focus, so moving the D-pad moves the scroll. Nothing ends up out of reach
        // rather than merely out of sight.
        modifier = modifier.verticalScroll(rememberScrollState()).helloBeFocusGroup(),
        verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)
    ) {
        Text(
            text = profile.name,
            style = HelloBeTheme.typography.headlineMedium,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = profile.detail,
            style = HelloBeTheme.typography.bodyMedium,
            color = HelloBeTheme.colors.textSecondary
        )

        listOf(
            R.string.profiles_edit_name to ProfileManagementAction.EditNameRequested(profile.id),
            R.string.profiles_change_picture to
                ProfileManagementAction.ChangeAvatarRequested(profile.id),
            R.string.profiles_reset to ProfileManagementAction.ResetProgressRequested(profile.id)
        ).forEach { (label, action) ->
            HelloBeAction(
                label = caregiverText(label),
                onClick = { onAction(action) },
                tone = HelloBeActionTone.QUIET,
                minHeight = HelloBeLayout.caregiverControlMinHeight,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // The separation the information architecture asks for. A fixed gap rather than the
        // remaining room, because a weighted child inside a scrolling column has no room to be
        // given: what mattered was that delete is set apart from the three routine actions, not
        // that it sits exactly on the floor.
        Spacer(modifier = Modifier.height(HelloBeTheme.spacing.space7))

        HelloBeAction(
            label = caregiverText(R.string.profiles_delete),
            onClick = { onAction(ProfileManagementAction.DeleteProfileRequested(profile.id)) },
            tone = HelloBeActionTone.DESTRUCTIVE,
            minHeight = HelloBeLayout.caregiverControlMinHeight,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** No children on this television yet, which is a state and not a failure. */
@Composable
private fun EmptyLibrary() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = HelloBeTheme.spacing.space3,
            alignment = Alignment.CenterVertically
        )
    ) {
        Text(
            text = caregiverText(R.string.profiles_empty_title),
            style = HelloBeTheme.typography.headlineMedium,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = caregiverText(R.string.profiles_empty_hint),
            style = HelloBeTheme.typography.bodyLarge,
            color = HelloBeTheme.colors.textSecondary
        )
    }
}
