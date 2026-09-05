package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryDialog
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The confirmation a caregiver sees before anything is destroyed.
 *
 * One component, two kinds, and every word chosen by the kind. Deleting removes a child from this
 * television; resetting keeps the child and restarts what they have learned, and the stop condition
 * asks for those to stay distinct. Nothing here says "OK": both choices name what they do, which is
 * what the approved draft calls out as its own anatomy item.
 *
 * Two things stop an accidental confirmation, and both are needed:
 *
 * The safe choice holds focus, so a caregiver pressing Select without moving keeps the profile.
 * That is [StoryDialog]'s contract and the reason the safe slot is the focused one.
 *
 * While the work is underway nothing can be pressed, so a caregiver who did move and then pressed
 * twice asks for the deletion once. The second press lands on a dialog that is already doing it.
 *
 * Back is the safe choice, as it is everywhere else in this product: it keeps the profile rather
 * than confirming, and it does nothing at all while the work is in flight.
 */
@Composable
fun CaregiverConfirmation(
    state: CaregiverConfirmationState,
    focusRestorer: HelloBeFocusRestorer,
    onAction: (CaregiverConfirmationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val actionable = isActionable(state.phase)

    // Back is the safe way out, and is refused outright while the work is underway: dismissing a
    // deletion that is already happening would tell a caregiver they had stopped something they
    // had not.
    BackHandler(enabled = actionable) {
        onAction(CaregiverConfirmationAction.Dismissed)
    }

    StoryDialog(
        title = caregiverText(titleFor(state.kind), state.profileName),
        description = descriptionFor(state),
        pipDescription = state.profileName,
        focusRestorer = focusRestorer,
        modifier = modifier,
        pip = { IdentityMark(state = state) },
        safeAction = { actionModifier ->
            HelloBeAction(
                label = caregiverText(keepLabelFor(state.kind)),
                onClick = { onAction(CaregiverConfirmationAction.Dismissed) },
                tone = HelloBeActionTone.POSITIVE,
                availability = availability(actionable),
                modifier = actionModifier
            )
        },
        secondaryAction = { actionModifier ->
            if (hasFailed(state.phase)) {
                // After a failure the second choice becomes the retry. Offering the destructive
                // action again next to a line saying nothing changed would read as though the
                // first press had half worked.
                HelloBeAction(
                    label = caregiverText(R.string.confirm_retry),
                    onClick = { onAction(CaregiverConfirmationAction.RetryRequested) },
                    tone = HelloBeActionTone.QUIET,
                    modifier = actionModifier
                )
            } else {
                HelloBeAction(
                    label = caregiverText(destructiveLabelFor(state.kind)),
                    onClick = { onAction(CaregiverConfirmationAction.Confirmed) },
                    tone = HelloBeActionTone.DESTRUCTIVE,
                    availability = availability(actionable),
                    modifier = actionModifier
                )
            }
        }
    )
}

/**
 * Who this is about, in the dialog's own identity slot.
 *
 * Delete shows the child's avatar, because the thing being removed is that child. Reset shows a
 * reset mark, because the child stays and only their learning restarts. The draft distinguishes
 * them exactly this way.
 */
@Composable
private fun IdentityMark(state: CaregiverConfirmationState) {
    Box(
        modifier = Modifier.size(HelloBeLayout.pipMinSize),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (state.kind) {
                CaregiverConfirmationKind.DELETE_PROFILE -> state.profileAvatar

                CaregiverConfirmationKind.RESET_PROGRESS ->
                    caregiverText(R.string.confirm_reset_mark)
            },
            style = HelloBeTheme.typography.headlineLarge,
            color = HelloBeTheme.colors.textSecondary
        )
    }
}

/**
 * What the dialog says, which changes with the phase.
 *
 * The failure line states that nothing changed. That is the transactional guarantee the design
 * brief asks for, and the only sentence that lets a caregiver stop worrying.
 */
@Composable
private fun descriptionFor(state: CaregiverConfirmationState): String = when (state.phase) {
    CaregiverConfirmationPhase.READY ->
        caregiverText(bodyFor(state.kind), state.profileName)

    CaregiverConfirmationPhase.WORKING -> caregiverText(workingFor(state.kind))

    CaregiverConfirmationPhase.FAILED ->
        caregiverText(failedFor(state.kind), state.profileName)
}

private fun availability(actionable: Boolean): HelloBeAvailability =
    if (actionable) HelloBeAvailability.ENABLED else HelloBeAvailability.UNAVAILABLE

private fun titleFor(kind: CaregiverConfirmationKind): Int = when (kind) {
    CaregiverConfirmationKind.DELETE_PROFILE -> R.string.confirm_delete_title
    CaregiverConfirmationKind.RESET_PROGRESS -> R.string.confirm_reset_title
}

private fun bodyFor(kind: CaregiverConfirmationKind): Int = when (kind) {
    CaregiverConfirmationKind.DELETE_PROFILE -> R.string.confirm_delete_body
    CaregiverConfirmationKind.RESET_PROGRESS -> R.string.confirm_reset_body
}

private fun keepLabelFor(kind: CaregiverConfirmationKind): Int = when (kind) {
    CaregiverConfirmationKind.DELETE_PROFILE -> R.string.confirm_delete_keep
    CaregiverConfirmationKind.RESET_PROGRESS -> R.string.confirm_reset_keep
}

private fun destructiveLabelFor(kind: CaregiverConfirmationKind): Int = when (kind) {
    CaregiverConfirmationKind.DELETE_PROFILE -> R.string.confirm_delete_do
    CaregiverConfirmationKind.RESET_PROGRESS -> R.string.confirm_reset_do
}

private fun workingFor(kind: CaregiverConfirmationKind): Int = when (kind) {
    CaregiverConfirmationKind.DELETE_PROFILE -> R.string.confirm_delete_working
    CaregiverConfirmationKind.RESET_PROGRESS -> R.string.confirm_reset_working
}

private fun failedFor(kind: CaregiverConfirmationKind): Int = when (kind) {
    CaregiverConfirmationKind.DELETE_PROFILE -> R.string.confirm_delete_failed
    CaregiverConfirmationKind.RESET_PROGRESS -> R.string.confirm_reset_failed
}
