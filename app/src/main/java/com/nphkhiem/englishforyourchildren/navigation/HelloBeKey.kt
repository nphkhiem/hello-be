package com.nphkhiem.englishforyourchildren.navigation

import androidx.compose.runtime.Immutable
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.model.ShelfId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId

/** Which mode the profile picker was opened in. */
sealed interface ProfilePickerMode {
    data object Launch : ProfilePickerMode

    data class Switch(val currentProfileId: ProfileId) : ProfilePickerMode
}

/**
 * Where a finished lesson goes back to.
 *
 * A small enum rather than a nested key, so keys stay flat, comparable and free of recursion. The
 * information architecture only ever returns a completed lesson to child home or to the learning
 * path, and naming those two is enough.
 */
enum class LessonReturnTarget {
    CHILD_HOME,
    LEARNING_PATH
}

/** The profile-aware child surface an adult gate was opened from. */
enum class ChildReturnTarget {
    CHILD_HOME,
    LEARNING_PATH,
    FREE_PLAY
}

/** Why a recovery destination is on screen. */
enum class RecoveryReason {
    LESSON_UNAVAILABLE,
    APP_NEEDS_GROWN_UP,
    NO_VALID_ROOT_CONTENT
}

/**
 * Every destination this app has, as a type.
 *
 * There are no route strings anywhere in this file or the host that reads it. A destination is a
 * value, its arguments are its fields, and the compiler decides whether a navigation is possible.
 *
 * Overlays are deliberately absent. The stop-for-now dialog, the audio-unavailable overlay, the
 * pending-save status and the play-together prompt are local state on the screens that own them,
 * exactly as the information architecture files them, so no key exists that could route to one.
 */
@Immutable
sealed interface HelloBeKey {
    data object ProfileCreate : HelloBeKey

    data class ProfilePicker(val mode: ProfilePickerMode) : HelloBeKey

    data class ChildHome(val profileId: ProfileId) : HelloBeKey

    data class LearningPath(val profileId: ProfileId, val preferredUnitId: UnitId? = null) :
        HelloBeKey

    data class Lesson(val profileId: ProfileId, val lessonId: LessonId) : HelloBeKey

    data class LessonCelebration(
        val profileId: ProfileId,
        val lessonId: LessonId,
        val returnTarget: LessonReturnTarget
    ) : HelloBeKey

    data class FreePlay(val profileId: ProfileId, val preferredShelfId: ShelfId? = null) :
        HelloBeKey

    data class CaregiverGate(val profileId: ProfileId?, val returnTarget: ChildReturnTarget) :
        HelloBeKey

    data class CaregiverDashboard(val profileId: ProfileId?) : HelloBeKey

    data class CaregiverSettings(val profileId: ProfileId?) : HelloBeKey

    data class ProfileManagement(val selectedProfileId: ProfileId?) : HelloBeKey

    data class DeleteProfileConfirmation(val profileId: ProfileId) : HelloBeKey

    data class ResetProgressConfirmation(val profileId: ProfileId) : HelloBeKey

    data class Recovery(
        val reason: RecoveryReason,
        val safeReturnTarget: ChildReturnTarget? = null
    ) : HelloBeKey
}

/** True for the destinations that sit behind the adult gate. */
internal fun HelloBeKey.isCaregiver(): Boolean = when (this) {
    is HelloBeKey.CaregiverDashboard,
    is HelloBeKey.CaregiverSettings,
    is HelloBeKey.ProfileManagement,
    is HelloBeKey.DeleteProfileConfirmation,
    is HelloBeKey.ResetProgressConfirmation -> true

    else -> false
}
