package com.nphkhiem.englishforyourchildren.feature.caregiver

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ProfileManagementRulesTest {

    @Test
    fun givenFewerThanFourProfiles_whenAddingIsConsidered_thenAnotherChildFits() {
        assertThat(canAddProfile(state(profiles(3)))).isTrue()
    }

    @Test
    fun givenFourProfiles_whenAddingIsConsidered_thenTheLimitIsReached() {
        assertThat(canAddProfile(state(profiles(MAX_PROFILES)))).isFalse()
    }

    @Test
    fun givenTheSelectedProfileIsPresent_whenItIsResolved_thenThatProfileComesBack() {
        val list = profiles(3)

        assertThat(selectedProfile(state(list, selectedId = list[1].id))).isEqualTo(list[1])
    }

    @Test
    fun givenTheSelectedProfileHasGone_whenItIsResolved_thenItFallsToTheFirst() {
        // A caregiver can delete the profile that was selected. Pointing the detail pane at a
        // child who no longer exists would offer actions for them.
        val list = profiles(2)

        assertThat(selectedProfile(state(list, selectedId = "deleted"))).isEqualTo(list.first())
    }

    @Test
    fun givenNothingSelected_whenItIsResolved_thenItFallsToTheFirst() {
        val list = profiles(2)

        assertThat(selectedProfile(state(list, selectedId = null))).isEqualTo(list.first())
    }

    @Test
    fun givenNoProfilesAtAll_whenSelectionIsResolved_thenThereIsNone() {
        assertThat(selectedProfile(state(emptyList()))).isNull()
    }

    @Test
    fun givenAnyNumberOfProfiles_whenCapacityIsRead_thenItCountsWhatIsThere() {
        assertThat(capacityUsed(state(profiles(2)))).isEqualTo(2)
        assertThat(capacityUsed(state(emptyList()))).isEqualTo(0)
    }

    private fun profiles(count: Int) = (1..count).map {
        ManagedProfile(id = "p$it", name = "Child $it", avatar = "C", detail = "Age $it")
    }

    private fun state(profiles: List<ManagedProfile>, selectedId: String? = null) =
        ProfileManagementUiState(
            profiles = profiles,
            selectedId = selectedId,
            persistenceFailed = false
        )
}
