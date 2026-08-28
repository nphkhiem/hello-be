package com.nphkhiem.englishforyourchildren.feature.profiles

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ProfilePickerRulesTest {

    @Test
    fun givenARememberedProfile_whenFocusIsDecided_thenItGoesThere() {
        assertThat(pickerFocusTarget(stateWith(count = 3, remembered = "p1")))
            .isEqualTo(PickerFocusTarget.REMEMBERED_PROFILE)
    }

    @Test
    fun givenTheRememberedProfileIsNoLongerThere_whenFocusIsDecided_thenItFallsToTheFirst() {
        // The architecture's stale-remembered-profile row: honouring an id that is gone would put
        // focus nowhere.
        assertThat(pickerFocusTarget(stateWith(count = 3, remembered = "gone")))
            .isEqualTo(PickerFocusTarget.FIRST_PROFILE)
    }

    @Test
    fun givenTheRememberedProfileIsUnavailable_whenFocusIsDecided_thenItFallsToTheFirst() {
        val state = stateWith(count = 3, remembered = "p1").let { base ->
            base.copy(
                profiles = base.profiles.map {
                    if (it.id == "p1") it.copy(available = false) else it
                }
            )
        }

        assertThat(pickerFocusTarget(state)).isEqualTo(PickerFocusTarget.FIRST_PROFILE)
    }

    @Test
    fun givenNoProfilesAtAll_whenFocusIsDecided_thenItGoesToAddChild() {
        assertThat(pickerFocusTarget(stateWith(count = 0, remembered = null)))
            .isEqualTo(PickerFocusTarget.ADD_PROFILE)
    }

    @Test
    fun givenEveryProfileIsUnavailable_whenFocusIsDecided_thenItGoesToAddChild() {
        // Focus must never land on something that cannot be chosen.
        val state = stateWith(count = 2, remembered = "p1").let { base ->
            base.copy(profiles = base.profiles.map { it.copy(available = false) })
        }

        assertThat(pickerFocusTarget(state)).isEqualTo(PickerFocusTarget.ADD_PROFILE)
    }

    @Test
    fun givenFewerThanFour_whenAddingIsConsidered_thenAnotherChildFits() {
        assertThat(canAddProfile(stateWith(count = 3, remembered = null))).isTrue()
    }

    @Test
    fun givenFour_whenAddingIsConsidered_thenItDoesNot() {
        assertThat(canAddProfile(stateWith(count = 4, remembered = null))).isFalse()
    }

    @Test
    fun givenMoreThanFourSomehow_whenAddingIsConsidered_thenItStillDoesNot() {
        // Over the limit still draws every child. Only the Add control closes.
        assertThat(canAddProfile(stateWith(count = 5, remembered = null))).isFalse()
    }

    private fun stateWith(count: Int, remembered: String?) = ProfilePickerUiState(
        profiles = List(count) { index ->
            ChildProfile(
                id = "p${index + 1}",
                nickname = "Child ${index + 1}",
                avatar = "A",
                summary = "Continue learning"
            )
        },
        rememberedProfileId = remembered,
        loading = false
    )
}
