package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipGuide
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipPose
import com.nphkhiem.englishforyourchildren.ui.tv.component.RecoveryPanel
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryDialog
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout

/**
 * The four things that can go wrong in front of a child, and what each of them offers.
 *
 * [keepsContextBehind] separates a failure that interrupts an activity from one that replaces a
 * destination. Audio failing and a checkpoint not landing both happen while a child is in the
 * middle of something, and the approved rules ask for the interrupted activity to stay visible, so
 * those two arrive as an overlay over the lesson. A lesson that cannot open, and a shelf with
 * nothing on it, have no activity behind them to keep.
 *
 * There is deliberately no field for a diagnostic code anywhere in this file. Technical detail
 * belongs behind the adult gate, and the way to hold that is for the child's model to have nowhere
 * to put one.
 */
enum class ChildRecoveryReason(internal val keepsContextBehind: Boolean) {
    AUDIO_UNAVAILABLE(keepsContextBehind = true),
    PROGRESS_PENDING(keepsContextBehind = true),
    LESSON_UNAVAILABLE(keepsContextBehind = false),
    EMPTY_LIBRARY(keepsContextBehind = false)
}

/**
 * What a child recovery reports upward.
 *
 * One safe action per reason and one alternative at most. Every one of them names a destination
 * that exists, so no recovery can leave a child somewhere with nothing to press.
 */
sealed interface ChildRecoveryAction {
    /** Try the sound again. */
    data object AudioRetryRequested : ChildRecoveryAction

    /** Take the unscored demonstration instead of an unfair question. */
    data object DemonstrationRequested : ChildRecoveryAction

    /** Carry on, with the checkpoint still pending. Nothing claims it was saved. */
    data object KeepLearningRequested : ChildRecoveryAction

    /** Fetch a grown-up. Opens the adult gate, which is the host's to show. */
    data object CaregiverHelpRequested : ChildRecoveryAction

    /** Return to the learning path, where a valid lesson is focused. */
    data object LearningPathRequested : ChildRecoveryAction
}

/**
 * The recovery a child sees, chosen entirely by [reason].
 *
 * The safe action is the focused one in every variant, which is the first of the approved recovery
 * rules: recovery never opens with a destructive action focused. Neither variant here has a
 * destructive action at all.
 */
@Composable
fun ChildRecovery(
    reason: ChildRecoveryReason,
    focusRestorer: HelloBeFocusRestorer,
    onAction: (ChildRecoveryAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reason.keepsContextBehind) {
        InterruptingRecovery(
            reason = reason,
            focusRestorer = focusRestorer,
            onAction = onAction,
            modifier = modifier
        )
    } else {
        ReplacingRecovery(reason = reason, onAction = onAction, modifier = modifier)
    }
}

/** Over the activity, so the child can still see the thing they were working on. */
@Composable
private fun InterruptingRecovery(
    reason: ChildRecoveryReason,
    focusRestorer: HelloBeFocusRestorer,
    onAction: (ChildRecoveryAction) -> Unit,
    modifier: Modifier
) {
    val audio = reason == ChildRecoveryReason.AUDIO_UNAVAILABLE

    StoryDialog(
        title = stringResource(
            if (audio) R.string.recovery_audio_title else R.string.recovery_pending_title
        ),
        description = stringResource(
            if (audio) R.string.recovery_audio_body else R.string.recovery_pending_body
        ),
        pipDescription = stringResource(R.string.recovery_pip),
        focusRestorer = focusRestorer,
        modifier = modifier,
        safeAction = { actionModifier ->
            HelloBeAction(
                label = stringResource(
                    if (audio) R.string.recovery_audio_retry else R.string.recovery_pending_keep
                ),
                onClick = {
                    onAction(
                        if (audio) {
                            ChildRecoveryAction.AudioRetryRequested
                        } else {
                            ChildRecoveryAction.KeepLearningRequested
                        }
                    )
                },
                tone = HelloBeActionTone.POSITIVE,
                modifier = actionModifier
            )
        },
        secondaryAction = { actionModifier ->
            HelloBeAction(
                label = stringResource(
                    if (audio) R.string.recovery_audio_show else R.string.recovery_pending_grownup
                ),
                onClick = {
                    onAction(
                        if (audio) {
                            ChildRecoveryAction.DemonstrationRequested
                        } else {
                            ChildRecoveryAction.CaregiverHelpRequested
                        }
                    )
                },
                tone = HelloBeActionTone.QUIET,
                modifier = actionModifier
            )
        }
    )
}

/** In place of the destination, because there is no activity left behind it to keep. */
@Composable
private fun ReplacingRecovery(
    reason: ChildRecoveryReason,
    onAction: (ChildRecoveryAction) -> Unit,
    modifier: Modifier
) {
    val empty = reason == ChildRecoveryReason.EMPTY_LIBRARY

    RecoveryPanel(
        modifier = modifier,
        kicker = stringResource(
            if (empty) R.string.recovery_empty_kicker else R.string.recovery_lesson_kicker
        ),
        title = stringResource(
            if (empty) R.string.recovery_empty_title else R.string.recovery_lesson_title
        ),
        message = stringResource(
            if (empty) R.string.recovery_empty_body else R.string.recovery_lesson_body
        ),
        illustration = {
            // Explicitly sized, as every other caller sizes it. Without a size PipGuide
            // measures to nothing and takes the panel around it down with it, which is filed
            // separately as a defect in the component rather than worked around quietly here.
            PipGuide(
                pose = PipPose.GREETING,
                contentDescription = stringResource(R.string.recovery_pip),
                modifier = Modifier.size(HelloBeLayout.pipMinSize)
            )
        },
        safeAction = { actionModifier ->
            HelloBeAction(
                label = stringResource(
                    if (empty) R.string.recovery_empty_action else R.string.recovery_lesson_action
                ),
                onClick = { onAction(ChildRecoveryAction.LearningPathRequested) },
                tone = HelloBeActionTone.PRIMARY,
                modifier = actionModifier
            )
        }
    )
}
