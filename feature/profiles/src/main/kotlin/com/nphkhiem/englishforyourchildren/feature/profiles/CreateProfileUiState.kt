package com.nphkhiem.englishforyourchildren.feature.profiles

import androidx.compose.runtime.Immutable

/**
 * The profile being made.
 *
 * [nickname] and [avatarId] arrive already chosen: the screen renders a draft, it does not invent
 * one. Generating a name here would put logic where there should be none and would make the
 * generated name unreviewable.
 *
 * [age] is nullable because "not chosen yet" is the state the required-age rule turns on. A default
 * would make the screen assert a choice nobody made.
 */
@Immutable
data class ProfileDraft(val nickname: String, val avatarId: String, val age: Int?)

/**
 * Everything the create screen needs to draw itself.
 *
 * [ages] and [avatarChoices] arrive in state rather than being constants here, so the curriculum
 * and the art set can change without touching a screen.
 */
@Immutable
data class CreateProfileUiState(
    val draft: ProfileDraft,
    val ages: List<Int>,
    val avatarChoices: List<String>,
    val choosingAvatar: Boolean,
    val capacityReached: Boolean,
    val saveFailed: Boolean
)

/** What the create screen reports upward. */
sealed interface CreateProfileAction {
    data class AgeChosen(val age: Int) : CreateProfileAction

    data class AvatarChosen(val avatarId: String) : CreateProfileAction

    data object ChangePictureRequested : CreateProfileAction

    /** Renaming needs a keyboard, which belongs to profile management rather than to setup. */
    data object ChangeNameRequested : CreateProfileAction

    data class CreateRequested(val draft: ProfileDraft) : CreateProfileAction
}
