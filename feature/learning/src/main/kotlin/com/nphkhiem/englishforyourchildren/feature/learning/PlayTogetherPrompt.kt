package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryDialog

/**
 * One idea a caregiver and child can carry away from the television.
 *
 * The activity's own words are the dialog's words: the question is the title and the instruction
 * is the description, both supplied by the host, because a concrete thing to do with a real chair
 * cannot be written here.
 *
 * Both choices carry the same tone. `StopForNowDialog` emphasises its safe choice because one
 * outcome there is genuinely worse than the other; neither outcome here is risky, and giving the
 * decline the heavier treatment would quietly argue against the participation the design brief
 * spends a section inviting. What keeps a child from committing a caregiver to anything is focus,
 * not emphasis: **Maybe later** is the focused slot, per S08's "to avoid pressure and accidental
 * commitment".
 *
 * Like [StopForNowDialog] this supplies copy and nothing else. Focus containment behind the scrim,
 * the safe choice focused on appearance and focus returned on disposal all belong to [StoryDialog].
 */
@Composable
internal fun PlayTogetherPrompt(
    activity: PlayTogetherActivity,
    focusRestorer: HelloBeFocusRestorer,
    onAction: (CelebrationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    StoryDialog(
        title = activity.title,
        description = activity.instruction,
        pipDescription = stringResource(R.string.play_together_pip),
        focusRestorer = focusRestorer,
        modifier = modifier,
        // The declining choice takes the safe slot, because that is the slot StoryDialog focuses.
        //
        // This also puts it above the offer, where the draft has it below. StoryDialog couples
        // "focused" with "first" and that coupling is worth more than the draft's ordering: every
        // dialog in this app leads with the choice that already has focus, and making this the one
        // exception would cost a child the grammar they have learned everywhere else.
        safeAction = { actionModifier ->
            HelloBeAction(
                label = stringResource(R.string.play_together_decline),
                onClick = { onAction(CelebrationAction.MaybeLaterRequested) },
                tone = HelloBeActionTone.SECONDARY,
                modifier = actionModifier
            )
        },
        secondaryAction = { actionModifier ->
            HelloBeAction(
                label = stringResource(R.string.play_together_accept),
                onClick = { onAction(CelebrationAction.PlayTogetherAccepted) },
                tone = HelloBeActionTone.SECONDARY,
                modifier = actionModifier
            )
        }
    )
}
