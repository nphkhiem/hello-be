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
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.ChoiceCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.LearningObjectCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Letter and sound: a child sees a letter in both cases, hears the sound it makes, and picks the
 * picture whose word starts with it.
 *
 * The third family on the same shell, and again nothing new is invented. The letter sits where
 * picture matching puts its source, the choices sit where every family puts its answers, and the
 * sound arrives through the replay control the child already knows.
 *
 * Spelling never carries the task alone: the letter is shown, but the question is about the sound,
 * and the answers are pictures rather than words.
 */
@Composable
fun LetterAndSoundActivity(
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

            LetterBoard(state = state, onAction = onAction, entryModifier = entryModifier)
        }
    }
}

/**
 * The letter beside its picture choices, in the proportions of the approved draft.
 *
 * Unlike picture matching, these proportions fit without adjustment: three choices at 66% of the
 * board each keep a slot well above `cardFiveColumnSet`, so nothing has to give way.
 *
 * The letter card takes its height from the choice row rather than the draft's 265dp, which the
 * stage does not have room for once the prompt is drawn.
 */
// The Modifier here is not this composable's own decoration but the scaffold's entry-focus
// requester, routed to one specific child. Naming it `modifier` would claim it decorates this
// composable's root, which is exactly what it must not do.
@Suppress("ModifierParameter")
@Composable
private fun LetterBoard(
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
            LearningObjectCard(
                label = letterPair(state.learningObject.label),
                // The letter carries its own emphasis at four times the size of anything else on
                // the stage, so the card keeps the same quiet treatment as every other learning
                // object rather than borrowing the focus grammar's gold.
                labelStyle = HelloBeTheme.typography.learningGlyph,
                modifier = Modifier.weight(LETTER_WEIGHT).fillMaxHeight()
            )
        }

        if (state.answers.isNotEmpty()) {
            ChoiceRow(state = state, onAction = onAction, entryModifier = entryModifier)
        }
    }
}

// The Modifier here is not this composable's own decoration but the scaffold's entry-focus
// requester, routed to one specific child. Naming it `modifier` would claim it decorates this
// composable's root, which is exactly what it must not do.
@Suppress("ModifierParameter")
@Composable
private fun RowScope.ChoiceRow(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    entryModifier: Modifier
) {
    val availability = answerAvailability(state.phase)

    Row(
        modifier = Modifier.weight(CHOICES_WEIGHT).helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        state.answers.forEachIndexed { index, answer ->
            ChoiceCard(
                label = answer.label,
                onClick = { onAction(LessonAction.AnswerChosen(answer.id)) },
                feedback = answer.feedback,
                availability = availability,
                // Labels are drawn here, unlike picture matching: the prompt asks about a sound
                // rather than naming the target, so a caption gives nothing away.
                modifier = (if (index == 0) entryModifier else Modifier)
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

/** Board proportions from the approved S05-C draft. */
private const val LETTER_WEIGHT = 34f
private const val CHOICES_WEIGHT = 66f
