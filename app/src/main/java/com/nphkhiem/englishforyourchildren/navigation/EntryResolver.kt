package com.nphkhiem.englishforyourchildren.navigation

/**
 * What the app knows about stored profiles when it starts.
 *
 * [storageReadable] is separate from an empty list on purpose: a television that cannot read its
 * own storage is not a television with no children on it, and the two lead to opposite places.
 * A boolean beside a list would let the pair that means neither exist, so this is checked first
 * and the list is only consulted once storage has answered.
 */
data class ProfileSnapshot(
    val storageReadable: Boolean,
    val validProfileIds: List<ProfileId>,
    val rememberedProfileId: ProfileId?
)

/**
 * Where the app opens, decided entirely by what is stored.
 *
 * This is the information architecture's entry-resolution table as a function. It is pure so that
 * every row of that table can be asserted without a device, which matters because launch is the one
 * flow a user cannot avoid and the one that is hardest to reach by hand.
 *
 * The remembered profile deliberately does not change which destination opens. A stale remembered
 * profile resolves by profile count like any other launch, and the remembered value only decides
 * where focus lands once the picker is showing.
 */
fun resolveEntry(snapshot: ProfileSnapshot): HelloBeKey {
    if (!snapshot.storageReadable) {
        return HelloBeKey.Recovery(reason = RecoveryReason.APP_NEEDS_GROWN_UP)
    }

    val profiles = snapshot.validProfileIds
    return when (profiles.size) {
        0 -> HelloBeKey.ProfileCreate
        1 -> HelloBeKey.ChildHome(profileId = profiles.single())
        else -> HelloBeKey.ProfilePicker(mode = ProfilePickerMode.Launch)
    }
}

/**
 * Which profile the launch picker should focus.
 *
 * The remembered one when it is still there, and the first otherwise. Aiming focus at a profile
 * that has been deleted would strand it, which is the same validated-resume rule free play and
 * profile management already apply to their own remembered selections.
 */
fun resolveLaunchFocus(snapshot: ProfileSnapshot): ProfileId? {
    val remembered = snapshot.rememberedProfileId
        ?.takeIf { it in snapshot.validProfileIds }
    return remembered ?: snapshot.validProfileIds.firstOrNull()
}
