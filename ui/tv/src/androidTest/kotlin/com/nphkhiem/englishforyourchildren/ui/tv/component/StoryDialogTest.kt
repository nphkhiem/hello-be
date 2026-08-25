package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class StoryDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenStoryDialog_whenOpened_thenSafeActionHasInitialFocus() {
        setDialogStage()

        // A child pressing Select the instant the dialog appears must not leave their lesson.
        composeTestRule.onNodeWithText(KEEP_LEARNING).assertIsFocused()
    }

    @Test
    fun givenStoryDialog_whenOpened_thenPipTitleDescriptionAndBothActionsArePresent() {
        setDialogStage()

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(DESCRIPTION).assertIsDisplayed()
        composeTestRule.onNodeWithText(KEEP_LEARNING).assertIsDisplayed()
        composeTestRule.onNodeWithText(STOP_FOR_NOW).assertIsDisplayed()
        // Pip is announced here rather than decorative: this dialog has no other description of
        // the guide, unlike a feedback panel that speaks for the whole surface.
        composeTestRule.onNodeWithContentDescription(PIP, useUnmergedTree = true).assertExists()
    }

    @Test
    fun givenDialogIsOpen_whenTheChildPressesEveryDirection_thenFocusNeverLeavesTheDialog() {
        setDialogStage()

        // The scrim says the stage behind is unreachable, so D-pad movement must agree. Without
        // containment a child could walk focus onto a control they can barely see and act on it.
        listOf(Key.DirectionLeft, Key.DirectionUp, Key.DirectionDown, Key.DirectionRight)
            .forEach { key ->
                repeat(PRESSES_PER_DIRECTION) {
                    composeTestRule.onNodeWithText(KEEP_LEARNING)
                        .performKeyInput { pressKey(key) }
                }
                composeTestRule.waitForIdle()
                composeTestRule.onNodeWithText(OPENER).assertIsNotFocused()
            }
    }

    @Test
    fun givenStoryDialog_whenOpened_thenTheScrimCoversTheWholeStage() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(
                    Modifier
                        .size(HelloBeLayout.referenceWidth, HelloBeLayout.referenceHeight)
                        .testTag(STAGE)
                ) {
                    StoryDialog(
                        title = TITLE,
                        description = DESCRIPTION,
                        pipDescription = PIP,
                        focusRestorer = rememberHelloBeFocusRestorer(),
                        modifier = Modifier.testTag(DIALOG),
                        safeAction = { modifier ->
                            HelloBeAction(label = KEEP_LEARNING, onClick = {}, modifier = modifier)
                        },
                        secondaryAction = { HelloBeAction(label = STOP_FOR_NOW, onClick = {}) }
                    )
                }
            }
        }

        // The scrim must cover everything behind it, so nothing underneath looks reachable while
        // the dialog is open. A dialog that scrimmed only part of the stage would misrepresent
        // what a child can still interact with.
        val stage = composeTestRule.onNodeWithTag(STAGE).getUnclippedBoundsInRoot()
        val dialog = composeTestRule.onNodeWithTag(DIALOG).getUnclippedBoundsInRoot()

        assertThat(dialog.left.value).isWithin(TOLERANCE).of(stage.left.value)
        assertThat(dialog.top.value).isWithin(TOLERANCE).of(stage.top.value)
        assertThat(dialog.right.value).isWithin(TOLERANCE).of(stage.right.value)
        assertThat(dialog.bottom.value).isWithin(TOLERANCE).of(stage.bottom.value)
    }

    @Test
    fun givenStoryDialog_whenDismissed_thenFocusReturnsToTheOpener() {
        val open = setDialogStage()

        composeTestRule.onNodeWithText(KEEP_LEARNING).assertIsFocused()

        composeTestRule.runOnUiThread { open.value = false }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(OPENER).assertIsFocused()
    }

    @Test
    fun givenOpenerHasGone_whenDialogIsDismissed_thenItIsANoOpRatherThanACrash() {
        lateinit var open: MutableState<Boolean>
        lateinit var openerPresent: MutableState<Boolean>

        composeTestRule.setContent {
            HelloBeTheme {
                open = remember { mutableStateOf(true) }
                openerPresent = remember { mutableStateOf(true) }
                val restorer = rememberHelloBeFocusRestorer()

                Box(Modifier.fillMaxSize()) {
                    if (openerPresent.value) {
                        HelloBeAction(
                            label = OPENER,
                            onClick = {},
                            modifier = Modifier.focusRequester(restorer.returnTarget)
                        )
                    }
                    if (open.value) {
                        StoryDialog(
                            title = TITLE,
                            description = DESCRIPTION,
                            pipDescription = PIP,
                            focusRestorer = restorer,
                            safeAction = { modifier ->
                                HelloBeAction(
                                    label = KEEP_LEARNING,
                                    onClick = {},
                                    modifier = modifier
                                )
                            },
                            secondaryAction = {
                                HelloBeAction(label = STOP_FOR_NOW, onClick = {})
                            }
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // The screen behind the dialog changed while it was open, so there is nothing to return
        // focus to. A child must see the dialog close, not the lesson fall over.
        composeTestRule.runOnUiThread { openerPresent.value = false }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread { open.value = false }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
    }

    private fun setDialogStage(): MutableState<Boolean> {
        lateinit var open: MutableState<Boolean>
        composeTestRule.setContent {
            HelloBeTheme {
                open = remember { mutableStateOf(false) }
                val restorer = rememberHelloBeFocusRestorer()

                Box(Modifier.fillMaxSize()) {
                    HelloBeAction(
                        label = OPENER,
                        onClick = { open.value = true },
                        modifier = Modifier.focusRequester(restorer.returnTarget)
                    )
                    if (open.value) {
                        StoryDialog(
                            title = TITLE,
                            description = DESCRIPTION,
                            pipDescription = PIP,
                            focusRestorer = restorer,
                            safeAction = { modifier ->
                                HelloBeAction(
                                    label = KEEP_LEARNING,
                                    onClick = {},
                                    modifier = modifier
                                )
                            },
                            secondaryAction = {
                                HelloBeAction(label = STOP_FOR_NOW, onClick = {})
                            }
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText(OPENER).requestFocus()
        composeTestRule.runOnUiThread { open.value = true }
        composeTestRule.waitForIdle()
        return open
    }

    private companion object {
        const val TITLE = "Stop for now?"
        const val DESCRIPTION = "We can keep your place and come back later."
        const val KEEP_LEARNING = "Keep learning"
        const val STOP_FOR_NOW = "Stop for now"
        const val OPENER = "Continue adventure"
        const val STAGE = "stage"
        const val DIALOG = "dialog"
        const val TOLERANCE = 0.5f
        const val PIP = "Pip is waiting with you"
        const val PRESSES_PER_DIRECTION = 3
    }
}
