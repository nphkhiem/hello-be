package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private companion object {
        const val PIP_GREETING = "Pip is waving hello"
        const val PIP_POINTING = "Pip is pointing at the picture"
        const val PIP_CHANGING = "Pip is changing pose"
        val RESTING = PipPose.RESTING.name
        val CELEBRATING = PipPose.CELEBRATING.name
        val POSE_SIZE = 48.dp
        val SWITCHER_SIZE = 120.dp
    }
}
