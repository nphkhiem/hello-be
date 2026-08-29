package com.nphkhiem.englishforyourchildren.feature.caregiver

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CaregiverConfirmationRulesTest {

    @Test
    fun givenAReadyConfirmation_whenItIsRead_thenBothChoicesCanBePressed() {
        assertThat(isActionable(CaregiverConfirmationPhase.READY)).isTrue()
    }

    @Test
    fun givenWorkIsUnderway_whenItIsRead_thenNothingCanBePressed() {
        // The second half of the protection against an accidental confirmation: a caregiver who
        // moved focus and pressed twice asks for the deletion once.
        assertThat(isActionable(CaregiverConfirmationPhase.WORKING)).isFalse()
    }

    @Test
    fun givenTheWorkFailed_whenItIsRead_thenChoicesComeBack() {
        // A caregiver has to be able to leave, or try again, after a failure.
        assertThat(isActionable(CaregiverConfirmationPhase.FAILED)).isTrue()
    }

    @Test
    fun givenTheWorkFailed_whenTheNoticeIsConsidered_thenItBelongsOnScreen() {
        assertThat(hasFailed(CaregiverConfirmationPhase.FAILED)).isTrue()
    }

    @Test
    fun givenAnyOtherPhase_whenTheNoticeIsConsidered_thenThereIsNothingToReport() {
        assertThat(hasFailed(CaregiverConfirmationPhase.READY)).isFalse()
        assertThat(hasFailed(CaregiverConfirmationPhase.WORKING)).isFalse()
    }
}
