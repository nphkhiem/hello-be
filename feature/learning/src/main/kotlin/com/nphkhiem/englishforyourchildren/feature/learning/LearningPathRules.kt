package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability

/** Where entry focus goes when the path opens. */
internal enum class PathFocusTarget {
    RECOMMENDED_LESSON,
    UNIT_STEPPER,
    RECOVERY
}

/**
 * Whether a lesson can be reached, and what a press does when it is.
 *
 * A future lesson is skipped by focus rather than made unavailable. The alternative, keeping it
 * focusable so a screen reader can announce "later", would put a refusing card in the middle of
 * every walk along the path; on a five lesson unit with two lessons still ahead, a third of a
 * child's presses would land somewhere that does nothing. The information architecture asks for
 * future lessons to read as later and not as a punishment, and a control that takes focus only to
 * decline is a small punishment. The cost is real and accepted: that announcement is lost, and the
 * future treatment carries the meaning visually instead.
 *
 * A lesson whose content will not load stays focusable, because that is news the child has no
 * other way to receive.
 */
internal fun lessonAvailability(node: LessonNodeState): HelloBeAvailability = when {
    node.progress == LessonProgress.FUTURE -> HelloBeAvailability.DISABLED
    !node.openable -> HelloBeAvailability.UNAVAILABLE
    else -> HelloBeAvailability.ENABLED
}

/**
 * The unit the screen actually draws, or null when there is nothing to draw.
 *
 * A unit carrying no lessons is not a unit the child can use, so it is folded into the same
 * absence rather than reaching the composition as an empty page. Both the recovery rule and the
 * composition read this, so the two can never disagree about what an empty unit means.
 */
internal fun visibleUnit(state: LearningPathUiState): UnitPageState? =
    state.unit?.takeIf { it.lessons.isNotEmpty() }

/** True when there is no unit worth drawing and the child needs a way back instead. */
internal fun isRecovering(state: LearningPathUiState): Boolean = visibleUnit(state) == null

/**
 * The lesson Pip points at and focus opens on.
 *
 * The named recommendation wins. Where a unit has none, the first lesson focus can actually reach
 * stands in, so entry focus never aims at a card that will not take it.
 *
 * This is also what restores position after Back: a child returning from the third lesson arrives
 * in a state that names the third lesson, so focus lands where they left rather than at the start
 * of the unit. No focus memory is kept here, because the state already carries the answer.
 */
internal fun recommendedLessonId(unit: UnitPageState?): String? {
    val lessons = unit?.lessons.orEmpty()
    val recommended = lessons.firstOrNull { it.progress == LessonProgress.RECOMMENDED }
    val reachable = lessons.firstOrNull {
        lessonAvailability(it) != HelloBeAvailability.DISABLED
    }
    return (recommended ?: reachable)?.id
}

/**
 * Where the screen puts focus when it opens.
 *
 * A unit with nothing reachable still has to catch focus somewhere, so it falls to the stepper:
 * the child can page to a unit that works instead of landing on nothing.
 */
internal fun pathFocusTarget(state: LearningPathUiState): PathFocusTarget = when {
    isRecovering(state) -> PathFocusTarget.RECOVERY
    recommendedLessonId(state.unit) != null -> PathFocusTarget.RECOMMENDED_LESSON
    else -> PathFocusTarget.UNIT_STEPPER
}
