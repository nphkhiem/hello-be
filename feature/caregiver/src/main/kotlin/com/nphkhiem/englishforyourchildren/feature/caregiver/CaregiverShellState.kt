package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.runtime.Immutable

/**
 * The three sections the caregiver rail contains, and exactly those.
 *
 * An enum rather than a list of destinations, because the information architecture says the rail
 * contains exactly Overview, Settings and Profiles. A list would let a fourth appear, and the whole
 * point of a stable rail is that it does not grow.
 */
enum class CaregiverSection {
    OVERVIEW,
    SETTINGS,
    PROFILES
}

/** Everything the caregiver shell needs to draw itself. */
@Immutable
data class CaregiverShellState(val profileName: String, val section: CaregiverSection)

/** What the caregiver shell reports upward. */
sealed interface CaregiverShellAction {
    data class SectionChosen(val section: CaregiverSection) : CaregiverShellAction

    /**
     * Leave the caregiver area. Separate from choosing a section because it ends the caregiver
     * session rather than moving within it, which is also why the rail keeps it apart on screen.
     */
    data object ReturnToChildRequested : CaregiverShellAction
}
