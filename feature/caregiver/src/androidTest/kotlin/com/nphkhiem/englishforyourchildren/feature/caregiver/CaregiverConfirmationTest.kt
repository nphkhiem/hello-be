package com.nphkhiem.englishforyourchildren.feature.caregiver

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberHelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaregiverConfirmationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenDeleteProfileDialog_whenOpened_thenCancelHasInitialFocus() {
        setConfirmation(CaregiverFixtures.deleteConfirmation())

        composeTestRule.onNodeWithText(keepProfile()).assertIsFocused()
    }

    @Test
    fun givenResetProgressDialog_whenOpened_thenKeepingProgressHasInitialFocus() {
        setConfirmation(CaregiverFixtures.resetConfirmation())

        composeTestRule.onNodeWithText(keepProgress()).assertIsFocused()
    }

    @Test
    fun givenEitherDialog_whenSelectIsPressedRepeatedly_thenNothingIsEverDestroyed() {
        // The stop condition. Focus rests on the safe choice, so pressing without moving keeps
        // the profile however many times it happens.
        val actions = mutableListOf<CaregiverConfirmationAction>()
        setConfirmation(CaregiverFixtures.deleteConfirmation(), onAction = { actions += it })

        repeat(POUNDING) {
            composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
            composeTestRule.waitForIdle()
        }

        assertThat(actions).isNotEmpty()
        assertThat(actions).doesNotContain(CaregiverConfirmationAction.Confirmed)
    }

    @Test
    fun givenTheWorkIsUnderway_whenTheDestructiveChoiceIsPressedAgain_thenItAsksOnlyOnce() {
        // The other half: a caregiver who did move focus and pressed twice on a slow delete.
        val actions = mutableListOf<CaregiverConfirmationAction>()
        setConfirmation(CaregiverFixtures.deleteWorking(), onAction = { actions += it })

        composeTestRule.onNodeWithText(deleteProfile()).requestFocus()
        composeTestRule.onNodeWithText(deleteProfile())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenAReadyDialog_whenTheDestructiveChoiceIsPressed_thenItAsksToDoIt() {
        val actions = mutableListOf<CaregiverConfirmationAction>()
        setConfirmation(CaregiverFixtures.deleteConfirmation(), onAction = { actions += it })

        composeTestRule.onNodeWithText(deleteProfile()).requestFocus()
        composeTestRule.onNodeWithText(deleteProfile())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(CaregiverConfirmationAction.Confirmed)
    }

    @Test
    fun givenTheDeleteDialog_whenItIsRead_thenTheConsequenceIsStatedInFull() {
        // Transcribed from the approved draft, not paraphrased. Softening what deleting a child's
        // profile does is the one place this product must not do it.
        setConfirmation(CaregiverFixtures.deleteConfirmation())

        composeTestRule.onNodeWithText(deleteTitle()).assertIsDisplayed()
        composeTestRule.onNodeWithText(deleteBody()).assertIsDisplayed()
    }

    @Test
    fun givenTheResetDialog_whenItIsRead_thenItSaysWhatStaysAndWhatRestarts() {
        setConfirmation(CaregiverFixtures.resetConfirmation())

        composeTestRule.onNodeWithText(resetTitle()).assertIsDisplayed()
        composeTestRule.onNodeWithText(resetBody()).assertIsDisplayed()
    }

    @Test
    fun givenTheTwoDialogs_whenTheirWordsAreCompared_thenNeitherBorrowsTheOthers() {
        // "Delete remains semantically distinct from Reset". Every piece of copy is chosen by the
        // kind, so a shared phrase would be a defect rather than a convenience.
        setConfirmation(CaregiverFixtures.resetConfirmation())

        composeTestRule.onNodeWithText(deleteTitle()).assertDoesNotExist()
        composeTestRule.onNodeWithText(deleteBody()).assertDoesNotExist()
        composeTestRule.onNodeWithText(deleteProfile()).assertDoesNotExist()
        composeTestRule.onNodeWithText(keepProfile()).assertDoesNotExist()
    }

    @Test
    fun givenTheWorkFailed_whenTheDialogIsRead_thenItSaysNothingChanged() {
        // The transactional guarantee, and the only sentence that lets a caregiver stop worrying.
        setConfirmation(CaregiverFixtures.deleteFailed())

        composeTestRule.onNodeWithText(deleteFailed()).assertIsDisplayed()
    }

    @Test
    fun givenTheWorkFailed_whenTheSecondChoiceIsRead_thenItOffersToTryAgainRatherThanToDelete() {
        // Offering the destructive action again beside a line saying nothing changed would read
        // as though the first press had half worked.
        val actions = mutableListOf<CaregiverConfirmationAction>()
        setConfirmation(CaregiverFixtures.deleteFailed(), onAction = { actions += it })

        composeTestRule.onNodeWithText(deleteProfile()).assertDoesNotExist()
        composeTestRule.onNodeWithText(retry()).requestFocus()
        composeTestRule.onNodeWithText(retry()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(CaregiverConfirmationAction.RetryRequested)
    }

    @Test
    fun givenAReadyDialog_whenBackIsPressed_thenItKeepsTheProfile() {
        val actions = mutableListOf<CaregiverConfirmationAction>()
        setConfirmation(CaregiverFixtures.deleteConfirmation(), onAction = { actions += it })

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(CaregiverConfirmationAction.Dismissed)
    }

    @Test
    fun givenTheWorkIsUnderway_whenBackIsPressed_thenItDoesNotPretendToStopIt() {
        // Dismissing a deletion that is already happening would tell a caregiver they had stopped
        // something they had not.
        val actions = mutableListOf<CaregiverConfirmationAction>()
        setConfirmation(CaregiverFixtures.deleteWorking(), onAction = { actions += it })

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    private fun setConfirmation(
        state: CaregiverConfirmationState,
        onAction: (CaregiverConfirmationAction) -> Unit = {}
    ) {
        composeTestRule.setContent {
            HelloBeTheme {
                CaregiverConfirmation(
                    state = state,
                    focusRestorer = rememberHelloBeFocusRestorer(),
                    onAction = onAction
                )
            }
        }
    }

    private fun deleteTitle() =
        resources.getString(R.string.confirm_delete_title, CaregiverFixtures.PROFILE)

    private fun deleteBody() =
        resources.getString(R.string.confirm_delete_body, CaregiverFixtures.PROFILE)

    private fun deleteFailed() =
        resources.getString(R.string.confirm_delete_failed, CaregiverFixtures.PROFILE)

    private fun resetTitle() =
        resources.getString(R.string.confirm_reset_title, CaregiverFixtures.PROFILE)

    private fun resetBody() =
        resources.getString(R.string.confirm_reset_body, CaregiverFixtures.PROFILE)

    private fun keepProfile() = resources.getString(R.string.confirm_delete_keep)

    private fun keepProgress() = resources.getString(R.string.confirm_reset_keep)

    private fun deleteProfile() = resources.getString(R.string.confirm_delete_do)

    private fun retry() = resources.getString(R.string.confirm_retry)

    private companion object {
        const val POUNDING = 8
    }
}
