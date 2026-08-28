package com.nphkhiem.englishforyourchildren.feature.profiles

/**
 * Whether the profile can be made yet.
 *
 * An age is required, and this is how that requirement is expressed: Create stays unreachable
 * until one is chosen, rather than a message appearing after someone presses it.
 *
 * Capacity is checked too. The picker closes Add child at four so this should be unreachable, and
 * unreachable states are how screens break.
 */
internal fun canCreateProfile(state: CreateProfileUiState): Boolean =
    state.draft.age != null && !state.capacityReached
