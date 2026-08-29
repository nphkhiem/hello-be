package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.Text
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PipGuideTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenPipGuide_whenRendered_thenItIsAnnouncedByItsContentDescription() {
        composeTestRule.setContent {
            HelloBeTheme {
                PipGuide(pose = PipPose.GREETING, contentDescription = PIP_GREETING)
            }
        }

        composeTestRule.onNodeWithContentDescription(PIP_GREETING).assertIsDisplayed()
    }

    // Pip merges its descendants so assistive technology announces one element rather than a
    // tree. These assertions look under that merge on purpose, to inspect the illustration slot.
    @Test
    fun givenCustomIllustration_whenSupplied_thenItReplacesThePlaceholder() {
        composeTestRule.setContent {
            HelloBeTheme {
                PipGuide(
                    pose = PipPose.POINTING,
                    contentDescription = PIP_POINTING,
                    illustration = { pose -> Box(Modifier.size(POSE_SIZE).testTag(pose.name)) }
                )
            }
        }

        composeTestRule.onNodeWithTag(
            PipPose.POINTING.name,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun givenReducedMotion_whenPipPoseChanges_thenThePoseSwapsWithoutAnimating() {
        val pose = setPoseSwitcher(reduceMotion = true)

        changePose(pose, PipPose.CELEBRATING)

        // A swap composes only the new pose; a crossfade would still be holding both.
        composeTestRule.onNodeWithTag(CELEBRATING, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(RESTING, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun givenStandardMotion_whenPipPoseChanges_thenBothPosesAreBrieflyPresent() {
        val pose = setPoseSwitcher(reduceMotion = false)

        changePose(pose, PipPose.CELEBRATING)

        composeTestRule.onNodeWithTag(RESTING, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CELEBRATING, useUnmergedTree = true).assertIsDisplayed()
    }

    /** Hoists the pose so the test drives it, rather than writing state during composition. */
    private fun setPoseSwitcher(reduceMotion: Boolean): MutableState<PipPose> {
        lateinit var pose: MutableState<PipPose>
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            HelloBeTheme(reduceMotion = reduceMotion) {
                pose = remember { mutableStateOf(PipPose.RESTING) }
                Box(Modifier.size(SWITCHER_SIZE)) {
                    PipGuide(
                        pose = pose.value,
                        contentDescription = PIP_CHANGING,
                        illustration = { current ->
                            Box(Modifier.size(POSE_SIZE).testTag(current.name))
                        }
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()
        return pose
    }

    private fun changePose(pose: MutableState<PipPose>, target: PipPose) {
        composeTestRule.runOnUiThread { pose.value = target }
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun givenNoSizeFromTheCaller_whenPipIsDrawn_thenItHonoursItsOwnMinimum() {
        // PipGuide declares a minimum through defaultMinSize, which reads as a promise that it has
        // a sensible size on its own. Every caller in this app passes an explicit size, so the
        // promise had never been tested.
        composeTestRule.setContent {
            HelloBeTheme {
                PipGuide(pose = PipPose.GREETING, contentDescription = UNSIZED_PIP)
            }
        }

        val pip = composeTestRule.onNodeWithContentDescription(UNSIZED_PIP).fetchSemanticsNode()
        val minimum = with(composeTestRule.density) { HelloBeLayout.pipMinSize.toPx() }

        assertThat(pip.size.height.toFloat()).isAtLeast(minimum)
        assertThat(pip.size.width.toFloat()).isAtLeast(minimum)
    }

    @Test
    fun givenNoSizeFromTheCaller_whenPipSitsBesideOtherContent_thenThatContentKeepsItsHeight() {
        // The failure as it actually appeared, in HB-D22's recovery panel: not Pip measuring
        // small, but everything around it losing its height. The placeholder draws on a canvas
        // that fills what it is given, and given a column's whole height it took the lot.
        composeTestRule.setContent {
            HelloBeTheme {
                Column(verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)) {
                    PipGuide(pose = PipPose.GREETING, contentDescription = UNSIZED_PIP)
                    Text(text = NEIGHBOUR, style = HelloBeTheme.typography.bodyLarge)
                }
            }
        }

        val neighbour = composeTestRule.onNodeWithText(NEIGHBOUR).fetchSemanticsNode()

        assertThat(neighbour.size.height).isGreaterThan(0)
    }

    private companion object {
        const val PIP_GREETING = "Pip is waving hello"
        const val PIP_POINTING = "Pip is pointing at the picture"
        const val PIP_CHANGING = "Pip is changing pose"
        val RESTING = PipPose.RESTING.name
        val CELEBRATING = PipPose.CELEBRATING.name
        val POSE_SIZE = 48.dp
        val SWITCHER_SIZE = 120.dp
        const val UNSIZED_PIP = "Pip with no size"
        const val NEIGHBOUR = "Still here"
    }
}
