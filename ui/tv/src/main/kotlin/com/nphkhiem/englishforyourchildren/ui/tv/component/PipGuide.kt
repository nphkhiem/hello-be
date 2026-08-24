package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.lerp
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeIllustration
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The poses Pip may hold, each carrying its own geometry so adding a pose is a single edit here
 * rather than a change spread across separate lookups.
 *
 * A closed set on purpose: Pip guides, models and reassures, so there is deliberately no pose for
 * disappointment or disapproval.
 *
 * @param headTurn how far the head turns from centre, as a fraction of body radius. This is what
 *   distinguishes pointing from modelling.
 * @param headLift how high the head sits, from 0 resting on the body to 1 at the top of the
 *   available space. This is what distinguishes celebrating from resting.
 */
enum class PipPose(internal val headTurn: Float, internal val headLift: Float) {
    RESTING(headTurn = 0f, headLift = 0f),
    GREETING(headTurn = 0f, headLift = 0.35f),
    POINTING(headTurn = 0.45f, headLift = 0.1f),
    MODELING(headTurn = -0.45f, headLift = 0.1f),
    CELEBRATING(headTurn = 0f, headLift = 1f)
}

/**
 * Pip, the child's guide, drawn in the stage's predictable supporting spot.
 *
 * [illustration] is the seam for real artwork. It defaults to a placeholder silhouette so layout,
 * focus, contrast and reduced-motion behaviour can all be verified before an illustrator delivers
 * Pip, and so that delivery does not touch this file.
 *
 * Pose changes cross-fade over `motion.duration.normal`. Under reduced motion the pose simply
 * swaps, matching the "Pip flutter or point becomes a static target pose" substitution in
 * DESIGN_TOKENS.md, which is why nothing here animates when that setting is on. The second half of
 * that substitution, outlining the object Pip refers to, belongs to whatever Pip is pointing at
 * and so lives with the lesson surfaces rather than here.
 *
 * Pip is a guide rather than decoration, so `scenery.contrastMaximum` deliberately does not cap
 * Pip: that ceiling exists to hold decorative scenery below the interactive hierarchy, and Pip
 * must stay clearly visible.
 *
 * [contentDescription] may be null, following the same contract as Compose's own `Icon`: null
 * means Pip is decorative here because the surrounding panel already announces the whole
 * thing. Without that, a panel containing Pip announces its message twice.
 */
@Composable
fun PipGuide(
    pose: PipPose,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    illustration: @Composable (PipPose) -> Unit = { PipPlaceholder(it) }
) {
    val motion = HelloBeTheme.motion

    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = HelloBeLayout.pipMinSize,
                minHeight = HelloBeLayout.pipMinSize
            )
            .then(
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics(mergeDescendants = true) {
                        this.contentDescription = contentDescription
                    }
                }
            )
    ) {
        if (motion.reduceMotion) {
            illustration(pose)
        } else {
            Crossfade(
                targetState = pose,
                animationSpec = tween(durationMillis = motion.durations.normal),
                label = "pipPose"
            ) { current ->
                illustration(current)
            }
        }
    }
}

/**
 * A deliberately provisional silhouette: body and head only, stroked at
 * `illustration.strokeReference` so the outline survives being rasterised at 720p, 1080p and 4K.
 *
 * Proportions are unitless fractions of the space given, so the figure scales rather than being
 * pinned to fixed sizes, and every part is kept inside the bounds so no pose loses its head to
 * clipping. The head is dropped entirely below `illustration.detailMinimum`, because that is the
 * detail the rule says to remove at small sizes; the body always draws, so Pip never vanishes.
 *
 * This is a stand-in for real artwork, not an attempt at it.
 */
@Composable
fun PipPlaceholder(pose: PipPose) {
    val colors = HelloBeTheme.colors
    val strokeReference = HelloBeIllustration.strokeReference
    val detailMinimum = HelloBeIllustration.detailMinimum

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = strokeReference.toPx()
        val inset = strokeWidth / 2f
        val bodyRadius = size.minDimension / BODY_DIVISOR
        val headRadius = bodyRadius / HEAD_DIVISOR

        val bodyCentre = Offset(size.width / 2f, size.height - bodyRadius - inset)
        drawSilhouettePart(bodyCentre, bodyRadius, strokeWidth, colors.accentPip, colors.focusGuard)

        if (headRadius < detailMinimum.toPx()) return@Canvas

        // Resting head sits on the body; lifted head stops exactly where clipping would begin.
        val headResting = bodyCentre.y - bodyRadius
        val headHighest = headRadius + inset
        val headCentre = Offset(
            x = (size.width / 2f + bodyRadius * pose.headTurn)
                .coerceIn(headRadius + inset, size.width - headRadius - inset),
            y = lerp(headResting, headHighest, pose.headLift).coerceAtLeast(headHighest)
        )
        drawSilhouettePart(headCentre, headRadius, strokeWidth, colors.accentPip, colors.focusGuard)
    }
}

/** One filled shape plus its outline, which is the whole vocabulary of the placeholder. */
private fun DrawScope.drawSilhouettePart(
    centre: Offset,
    radius: Float,
    strokeWidth: Float,
    fill: Color,
    outline: Color
) {
    drawCircle(color = fill, radius = radius, center = centre)
    drawCircle(
        color = outline,
        radius = radius,
        center = centre,
        style = Stroke(width = strokeWidth)
    )
}

private const val BODY_DIVISOR = 3f
private const val HEAD_DIVISOR = 1.8f
