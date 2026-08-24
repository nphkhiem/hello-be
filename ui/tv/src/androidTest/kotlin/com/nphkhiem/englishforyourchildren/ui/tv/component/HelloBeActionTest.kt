package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelloBeActionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenChildActionRow_whenDpadMovesRight_thenFocusMovesToTheNextEnabledAction() {
        composeTestRule.setContent {
            HelloBeTheme {
                Row(horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)) {
                    HelloBeAction(label = FIRST, onClick = {})
                    HelloBeAction(
                        label = SKIPPED,
                        onClick = {},
                        availability = HelloBeAvailability.DISABLED
                    )
                    HelloBeAction(label = THIRD, onClick = {})
                }
            }
        }

        composeTestRule.onNodeWithText(FIRST).requestFocus()
        composeTestRule.onNodeWithText(FIRST).assertIsFocused()

        composeTestRule.onNodeWithText(FIRST).performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(THIRD).assertIsFocused()
        composeTestRule.onNodeWithText(SKIPPED).assertIsNotFocused()
    }

    @Test
    fun givenEnabledAction_whenSelectIsPressed_thenClickIsEmittedOnce() {
        var clicks = 0

        composeTestRule.setContent {
            HelloBeTheme {
                HelloBeAction(label = FIRST, onClick = { clicks++ })
            }
        }

        composeTestRule.onNodeWithText(FIRST).requestFocus()
        composeTestRule.onNodeWithText(FIRST).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun givenDisabledAction_whenRendered_thenItIsNotEnabledAndCannotHoldFocus() {
        composeTestRule.setContent {
            HelloBeTheme {
                Row {
                    HelloBeAction(label = FIRST, onClick = {})
                    HelloBeAction(
                        label = SKIPPED,
                        onClick = {},
                        availability = HelloBeAvailability.DISABLED
                    )
                }
            }
        }

        composeTestRule.onNodeWithText(SKIPPED).assertIsNotEnabled()
        composeTestRule.onNodeWithText(SKIPPED).assertIsNotFocused()
    }

    @Test
    fun givenUnavailableAction_whenSelectIsPressed_thenItHoldsFocusButEmitsNoClick() {
        var clicks = 0

        composeTestRule.setContent {
            HelloBeTheme {
                HelloBeAction(
                    label = FIRST,
                    onClick = { clicks++ },
                    availability = HelloBeAvailability.UNAVAILABLE,
                    stateDescription = LATER
                )
            }
        }

        composeTestRule.onNodeWithText(FIRST).requestFocus()
        composeTestRule.onNodeWithText(FIRST).assertIsFocused()
        composeTestRule.onNodeWithText(FIRST).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(clicks).isEqualTo(0)
    }

    @Test
    fun givenIconAction_whenRendered_thenItIsAnnouncedByItsContentDescription() {
        composeTestRule.setContent {
            HelloBeTheme {
                HelloBeIconAction(icon = BlankIcon, contentDescription = REPLAY, onClick = {})
            }
        }

        composeTestRule.onNodeWithContentDescription(REPLAY).assertIsDisplayed()
    }

    private companion object {
        const val FIRST = "Continue"
        const val SKIPPED = "Not yet"
        const val THIRD = "Free play"
        const val LATER = "Later"
        const val REPLAY = "Replay"

        /** A blank vector keeps these tests off the material-icons dependency. */
        val BlankIcon: ImageVector =
            ImageVector.Builder(
                name = "blank",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).build()
    }
}
