package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.RecoveryPanel

/**
 * The local learning data could not be opened.
 *
 * The only recovery that carries a diagnostic code, and the only one a child never sees. It exists
 * on a caregiver surface, behind the adult gate, which is what makes the code safe to show at all.
 *
 * [code] is a short identifier with nothing of the child in it. The copy that frames it says so,
 * because a caregiver reading a code aloud to someone helping them should know what they are
 * handing over.
 */
@androidx.compose.runtime.Immutable
data class CaregiverRecoveryState(val code: String)

/** What the caregiver recovery reports upward. */
sealed interface CaregiverRecoveryAction {
    /** Open the local data again. Always offered first, and always focused. */
    data object RetryRequested : CaregiverRecoveryAction

    /**
     * Look at resetting, which opens the destructive confirmation rather than resetting anything.
     *
     * The approved draft is explicit that reset "remains the final path, never the first
     * suggestion", so this reviews and the confirmation decides. Nothing is destroyed by pressing
     * it, which is the whole of the gating this task asks for.
     */
    data object ResetReviewRequested : CaregiverRecoveryAction
}

/**
 * What a caregiver sees when the local data will not open.
 *
 * Retry is the focused action. Reset is offered beside it in the destructive tone but opens a
 * confirmation, so the destructive path takes two deliberate choices and never starts focused.
 */
@Composable
fun CaregiverRecovery(
    state: CaregiverRecoveryState,
    onAction: (CaregiverRecoveryAction) -> Unit,
    modifier: Modifier = Modifier
) {
    RecoveryPanel(
        modifier = modifier,
        kicker = caregiverText(R.string.recovery_db_kicker),
        title = caregiverText(R.string.recovery_db_title),
        message = caregiverText(R.string.recovery_db_body),
        technicalDetail = caregiverText(R.string.recovery_db_code, state.code),
        safeAction = { actionModifier ->
            HelloBeAction(
                label = caregiverText(R.string.recovery_db_retry),
                onClick = { onAction(CaregiverRecoveryAction.RetryRequested) },
                tone = HelloBeActionTone.POSITIVE,
                modifier = actionModifier
            )
        },
        secondaryAction = { actionModifier ->
            HelloBeAction(
                label = caregiverText(R.string.recovery_db_review_reset),
                onClick = { onAction(CaregiverRecoveryAction.ResetReviewRequested) },
                tone = HelloBeActionTone.DESTRUCTIVE,
                modifier = actionModifier
            )
        }
    )
}
