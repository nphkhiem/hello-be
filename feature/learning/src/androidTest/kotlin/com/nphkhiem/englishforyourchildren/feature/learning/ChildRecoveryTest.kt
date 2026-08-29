package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
class ChildRecoveryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenAnInterruptingRecovery_whenItOpens_thenTheSafeActionHoldsFocus() {
        // The first of the approved recovery rules. One test per presentation rather than a loop,
        // because a compose rule accepts setContent exactly once.
        setRecovery(ChildRecoveryReason.PROGRESS_PENDING)

        composeTestRule.onNodeWithText(keepLearning()).assertIsFocused()
    }

    @Test
    fun givenAReplacingRecovery_whenItOpens_thenItsOnlyActionHoldsFocus() {
        setRecovery(ChildRecoveryReason.EMPTY_LIBRARY)

        composeTestRule.onNodeWithText(findAdventure()).assertIsFocused()
    }

    @Test
    fun givenAudioIsUnavailable_whenItIsRead_thenItOffersRetryAndAFairDemonstration() {
        val actions = mutableListOf<ChildRecoveryAction>()
        setRecovery(ChildRecoveryReason.AUDIO_UNAVAILABLE, onAction = { actions += it })

        composeTestRule.onNodeWithText(audioRetry()).assertIsFocused()
        composeTestRule.onNodeWithText(audioRetry())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.onNodeWithText(showMe()).requestFocus()
        composeTestRule.onNodeWithText(showMe()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(
            ChildRecoveryAction.AudioRetryRequested,
            ChildRecoveryAction.DemonstrationRequested
        ).inOrder()
    }

    @Test
    fun givenProgressIsPending_whenItIsRead_thenNothingClaimsItWasSaved() {
        // The truthfulness rule. The child is told this part may not be remembered, and no
        // reassuring word appears anywhere near it.
        setRecovery(ChildRecoveryReason.PROGRESS_PENDING)

        composeTestRule.onNodeWithText(pendingBody()).assertIsDisplayed()
        listOf("saved", "Saved", "remembered it", "fixed").forEach { claim ->
            composeTestRule.onAllNodesWithText(claim, substring = true).fetchSemanticsNodes()
                .also { assertThat(it).isEmpty() }
        }
    }

    @Test
    fun givenProgressIsPending_whenTheAlternativeIsPressed_thenItFetchesAGrownUp() {
        val actions = mutableListOf<ChildRecoveryAction>()
        setRecovery(ChildRecoveryReason.PROGRESS_PENDING, onAction = { actions += it })

        composeTestRule.onNodeWithText(askGrownUp()).requestFocus()
        composeTestRule.onNodeWithText(askGrownUp())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ChildRecoveryAction.CaregiverHelpRequested)
    }

    @Test
    fun givenALessonThatCannotOpen_whenItIsRead_thenOneActionLeadsSomewhereThatExists() {
        // No dangling navigation: the only action goes to the path, which always has a lesson.
        val actions = mutableListOf<ChildRecoveryAction>()
        setRecovery(ChildRecoveryReason.LESSON_UNAVAILABLE, onAction = { actions += it })

        composeTestRule.onNodeWithText(lessonTitle()).assertIsDisplayed()
        composeTestRule.onNodeWithText(backToPath())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ChildRecoveryAction.LearningPathRequested)
    }

    @Test
    fun givenAnEmptyLibrary_whenItIsRead_thenItExplainsWhyAndDoesNotReadAsAFault() {
        setRecovery(ChildRecoveryReason.EMPTY_LIBRARY)

        composeTestRule.onNodeWithText(emptyTitle()).assertIsDisplayed()
        composeTestRule.onNodeWithText(emptyBody()).assertIsDisplayed()
        listOf("error", "Error", "sorry", "Sorry", "failed", "problem").forEach { blame ->
            composeTestRule.onAllNodesWithText(blame, substring = true).fetchSemanticsNodes()
                .also { assertThat(it).isEmpty() }
        }
    }

    @Test
    fun givenAChildRecovery_whenItIsRead_thenNoDiagnosticCodeIsAnywhereOnIt() {
        // Technical detail belongs behind the adult gate. The child's model has no field that
        // could carry one, and this asserts the consequence on the variant that would be most
        // tempted by it: the one where something genuinely failed to load.
        setRecovery(ChildRecoveryReason.LESSON_UNAVAILABLE)

        listOf("code", "Code", "DB-", "Recovery code").forEach { technical ->
            composeTestRule.onAllNodesWithText(technical, substring = true)
                .fetchSemanticsNodes()
                .also { assertThat(it).isEmpty() }
        }
    }

    private fun setRecovery(
        reason: ChildRecoveryReason,
        onAction: (ChildRecoveryAction) -> Unit = {}
    ) {
        composeTestRule.setContent {
            HelloBeTheme {
                ChildRecovery(
                    reason = reason,
                    focusRestorer = rememberHelloBeFocusRestorer(),
                    onAction = onAction
                )
            }
        }
    }

    private fun audioRetry() = resources.getString(R.string.recovery_audio_retry)

    private fun showMe() = resources.getString(R.string.recovery_audio_show)

    private fun keepLearning() = resources.getString(R.string.recovery_pending_keep)

    private fun askGrownUp() = resources.getString(R.string.recovery_pending_grownup)

    private fun pendingBody() = resources.getString(R.string.recovery_pending_body)

    private fun lessonTitle() = resources.getString(R.string.recovery_lesson_title)

    private fun backToPath() = resources.getString(R.string.recovery_lesson_action)

    private fun emptyTitle() = resources.getString(R.string.recovery_empty_title)

    private fun emptyBody() = resources.getString(R.string.recovery_empty_body)

    private fun findAdventure() = resources.getString(R.string.recovery_empty_action)
}
