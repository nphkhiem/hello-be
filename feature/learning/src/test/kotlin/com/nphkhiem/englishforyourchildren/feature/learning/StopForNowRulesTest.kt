package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class StopForNowRulesTest {

    @Test
    fun givenProgressIsSaved_whenTheDescriptionIsChosen_thenItPromisesPipRemembers() {
        assertThat(stopForNowDescription(pendingSave = false))
            .isEqualTo(R.string.stop_for_now_saved)
    }

    @Test
    fun givenProgressIsNotSavedYet_whenTheDescriptionIsChosen_thenItDoesNotClaimItIs() {
        // The pending copy is the whole reason this rule exists. Telling a child Pip will remember
        // when it might not is the one thing this dialog must never do.
        assertThat(stopForNowDescription(pendingSave = true))
            .isEqualTo(R.string.stop_for_now_pending)
    }
}
