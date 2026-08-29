package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.runtime.Immutable

/**
 * A profile as the caregiver area needs to draw it.
 *
 * Deliberately this module's own type rather than one borrowed from `:feature:profiles`. The task
 * forbids a dependency between the two feature modules, and `:domain` carries no shared profile
 * model yet, so caregiver describes what caregiver draws. When a shared model does land this is
 * the type to replace.
 *
 * [detail] is a finished line rather than an age and a count, because what belongs on it is a
 * product decision that changes without this screen changing.
 */
@Immutable
data class ManagedProfile(val id: String, val name: String, val avatar: String, val detail: String)

/**
 * Everything profile management needs to draw itself.
 *
 * [selectedId] may name a profile that is no longer here, which is the brief's stale-selection
 * state. The rules resolve it rather than trusting it, so the detail pane never points at nothing.
 */
@Immutable
data class ProfileManagementUiState(
    val profiles: List<ManagedProfile>,
    val selectedId: String?,
    val persistenceFailed: Boolean
)

/** What profile management reports upward. Every action names the profile it is about. */
sealed interface ProfileManagementAction {
    data class ProfileSelected(val id: String) : ProfileManagementAction

    data object AddProfileRequested : ProfileManagementAction

    data class EditNameRequested(val id: String) : ProfileManagementAction

    data class ChangeAvatarRequested(val id: String) : ProfileManagementAction

    /** Asks for the reset confirmation. Nothing is reset by pressing this. */
    data class ResetProgressRequested(val id: String) : ProfileManagementAction

    /** Asks for the delete confirmation. Nothing is deleted by pressing this. */
    data class DeleteProfileRequested(val id: String) : ProfileManagementAction
}
