package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PictureMatchingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // The name is fixed by the task definition in TASKS.md, so it is kept verbatim rather than
    // shortened to fit the column limit.
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun givenSourceSelected_whenDpadMoves_thenOnlyValidDestinationsRemainFocusable() {
        // The source is presented already committed and is not a choice. No direction from any
        // destination may reach it, or a child would press Select on the answer to their own
        // question and nothing would happen.
        //
        // Stated spatially rather than as "the source is never focused". The source carries no
        // focus semantics at all, so asserting a property on it would pass without proving
        // anything. What is worth proving is that pressing toward it does not move focus, from
        // both ends of the row. Up is deliberately not asserted: leaving the board upward to
        // reach Replay is correct, and listen and choose does the same.
        setActivity(PictureMatchingFixtures.answering())

        composeTestRule.onNodeWithContentDescription(CHAIR).requestFocus()
        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsFocused()

        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(BED).assertIsFocused()

        // And back left along the row, stopping at the first card rather than stepping onto the
        // source that sits beside it.
        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsFocused()

        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsFocused()
    }

    @Test
    fun givenMatchingAppears_whenFocusEnters_thenTheFirstDestinationTakesIt() {
        setActivity(PictureMatchingFixtures.answering())

        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsFocused()
    }

    @Test
    fun givenADestinationIsChosen_whenSelectIsPressed_thenExactlyOneActionCarriesThatId() {
        val actions = mutableListOf<LessonAction>()
        setActivity(PictureMatchingFixtures.answering(), onAction = { actions += it })

        composeTestRule.onNodeWithContentDescription(DOOR).requestFocus()
        composeTestRule.onNodeWithContentDescription(DOOR)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LessonAction.AnswerChosen(DOOR))
    }

    @Test
    fun givenSupportiveRetry_whenStateUpdates_thenEveryDestinationStaysReachable() {
        val actions = mutableListOf<LessonAction>()
        setActivity(
            PictureMatchingFixtures.supportiveRetry(SupportLevel.REPEAT),
            onAction = { actions += it }
        )

        listOf(CHAIR, DOOR, LAMP).forEach { description ->
            composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
        }
        composeTestRule.onNodeWithContentDescription(CHAIR).requestFocus()
        composeTestRule.onNodeWithContentDescription(CHAIR)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LessonAction.AnswerChosen(CHAIR))
    }

    @Test
    fun givenThePromptIsPlaying_whenTheChildTries_thenNoDestinationCanBeReachedOrChosen() {
        val actions = mutableListOf<LessonAction>()
        setActivity(PictureMatchingFixtures.prompting(), onAction = { actions += it })

        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsNotFocused()
        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsNotFocused()
        assertThat(actions).isEmpty()
    }

    @Test
    fun givenFourDestinations_whenTheBoardIsDrawn_thenTheyShareOneRow() {
        setActivity(PictureMatchingFixtures.answering())

        val bounds = listOf(CHAIR, BED, DOOR, LAMP).map {
            composeTestRule.onNodeWithContentDescription(it).getUnclippedBoundsInRoot()
        }

        // One row, left to right. Asserted in pixels because the claim is about where they are,
        // not how many there are.
        bounds.zipWithNext().forEach { (left, right) ->
            assertThat(right.top).isEqualTo(left.top)
            assertThat(right.left.value).isGreaterThan(left.left.value)
        }
    }

    @Test
    fun givenFourDestinations_whenTheBoardIsDrawn_thenNoneIsBelowTheChildMinimum() {
        // The 2x2 grid the draft shows does not fit this shell, and the way it failed was by
        // silently shrinking the cards to roughly half of childChoiceMinHeight. Height is the one
        // dimension that gives way to nothing, so it is asserted rather than assumed.
        setActivity(PictureMatchingFixtures.answering())

        listOf(CHAIR, BED, DOOR, LAMP).forEach { description ->
            val card = composeTestRule.onNodeWithContentDescription(description)
                .getUnclippedBoundsInRoot()

            assertThat((card.bottom - card.top).value)
                .isAtLeast(HelloBeLayout.childChoiceMinHeight.value)
        }
    }

    @Test
    fun givenTwoDestinations_whenTheBoardIsDrawn_thenTheyShareOneRow() {
        setActivity(PictureMatchingFixtures.twoDestinations())

        val first = composeTestRule.onNodeWithContentDescription(CHAIR).getUnclippedBoundsInRoot()
        val second = composeTestRule.onNodeWithContentDescription(BED)
            .getUnclippedBoundsInRoot()

        assertThat(second.top).isEqualTo(first.top)
    }

    @Test
    fun givenTheBoard_whenItIsRead_thenTheSourceShowsItsWordWhileDestinationsDoNot() {
        // "Match the bed" beside a card captioned "bed" would be solvable by reading. The source
        // may say its word because it is the question, not the answer.
        setActivity(PictureMatchingFixtures.answering())

        composeTestRule.onNodeWithText(BED).assertIsDisplayed()
        composeTestRule.onNodeWithText(CHAIR).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsDisplayed()
    }

    @Test
    fun givenNoLearningObject_whenTheBoardIsDrawn_thenTheDestinationsStillStand() {
        // A malformed activity still presents something answerable rather than an empty board.
        // The arrow's absence is not asserted here and deliberately so: it is decorative, its
        // semantics are cleared, and it is drawn inside the same branch as the source, so it
        // cannot outlive it. A test that claimed to check it would be checking nothing.
        setActivity(PictureMatchingFixtures.answering().copy(learningObject = null))

        composeTestRule.onNodeWithText(BED).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsDisplayed()
    }

    private fun setActivity(state: LessonUiState, onAction: (LessonAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                PictureMatchingActivity(state = state, onAction = onAction)
            }
        }
    }

    private companion object {
        // The source and the correct destination are both a bed. Which one a query reaches is
        // decided by how it asks: the source draws its word, the destination only announces it.
        const val BED = PictureMatchingFixtures.BED
        const val CHAIR = PictureMatchingFixtures.CHAIR
        const val DOOR = PictureMatchingFixtures.DOOR
        const val LAMP = PictureMatchingFixtures.LAMP
    }
}
