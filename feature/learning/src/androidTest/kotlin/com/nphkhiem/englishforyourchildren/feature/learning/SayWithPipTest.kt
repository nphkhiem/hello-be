package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SayWithPipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    // The name is fixed by the task definition in TASKS.md, so it is kept verbatim rather than
    // shortened to fit the column limit.
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun givenModelPlaybackCompletes_whenPauseStateAppears_thenNoMicrophonePermissionOrScoreIsShown() {
        // The stop condition of this task is an absence, so it is asserted as one. Nothing may
        // suggest the app heard, judged, kept or uploaded anything the child said.
        setActivity(SayWithPipFixtures.pauseHalfway())

        composeTestRule.onNodeWithText(yourTurn()).assertIsDisplayed()
        FORBIDDEN.forEach { word ->
            composeTestRule.onNode(hasText(word, substring = true, ignoreCase = true))
                .assertDoesNotExist()
        }
    }

    @Test
    fun givenAnyReviewState_whenWalked_thenNoneOfThemCanEverBeCorrect() {
        // A fixture offering a correct state would describe a screen a child must never see,
        // because there is nothing here to be correct about.
        val phases = SayWithPipFixtures.reviewStates().map { (_, state) -> state.phase }

        assertThat(phases).doesNotContain(LessonPhase.CORRECT)
    }

    @Test
    fun givenPipIsStillModelling_whenTheChildTries_thenNeitherControlCanBeReached() {
        val actions = mutableListOf<LessonAction>()
        setActivity(SayWithPipFixtures.modelling(), onAction = { actions += it })

        composeTestRule.onNodeWithText(next()).assertDoesNotExist()
        composeTestRule.onNodeWithText(again()).assertDoesNotExist()
        assertThat(actions).isEmpty()
    }

    @Test
    fun givenThePauseBegins_whenFocusEnters_thenNextHoldsIt() {
        setActivity(SayWithPipFixtures.pauseBeginning())

        composeTestRule.onNodeWithText(next()).assertIsFocused()
    }

    @Test
    fun givenThePauseHasEnded_whenTheStageSettles_thenFocusHasNotMoved() {
        // ADR 0004 refused to move focus the moment audio finishes, because it moves focus while a
        // child is holding the remote. The same refusal applies when the pause runs out.
        setActivity(SayWithPipFixtures.pauseEnding())

        composeTestRule.onNodeWithText(next()).assertIsFocused()
    }

    @Test
    fun givenTheChildWantsAnotherGo_whenAgainIsPressed_thenOneReplayIsRequested() {
        val actions = mutableListOf<LessonAction>()
        setActivity(SayWithPipFixtures.pauseHalfway(), onAction = { actions += it })

        composeTestRule.onNodeWithText(again()).requestFocus()
        composeTestRule.onNodeWithText(again()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LessonAction.ReplayRequested)
    }

    @Test
    fun givenTheChildIsDone_whenNextIsPressed_thenOneContinueIsRequested() {
        val actions = mutableListOf<LessonAction>()
        setActivity(SayWithPipFixtures.pauseHalfway(), onAction = { actions += it })

        composeTestRule.onNodeWithText(next()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LessonAction.ContinueRequested)
    }

    @Test
    fun givenNoTimingIsWiredYet_whenThePauseAppears_thenTheInvitationStillStands() {
        setActivity(SayWithPipFixtures.pauseWithoutTiming())

        composeTestRule.onNodeWithText(yourTurn()).assertIsDisplayed()
        composeTestRule.onNodeWithText(next()).assertIsFocused()
    }

    @Test
    fun givenTheStage_whenItIsRead_thenThePhraseLeadsAndTheInstructionFollows() {
        setActivity(SayWithPipFixtures.pauseHalfway())

        composeTestRule.onNodeWithText(PHRASE).assertIsDisplayed()
        composeTestRule.onNodeWithText(instruction()).assertIsDisplayed()
    }

    private fun setActivity(state: LessonUiState, onAction: (LessonAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                SayWithPipActivity(state = state, onAction = onAction)
            }
        }
    }

    private fun again() = resources.getString(R.string.say_again)

    private fun next() = resources.getString(R.string.say_next)

    private fun yourTurn() = resources.getString(R.string.say_your_turn)

    private fun instruction() = resources.getString(R.string.say_instruction)

    private companion object {
        const val PHRASE = "\"This is a chair.\""

        /** Words that would imply the app listened, judged or kept something. */
        val FORBIDDEN = listOf(
            "microphone",
            "mic",
            "record",
            "listening",
            "score",
            "correct",
            "wrong",
            "try again",
            "permission",
            "upload"
        )
    }
}
