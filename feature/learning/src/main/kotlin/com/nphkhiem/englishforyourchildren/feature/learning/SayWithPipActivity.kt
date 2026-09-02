package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.LearningObjectCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.PackagedPicture
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberPackagedPicture
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Say with Pip: Pip models a phrase, then leaves a silence for the child to fill.
 *
 * The family defined as much by what it does not do as by what it does. Nothing here listens,
 * records, scores or judges, and nothing on screen may suggest otherwise: no microphone, no
 * waveform, no permission, no mark. The child speaks to a television that is simply waiting, and
 * the only honest thing the screen can offer is time and an invitation to go again.
 *
 * There is no correct phase in this family, because there is nothing to be correct about.
 *
 * The pause is a value this screen draws. It runs no clock, per ADR 0003.
 */
@Composable
fun SayWithPipActivity(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LessonScaffold(state = state, onAction = onAction, modifier = modifier) { entryModifier ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = HelloBeTheme.spacing.cardGap,
                alignment = Alignment.CenterVertically
            )
        ) {
            // The phrase is the hero, as the draft shows. It varies per activity, so it is state.
            Text(
                text = state.prompt,
                style = HelloBeTheme.typography.headlineLarge,
                color = HelloBeTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            // The instruction never varies across this family, so it is copy rather than state.
            Text(
                text = stringResource(R.string.say_instruction),
                style = HelloBeTheme.typography.bodyLarge,
                color = HelloBeTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            SpeakBoard(state = state, onAction = onAction, entryModifier = entryModifier)
        }
    }
}

@Suppress("ModifierParameter")
@Composable
private fun SpeakBoard(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    entryModifier: Modifier
) {
    val boardHeight = HelloBeTheme.layout.childChoiceMinHeight + HelloBeTheme.focus.clearance * 2

    Row(
        modifier = Modifier.fillMaxWidth().height(boardHeight),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.learningObject != null) {
            val target = rememberPackagedPicture(state.learningObject.image)

            LearningObjectCard(
                label = state.learningObject.label,
                // A child saying a word aloud should be looking at the thing, not at its spelling.
                labelVisible = target == null,
                illustration = target?.let { { PackagedPicture(it) } },
                modifier = Modifier.weight(TARGET_WEIGHT).fillMaxHeight()
            )
        }

        Column(
            modifier = Modifier.weight(RESPONSE_WEIGHT).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(
                space = HelloBeTheme.spacing.space4,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.phase == LessonPhase.RESPONDING) {
                PauseIndicator(progress = state.pauseProgress)
            }

            SpeakActions(state = state, onAction = onAction, entryModifier = entryModifier)
        }
    }
}

/**
 * The visible, listening-free pause.
 *
 * A bar rather than a countdown, and no numbers: a child who cannot read a clock should not be
 * shown one, and nothing here should feel like being timed. A null progress still draws the
 * invitation, so a host that has not wired timing yet shows a usable screen rather than a blank
 * one or a bar stuck at zero.
 */
@Composable
private fun PauseIndicator(progress: Float?) {
    val colors = HelloBeTheme.colors

    Text(
        text = stringResource(R.string.say_your_turn),
        style = HelloBeTheme.typography.titleMedium,
        color = colors.textSecondary
    )

    if (progress != null) {
        val filled = progress.coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HelloBeTheme.layout.trailSegmentHeight)
                .background(colors.surfaceMuted, HelloBeShapes.full)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(filled)
                    .fillMaxHeight()
                    .background(colors.accentPip, HelloBeShapes.full)
            )
        }
    }
}

@Suppress("ModifierParameter")
@Composable
private fun SpeakActions(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    entryModifier: Modifier
) {
    // Out of the focus order while Pip is still modelling, so a child cannot act before they have
    // heard, per ADR 0004. They arrive once and focus does not move again when the pause ends.
    val reachable = state.phase != LessonPhase.PREPARING && state.phase != LessonPhase.PROMPTING

    if (!reachable) return

    Row(
        modifier = Modifier.helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        HelloBeAction(
            label = stringResource(R.string.say_again),
            onClick = { onAction(LessonAction.ReplayRequested) },
            tone = HelloBeActionTone.SECONDARY
        )
        // Next holds entry focus, per the draft. A child may end their own turn early: the pause
        // is an invitation, and an invitation that cannot be declined is a demand.
        HelloBeAction(
            label = stringResource(R.string.say_next),
            onClick = { onAction(LessonAction.ContinueRequested) },
            tone = HelloBeActionTone.PRIMARY,
            modifier = entryModifier
        )
    }
}

/** Board proportions from the approved S05-D draft: two equal columns. */
private const val TARGET_WEIGHT = 1f
private const val RESPONSE_WEIGHT = 1f
