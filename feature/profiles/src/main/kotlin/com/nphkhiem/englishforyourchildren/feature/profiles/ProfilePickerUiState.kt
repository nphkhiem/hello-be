package com.nphkhiem.englishforyourchildren.feature.profiles

import androidx.compose.runtime.Immutable

/**
 * One child who learns on this television.
 *
 * [summary] is a line the screen draws, not a progress model it interprets. The picker does not
 * know what "5 adventures" counts, which keeps counting out of a screen.
 *
 * [available] carries the brief's unavailable-profile recovery: a profile whose data will not load
 * stays visible and says so, rather than vanishing while a child is looking for themselves.
 */
@Immutable
data class ChildProfile(
    val id: String,
    val nickname: String,
    val avatar: String,
    val summary: String,
    val available: Boolean = true
)

/**
 * Everything the picker needs to draw itself.
 *
 * Capacity is deliberately absent: it follows from [profiles], and a stored flag could disagree
 * with the list sitting next to it.
 */
@Immutable
data class ProfilePickerUiState(
    val profiles: List<ChildProfile>,
    val rememberedProfileId: String?,
    val loading: Boolean
)

/** What the picker reports upward. Typed, so no untyped escape hatch can grow here. */
sealed interface ProfileAction {
    data class ProfileChosen(val profileId: String) : ProfileAction

    data object AddProfileRequested : ProfileAction

    /** Emitted, not performed. Routing this through the adult gate is the host's job. */
    data object CaregiverEntryRequested : ProfileAction
}
