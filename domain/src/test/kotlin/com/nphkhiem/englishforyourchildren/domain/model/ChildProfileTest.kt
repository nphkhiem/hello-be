package com.nphkhiem.englishforyourchildren.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChildProfileTest {
    @Test
    fun givenABlankNickname_whenAProfileIsCreated_thenItIsRejected() {
        // A profile with no name is a profile a child cannot recognise on the picker, which is the
        // one screen they have to read before they can read.
        assertThrows<IllegalArgumentException> { profile(nickname = "") }
        assertThrows<IllegalArgumentException> { profile(nickname = "   ") }
    }

    @Test
    fun givenPaddingAroundANickname_whenAProfileIsCreated_thenItIsKeptAsGiven() {
        // The domain does not tidy what a caregiver typed. Trimming belongs where the text is
        // captured, in one place, not in every model that happens to hold a name.
        assertThat(profile(nickname = " Minh ").nickname).isEqualTo(" Minh ")
    }

    // There is deliberately no test asserting that ChildProfile has no birth date or photograph.
    // Reflecting over the members needs kotlin-reflect, which is a dependency this module does not
    // have, and the assertion it would buy is weak: it can only name the fields someone might have
    // added, and nobody adds a field called photoUri by accident. The property is held by the type
    // having nowhere to put one, and stated in its documentation.

    @Test
    fun givenTheSupportedAges_whenAgeBandsAreListed_thenThreeToFiveAreCovered() {
        // Three to five is the audience the brief names. A fourth band would be a product decision,
        // not a modelling one, so it fails here first.
        assertThat(AgeBand.entries).containsExactly(AgeBand.THREE, AgeBand.FOUR, AgeBand.FIVE)
    }

    private fun profile(nickname: String) = ChildProfile(
        id = ProfileId(PROFILE),
        nickname = nickname,
        ageBand = AgeBand.THREE,
        avatarId = AvatarId(AVATAR)
    )

    private companion object {
        const val PROFILE = "p1"
        const val AVATAR = "rabbit"
    }
}
