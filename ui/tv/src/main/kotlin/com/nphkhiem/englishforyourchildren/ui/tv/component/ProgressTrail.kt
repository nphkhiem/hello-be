package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/** What one step of a trail is showing. */
internal enum class TrailSegment {
    COMPLETED,
    CURRENT,
    UPCOMING
}

/**
 * Works out what each step of the trail is showing.
 *
 * Step counts will eventually come from packaged content, so an out-of-range step is clamped and
 * an empty trail simply has nothing to draw. Neither throws: a child mid-lesson must never lose
 * the screen to a bad count.
 */
internal fun progressTrailSegments(totalSteps: Int, currentStep: Int): List<TrailSegment> {
    if (totalSteps <= 0) return emptyList()
    val safeStep = currentStep.coerceIn(1, totalSteps)
    return (1..totalSteps).map { step ->
        when {
            step < safeStep -> TrailSegment.COMPLETED
            step == safeStep -> TrailSegment.CURRENT
            else -> TrailSegment.UPCOMING
        }
    }
}

/**
 * Where the child is in the current lesson or unit, shown as segments rather than a number.
 *
 * Nothing numeric is drawn: the design brief is explicit that this is a location, never a streak
 * or a rank, and it faces children who cannot read. [stateDescription] carries the exact position
 * for assistive technology and caregivers, which is how both requirements are met at once.
 *
 * The current step borrows `action.selected` because "you are here" is a selection-shaped idea,
 * and reusing that token keeps selection meaning one thing across the app. It is also drawn wider,
 * so position never depends on colour alone.
 *
 * [describePosition] receives the clamped step and total, so what is announced can never disagree
 * with what is drawn, even if a caller passes a step from content data that is out of range.
 */
@Composable
fun ProgressTrail(
    totalSteps: Int,
    currentStep: Int,
    describePosition: (currentStep: Int, totalSteps: Int) -> String,
    modifier: Modifier = Modifier
) {
    val segments = progressTrailSegments(totalSteps, currentStep)
    if (segments.isEmpty()) return
    val safeStep = currentStep.coerceIn(1, totalSteps)

    val colors = HelloBeTheme.colors
    val motion = HelloBeTheme.motion

    Row(
        // A node carrying only a state description may never be reached by a screen reader, so
        // the position is the node's name as well as its state.
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = describePosition(safeStep, totalSteps)
            this.stateDescription = describePosition(safeStep, totalSteps)
        },
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space2)
    ) {
        segments.forEach { segment ->
            val target = when (segment) {
                TrailSegment.COMPLETED -> colors.actionPrimary
                TrailSegment.CURRENT -> colors.actionSelected
                TrailSegment.UPCOMING -> colors.borderSecondary
            }
            val segmentColor by animateColorAsState(
                targetValue = target,
                animationSpec = tween(durationMillis = motion.progressTrailTransitionMillis),
                label = "trailSegment"
            )
            TrailPip(color = segmentColor, wide = segment == TrailSegment.CURRENT)
        }
    }
}

/** The current step is drawn wider, so position never depends on colour alone. */
@Composable
private fun TrailPip(color: Color, wide: Boolean) {
    val width =
        if (wide) HelloBeLayout.trailSegmentCurrentWidth else HelloBeLayout.trailSegmentWidth
    Box(
        modifier = Modifier
            .width(width)
            .height(HelloBeLayout.trailSegmentHeight)
            .background(color, HelloBeShapes.full)
    )
}
