package com.nphkhiem.englishforyourchildren.navigation

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import org.junit.jupiter.api.Test

class EntryResolverTest {

    @Test
    fun givenNoProfiles_whenAppStarts_thenCreateProfileIsRoot() {
        assertThat(resolveEntry(snapshot(profiles = emptyList())))
            .isEqualTo(HelloBeKey.ProfileCreate)
    }

    @Test
    fun givenOneProfile_whenAppStarts_thenChildHomeIsRoot() {
        val only = ProfileId("minh")

        assertThat(resolveEntry(snapshot(profiles = listOf(only))))
            .isEqualTo(HelloBeKey.ChildHome(profileId = only))
    }

    @Test
    fun givenMultipleProfiles_whenAppStarts_thenProfilePickerIsRoot() {
        val two = listOf(ProfileId("minh"), ProfileId("lan"))

        assertThat(resolveEntry(snapshot(profiles = two)))
            .isEqualTo(HelloBeKey.ProfilePicker(mode = ProfilePickerMode.Launch))
    }

    @Test
    fun givenFourProfiles_whenAppStarts_thenThePickerIsStillTheRoot() {
        val four = (1..4).map { ProfileId("p$it") }

        assertThat(resolveEntry(snapshot(profiles = four)))
            .isEqualTo(HelloBeKey.ProfilePicker(mode = ProfilePickerMode.Launch))
    }

    @Test
    fun givenStorageCannotBeRead_whenAppStarts_thenItAsksForAGrownUp() {
        // Checked before the profile list, because a television that cannot read its storage is
        // not a television with no children on it.
        val unreadable = snapshot(profiles = emptyList()).copy(storageReadable = false)

        assertThat(resolveEntry(unreadable))
            .isEqualTo(HelloBeKey.Recovery(reason = RecoveryReason.APP_NEEDS_GROWN_UP))
    }

    @Test
    fun givenStorageCannotBeReadButProfilesAreKnown_whenAppStarts_thenItStillAsksForAGrownUp() {
        val unreadable = snapshot(profiles = listOf(ProfileId("minh")))
            .copy(storageReadable = false)

        assertThat(resolveEntry(unreadable))
            .isEqualTo(HelloBeKey.Recovery(reason = RecoveryReason.APP_NEEDS_GROWN_UP))
    }

    @Test
    fun givenAStaleRememberedProfile_whenAppStarts_thenTheRootIsResolvedByCount() {
        // The remembered profile does not choose the destination. Two profiles remain, so the
        // picker opens whether or not the remembered one is still there.
        val two = listOf(ProfileId("minh"), ProfileId("lan"))
        val stale = snapshot(profiles = two, remembered = ProfileId("deleted"))

        assertThat(resolveEntry(stale))
            .isEqualTo(HelloBeKey.ProfilePicker(mode = ProfilePickerMode.Launch))
    }

    @Test
    fun givenAStaleRememberedProfileAndOneLeft_whenAppStarts_thenChildHomeIsRoot() {
        val one = listOf(ProfileId("minh"))
        val stale = snapshot(profiles = one, remembered = ProfileId("deleted"))

        assertThat(resolveEntry(stale)).isEqualTo(HelloBeKey.ChildHome(profileId = one.single()))
    }

    @Test
    fun givenARememberedProfileThatRemains_whenThePickerOpens_thenItIsFocused() {
        val two = listOf(ProfileId("minh"), ProfileId("lan"))

        assertThat(resolveLaunchFocus(snapshot(profiles = two, remembered = two[1])))
            .isEqualTo(two[1])
    }

    @Test
    fun givenAStaleRememberedProfile_whenThePickerOpens_thenTheFirstProfileIsFocused() {
        // Aiming focus at a profile that has been deleted would strand it.
        val two = listOf(ProfileId("minh"), ProfileId("lan"))

        assertThat(resolveLaunchFocus(snapshot(profiles = two, remembered = ProfileId("gone"))))
            .isEqualTo(two.first())
    }

    @Test
    fun givenNoProfilesAtAll_whenFocusIsResolved_thenThereIsNoneToFocus() {
        assertThat(resolveLaunchFocus(snapshot(profiles = emptyList()))).isNull()
    }

    private fun snapshot(profiles: List<ProfileId>, remembered: ProfileId? = null) =
        ProfileSnapshot(
            storageReadable = true,
            validProfileIds = profiles,
            rememberedProfileId = remembered
        )
}
