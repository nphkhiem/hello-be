package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.runtime.Immutable

/** The three groups a settings list is allowed to have, per the information architecture. */
enum class SettingGroup {
    LANGUAGE,
    ACCESSIBILITY,
    AUDIO
}

/** Which setting a row is. Typed, so a row cannot be reported by a string a caller invented. */
enum class SettingId {
    VIETNAMESE_HELP,
    CAREGIVER_LANGUAGE,
    CAPTIONS,
    REDUCED_MOTION,
    HIGH_CONTRAST,
    BACKGROUND_MUSIC
}

/**
 * What a setting currently is.
 *
 * Both carry their state as text. The approved draft is explicit that state stays visible "in text
 * and semantics instead of relying on toggle color", which is the same rule this product applies to
 * lesson feedback: colour may reinforce a state, never carry it alone.
 */
sealed interface SettingValue {
    /** On or off, and the word for it. */
    data class Toggle(val on: Boolean) : SettingValue

    /**
     * One of several named options.
     *
     * [options] is here so the row can expand in place. A choice that opened a destination would
     * make settings deeper than the information architecture allows.
     */
    data class Choice(val current: String, val options: List<String>) : SettingValue
}

/** One row: what it is, what it does to the child's experience, and where it stands. */
@Immutable
data class SettingRow(
    val id: SettingId,
    val group: SettingGroup,
    val title: String,
    /** The child-facing consequence, which the information architecture asks for by name. */
    val consequence: String,
    val value: SettingValue
)

/** Whether the last change reached storage. */
sealed interface SettingsSaveStatus {
    data object Idle : SettingsSaveStatus

    data object Saving : SettingsSaveStatus

    /** Saving failed. Said plainly, because a caregiver who changed something must know it held. */
    data object Failed : SettingsSaveStatus
}

/**
 * Everything the settings list needs to draw itself.
 *
 * [expandedRow] is the one choice row showing its options, or null. A row expands in place rather
 * than pushing a destination, which is what keeps this screen shallow.
 */
@Immutable
data class CaregiverSettingsUiState(
    val rows: List<SettingRow>,
    val expandedRow: SettingId?,
    val saveStatus: SettingsSaveStatus,
    /** True only when something differs from its default, per "available only when meaningful". */
    val canRestoreDefaults: Boolean
)

/** What the settings list reports upward. */
sealed interface CaregiverSettingsAction {
    data class SettingToggled(val id: SettingId) : CaregiverSettingsAction

    /** A choice row was opened or closed. Expansion is state, so the host owns it. */
    data class SettingExpanded(val id: SettingId) : CaregiverSettingsAction

    data class SettingChoiceChosen(val id: SettingId, val option: String) :
        CaregiverSettingsAction

    data object RestoreDefaultsRequested : CaregiverSettingsAction
}
