package com.nphkhiem.englishforyourchildren.feature.caregiver

/**
 * Four, and this is the second place that number is written.
 *
 * `:feature:profiles` states it too, and the two modules must not depend on each other, so the cap
 * is duplicated rather than shared. That is the trade the task asks for: duplication over coupling
 * between features. It belongs in `:domain` the day a shared profile model lands there, and until
 * then both copies have to move together.
 */
internal const val MAX_PROFILES = 4

/** Whether another child can be added, which is what makes the add control meaningful. */
internal fun canAddProfile(state: ProfileManagementUiState): Boolean =
    state.profiles.size < MAX_PROFILES

/**
 * The profile the detail pane is about.
 *
 * The named selection is checked against the list before it is used. A caregiver can delete the
 * profile that was selected, and pointing the detail pane at one that is gone would show actions
 * for a child who no longer exists. Falls back to the first, and to nothing only when the list is
 * empty.
 */
internal fun selectedProfile(state: ProfileManagementUiState): ManagedProfile? {
    val named = state.profiles.firstOrNull { it.id == state.selectedId }
    return named ?: state.profiles.firstOrNull()
}

/** How many of the four are in use, for the capacity line the draft puts in the header. */
internal fun capacityUsed(state: ProfileManagementUiState): Int = state.profiles.size
