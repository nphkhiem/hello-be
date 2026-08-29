package com.nphkhiem.englishforyourchildren.feature.caregiver

/**
 * The rows of one group, in the order the state supplies them.
 *
 * Grouping is a read over the list rather than a second list per group, so a row cannot exist in
 * two groups or in none.
 */
internal fun rowsIn(rows: List<SettingRow>, group: SettingGroup): List<SettingRow> =
    rows.filter { it.group == group }

/**
 * The groups that actually have rows, in their declared order.
 *
 * An empty group draws no heading. A heading over nothing tells a caregiver a category exists and
 * then shows them it does not.
 */
internal fun groupsWithRows(rows: List<SettingRow>): List<SettingGroup> =
    SettingGroup.entries.filter { group -> rows.any { it.group == group } }

/**
 * Whether the restore action is offered.
 *
 * Only when something differs from its default, which the information architecture asks for as
 * "available only when meaningful", and never while a save is still in flight: restoring on top of
 * a write that has not landed would leave a caregiver unable to say what they had asked for.
 */
internal fun canOfferRestore(state: CaregiverSettingsUiState): Boolean =
    state.canRestoreDefaults && state.saveStatus != SettingsSaveStatus.Saving

/** Whether this row is the one showing its options. */
internal fun isExpanded(state: CaregiverSettingsUiState, row: SettingRow): Boolean =
    state.expandedRow == row.id && row.value is SettingValue.Choice
