package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
class ReviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenMixedRecallItems_whenEachIsRendered_thenBothDrawTheirAnswersWithoutWords() {
        // Mixed recall means some items bring back a learning object and some do not. Both are
        // the same screen with the same row, which is the point.
        setActivity(ReviewFixtures.answering())

        listOf(CHAIR, SOFA, LAMP).forEach { id ->
            composeTestRule.onNodeWithContentDescription(id).assertIsDisplayed()
            composeTestRule.onNodeWithText(id).assertDoesNotExist()
        }

        // And no reminder beside them. Asserted by span rather than by looking for a card that
        // should not exist: with nothing sharing the board the answers fill it, and a reminder
        // taking its 30% share would pull them in well below this.
        val root = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val first = composeTestRule.onNodeWithContentDescription(CHAIR).getUnclippedBoundsInRoot()
        val last = composeTestRule.onNodeWithContentDescription(LAMP).getUnclippedBoundsInRoot()

        assertThat((last.right - first.left).value)
            .isGreaterThan((root.right - root.left).value * ANSWERS_FILL_THE_BOARD)
    }

    @Test
    fun givenALetterRecallItem_whenRendered_thenTheObjectJoinsTheSameRow() {
        setActivity(ReviewFixtures.letterRecall())

        composeTestRule.onNodeWithText(LETTER).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(SOFA).assertIsDisplayed()
    }

    @Test
    fun givenTheChildHadMovedAlongTheAnswers_whenThePromptTypeChanges_thenFocusStaysPut() {
        // The one thing review must not do is move a child's place when the question changes
        // underneath them.
        var letter by mutableStateOf(false)

        composeTestRule.setContent {
            HelloBeTheme {
                val state = if (letter) {
                    ReviewFixtures.letterRecall()
                } else {
                    ReviewFixtures.answering()
                }
                ReviewActivity(state = state, onAction = {})
            }
        }

        composeTestRule.onNodeWithContentDescription(SOFA).requestFocus()
        composeTestRule.onNodeWithContentDescription(SOFA).assertIsFocused()

        letter = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(SOFA).assertIsFocused()
    }

    @Test
    fun givenTheFinalReviewIsOver_whenRendered_thenTheCompletedStateStands() {
        setActivity(ReviewFixtures.completed())

        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsDisplayed()
    }

    @Test
    fun givenTheRoom_whenItIsDrawn_thenNothingAboutItCanBeReachedOrChosen() {
        // Scenery is decoration. It carries no meaning, so it carries no semantics and no focus,
        // and a child can never choose it. Exercised directly rather than through a test hook in
        // the activity: production code should not know it is being looked at.
        composeTestRule.setContent {
            HelloBeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag(STAGE)) { ReviewRoom() }
            }
        }

        composeTestRule.onNodeWithTag(STAGE).assertIsDisplayed()

        // Not "no semantics at all": drawing with a shape publishes a Shape property, so that
        // claim was never true. What matters is that nothing here can be reached, pressed or
        // announced.
        composeTestRule.onNodeWithTag(STAGE).onChildren().onFirst()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.RequestFocus))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun givenTheRoom_whenTheAnswersAreDrawn_thenTheHorizonDoesNotCutAcrossThem() {
        // Scenery sits behind the answers, so its top edge has to clear them. A horizon crossing
        // the middle of the cards reads as a rendering fault rather than as a room.
        composeTestRule.setContent {
            HelloBeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag(STAGE)) {
                    ReviewActivity(state = ReviewFixtures.answering(), onAction = {})
                }
            }
        }

        val room = composeTestRule.onNodeWithTag(STAGE).onChildren().onFirst()
            .getUnclippedBoundsInRoot()
        val answer = composeTestRule.onNodeWithContentDescription(CHAIR)
            .getUnclippedBoundsInRoot()

        assertThat(room.top.value).isLessThan(answer.top.value)
        assertThat(room.bottom.value).isAtLeast(answer.bottom.value)
    }

    @Test
    fun givenThreeAnswers_whenTheRowIsDrawn_thenNoneIsBelowTheChildMinimum() {
        setActivity(ReviewFixtures.answering())

        listOf(CHAIR, SOFA, LAMP).forEach { id ->
            val card = composeTestRule.onNodeWithContentDescription(id).getUnclippedBoundsInRoot()

            assertThat((card.bottom - card.top).value)
                .isAtLeast(HelloBeLayout.childChoiceMinHeight.value)
        }
    }

    @Test
    fun givenAnAnswerIsChosen_whenSelectIsPressed_thenExactlyOneActionCarriesThatId() {
        val actions = mutableListOf<LessonAction>()
        setActivity(ReviewFixtures.answering(), onAction = { actions += it })

        composeTestRule.onNodeWithContentDescription(SOFA).requestFocus()
        composeTestRule.onNodeWithContentDescription(SOFA)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LessonAction.AnswerChosen(SOFA))
    }

    private fun setActivity(state: LessonUiState, onAction: (LessonAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                ReviewActivity(state = state, onAction = onAction)
            }
        }
    }

    private companion object {
        const val CHAIR = ReviewFixtures.CHAIR
        const val SOFA = ReviewFixtures.SOFA
        const val LAMP = ReviewFixtures.LAMP
        const val LETTER = ReviewFixtures.LETTER
        const val STAGE = "stage"

        /** Three answers alone span most of the stage; a reminder beside them cannot. */
        const val ANSWERS_FILL_THE_BOARD = 0.75f
    }
}
