package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Lessons row for a caregiver who listens rather than looks, who uses the largest text a
 * television offers, or who only has a remote.
 *
 * Measured first: the row was reachable, but a download a caregiver had walked away from changed
 * silently, and at the largest text its longest state took the whole width and squeezed the title
 * into a column of single words, making the row taller than the screen.
 */
@RunWith(AndroidJUnit4::class)
class CaregiverSettingsAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenADownload_whenItsStateChanges_thenItIsAnnouncedWithoutNeedingFocus() {
        show(TaskState.Working(done = 2, total = 5))

        assertThat(liveRegionOf(CaregiverFixtures.NEW_LESSONS)).isEqualTo(LiveRegionMode.Polite)
    }

    @Test
    fun givenASwitch_whenItChanges_thenItIsNotAnnouncedTwice() {
        // A switch changes while it is focused, where its new state is read anyway.
        show(TaskState.UpToDate)

        assertThat(liveRegionOf(CaregiverFixtures.CAPTIONS)).isNull()
    }

    @Test
    fun givenTheLargestText_whenTheRowSaysItsLongestState_thenTheTitleKeepsMostOfTheRow() {
        show(TaskState.Failed(TaskFailure.COULD_NOT_FINISH), fontScale = LARGEST)

        val row = composeTestRule.onNodeWithText(CaregiverFixtures.NEW_LESSONS)
            .fetchSemanticsNode().size
        val consequence = composeTestRule
            .onNodeWithText(LESSONS_CONSEQUENCE, useUnmergedTree = true)
            .fetchSemanticsNode().size

        assertThat(consequence.width.toFloat()).isAtLeast(row.width * TITLE_SHARE)
        assertThat(row.height).isLessThan(screenHeight())
    }

    @Test
    fun givenOnlyARemote_whenWalkingDownTheSettings_thenTheLessonsRowIsReached() {
        show(TaskState.Available)
        composeTestRule.onNodeWithText(CaregiverFixtures.VIETNAMESE_HELP).requestFocus()

        repeat(ROWS_ABOVE_LESSONS) {
            composeTestRule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithText(CaregiverFixtures.NEW_LESSONS).assertIsFocused()
    }

    private fun show(task: TaskState, fontScale: Float = 1f) {
        composeTestRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                HelloBeTheme {
                    Box(
                        Modifier.size(HelloBeLayout.referenceWidth, HelloBeLayout.referenceHeight)
                    ) {
                        CaregiverSettingsScreen(
                            state = CaregiverFixtures.settingsWithLessons(task),
                            onAction = {}
                        )
                    }
                }
            }
        }
    }

    private fun liveRegionOf(title: String): LiveRegionMode? =
        composeTestRule.onNodeWithText(title).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.LiveRegion)

    private fun screenHeight(): Int = composeTestRule.onRoot().fetchSemanticsNode().size.height

    private companion object {
        const val LARGEST = 2f
        const val LESSONS_CONSEQUENCE = "Arrive between lessons, never during one"

        /** The value may take at most 40% of a row, so the title and consequence keep the rest. */
        const val TITLE_SHARE = 0.5f

        /** Six settings sit above Lessons, so six presses down from the first. */
        const val ROWS_ABOVE_LESSONS = 6
    }
}
