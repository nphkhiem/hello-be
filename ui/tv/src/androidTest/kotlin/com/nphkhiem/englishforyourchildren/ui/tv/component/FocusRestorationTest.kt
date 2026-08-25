package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A child moves along a row, drops down to another, then comes back. Coming back must return them
 * to where they were, not to the start of the row: losing their place mid-lesson means hunting for
 * the thing they were about to choose.
 */
@RunWith(AndroidJUnit4::class)
class FocusRestorationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenFocusLeftARow_whenItReturns_thenItLandsOnTheControlItLeftFrom() {
        setTwoRows()

        composeTestRule.onNodeWithText(TOP_FIRST).requestFocus()
        composeTestRule.onNodeWithText(TOP_FIRST).performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TOP_THIRD).performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TOP_THIRD).assertIsFocused()

        composeTestRule.onNodeWithText(TOP_THIRD).performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(BOTTOM_FIRST).performKeyInput { pressKey(Key.DirectionUp) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TOP_THIRD).assertIsFocused()
    }

    @Test
    fun givenFocusLeftTheSecondRow_whenItReturns_thenItAlsoLandsWhereItLeft() {
        setTwoRows()

        composeTestRule.onNodeWithText(BOTTOM_FIRST).requestFocus()
        composeTestRule.onNodeWithText(BOTTOM_FIRST).performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(BOTTOM_SECOND).assertIsFocused()

        composeTestRule.onNodeWithText(BOTTOM_SECOND).performKeyInput { pressKey(Key.DirectionUp) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TOP_FIRST).performKeyInput { pressKey(Key.DirectionDown) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(BOTTOM_SECOND).assertIsFocused()
    }

    private fun setTwoRows() {
        composeTestRule.setContent {
            HelloBeTheme {
                Column(verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)) {
                    Row(
                        modifier = Modifier.helloBeFocusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
                    ) {
                        HelloBeAction(label = TOP_FIRST, onClick = {})
                        HelloBeAction(label = TOP_SECOND, onClick = {})
                        HelloBeAction(label = TOP_THIRD, onClick = {})
                    }
                    Row(
                        modifier = Modifier.helloBeFocusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
                    ) {
                        HelloBeAction(label = BOTTOM_FIRST, onClick = {})
                        HelloBeAction(label = BOTTOM_SECOND, onClick = {})
                    }
                }
            }
        }
    }

    private companion object {
        const val TOP_FIRST = "Continue adventure"
        const val TOP_SECOND = "Free play"
        const val TOP_THIRD = "Delete profile"
        const val BOTTOM_FIRST = "Unit one"
        const val BOTTOM_SECOND = "Unit two"
    }
}
