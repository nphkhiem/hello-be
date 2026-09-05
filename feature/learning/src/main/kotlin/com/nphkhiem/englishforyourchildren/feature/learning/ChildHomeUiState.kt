package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Immutable
import com.nphkhiem.englishforyourchildren.domain.model.LessonId

/**
 * What the dominant slot on child home offers.
 *
 * A sealed type rather than a set of flags, because flags would permit states that cannot occur,
 * such as a finished course whose checkpoint will not load. Only the resuming cases carry a
 * context line, because only they have somewhere to point.
 */
sealed interface HomePrimary {
    /**
     * There is a lesson to carry on with, and [context] names it.
     *
     * Not only a half-finished lesson. A child who finished one adventure and comes back tomorrow
     * is continuing as much as one who stopped in the middle, and both want the same word on the
     * same control. Where inside the lesson they land is the lesson's business, from its own
     * checkpoint.
     */
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
    /**
     * The lesson the dominant control opens, or null when it opens nothing.
     *
     * The brief asks for one Select press to the next useful activity, and a control that landed on
     * the learning path would be two: the child would arrive somewhere they then have to read. Null
     * for a finished course and for a checkpoint the course has lost, which are the two cases where
     * there is nothing to open and the layout already says so.
     */
    val continueTarget: LessonId? = null,
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
