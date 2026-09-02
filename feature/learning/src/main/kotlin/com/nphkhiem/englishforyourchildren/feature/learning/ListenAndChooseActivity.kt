package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.ChoiceCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.PackagedPicture
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberPackagedPicture
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Listen and choose: a child hears a question, then picks between two and four pictures.
 *
 * The activity every other family borrows from, so its grammar is deliberately plain. It renders
 * the prompt and the answers it is given and reports which one was chosen. It does not know which
 * answer is correct, cannot mark one, and never decides when help should escalate.
 */
@Composable
fun ListenAndChooseActivity(
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

            // An activity that arrives with no answers shows the prompt alone. Drawing an empty
            // row would present a child with a question they have no way to answer.
            if (state.answers.isNotEmpty()) {
                AnswerRow(state = state, onAction = onAction, entryModifier = entryModifier)
            }
        }
    }
}

// The Modifier here is not this composable's own decoration but the scaffold's entry-focus
// requester, routed to one specific child. Naming it `modifier` would claim it decorates this
// composable's root, which is exactly what it must not do.
@Suppress("ModifierParameter")
@Composable
private fun AnswerRow(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    entryModifier: Modifier
) {
    val availability = answerAvailability(state.phase)

    Row(
        modifier = Modifier.helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        state.answers.forEachIndexed { index, answer ->
            val picture = rememberPackagedPicture(answer.image)

            ChoiceCard(
                label = answer.label,
                onClick = { onAction(LessonAction.AnswerChosen(answer.id)) },
                feedback = answer.feedback,
                availability = availability,
                // A packaged picture is the answer, so the word steps aside for it. Until one is
                // drawn the word is all there is, and it beats a blank card.
                labelVisible = picture == null,
                illustration = picture?.let { { PackagedPicture(it) } },
                // Entry focus goes to the first answer by position. It is never routed by which
                // answer is correct, because this screen is not told which one that is.
                modifier = if (index == 0) {
                    entryModifier.width(HelloBeLayout.cardThreeColumnSet)
                } else {
                    Modifier.width(HelloBeLayout.cardThreeColumnSet)
                }
            )
        }
    }
}
