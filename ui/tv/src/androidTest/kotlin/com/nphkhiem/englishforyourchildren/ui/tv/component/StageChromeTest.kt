package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StageChromeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenCaptionsAreOn_whenTheCaptionLineChanges_thenTheFocalObjectDoesNotMove() {
        val harness = setCaptionStage()

        val withText = focalBounds()
        composeTestRule.runOnUiThread { harness.text.value = null }
        composeTestRule.waitForIdle()
        val withoutText = focalBounds()

        assertThat(withText.top.value).isWithin(TOLERANCE).of(withoutText.top.value)
        assertThat(withText.bottom.value).isWithin(TOLERANCE).of(withoutText.bottom.value)
        assertThat(heightOf(withText)).isWithin(TOLERANCE).of(heightOf(withoutText))
    }

    @Test
    fun givenCaptionsAreOff_whenTheStageIsMeasured_thenTheCaptionOccupiesNoSpace() {
        val harness = setCaptionStage()

        val captionsOn = focalBounds()
        composeTestRule.runOnUiThread { harness.captionsEnabled.value = false }
        composeTestRule.waitForIdle()
        val captionsOff = focalBounds()

        assertThat(heightOf(captionsOff)).isGreaterThan(heightOf(captionsOn))
    }

    @Test
    fun givenCaptionsAreOn_whenALineWrapsToTwoLines_thenTheFocalObjectStillDoesNotMove() {
        val harness = setCaptionStage()

        val shortLine = focalBounds()
        composeTestRule.runOnUiThread { harness.text.value = LONG_LINE }
        composeTestRule.waitForIdle()
        val longLine = focalBounds()

        // A minimum height would have grown here; a fixed height cannot.
        assertThat(heightOf(longLine)).isWithin(TOLERANCE).of(heightOf(shortLine))
        assertThat(longLine.top.value).isWithin(TOLERANCE).of(shortLine.top.value)
    }

    @Test
    fun givenLargeFontScale_whenALongLineIsCaptioned_thenTheFocalObjectStillDoesNotMove() {
        lateinit var line: MutableState<String?>
        composeTestRule.setContent {
            // The font scale a caregiver can raise is exactly where a minimum height and a fixed
            // height diverge: a minimum grows with the text, a fixed reservation does not.
            CompositionLocalProvider(
                LocalDensity provides Density(density = 2f, fontScale = LARGE_FONT_SCALE)
            ) {
                HelloBeTheme {
                    line = remember { mutableStateOf<String?>(SPOKEN_LINE) }
                    Box(
                        Modifier.size(HelloBeLayout.referenceWidth, HelloBeLayout.referenceHeight)
                    ) {
                        StorybookScaffold(
                            support = {
                                CaptionPanel(
                                    text = line.value,
                                    visible = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        ) {
                            Box(Modifier.fillMaxSize().testTag(FOCAL))
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val shortLine = focalBounds()
        composeTestRule.runOnUiThread { line.value = LONG_LINE }
        composeTestRule.waitForIdle()
        val longLine = focalBounds()

        assertThat(heightOf(longLine)).isWithin(TOLERANCE).of(heightOf(shortLine))
    }

    @Test
    fun givenCaptionsAreOn_whenALineIsSpoken_thenItIsShownAsText() {
        composeTestRule.setContent {
            HelloBeTheme {
                CaptionPanel(text = SPOKEN_LINE, visible = true)
            }
        }

        composeTestRule.onNodeWithText(SPOKEN_LINE).assertIsDisplayed()
    }

    @Test
    fun givenProgressTrail_whenRendered_thenPositionIsAnnouncedWithoutADrawnNumber() {
        composeTestRule.setContent {
            HelloBeTheme {
                ProgressTrail(
                    totalSteps = 4,
                    currentStep = 2,
                    describePosition = ::describePosition,
                    modifier = Modifier.testTag(TRAIL)
                )
            }
        }

        // Announced to assistive technology...
        composeTestRule.onNodeWithContentDescription(POSITION).assertExists()
        // ...but never drawn, for a child who cannot read. This pair is the whole requirement:
        // the same string must be present in semantics and absent from the screen.
        composeTestRule.onAllNodesWithText(POSITION).assertCountEquals(0)
    }

    @Test
    fun givenStepBeyondTheEnd_whenTrailIsRendered_thenTheAnnouncementMatchesWhatIsDrawn() {
        composeTestRule.setContent {
            HelloBeTheme {
                ProgressTrail(
                    totalSteps = 4,
                    currentStep = 99,
                    describePosition = ::describePosition,
                    modifier = Modifier.testTag(TRAIL)
                )
            }
        }

        // Clamped to the last step, and the announcement is derived from the clamped value, so it
        // cannot claim a position the trail is not showing.
        composeTestRule.onNodeWithContentDescription(LAST_POSITION).assertExists()
    }

    @Test
    fun givenNoSteps_whenTrailIsRendered_thenNothingIsDrawnRatherThanCrashing() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(
                    Modifier
                        .size(HelloBeLayout.referenceWidth, HelloBeLayout.trailSegmentHeight)
                        .testTag(TRAIL)
                ) {
                    ProgressTrail(
                        totalSteps = 0,
                        currentStep = 1,
                        describePosition = ::describePosition
                    )
                }
            }
        }

        // Reaching this assertion at all proves no throw; the container is empty, not missing.
        composeTestRule.onNodeWithTag(TRAIL).assertIsDisplayed()
        composeTestRule.onNodeWithText(POSITION).assertDoesNotExist()
    }

    @Test
    fun givenStoryHeader_whenRendered_thenContextTitleAndActionAreAllPresent() {
        composeTestRule.setContent {
            HelloBeTheme {
                StoryHeader(
                    title = TITLE,
                    contextLabel = UNIT,
                    progress = {
                        ProgressTrail(
                            totalSteps = 3,
                            currentStep = 1,
                            describePosition = ::describePosition
                        )
                    },
                    action = { HelloBeAction(label = REPLAY, onClick = {}) }
                )
            }
        }

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(UNIT).assertIsDisplayed()
        composeTestRule.onNodeWithText(REPLAY).assertIsDisplayed()
    }

    private fun describePosition(current: Int, total: Int): String = "Activity $current of $total"

    private fun heightOf(bounds: DpRect): Float = (bounds.bottom - bounds.top).value

    private fun focalBounds(): DpRect =
        composeTestRule.onNodeWithTag(FOCAL).getUnclippedBoundsInRoot()

    private class CaptionHarness(
        val captionsEnabled: MutableState<Boolean>,
        val text: MutableState<String?>
    )

    /** One stage, driven by hoisted state, because a test rule may set content only once. */
    private fun setCaptionStage(): CaptionHarness {
        lateinit var enabled: MutableState<Boolean>
        lateinit var line: MutableState<String?>
        composeTestRule.setContent {
            HelloBeTheme {
                enabled = remember { mutableStateOf(true) }
                line = remember { mutableStateOf<String?>(SPOKEN_LINE) }
                Box(
                    Modifier.size(HelloBeLayout.referenceWidth, HelloBeLayout.referenceHeight)
                ) {
                    StorybookScaffold(
                        support = {
                            CaptionPanel(
                                text = line.value,
                                visible = enabled.value,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    ) {
                        Box(Modifier.fillMaxSize().testTag(FOCAL))
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        return CaptionHarness(enabled, line)
    }

    private companion object {
        const val FOCAL = "focal"
        const val TRAIL = "trail"
        const val SPOKEN_LINE = "Where is the chair?"
        const val POSITION = "Activity 2 of 4"
        const val TITLE = "Listen and choose"
        const val UNIT = "My Home"
        const val REPLAY = "Replay"
        const val TOLERANCE = 0.5f
        const val LAST_POSITION = "Activity 4 of 4"
        const val LARGE_FONT_SCALE = 2f
        const val LONG_LINE =
            "Where is the chair that Pip is looking at right now in this room, can you find it"
    }
}
