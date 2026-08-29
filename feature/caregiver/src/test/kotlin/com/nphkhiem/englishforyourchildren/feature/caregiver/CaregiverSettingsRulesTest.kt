package com.nphkhiem.englishforyourchildren.feature.caregiver

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CaregiverSettingsRulesTest {

    @Test
    fun givenRowsInSeveralGroups_whenOneGroupIsRead_thenOnlyItsRowsComeBack() {
        val rows = listOf(
            row(SettingId.CAPTIONS, SettingGroup.ACCESSIBILITY),
            row(SettingId.VIETNAMESE_HELP, SettingGroup.LANGUAGE),
            row(SettingId.REDUCED_MOTION, SettingGroup.ACCESSIBILITY)
        )

        assertThat(rowsIn(rows, SettingGroup.ACCESSIBILITY).map { it.id })
            .containsExactly(SettingId.CAPTIONS, SettingId.REDUCED_MOTION).inOrder()
    }

    @Test
    fun givenSomeGroupsAreEmpty_whenTheHeadingsAreChosen_thenOnlyOccupiedGroupsAppear() {
        // A heading over nothing announces a category and then shows a caregiver it is not there.
        val rows = listOf(row(SettingId.CAPTIONS, SettingGroup.ACCESSIBILITY))

        assertThat(groupsWithRows(rows)).containsExactly(SettingGroup.ACCESSIBILITY)
    }

    @Test
    fun givenEveryGroupHasRows_whenTheHeadingsAreChosen_thenTheyKeepTheirDeclaredOrder() {
        val rows = listOf(
            row(SettingId.BACKGROUND_MUSIC, SettingGroup.AUDIO),
            row(SettingId.CAPTIONS, SettingGroup.ACCESSIBILITY),
            row(SettingId.VIETNAMESE_HELP, SettingGroup.LANGUAGE)
        )

        assertThat(groupsWithRows(rows))
            .containsExactly(
                SettingGroup.LANGUAGE,
                SettingGroup.ACCESSIBILITY,
                SettingGroup.AUDIO
            ).inOrder()
    }

    @Test
    fun givenNothingHasChanged_whenRestoreIsConsidered_thenItIsNotOffered() {
        assertThat(canOfferRestore(state(canRestore = false))).isFalse()
    }

    @Test
    fun givenSomethingHasChanged_whenRestoreIsConsidered_thenItIsOffered() {
        assertThat(canOfferRestore(state(canRestore = true))).isTrue()
    }

    @Test
    fun givenASaveInFlight_whenRestoreIsConsidered_thenItWaits() {
        // Restoring on top of a write that has not landed leaves a caregiver unable to say what
        // they asked for.
        val saving = state(canRestore = true, status = SettingsSaveStatus.Saving)

        assertThat(canOfferRestore(saving)).isFalse()
    }

    @Test
    fun givenASaveFailed_whenRestoreIsConsidered_thenItIsStillOffered() {
        // A failed save is exactly when a caregiver may want to put things back.
        val failed = state(canRestore = true, status = SettingsSaveStatus.Failed)

        assertThat(canOfferRestore(failed)).isTrue()
    }

    @Test
    fun givenAChoiceRowIsNamed_whenExpansionIsRead_thenThatRowIsExpanded() {
        val choice = row(
            SettingId.CAREGIVER_LANGUAGE,
            SettingGroup.LANGUAGE,
            SettingValue.Choice(current = "English", options = listOf("English", "Tiếng Việt"))
        )
        val expanded = state(canRestore = false).copy(expandedRow = SettingId.CAREGIVER_LANGUAGE)

        assertThat(isExpanded(expanded, choice)).isTrue()
    }

    @Test
    fun givenAToggleRowIsNamed_whenExpansionIsRead_thenItStillCannotExpand() {
        // A toggle has nothing to expand into, so naming it must not open anything.
        val toggle = row(SettingId.CAPTIONS, SettingGroup.ACCESSIBILITY)
        val expanded = state(canRestore = false).copy(expandedRow = SettingId.CAPTIONS)

        assertThat(isExpanded(expanded, toggle)).isFalse()
    }

    private fun row(
        id: SettingId,
        group: SettingGroup,
        value: SettingValue = SettingValue.Toggle(on = true)
    ) = SettingRow(id = id, group = group, title = id.name, consequence = "", value = value)

    private fun state(canRestore: Boolean, status: SettingsSaveStatus = SettingsSaveStatus.Idle) =
        CaregiverSettingsUiState(
            rows = emptyList(),
            expandedRow = null,
            saveStatus = status,
            canRestoreDefaults = canRestore
        )
}
