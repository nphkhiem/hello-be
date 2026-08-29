package com.nphkhiem.englishforyourchildren.feature.caregiver

/**
 * Whether the confirmation's choices can be pressed.
 *
 * False while the work is underway, which is what prevents a second confirmation. A caregiver
 * pressing Select twice on a slow delete would otherwise ask for it twice, and the second press
 * lands on a dialog that is already doing the thing.
 *
 * The first half of that protection is focus: the safe choice holds it, so repeated Select without
 * moving keeps the profile rather than removing it. This is the second half, for the caregiver who
 * did move and then pressed twice.
 */
internal fun isActionable(phase: CaregiverConfirmationPhase): Boolean =
    phase != CaregiverConfirmationPhase.WORKING

/** Whether the failure notice and its retry belong on screen. */
internal fun hasFailed(phase: CaregiverConfirmationPhase): Boolean =
    phase == CaregiverConfirmationPhase.FAILED
