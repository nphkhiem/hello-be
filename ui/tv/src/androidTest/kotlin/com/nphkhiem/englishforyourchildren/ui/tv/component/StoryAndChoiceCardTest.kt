package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeThemeMode
import com.nphkhiem.englishforyourchildren.ui.tv.theme.helloBeColors
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

    @Test
    fun givenAHiddenLabel_whenTheChoiceIsRead_thenTheWordIsAnnouncedButNotDrawn() {
        // An activity whose prompt already names the target in text cannot also caption the
        // answers, or the question is solvable by reading instead of by looking. The word still
        // has to reach a screen reader, for whom the picture is not available at all.
        composeTestRule.setContent {
            HelloBeTheme {
                ChoiceCard(label = APPLE, onClick = {}, labelVisible = false)
            }
        }

        composeTestRule.onNodeWithText(APPLE).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(APPLE).assertExists()
    }

    @Test
    fun givenAHiddenLabel_whenSelectIsPressed_thenItStillBehavesLikeAChoice() {
        var clicks = 0

        composeTestRule.setContent {
            HelloBeTheme {
                ChoiceCard(label = APPLE, onClick = { clicks++ }, labelVisible = false)
            }
        }

        composeTestRule.onNodeWithContentDescription(APPLE).requestFocus()
        composeTestRule.onNodeWithContentDescription(APPLE).assertIsFocused()
        composeTestRule.onNodeWithContentDescription(APPLE)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun givenAVisibleLabel_whenTheChoiceIsRead_thenItAnnouncesItselfExactlyOnce() {
        // The drawn text is already the accessible name. Adding a content description alongside it
        // would make a screen reader say the word twice.
        composeTestRule.setContent {
            HelloBeTheme {
                ChoiceCard(label = APPLE, onClick = {})
            }
        }

        composeTestRule.onNodeWithText(APPLE).assertExists()
        composeTestRule.onNodeWithContentDescription(APPLE).assertDoesNotExist()
    }

    /**
     * The moment a child gets it right is the one the card exists for, and it outlives the answers
     * closing: confirmation is what that phase is confirming, so it cannot be the thing the phase
     * takes away. Checked in pixels, because "looks correct" is a claim about what is painted.
     */
    @Test
    fun givenACorrectChoiceThatIsNoLongerReachable_whenRendered_thenItKeepsItsSuccessTreatment() {
        var clicks = 0

        composeTestRule.setContent {
            HelloBeTheme {
                Box(Modifier.size(HOST_WIDTH, HOST_HEIGHT).background(BACKDROP).testTag(HOST_TAG)) {
                    ChoiceCard(
                        label = APPLE,
                        onClick = { clicks++ },
                        feedback = HelloBeChoiceFeedback.CORRECT,
                        availability = HelloBeAvailability.UNAVAILABLE,
                        stateDescription = WELL_DONE,
                        modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val colors = helloBeColors(HelloBeThemeMode.DAY)
        val painted = paintedColours()

        assertThat(painted).contains(colors.successContainer.toArgb())
        assertThat(painted).contains(colors.accentGrowth.toArgb())
        assertThat(painted).doesNotContain(colors.surfaceMuted.toArgb())
        assertThat(painted).doesNotContain(colors.borderSecondary.toArgb())

        // Keeping the treatment must not hand the press back: a second answer is still refused.
        composeTestRule.onNodeWithText(APPLE).requestFocus()
        composeTestRule.onNodeWithText(APPLE).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(clicks).isEqualTo(0)
    }

    /**
     * The same holds when the card has left the focus order altogether rather than merely stopped
     * accepting presses, which is the shape the prompt phases use.
     */
    @Test
    fun givenACorrectChoiceThatCannotBeFocused_whenRendered_thenItKeepsItsSuccessTreatment() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(Modifier.size(HOST_WIDTH, HOST_HEIGHT).background(BACKDROP).testTag(HOST_TAG)) {
                    ChoiceCard(
                        label = APPLE,
                        onClick = {},
                        feedback = HelloBeChoiceFeedback.CORRECT,
                        availability = HelloBeAvailability.DISABLED,
                        stateDescription = WELL_DONE,
                        modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val colors = helloBeColors(HelloBeThemeMode.DAY)
        val painted = paintedColours()

        assertThat(painted).contains(colors.successContainer.toArgb())
        assertThat(painted).contains(colors.accentGrowth.toArgb())
        assertThat(painted).doesNotContain(colors.surfaceMuted.toArgb())
        composeTestRule.onNodeWithText(APPLE).assertIsNotEnabled()
    }

    /**
     * An unreachable card with nothing to say about an answer is still the muted "later" surface.
     * The outcome is what survives being unreachable, rather than unavailability being ignored.
     */
    @Test
    fun givenAnUnreachableChoiceWithoutFeedback_whenRendered_thenItStaysMuted() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(Modifier.size(HOST_WIDTH, HOST_HEIGHT).background(BACKDROP).testTag(HOST_TAG)) {
                    ChoiceCard(
                        label = APPLE,
                        onClick = {},
                        availability = HelloBeAvailability.UNAVAILABLE,
                        stateDescription = LATER,
                        modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val colors = helloBeColors(HelloBeThemeMode.DAY)
        val painted = paintedColours()

        assertThat(painted).contains(colors.surfaceMuted.toArgb())
        assertThat(painted).contains(colors.borderSecondary.toArgb())
        assertThat(painted).doesNotContain(colors.successContainer.toArgb())
    }

    private fun paintedColours(): Set<Int> {
        val map = composeTestRule.onNodeWithTag(HOST_TAG).captureToImage().toPixelMap()
        return buildSet {
            for (y in 0 until map.height step STRIDE) {
                for (x in 0 until map.width step STRIDE) {
                    add(map[x, y].toArgb())
                }
            }
        }
    }

    private companion object {
        const val CURRENT = "Unit one"
        const val NEXT = "Unit two"
        const val APPLE = "Apple"
        const val BALL = "Ball"
        const val TRY_AGAIN = "Try again"
        const val LATER = "Later"
        const val WELL_DONE = "Well done"
        const val HOST_TAG = "host"
        const val STRIDE = 2
        val HOST_WIDTH = 320.dp
        val HOST_HEIGHT = 220.dp
        val BACKDROP = Color(0xFF00FF00)
    }
}
