package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryDialog

/**
 * The confirmation a child sees when they press Back during a lesson.
 *
 * Back never exits a lesson directly, so this is what Back does instead. It exists to make an
 * accidental press recoverable, which is why staying is the focused choice and stopping is the
 * quiet one.
 *
 * This supplies copy and nothing else. Every focus guarantee - the safe choice focused on
 * appearance, focus contained behind the scrim, focus returned on disposal - belongs to
 * [StoryDialog] and must not be reimplemented here.
 *
 * The description is chosen by whether progress is saved yet. Both wordings are transcribed
 * verbatim from the approved draft rather than paraphrased, because a dialog that softens
 * "Pip may not remember this part yet" into something reassuring would be lying to a child.
 */
@Composable
internal fun StopForNowDialog(
    pendingSave: Boolean,
    focusRestorer: HelloBeFocusRestorer,
    onAction: (LessonAction) -> Unit,
    modifier: Modifier = Modifier
) {
    StoryDialog(
        title = stringResource(R.string.stop_for_now_title),
        description = stringResource(stopForNowDescription(pendingSave)),
        pipDescription = stringResource(R.string.stop_for_now_pip),
        focusRestorer = focusRestorer,
        modifier = modifier,
        safeAction = { actionModifier ->
            HelloBeAction(
                label = stringResource(R.string.stop_for_now_keep),
                onClick = { onAction(LessonAction.KeepLearningRequested) },
                tone = HelloBeActionTone.POSITIVE,
                modifier = actionModifier
            )
        },
        secondaryAction = { actionModifier ->
            HelloBeAction(
                label = stringResource(R.string.stop_for_now_stop),
                onClick = { onAction(LessonAction.StopForNowConfirmed) },
                tone = HelloBeActionTone.QUIET,
                modifier = actionModifier
            )
        }
    )
}
