package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.ChoiceCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.LearningObjectCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Picture matching: a child sees the object the lesson is about and finds it again among two to
 * four pictures.
 *
 * Deliberately the same grammar as listen and choose with one thing added. The source is presented
 * already committed rather than chosen, so there is no lock to make, nothing to cancel, and no
 * drag: a child who has done one activity can do this one without learning a new interaction.
 *
 * Like every activity here it renders the state it is given and reports what was pressed. It is
 * never told which destination is correct, so it cannot reveal it through focus or ordering.
 */
@Composable
fun PictureMatchingActivity(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LessonScaffold(state = state, onAction = onAction, modifier = modifier) { entryModifier ->
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

            MatchBoard(state = state, onAction = onAction, entryModifier = entryModifier)
        }
    }
}

/**
 * Source, relationship and destinations, in one row.
 *
 * The approved draft shows four destinations as a 2x2 grid, and that does not fit. At the 540dp
 * reference canvas the stage leaves 280dp for content, while two rows of cards at
 * `childChoiceMinHeight` plus their focus clearance need 312dp before the prompt is drawn at all.
 * The draft assumes Pip sits beside the prompt; this shell gives Pip a 96dp strip along the
 * bottom, and that is where the height went.
 *
 * So the destinations stay in a single row and the source gives up width instead of the cards
 * giving up height. A child's target may not shrink below the minimum; the source's share of the
 * board may. At four destinations this is the widest the source can be and still leave each card
 * a `cardFiveColumnSet` slot.
 */
@Composable
private fun MatchBoard(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    entryModifier: Modifier
) {
    // Derived from the tokens rather than measured intrinsically, so the cards cannot be squeezed
    // below the minimum by whatever height the stage happens to have left.
    val boardHeight = HelloBeTheme.layout.childChoiceMinHeight + HelloBeTheme.focus.clearance * 2

    Row(
        modifier = Modifier.fillMaxWidth().height(boardHeight),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.learningObject != null) {
            LearningObjectCard(
                label = state.learningObject.label,
                modifier = Modifier.weight(SOURCE_WEIGHT).fillMaxHeight()
            )
            // The arrow goes with the source. An activity that arrives without one must not be
            // left pointing at nothing.
            Text(
                text = stringResource(R.string.lesson_match_direction),
                style = HelloBeTheme.typography.headlineMedium,
                color = HelloBeTheme.colors.textTertiary,
                textAlign = TextAlign.Center,
                // The relationship is already stated by the prompt and by the reading order, so
                // announcing "right arrow" between them would add noise rather than meaning.
                modifier = Modifier.weight(ARROW_WEIGHT).clearAndSetSemantics { }
            )
        }

        if (state.answers.isNotEmpty()) {
            DestinationRow(state = state, onAction = onAction, entryModifier = entryModifier)
        }
    }
}

@Composable
private fun RowScope.DestinationRow(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    entryModifier: Modifier
) {
    val availability = answerAvailability(state.phase)

    Row(
        // One focus group, so leaving the board and coming back returns the child to the picture
        // they were about to choose rather than the nearest one.
        modifier = Modifier.weight(DESTINATIONS_WEIGHT).helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        state.answers.forEachIndexed { index, answer ->
            ChoiceCard(
                label = answer.label,
                onClick = { onAction(LessonAction.AnswerChosen(answer.id)) },
                feedback = answer.feedback,
                availability = availability,
                // The prompt already names the target, so a captioned answer would make the
                // question solvable by reading instead of by looking.
                labelVisible = false,
                // Entry focus goes to the first destination by position, never by which one is
                // correct, because this screen is not told which one that is.
                modifier = (if (index == 0) entryModifier else Modifier)
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * Board proportions. The draft's 31% source is narrowed so four destinations each keep a
 * `cardFiveColumnSet` slot; anything wider starves them.
 */
private const val SOURCE_WEIGHT = 20f
private const val ARROW_WEIGHT = 8f
private const val DESTINATIONS_WEIGHT = 72f
