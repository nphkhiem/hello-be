package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Immutable

/**
 * What the dominant slot on child home offers.
 *
 * A sealed type rather than a set of flags, because flags would permit states that cannot occur,
 * such as a finished course whose checkpoint will not load. Only the resuming cases carry a
 * context line, because only they have somewhere to point.
 */
sealed interface HomePrimary {
    /** A checkpoint exists and can be opened. */
    data class Resume(val context: String) : HomePrimary

    /** A checkpoint exists and will not open. Said out loud rather than hidden. */
    data class ResumeUnavailable(val context: String) : HomePrimary

    /** No checkpoint yet. */
    data object StartFirstAdventure : HomePrimary

    /** Every adventure is finished, so free play is what is genuinely left. */
    data object CourseComplete : HomePrimary
}

/** A destination offered on child home. */
enum class HomeTarget {
    CONTINUE,
    LEARNING_PATH,
    FREE_PLAY
}

/** Everything child home needs to draw itself. */
@Immutable
data class ChildHomeUiState(
    val profileName: String,
    val profileAvatar: String,
    val greeting: String,
    val greetingHint: String,
    val primary: HomePrimary,
    val pendingSave: Boolean
)

/** What child home reports upward. */
sealed interface ChildHomeAction {
    /**
     * Take me to the next thing. Continue and Start an adventure both mean this, so they share one
     * action rather than becoming a pair that must always be handled identically.
     */
    data object ContinueRequested : ChildHomeAction

    data object LearningPathRequested : ChildHomeAction

    data object FreePlayRequested : ChildHomeAction

    data object SwitchProfileRequested : ChildHomeAction

    data object CaregiverEntryRequested : ChildHomeAction
}
