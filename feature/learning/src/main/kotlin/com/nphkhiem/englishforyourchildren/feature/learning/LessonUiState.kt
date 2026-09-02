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

    /**
     * The silence a child fills in say with Pip, while nothing is listening.
     *
     * Deliberately not [ANSWERING]: there is no answer, it is never correct, and it can never be
     * wrong. Reusing that phase would make answer availability claim the choices are live for a
     * family that has none.
     */
    RESPONDING,

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
 * Which of the five activity families a lesson is showing.
 *
 * A UI enum rather than the domain's, for the same reason [LessonPhase] is one: what a screen needs
 * to know is which shape to draw, and nothing above it should be able to reach a domain type by
 * following this state. It exists so the activity-to-renderer mapping is a `when` the compiler can
 * check, rather than a default that quietly draws the wrong activity.
 */
enum class LessonActivityKind {
    LISTEN_AND_CHOOSE,
    PICTURE_MATCHING,
    LETTER_AND_SOUND,
    SAY_WITH_PIP,
    REVIEW
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
    /**
     * The picture for this choice, or null where no file has been drawn for it yet.
     *
     * A plain id rather than an [com.nphkhiem.englishforyourchildren.domain.model.AssetId], because
     * nothing above this state may reach a domain type by following it. Turning it into a file is
     * the job of whoever draws it.
     */
    val image: String? = null,
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
data class LearningObject(val id: String, val label: String, val image: String? = null)

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
    val pendingSave: Boolean,
    /**
     * Whether the stop-for-now confirmation is showing.
     *
     * State rather than something the screen remembers for itself, so both copy variants can be
     * driven by a fixture and reviewed on a television. The copy is the whole deliverable of
     * HB-D09, so it has to be walkable.
     */
    val stopForNowVisible: Boolean,
    /**
     * How far through the speaking pause the child is, from 0 to 1, or null for the four families
     * that have no pause.
     *
     * A value the state carries, not a clock this screen runs. See ADR 0003.
     */
    val pauseProgress: Float?,
    /**
     * Which shape to draw. See [LessonActivityKind].
     *
     * No default on purpose. A default would let a family that nobody has thought about render as
     * listen-and-choose, which is the bug this field exists to end.
     */
    val kind: LessonActivityKind
)

/** What a lesson screen reports upward. Typed, so no untyped escape hatch can grow here. */
sealed interface LessonAction {
    data class AnswerChosen(val answerId: String) : LessonAction

    data object ReplayRequested : LessonAction

    /** Only ever offered when audio is unavailable, so a broken question is not a dead end. */
    data object SkipRequested : LessonAction

    data object ContinueRequested : LessonAction

    /** Back was pressed on an active lesson. Never leaves the lesson; asks first. */
    data object BackRequested : LessonAction

    /** The child chose to stay, or pressed Back again while being asked. */
    data object KeepLearningRequested : LessonAction

    /** The child chose to stop. Emitted only; leaving the lesson is the host's job. */
    data object StopForNowConfirmed : LessonAction
}
