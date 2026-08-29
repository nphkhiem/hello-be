package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.runtime.Immutable

/**
 * Which destructive thing is being confirmed.
 *
 * Two kinds rather than one dialog with a flag, because the two are not degrees of the same act.
 * Deleting removes a child from this television; resetting keeps the child and restarts what they
 * have learned. The stop condition asks for them to stay semantically distinct, and a type is the
 * only way to hold that: every piece of copy is chosen by this enum, so neither can borrow the
 * other's words.
 */
enum class CaregiverConfirmationKind {
    DELETE_PROFILE,
    RESET_PROGRESS
}

/** Where the confirmation is in its own life. */
enum class CaregiverConfirmationPhase {
    /** Waiting for a caregiver to choose. */
    READY,

    /** The work is underway. Nothing can be pressed, which is what stops a second confirmation. */
    WORKING,

    /** The work did not happen, and nothing changed. */
    FAILED
}

/**
 * Everything a destructive confirmation needs to draw itself.
 *
 * The profile is named on every one of them. A caregiver with four children must never be asked to
 * delete "this profile".
 */
@Immutable
data class CaregiverConfirmationState(
    val kind: CaregiverConfirmationKind,
    val profileName: String,
    val profileAvatar: String,
    val phase: CaregiverConfirmationPhase
)

/** What a destructive confirmation reports upward. */
sealed interface CaregiverConfirmationAction {
    /** Keep the profile, or keep the progress. Back means this too. */
    data object Dismissed : CaregiverConfirmationAction

    /** Do the destructive thing. Emitted at most once per confirmation. */
    data object Confirmed : CaregiverConfirmationAction

    /** Try the failed work again. */
    data object RetryRequested : CaregiverConfirmationAction
}
