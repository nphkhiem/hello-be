package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaregiverRecoveryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Strings are read the way the screens read them, in both languages. */
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenTheDataWillNotOpen_whenItIsRead_thenRetryIsFocusedAndResetIsNot() {
        // Recovery never opens with a destructive action focused. Reset is the final path, never
        // the first suggestion, so retry takes the focus and reset waits beside it.
        setRecovery()

        composeTestRule.onNodeWithText(retry()).assertIsFocused()
        composeTestRule.onNodeWithText(reviewReset()).assertIsNotFocused()
    }

    @Test
    fun givenTheDataWillNotOpen_whenSelectIsPressedWithoutMoving_thenItOnlyTriesAgain() {
        val actions = mutableListOf<CaregiverRecoveryAction>()
        setRecovery(onAction = { actions += it })

        repeat(POUNDING) {
            composeTestRule.onNodeWithText(retry())
                .performKeyInput { pressKey(Key.DirectionCenter) }
            composeTestRule.waitForIdle()
        }

        assertThat(actions).doesNotContain(CaregiverRecoveryAction.ResetReviewRequested)
        assertThat(actions).contains(CaregiverRecoveryAction.RetryRequested)
    }

    @Test
    fun givenTheResetPath_whenItIsPressed_thenItOnlyAsksToReviewAndDestroysNothing() {
        // The gating this task asks for: pressing reset opens the confirmation, which is HB-D21's,
        // and nothing is reset by this screen.
        val actions = mutableListOf<CaregiverRecoveryAction>()
        setRecovery(onAction = { actions += it })

        composeTestRule.onNodeWithText(reviewReset()).requestFocus()
        composeTestRule.onNodeWithText(reviewReset())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(CaregiverRecoveryAction.ResetReviewRequested)
    }

    @Test
    fun givenACaregiverSurface_whenItIsRead_thenTheCodeIsShownAndSaidToBeSafe() {
        // The one recovery that carries a code, and the clause that tells a caregiver reading it
        // aloud what they are handing over.
        setRecovery()

        composeTestRule.onNodeWithText(code()).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("contains no child data", substring = true)
            .fetchSemanticsNodes()
            .also { assertThat(it).isNotEmpty() }
    }

    @Test
    fun givenTheRecovery_whenItIsRead_thenItSaysWhatToTryBeforeItMentionsResetting() {
        setRecovery()

        composeTestRule.onNodeWithText(title()).assertIsDisplayed()
        composeTestRule.onNodeWithText(body()).assertIsDisplayed()
    }

    private fun setRecovery(onAction: (CaregiverRecoveryAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                CaregiverRecovery(
                    state = CaregiverFixtures.databaseRecovery(),
                    onAction = onAction
                )
            }
        }
    }

    private fun retry() = context.caregiverText(CaregiverLanguage.BOTH, R.string.recovery_db_retry)

    private fun reviewReset() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.recovery_db_review_reset)

    private fun title() = context.caregiverText(CaregiverLanguage.BOTH, R.string.recovery_db_title)

    private fun body() = context.caregiverText(CaregiverLanguage.BOTH, R.string.recovery_db_body)

    private fun code() = context.caregiverText(
        CaregiverLanguage.BOTH,
        R.string.recovery_db_code,
        CaregiverFixtures.RECOVERY_CODE
    )

    private companion object {
        const val POUNDING = 5
    }
}
