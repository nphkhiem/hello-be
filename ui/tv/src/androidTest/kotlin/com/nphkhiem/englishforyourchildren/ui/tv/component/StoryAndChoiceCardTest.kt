package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoryAndChoiceCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenSelectedStoryCard_whenFocusMovesAway_thenItStaysSelected() {
        composeTestRule.setContent {
            HelloBeTheme {
                Row(horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)) {
                    StoryCard(
                        title = CURRENT,
                        onClick = {},
                        selected = true,
                        modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                    )
                    StoryCard(
                        title = NEXT,
                        onClick = {},
                        modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                    )
                }
            }
        }

        composeTestRule.onNodeWithText(CURRENT).requestFocus()
        composeTestRule.onNodeWithText(CURRENT).assertIsSelected()

        composeTestRule.onNodeWithText(CURRENT).performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(NEXT).assertIsFocused()
        composeTestRule.onNodeWithText(CURRENT).assertIsSelected()
        composeTestRule.onNodeWithText(NEXT).assertIsNotSelected()
    }

    @Test
    fun givenChoiceCardRow_whenDpadMovesRight_thenFocusFollowsVisualOrder() {
        composeTestRule.setContent {
            HelloBeTheme {
                Row(horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)) {
                    ChoiceCard(
                        label = APPLE,
                        onClick = {},
                        modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                    )
                    ChoiceCard(
                        label = BALL,
                        onClick = {},
                        modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                    )
                }
            }
        }

        composeTestRule.onNodeWithText(APPLE).requestFocus()
        composeTestRule.onNodeWithText(APPLE).performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(BALL).assertIsFocused()
    }

    @Test
    fun givenSupportiveRetryChoice_whenRendered_thenItRemainsSelectableAndAnnouncesItsState() {
        var clicks = 0

        composeTestRule.setContent {
            HelloBeTheme {
                ChoiceCard(
                    label = APPLE,
                    onClick = { clicks++ },
                    feedback = HelloBeChoiceFeedback.SUPPORTIVE_RETRY,
                    stateDescription = TRY_AGAIN
                )
            }
        }

        composeTestRule.onNodeWithText(APPLE).requestFocus()
        composeTestRule.onNodeWithText(APPLE).assertIsFocused()
        composeTestRule.onNodeWithText(APPLE).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun givenUnavailableChoice_whenSelectIsPressed_thenNoClickIsEmitted() {
        var clicks = 0

        composeTestRule.setContent {
            HelloBeTheme {
                ChoiceCard(
                    label = APPLE,
                    onClick = { clicks++ },
                    availability = HelloBeAvailability.UNAVAILABLE,
                    stateDescription = LATER
                )
            }
        }

        composeTestRule.onNodeWithText(APPLE).requestFocus()
        composeTestRule.onNodeWithText(APPLE).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(clicks).isEqualTo(0)
    }

    private companion object {
        const val CURRENT = "Unit one"
        const val NEXT = "Unit two"
        const val APPLE = "Apple"
        const val BALL = "Ball"
        const val TRY_AGAIN = "Try again"
        const val LATER = "Later"
    }
}
