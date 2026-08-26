package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListenAndChooseTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // The name is fixed by the task definition in TASKS.md, so it is kept verbatim rather than
    // shortened to fit the column limit.
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun givenListenAndChooseWithThreeAnswers_whenFocusEnters_thenRecommendedAnswerIsNotPreselected() {
        // The correct answer here is the chair. Focus must land on the first answer by position,
        // and nothing may mark the correct one, or choosing stops meaning anything.
        val state = LessonFixtures.answering().let { base ->
            base.copy(answers = base.answers.reversed())
        }
        setActivity(state)

        composeTestRule.onNodeWithText(BED).assertIsFocused()
        composeTestRule.onNodeWithText(CHAIR).assertIsNotFocused()
        state.answers.forEach { answer ->
            assertThat(answer.feedback).isEqualTo(HelloBeChoiceFeedback.NEUTRAL)
        }
    }

    @Test
    fun givenIncorrectAnswer_whenStateUpdates_thenSupportiveRetryKeepsChoicesAvailable() {
        val actions = mutableListOf<LessonAction>()
        setActivity(LessonFixtures.supportiveRetry(SupportLevel.REPEAT), onAction = {
            actions += it
        })

        // Every choice is still there and still choosable: a retry is a second invitation, not a
        // lockout, and the child must be able to pick again without leaving the question.
        listOf(CHAIR, LAMP, BED).forEach { label ->
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
        composeTestRule.onNodeWithText(CHAIR).requestFocusAndSelect()
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LessonAction.AnswerChosen(CHAIR_ID))
    }

    @Test
    fun givenThePromptIsPlaying_whenTheChildTries_thenNoAnswerCanBeReachedOrChosen() {
        val actions = mutableListOf<LessonAction>()
        setActivity(LessonFixtures.prompting(), onAction = { actions += it })

        composeTestRule.onNodeWithText(CHAIR).assertIsNotFocused()
        composeTestRule.onNodeWithText(REPLAY).assertIsFocused()
        composeTestRule.onNodeWithText(REPLAY).performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(CHAIR).assertIsNotFocused()
        assertThat(actions).isEmpty()
    }

    @Test
    fun givenAnAnswerIsChosen_whenSelectIsPressed_thenExactlyOneActionCarriesThatAnswer() {
        val actions = mutableListOf<LessonAction>()
        setActivity(LessonFixtures.answering(), onAction = { actions += it })

        composeTestRule.onNodeWithText(LAMP).requestFocusAndSelect()
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LessonAction.AnswerChosen(LAMP_ID))
    }

    @Test
    fun givenTheAnswerWasCorrect_whenRendered_thenTheQuestionStopsAcceptingFurtherPresses() {
        val actions = mutableListOf<LessonAction>()
        setActivity(LessonFixtures.correct(), onAction = { actions += it })

        composeTestRule.onNodeWithText(LAMP).requestFocusAndSelect()
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenNoAnswersAtAll_whenRendered_thenThePromptStandsAloneRatherThanAnEmptyRow() {
        setActivity(LessonFixtures.answering().copy(answers = emptyList()))

        composeTestRule.onNodeWithText(PROMPT).assertIsDisplayed()
        composeTestRule.onNodeWithText(CHAIR).assertDoesNotExist()
    }

    /** Focus a control the way a child would, then press Select on it. */
    private fun SemanticsNodeInteraction.requestFocusAndSelect() {
        requestFocus()
        performKeyInput { pressKey(Key.DirectionCenter) }
    }

    private fun setActivity(state: LessonUiState, onAction: (LessonAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                ListenAndChooseActivity(state = state, onAction = onAction)
            }
        }
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val CHAIR = "chair"
        const val LAMP = "lamp"
        const val BED = "bed"
        const val CHAIR_ID = "chair"
        const val LAMP_ID = "lamp"
        const val REPLAY = "Replay"
        const val PROMPT = "Where is the chair?"
    }
}
