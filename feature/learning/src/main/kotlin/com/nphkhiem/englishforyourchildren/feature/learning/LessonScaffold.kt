package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.CaptionPanel
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipGuide
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipPose
import com.nphkhiem.englishforyourchildren.ui.tv.component.ProgressTrail
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryHeader
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryLoading
import com.nphkhiem.englishforyourchildren.ui.tv.component.StorybookScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberHelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import java.util.Locale

/**
 * The stage a child sees in every activity family: where they are, what they are being asked,
 * Pip, the caption, the replay control, and the space the activity fills.
 *
 * Identical across families on purpose, so a child learns the screen once and the four activities
 * still to come add content rather than a new arrangement.
 *
 * Renders and emits, nothing else. There is no timer here, no attempt counter and no audio handle:
 * the support ladder arrives already decided, per ADR 0003.
 *
 * [content] receives a modifier carrying entry focus whenever the child should be choosing, so
 * the activity attaches it to its first answer instead of the scaffold guessing where focus
 * belongs inside an activity it cannot see.
 */
@Composable
fun LessonScaffold(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(Modifier) -> Unit
) {
    val replayFocus = remember { FocusRequester() }
    val firstAnswerFocus = remember { FocusRequester() }
    val focusTarget = lessonFocusTarget(state)
    val positionFormat = stringResource(R.string.lesson_position)
    val pipDescription = stringResource(pipDescriptionFor(state.phase, state.support))
    val stopForNowRestorer = rememberHelloBeFocusRestorer()

    // Back never exits an active lesson directly. It asks instead, and while it is asking, Back
    // and the safe choice are the same press, so a second dialog cannot stack on the first.
    BackHandler {
        onAction(
            if (state.stopForNowVisible) {
                LessonAction.KeepLearningRequested
            } else {
                LessonAction.BackRequested
            }
        )
    }

    Box(modifier = modifier) {
        StorybookScaffold(
            entryFocus = when (focusTarget) {
                LessonFocusTarget.REPLAY -> replayFocus
                LessonFocusTarget.CONTENT -> firstAnswerFocus
            },
            header = {
                StoryHeader(
                    modifier = Modifier.fillMaxWidth(),
                    title = state.activityTitle,
                    contextLabel = state.unitName,
                    progress = {
                        ProgressTrail(
                            totalSteps = state.activityCount,
                            currentStep = state.activityNumber,
                            describePosition = { current, total ->
                                String.format(Locale.getDefault(), positionFormat, current, total)
                            }
                        )
                    },
                    action = {
                        LessonHeaderActions(
                            state = state,
                            onAction = onAction,
                            replayModifier = Modifier
                                .focusRequester(replayFocus)
                                .then(
                                    // Focus comes back here when the content has nothing to return
                                    // to: preparing, while the prompt plays, or no answers at all.
                                    if (focusTarget == LessonFocusTarget.REPLAY) {
                                        Modifier.focusRequester(stopForNowRestorer.returnTarget)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                )
            },
            support = {
                PipGuide(
                    pose = pipPoseFor(state.phase, state.support),
                    contentDescription = pipDescription,
                    modifier = Modifier.size(HelloBeLayout.pipMinSize)
                )
                CaptionPanel(
                    text = state.caption,
                    visible = state.caption != null,
                    modifier = Modifier.weight(1f)
                )
                if (state.pendingSave) {
                    Text(
                        text = stringResource(R.string.lesson_pending_save),
                        style = HelloBeTheme.typography.labelSmall,
                        color = HelloBeTheme.colors.warningContent
                    )
                }
            }
        ) {
            if (state.phase == LessonPhase.PREPARING) {
                StoryLoading(
                    contentDescription = stringResource(R.string.lesson_preparing),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // The requester is pushed into the slot rather than left for the activity to
                // arrange, so an activity cannot forget it and leave entry focus on the header
                // while a child is meant to be choosing.
                // Wrapped in a focus group carrying the restorer's return target, so closing the
                // dialog returns the child to the answer they were on rather than the first one.
                // helloBeFocusGroup already applies focusRestorer, and the answer row inside carries
                // its own, so the two chain: the outer restores the row, the row restores the card.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (focusTarget == LessonFocusTarget.CONTENT) {
                                Modifier.focusRequester(stopForNowRestorer.returnTarget)
                            } else {
                                Modifier
                            }
                        ).helloBeFocusGroup()
                ) {
                    content(
                        if (focusTarget == LessonFocusTarget.CONTENT) {
                            Modifier.focusRequester(firstAnswerFocus)
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }

        if (state.stopForNowVisible) {
            StopForNowDialog(
                pendingSave = state.pendingSave,
                focusRestorer = stopForNowRestorer,
                onAction = onAction
            )
        }
    }
}

// The Modifier here is not this composable's own decoration but the scaffold's entry-focus
// requester, routed to one specific child. Naming it `modifier` would claim it decorates this
// composable's root, which is exactly what it must not do.
@Suppress("ModifierParameter")
@Composable
private fun LessonHeaderActions(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    replayModifier: Modifier
) {
    Row(modifier = Modifier.helloBeFocusGroup()) {
        HelloBeAction(
            label = stringResource(R.string.lesson_replay),
            onClick = { onAction(LessonAction.ReplayRequested) },
            tone = HelloBeActionTone.SECONDARY,
            availability = if (state.audioAvailable) {
                HelloBeAvailability.ENABLED
            } else {
                HelloBeAvailability.UNAVAILABLE
            },
            stateDescription = if (state.audioAvailable) {
                null
            } else {
                stringResource(R.string.lesson_replay_unavailable)
            },
            modifier = replayModifier
        )
        if (isUnscoredSkipOffered(state)) {
            HelloBeAction(
                label = stringResource(R.string.lesson_skip),
                onClick = { onAction(LessonAction.SkipRequested) },
                tone = HelloBeActionTone.QUIET
            )
        }
    }
}
