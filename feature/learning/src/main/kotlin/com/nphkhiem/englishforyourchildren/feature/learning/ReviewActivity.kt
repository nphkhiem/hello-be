package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.ChoiceCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.LearningObjectCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.PackagedPicture
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberPackagedPicture
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Review: familiar objects come back and a child recalls which one the question means.
 *
 * The last activity a child meets, and the one whose requirement is a feeling: it must seem
 * familiar rather than like a new game. So it adds nothing. No new state, no new action, no new
 * phase, no rule to compute. It is the shared shell, the shared answer row, and a room behind
 * them.
 *
 * The approved draft places the objects inside the room at scattered positions and sizes. They are
 * in the shared row here instead, and the room is scenery. `CONTEXT.md` reserves scenery for
 * decoration that never carries meaning on its own, and a room a child chooses out of would carry
 * it. Making the final activity the only one with a bespoke interactive layout is also the surest
 * way to make it feel like a new game.
 */
@Composable
fun ReviewActivity(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LessonScaffold(
        state = state,
        onAction = onAction,
        modifier = modifier,
        scenery = { ReviewRoom() }
    ) { entryModifier ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = HelloBeTheme.spacing.sectionGap,
                alignment = Alignment.CenterVertically
            )
        ) {
            Text(
                text = state.prompt,
                style = HelloBeTheme.typography.headlineLarge,
                color = HelloBeTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            RecallBoard(state = state, onAction = onAction, entryModifier = entryModifier)
        }
    }
}

/**
 * The room the objects are remembered from.
 *
 * Decoration and nothing else: no semantics, no focus, no meaning. Drawn from tokens rather than
 * from artwork, which is the same placeholder footing Pip and the loading blocks already stand on
 * while final art waits behind its own approval gate.
 *
 * Proportioned after the approved draft, which insets the room and anchors it to the lower part of
 * the stage so the horizon sits behind the answers rather than through them.
 */
@Composable
internal fun BoxScope.ReviewRoom(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(ROOM_WIDTH_FRACTION)
            .fillMaxHeight(ROOM_HEIGHT_FRACTION)
            // storyPage is already the draft's room outline: rounded at the top, squarer where it
            // meets the floor. Using it keeps the room on the token set rather than inventing a
            // one-off radius for a placeholder.
            .background(HelloBeTheme.colors.surfaceRaised, HelloBeShapes.storyPage)
            .border(
                width = HelloBeTheme.focus.guardWidth,
                color = HelloBeTheme.colors.borderSecondary,
                shape = HelloBeShapes.storyPage
            )
    )
}

@Suppress("ModifierParameter")
@Composable
private fun RecallBoard(
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
        // Present only for the recall items that bring an object back with them. This is the whole
        // of "mixed recall": the same row, sometimes with a reminder beside it.
        if (state.learningObject != null) {
            val subject = rememberPackagedPicture(state.learningObject.image)

            LearningObjectCard(
                label = state.learningObject.label,
                labelVisible = subject == null,
                illustration = subject?.let { { PackagedPicture(it) } },
                modifier = Modifier.weight(OBJECT_WEIGHT).fillMaxHeight()
            )
        }

        if (state.answers.isNotEmpty()) {
            RecallRow(state = state, onAction = onAction, entryModifier = entryModifier)
        }
    }
}

@Suppress("ModifierParameter")
@Composable
private fun RowScope.RecallRow(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    entryModifier: Modifier
) {
    val availability = answerAvailability(state.phase)

    Row(
        modifier = Modifier.weight(ANSWERS_WEIGHT).helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        state.answers.forEachIndexed { index, answer ->
            val picture = rememberPackagedPicture(answer.image)

            ChoiceCard(
                label = answer.label,
                onClick = { onAction(LessonAction.AnswerChosen(answer.id)) },
                feedback = answer.feedback,
                availability = availability,
                // Review is recall. A captioned answer would let a reader skip the remembering,
                // which is the only thing this activity is asking for, drawn or undrawn. Same
                // reason picture matching does not fall back to the word either.
                labelVisible = false,
                illustration = picture?.let { { PackagedPicture(it) } },
                modifier = (if (index == 0) entryModifier else Modifier)
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * Room proportions. The width follows the approved draft; the height is taller than its 53%
 * because the answers sit in the shared centred row rather than low inside the room, and a horizon
 * cutting across the cards reads as a mistake rather than as a room.
 */
private const val ROOM_WIDTH_FRACTION = 0.86f
private const val ROOM_HEIGHT_FRACTION = 0.66f

/** A recall item with an object beside it splits the board; without one the answers take it all. */
private const val OBJECT_WEIGHT = 30f
private const val ANSWERS_WEIGHT = 70f
