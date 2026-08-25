package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * What the stage is telling the child right now.
 *
 * Each tone carries the pose Pip holds for it, so adding a tone is one edit here rather than a
 * change spread across a separate lookup. Pip celebrates effort, models again, or simply guides:
 * there is deliberately no pose that reads as a reaction to a wrong answer.
 */
enum class FeedbackTone(internal val pose: PipPose) {
    SUPPORTIVE_RETRY(pose = PipPose.MODELING),
    CORRECT(pose = PipPose.CELEBRATING),
    INFORMATION(pose = PipPose.POINTING)
}

/**
 * Pip's response to what just happened, telling the child what to do next.
 *
 * Per ADR 0002 the choice card answers "which item did I touch" through its own visual state, and
 * this panel answers "what should I do now". The panel therefore uses the same neutral raised
 * surface for every tone: it never repeats the colour the card is already showing, so a child gets
 * one signal per question rather than two competing ones.
 *
 * Tone is carried by Pip's pose and by the words, never by the container colour. Supportive retry
 * in particular never touches the error palette, because a wrong answer is a second invitation
 * rather than a failure.
 *
 * [pipDescription] is what a child using a screen reader hears in place of seeing the pose, so the
 * tone reaches them too rather than being carried by colour and posture alone. It is required for
 * that reason: without it, two tones sharing a message would sound identical.
 *
 * The panel is a polite live region, so the response is heard when it changes without the child
 * having to move focus onto it.
 */
@Composable
fun FeedbackPanel(
    message: String,
    tone: FeedbackTone,
    pipDescription: String,
    modifier: Modifier = Modifier,
    pip: @Composable (PipPose) -> Unit = { pose ->
        PipGuide(
            pose = pose,
            contentDescription = pipDescription,
            modifier = Modifier.size(HelloBeLayout.pipMinSize)
        )
    }
) {
    val colors = HelloBeTheme.colors

    Row(
        modifier = modifier
            .background(colors.surfaceRaised, HelloBeShapes.large)
            .padding(HelloBeTheme.spacing.cardInternal)
            .semantics(mergeDescendants = true) {
                this.liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pip(tone.pose)
        Text(
            text = message,
            style = HelloBeTheme.typography.titleMedium,
            color = colors.textPrimary
        )
    }
}
