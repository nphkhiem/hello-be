package com.nphkhiem.englishforyourchildren.feature.profiles

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CreateProfileRulesTest {

    @Test
    fun givenNoAgeChosenYet_whenCreatingIsConsidered_thenItIsNotOffered() {
        // The age requirement is expressed by Create being unreachable, not by a message after
        // someone presses it.
        assertThat(canCreateProfile(stateWith(age = null))).isFalse()
    }

    @Test
    fun givenAnAgeIsChosen_whenCreatingIsConsidered_thenItIsOffered() {
        assertThat(canCreateProfile(stateWith(age = 3))).isTrue()
    }

    @Test
    fun givenTheTelevisionIsFull_whenCreatingIsConsidered_thenItIsNotOfferedEvenWithAnAge() {
        assertThat(canCreateProfile(stateWith(age = 4, capacity = true))).isFalse()
    }

    private fun stateWith(age: Int?, capacity: Boolean = false) = CreateProfileUiState(
        draft = ProfileDraft(nickname = "Bé 1", avatarId = "tiger", age = age),
        ages = listOf(3, 4, 5),
        avatarChoices = listOf("tiger", "rabbit"),
        choosingAvatar = false,
        capacityReached = capacity,
        saveFailed = false
    )
}
