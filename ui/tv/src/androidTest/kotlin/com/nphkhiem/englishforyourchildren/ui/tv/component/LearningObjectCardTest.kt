package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningObjectCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenACardBesideAChoice_whenDpadMovesToward_thenFocusCannotReachIt() {
        // The learning object is what the lesson is about, not something to choose. If focus could
        // land on it, a child would press Select on the answer to their own question and nothing
        // would happen, which is the silent press ADR 0004 exists to prevent.
        composeTestRule.setContent {
            HelloBeTheme {
                Row(horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)) {
                    LearningObjectCard(
                        label = BED,
                        modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                    )
                    ChoiceCard(
                        label = CHAIR,
                        onClick = {},
                        modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                    )
                }
            }
        }

        composeTestRule.onNodeWithText(CHAIR).requestFocus()
        composeTestRule.onNodeWithText(CHAIR).performKeyInput { pressKey(Key.DirectionLeft) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(CHAIR).assertIsFocused()
    }

    @Test
    fun givenNothingElseOnTheStage_whenItAppears_thenTheCardCarriesNoFocusSemantics() {
        // Asserting that nothing happens to be focused would pass even if the card were focusable,
        // because focus is not requested on its own. The claim worth making is stronger: the card
        // carries no focus semantics at all, so focus search has nothing to find. The tag is
        // needed because the card's surface does not merge its children, so addressing it by its
        // label would reach the inner text instead and assert nothing.
        composeTestRule.setContent {
            HelloBeTheme {
                LearningObjectCard(
                    label = BED,
                    modifier = Modifier
                        .testTag(CARD)
                        .width(HelloBeTheme.layout.cardThreeColumnSet)
                )
            }
        }

        composeTestRule.onNodeWithTag(CARD)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.RequestFocus))
    }

    @Test
    fun givenAHiddenLabel_whenTheCardIsRead_thenTheLabelIsStillAnnounced() {
        setCard(labelVisible = false)

        composeTestRule.onNodeWithText(BED).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(BED).assertExists()
    }

    @Test
    fun givenAVisibleLabel_whenTheCardIsRead_thenItAnnouncesItselfExactlyOnce() {
        // The drawn text is already the accessible name, so adding a content description here
        // would make a screen reader say the word twice.
        setCard(labelVisible = true)

        composeTestRule.onNodeWithText(BED).assertExists()
        composeTestRule.onNodeWithContentDescription(BED).assertDoesNotExist()
    }

    private fun setCard(labelVisible: Boolean) {
        composeTestRule.setContent {
            HelloBeTheme {
                LearningObjectCard(
                    label = BED,
                    labelVisible = labelVisible,
                    modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                )
            }
        }
    }

    private companion object {
        const val BED = "bed"
        const val CHAIR = "chair"
        const val CARD = "learningObjectCard"
    }
}
