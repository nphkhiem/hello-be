package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class FeedbackAndLoadingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * ADR 0002 says the panel must never repeat the colour a choice card is already showing. That
     * is a claim about pixels, so it is tested by looking at pixels rather than at the source.
     */
    @Test
    fun givenEveryTone_whenPanelIsRendered_thenTheContainerColourNeverChanges() {
        val tone = setFeedbackPanel()

        val correct = containerColourOf(FeedbackTone.CORRECT, tone)
        val retry = containerColourOf(FeedbackTone.SUPPORTIVE_RETRY, tone)
        val information = containerColourOf(FeedbackTone.INFORMATION, tone)

        assertThat(retry).isEqualTo(correct)
        assertThat(information).isEqualTo(correct)

        // Equality alone would also hold for a panel that painted nothing, so pin the colour to
        // the neutral surface and away from the containers a choice card uses.
        val colors = helloBeColors(HelloBeThemeMode.DAY)
        assertThat(correct).isEqualTo(colors.surfaceRaised.toArgb())
        assertThat(correct).isNotEqualTo(colors.successContainer.toArgb())
        assertThat(correct).isNotEqualTo(colors.supportiveRetryContainer.toArgb())
    }

    @Test
    fun givenFeedbackPanel_whenRendered_thenItAnnouncesMessageAndPoseAsOnePoliteRegion() {
        composeTestRule.setContent {
            HelloBeTheme {
                FeedbackPanel(
                    message = RETRY_MESSAGE,
                    tone = FeedbackTone.SUPPORTIVE_RETRY,
                    pipDescription = PIP_MODELLING
                )
            }
        }

        // The pose is what a sighted child reads as tone, so it must reach a screen reader too.
        composeTestRule.onNodeWithText(RETRY_MESSAGE)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
            )
        composeTestRule.onNodeWithContentDescription(PIP_MODELLING, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun givenDecorativePip_whenLoadingIsRendered_thenTheWaitIsAnnouncedExactlyOnce() {
        composeTestRule.setContent {
            HelloBeTheme {
                StoryLoading(contentDescription = LOADING_MESSAGE)
            }
        }

        // Pip carries no description of its own here, so the panel does not say the same thing
        // twice. This is the contract behind PipGuide accepting a null description.
        composeTestRule.onAllNodesWithContentDescription(LOADING_MESSAGE, useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun givenSupportiveRetry_whenPanelIsRendered_thenItAnnouncesTheMessageCalmly() {
        composeTestRule.setContent {
            HelloBeTheme {
                FeedbackPanel(
                    message = RETRY_MESSAGE,
                    tone = FeedbackTone.SUPPORTIVE_RETRY,
                    pipDescription = PIP_MODELLING
                )
            }
        }

        composeTestRule.onNodeWithText(RETRY_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun givenEachTone_whenPanelIsRendered_thenPipChangesPoseRatherThanTheColour() {
        val tone = setFeedbackPanel()

        composeTestRule.runOnUiThread { tone.value = FeedbackTone.CORRECT }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(PipPose.CELEBRATING.name, useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.runOnUiThread { tone.value = FeedbackTone.SUPPORTIVE_RETRY }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(PipPose.MODELING.name, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /** "Completely static" is also a claim about pixels, so the frames are compared directly. */
    @Test
    fun givenLoading_whenTimePasses_thenNothingOnScreenChanges() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelloBeTheme {
                Box(Modifier.size(PANEL_SIZE).testTag(PANEL)) {
                    StoryLoading(contentDescription = LOADING_MESSAGE)
                }
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        val first = composeTestRule.onNodeWithTag(PANEL).captureToImage().pixels()
        composeTestRule.mainClock.advanceTimeBy(HALF_A_SECOND)
        val later = composeTestRule.onNodeWithTag(PANEL).captureToImage().pixels()

        assertThat(later).isEqualTo(first)
    }

    @Test
    fun givenLoading_whenRendered_thenTheWaitIsAnnouncedRatherThanSilent() {
        composeTestRule.setContent {
            HelloBeTheme {
                StoryLoading(contentDescription = LOADING_MESSAGE)
            }
        }

        composeTestRule.onNodeWithContentDescription(LOADING_MESSAGE).assertIsDisplayed()
    }

    private fun setFeedbackPanel(): MutableState<FeedbackTone> {
        lateinit var tone: MutableState<FeedbackTone>
        composeTestRule.setContent {
            HelloBeTheme {
                tone = remember { mutableStateOf(FeedbackTone.INFORMATION) }
                Box(Modifier.size(PANEL_SIZE).testTag(PANEL)) {
                    FeedbackPanel(
                        message = RETRY_MESSAGE,
                        tone = tone.value,
                        pipDescription = PIP_MODELLING,
                        modifier = Modifier.size(PANEL_SIZE),
                        pip = { pose -> Box(Modifier.size(POSE_SIZE).testTag(pose.name)) }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        return tone
    }

    private fun containerColourOf(target: FeedbackTone, tone: MutableState<FeedbackTone>): Int {
        composeTestRule.runOnUiThread { tone.value = target }
        composeTestRule.waitForIdle()
        val image = composeTestRule.onNodeWithTag(PANEL).captureToImage()
        // Sample inside the panel but away from Pip and the text, so this reads the container.
        return image.toPixelMap()[image.width / 2, image.height - SAMPLE_INSET].toArgb()
    }

    private fun ImageBitmap.pixels(): List<Int> {
        val map = toPixelMap()
        return buildList {
            for (y in 0 until map.height step PIXEL_STRIDE) {
                for (x in 0 until map.width step PIXEL_STRIDE) {
                    add(map[x, y].toArgb())
                }
            }
        }
    }

    private companion object {
        const val PANEL = "panel"
        const val RETRY_MESSAGE = "Let us try that one together"
        const val LOADING_MESSAGE = "Getting your adventure ready"
        const val PIP_MODELLING = "Pip is showing you again"
        const val HALF_A_SECOND = 500L
        const val SAMPLE_INSET = 4
        const val PIXEL_STRIDE = 8
        val PANEL_SIZE = 320.dp
        val POSE_SIZE = 48.dp
    }
}
