package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Immutable

/**
 * How far a child has got with one lesson.
 *
 * Deliberately only progress. Whether the lesson's content will actually load is a separate fact
 * that can be true at any of these, so it lives beside this as its own field rather than becoming
 * a value here. That is the same separation `LessonUiState` makes between phase and
 * `audioAvailable`, and for the same reason: a flat enum cannot say two things at once.
 */
enum class LessonProgress {
    /** Finished. Still openable, because history is visitable and carries no score. */
    COMPLETED,

    /** The one Pip is pointing at, and where entry focus lands. */
    RECOMMENDED,

    /** Reachable, but not the suggestion. */
    AVAILABLE,

    /** Later. Shown so the path has a shape, skipped by focus so it never refuses a press. */
    FUTURE
}

/** What kind of lesson a node leads to. The review story closes a unit rather than teaching a new word. */
enum class LessonKind {
    PRACTICE,
    REVIEW
}

/**
 * One lesson on the path.
 *
 * [openable] is false when the lesson exists but its content will not load. Said out loud rather
 * than hidden, exactly as a checkpoint that will not open is said out loud on child home.
 */
@Immutable
data class LessonNodeState(
    val id: String,
    val title: String,
    val progress: LessonProgress,
    val kind: LessonKind,
    val openable: Boolean = true
)

/** Enough of a neighbouring unit to name it on the control that goes there. */
@Immutable
data class UnitSummary(val unitId: String, val unitNumber: Int, val theme: String)

/** One unit chapter and the lessons inside it. Never more than one of these is on screen. */
@Immutable
data class UnitPageState(
    val unitId: String,
    val unitNumber: Int,
    val unitCount: Int,
    val theme: String,
    val objective: String,
    val lessons: List<LessonNodeState>
)

/**
 * Everything the learning path needs to draw itself.
 *
 * [previousUnit] and [nextUnit] are null when there is no unit that way. The information
 * architecture asks for previous/next context "displayed only when reachable", and a nullable
 * summary makes that a property of the state rather than a condition every caller must remember.
 *
 * [unit] is null when the course has no unit to show, which is the recovery case.
 */
@Immutable
data class LearningPathUiState(
    val profileName: String,
    val profileAvatar: String,
    val unit: UnitPageState?,
    val previousUnit: UnitSummary?,
    val nextUnit: UnitSummary?,
    val pendingSave: Boolean
)

/** What the learning path reports upward. */
sealed interface LearningPathAction {
    data class LessonChosen(val lessonId: String) : LearningPathAction

    data object PreviousUnitRequested : LearningPathAction

    data object NextUnitRequested : LearningPathAction

    /** The way out of a unit that will not load. Leaving is the host's job. */
    data object HomeRequested : LearningPathAction

    data object SwitchProfileRequested : LearningPathAction
}
