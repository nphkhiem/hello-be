package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusFrame
import com.nphkhiem.englishforyourchildren.ui.tv.component.StorybookScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The barrier between child mode and the caregiver area.
 *
 * It is a capability check and not authentication. The information architecture forbids calling it
 * a password or a secure login, and nothing here should read as one: no field to type into, no
 * failure language, no lockout.
 *
 * What actually protects a child is where focus starts. Entry focus rests on an answer that is not
 * the correct one, so pressing Select without moving reports a wrong answer. Because the host
 * rotates the challenge on every wrong answer, walking the row and pressing each in turn gains
 * nothing either: the next question puts the correct answer somewhere else.
 *
 * Back is not intercepted. The gate is its own destination and the host's Back returns to the child
 * surface that opened it, which is what the information architecture asks for and what the hint at
 * the foot of the card explains in both languages.
 */
@Composable
fun AdultGateScreen(
    state: AdultGateUiState,
    onAction: (AdultGateAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val answerFocus = remember { FocusRequester() }
    val focusIndex = gateFocusIndex(state.challenge)

    StorybookScaffold(
        modifier = modifier,
        entryFocus = if (focusIndex != null) answerFocus else null
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GateCard(
                state = state,
                focusIndex = focusIndex,
                onAction = onAction,
                answerFocus = answerFocus
            )
        }
    }
}

@Composable
private fun GateCard(
    state: AdultGateUiState,
    focusIndex: Int?,
    onAction: (AdultGateAction) -> Unit,
    answerFocus: FocusRequester
) {
    Surface(
        modifier = Modifier.widthIn(max = HelloBeLayout.dialogMaxWidth),
        shape = HelloBeShapes.dialog,
        colors = SurfaceDefaults.colors(
            containerColor = HelloBeTheme.colors.surfaceRaised,
            contentColor = HelloBeTheme.colors.textPrimary
        ),
        border = HelloBeFocusFrame.resting(HelloBeShapes.dialog)
    ) {
        Column(
            modifier = Modifier.padding(HelloBeTheme.spacing.sectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
        ) {
            Text(
                text = stringResource(R.string.gate_title),
                style = HelloBeTheme.typography.headlineLarge,
                color = HelloBeTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            if (focusIndex != null) {
                // Withheld when there is no question. Asking a caregiver to solve one directly
                // above a line saying there is not one is the self-contradicting copy that
                // shipped on child home once already.
                Text(
                    text = stringResource(R.string.gate_instruction),
                    style = HelloBeTheme.typography.bodyMedium,
                    color = HelloBeTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            if (state.previousAnswerWasWrong) {
                // Neutral, and never the error palette. A grown-up who mistypes has not failed at
                // anything, and the design brief keeps failure language out of this product.
                Text(
                    text = stringResource(R.string.gate_retry),
                    style = HelloBeTheme.typography.bodyMedium,
                    color = HelloBeTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            if (focusIndex == null) {
                // Fails closed. A challenge that cannot offer a wrong answer to stand on would open
                // on a single press, so it offers nothing to press at all.
                Text(
                    text = stringResource(R.string.gate_unavailable),
                    style = HelloBeTheme.typography.bodyMedium,
                    color = HelloBeTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = state.challenge.question,
                    style = HelloBeTheme.typography.headlineMedium,
                    color = HelloBeTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                AnswerRow(
                    state = state,
                    focusIndex = focusIndex,
                    onAction = onAction,
                    answerFocus = answerFocus
                )
            }

            Text(
                text = stringResource(R.string.gate_back_hint),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnswerRow(
    state: AdultGateUiState,
    focusIndex: Int,
    onAction: (AdultGateAction) -> Unit,
    answerFocus: FocusRequester
) {
    // Equal thirds of the card, per the draft, rather than each answer wrapping its own digits.
    // Equal width is not only layout here: answers of different widths would make the correct one
    // findable by shape on a screen where nothing may single it out.
    Row(
        modifier = Modifier.fillMaxWidth().helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        state.challenge.answers.forEachIndexed { index, answer ->
            // Every answer is drawn identically. Nothing about the correct one differs in tone,
            // size, order or emphasis, so the only thing the correct index changes is where focus
            // refuses to start.
            HelloBeAction(
                label = answer,
                onClick = { onAction(AdultGateAction.AnswerChosen(index)) },
                tone = HelloBeActionTone.SECONDARY,
                // The primary action height rather than the caregiver control height. The draft
                // draws these at seventy pixels because they are the one thing on the screen a
                // caregiver aims at from a sofa, and the caregiver density is meant for the dense
                // section surfaces behind the gate, not for this.
                minHeight = HelloBeLayout.childPrimaryActionMinHeight,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (index == focusIndex) {
                            Modifier.focusRequester(answerFocus)
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
