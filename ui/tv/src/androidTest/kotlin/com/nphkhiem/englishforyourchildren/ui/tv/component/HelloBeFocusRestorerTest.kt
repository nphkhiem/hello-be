package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelloBeFocusRestorerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenNamedReturnTarget_whenRestoreIsCalled_thenFocusReturnsToIt() {
        lateinit var restorer: HelloBeFocusRestorer

        composeTestRule.setContent {
            HelloBeTheme {
                restorer = rememberHelloBeFocusRestorer()
                Row {
                    HelloBeAction(
                        label = OPENER,
                        onClick = {},
                        modifier = Modifier.focusRequester(restorer.returnTarget)
                    )
                    HelloBeAction(label = OTHER, onClick = {})
                }
            }
        }

        composeTestRule.onNodeWithText(OTHER).requestFocus()
        composeTestRule.onNodeWithText(OTHER).assertIsFocused()

        composeTestRule.runOnIdle { restorer.restore() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(OPENER).assertIsFocused()
    }

    @Test
    fun givenNothingAttached_whenRestoreIsCalled_thenItIsANoOpRatherThanACrash() {
        lateinit var restorer: HelloBeFocusRestorer

        composeTestRule.setContent {
            HelloBeTheme {
                restorer = rememberHelloBeFocusRestorer()
                HelloBeAction(label = OTHER, onClick = {})
            }
        }

        composeTestRule.onNodeWithText(OTHER).requestFocus()

        // Nothing was ever attached to returnTarget; restoring must not bring the lesson down.
        composeTestRule.runOnIdle { restorer.restore() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(OTHER).assertIsFocused()
    }

    private companion object {
        const val OPENER = "Stop for now"
        const val OTHER = "Keep learning"
    }
}
