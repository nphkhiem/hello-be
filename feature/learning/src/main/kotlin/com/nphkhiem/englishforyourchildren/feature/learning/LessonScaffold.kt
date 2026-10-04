package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
 * [content] receives a [LessonChoiceFocus] to attach to every control a child can choose, so the
 * activity names where entry focus lands instead of the scaffold guessing where focus belongs
 * inside an activity it cannot see.
 */
@Composable
fun LessonScaffold(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Decoration behind the stage, forwarded to [StorybookScaffold]. Defaulted, so the families
     * that have no scene are untouched. Whatever goes here must carry no meaning of its own: a
     * room a child chooses in front of is scenery, a room they choose out of is not.
     */
    scenery: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.(LessonChoiceFocus) -> Unit
) {
    val replayFocus = remember { FocusRequester() }
    val firstAnswerFocus = remember { FocusRequester() }
    val focusTarget = lessonFocusTarget(state)
    val positionFormat = stringResource(R.string.lesson_position)
    val pipDescription = stringResource(pipDescriptionFor(state.phase, state.support))
    val stopForNowRestorer = rememberHelloBeFocusRestorer()
    // The choice the child was last on, forgotten with the activity it belonged to.
    val heldChoice = remember(state.activityNumber) { mutableStateOf<Any?>(null) }

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
            scenery = scenery,
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
                // The requesters are pushed into the slot rather than left for the activity to
                // arrange, so an activity cannot forget them and leave entry focus on the header
                // while a child is meant to be choosing.
                val choosing = focusTarget == LessonFocusTarget.CONTENT
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            // Until a choice has held focus there is no control to hand focus back
                            // to, so the stage itself holds the dialog's return target.
                            if (choosing && heldChoice.value == null) {
                                Modifier.focusRequester(stopForNowRestorer.returnTarget)
                            } else {
                                Modifier
                            }
                        ).helloBeFocusGroup()
                ) {
                    content(
                        LessonChoiceFocus(
                            entry = firstAnswerFocus.takeIf { choosing },
                            returnTarget = stopForNowRestorer.returnTarget.takeIf { choosing },
                            held = heldChoice
                        )
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

/**
 * Focus wiring for the controls an activity offers, attached to each through [lessonChoice].
 *
 * Carries entry focus, and the way back from Stop for now. The dialog takes focus
 * programmatically, and no focus group behind it sees that as an exit, so no `focusRestorer`
 * there remembers which control the child was on. The restore used to land on the top-left
 * answer, or on the right one only when the dialog's button happened to sit nearer it. So the
 * control remembers instead: whichever one the child was last on holds the dialog's return
 * target, which is the contract `HelloBeFocusRestorer` asks for.
 */
class LessonChoiceFocus internal constructor(
    internal val entry: FocusRequester?,
    internal val returnTarget: FocusRequester?,
    internal val held: MutableState<Any?>
)

/**
 * Marks this as the control identified by [key], which must be unique among the controls on the
 * stage. [entry] marks the one focus lands on first.
 */
fun Modifier.lessonChoice(focus: LessonChoiceFocus, key: Any, entry: Boolean = false): Modifier {
    val entryFocus = focus.entry?.takeIf { entry }
    val returnFocus = focus.returnTarget?.takeIf { focus.held.value == key }

    return this
        .then(entryFocus?.let { Modifier.focusRequester(it) } ?: Modifier)
        .then(returnFocus?.let { Modifier.focusRequester(it) } ?: Modifier)
        .onFocusChanged { if (it.isFocused) focus.held.value = key }
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
