package com.nphkhiem.englishforyourchildren.feature.profiles

/** The most profiles this television holds, per the design brief. */
internal const val MAX_PROFILES = 4

/** Where the picker puts focus when it appears. */
enum class PickerFocusTarget {
    REMEMBERED_PROFILE,
    FIRST_PROFILE,
    ADD_PROFILE
}

/**
 * Whether another child can be added.
 *
 * Derived rather than stored. More than the maximum still renders every profile: capacity governs
 * the Add control only, because refusing to draw a child's own card would be the worse failure.
 */
internal fun canAddProfile(state: ProfilePickerUiState): Boolean =
    state.profiles.size < MAX_PROFILES

/**
 * Where focus belongs, per `DESIGN_BRIEF.md` S01 and the information architecture: the most
 * recently selected valid profile, otherwise the first, otherwise Add child.
 *
 * A remembered id that no longer matches an available profile falls through rather than being
 * honoured, which is the architecture's stale-remembered-profile row. This is the one rule on the
 * screen that meaningfully reduces how many presses a returning child needs.
 */
internal fun pickerFocusTarget(state: ProfilePickerUiState): PickerFocusTarget = when {
    state.profiles.any { it.available && it.id == state.rememberedProfileId } ->
        PickerFocusTarget.REMEMBERED_PROFILE

    state.profiles.any { it.available } -> PickerFocusTarget.FIRST_PROFILE

    else -> PickerFocusTarget.ADD_PROFILE
}
