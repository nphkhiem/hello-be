package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Immutable
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback

/**
 * Where a lesson is in its own life.
 *
 * Deliberately only the lifecycle. The other things the design brief calls "states", such as
 * audio being unavailable or progress not yet saved, are not phases: they can be true during any
 * of these, so they live alongside as their own fields rather than as values here.
 */
enum class LessonPhase {
    /** Content is being made ready and there is nothing to answer yet. */
    PREPARING,

    /** The question is being spoken. Answers are not reachable, per ADR 0004. */
    PROMPTING,

    /** The child may choose. */
    ANSWERING,

    /** The child chose correctly and the answer is being confirmed. */
    CORRECT,

    /** The activity is over and is handing on. */
    COMPLETED
}

/**
 * How much help Pip is currently giving, per the support ladder in the design brief.
 *
 * This module renders the rung it is handed and never decides to climb it. Counting attempts and
 * watching the eight second idle window belong to the ViewModel, per ADR 0003.
 */
enum class SupportLevel {
    NONE,

    /** Pip repeats the same English prompt calmly. */
    REPEAT,

    /** Pip repeats more slowly with a stronger demonstration. */
    SLOWER,

    /** Pip adds one brief Vietnamese phrase clarifying the action, not translating the words. */
    VIETNAMESE
}

/**
 * One picture a child can choose.
 *
 * [feedback] arrives from state rather than being worked out here from a correct answer id. That
 * is the point: the screen is never told which answer is right, so it cannot give it away through
 * focus, ordering or selection.
 */
@Immutable
data class AnswerOption(
    val id: String,
    val label: String,
    val feedback: HelloBeChoiceFeedback = HelloBeChoiceFeedback.NEUTRAL
)

/**
 * The single thing a lesson is currently about: picture matching's fixed source, the letter pair,
 * the say-with-Pip target.
 *
 * It carries no feedback and no availability because it is never right, never wrong and never
 * reachable. Modelling those would describe states that cannot occur.
 */
@Immutable
data class LearningObject(val id: String, val label: String)

/**
 * Everything a lesson screen needs in order to draw itself, and nothing it needs to think with.
 *
 * A phase plus independent dimensions rather than one enum, because a lesson can be answering,
 * with audio unavailable, with progress pending save, all at the same time. A flat enum could not
 * say that, which is the same shape of mistake that cost this project three iterations on the
 * chip before "focused" and "selected" were recognised as combinable.
 */
@Immutable
data class LessonUiState(
    val unitName: String,
    val activityTitle: String,
    val prompt: String,
    val caption: String?,
    val activityNumber: Int,
    val activityCount: Int,
    val phase: LessonPhase,
    val support: SupportLevel,
    /** Null for the families that have no focal object, such as listen and choose. See ADR 0005. */
    val learningObject: LearningObject?,
    val answers: List<AnswerOption>,
    val audioAvailable: Boolean,
    val pendingSave: Boolean
)

/** What a lesson screen reports upward. Typed, so no untyped escape hatch can grow here. */
sealed interface LessonAction {
    data class AnswerChosen(val answerId: String) : LessonAction

    data object ReplayRequested : LessonAction

    /** Only ever offered when audio is unavailable, so a broken question is not a dead end. */
    data object SkipRequested : LessonAction

    data object ContinueRequested : LessonAction

    data object BackRequested : LessonAction
}
