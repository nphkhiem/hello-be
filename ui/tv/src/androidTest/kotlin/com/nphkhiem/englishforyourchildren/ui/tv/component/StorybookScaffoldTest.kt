package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
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
class StorybookScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * DESIGN_TOKENS.md defines tv720, tv1080 and tv4k as the same dp layout at different raster
     * densities, so varying the canvas in dp would prove nothing about resolution. What actually
     * differs between the three profiles is density, so that is what these cases vary.
     */
    @Test
    fun given720pDensity_whenSlotsAreMeasured_thenTheyStayInsideTheSafeArea() {
        assertSlotsAreInsideSafeArea(density = DENSITY_720P)
    }

    @Test
    fun given1080pDensity_whenSlotsAreMeasured_thenTheyStayInsideTheSafeArea() {
        assertSlotsAreInsideSafeArea(density = DENSITY_1080P)
    }

    @Test
    fun given4kDensity_whenSlotsAreMeasured_thenTheyStayInsideTheSafeArea() {
        assertSlotsAreInsideSafeArea(density = DENSITY_4K)
    }

    @Test
    fun givenCrampedCanvas_whenSlotsAreMeasured_thenTheyStillStayInsideTheSafeArea() {
        assertSlotsAreInsideSafeArea(
            density = DENSITY_1080P,
            canvasWidth = CRAMPED_WIDTH,
            canvasHeight = CRAMPED_HEIGHT
        )
    }

    @Test
    fun givenScenery_whenMeasured_thenOnlySceneryReachesEveryCanvasEdge() {
        setStage(density = DENSITY_1080P)

        val canvas = boundsOf(CANVAS)
        val scenery = boundsOf(SCENERY)

        assertThat(scenery.left.value).isWithin(TOLERANCE).of(canvas.left.value)
        assertThat(scenery.right.value).isWithin(TOLERANCE).of(canvas.right.value)
        assertThat(scenery.top.value).isWithin(TOLERANCE).of(canvas.top.value)
        assertThat(scenery.bottom.value).isWithin(TOLERANCE).of(canvas.bottom.value)
    }

    @Test
    fun givenStageAppears_whenEntryFocusIsDeclared_thenItLandsOnTheNamedAction() {
        composeTestRule.setContent {
            HelloBeTheme {
                val entry = remember { FocusRequester() }
                StorybookScaffold(entryFocus = entry) {
                    Row {
                        HelloBeAction(label = OTHER, onClick = {})
                        HelloBeAction(
                            label = SAFE,
                            onClick = {},
                            modifier = Modifier.focusRequester(entry)
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText(SAFE).assertIsFocused()
    }

    /**
     * Lint's RememberInComposition rule is suppressed on purpose here: building the
     * FocusRequester without remember IS the caller mistake under test. The scaffold promises
     * entry focus is claimed once even then, and this test fails if that promise is broken.
     */
    @Suppress("RememberInComposition")
    @Test
    fun givenChildMovedFocus_whenStageStateChanges_thenEntryFocusIsNotReclaimed() {
        composeTestRule.setContent {
            HelloBeTheme {
                var attempts by remember { mutableIntStateOf(0) }
                // Deliberately NOT remembered, reproducing the caller mistake the KDoc promises
                // to tolerate: a requester rebuilt on every recomposition must not cause entry
                // focus to be claimed again. Read the state here, in the scope that calls the
                // scaffold, so the change recomposes StorybookScaffold itself.
                val entry = FocusRequester()
                val headerLabel = "$PROMPT $attempts"
                StorybookScaffold(
                    entryFocus = entry,
                    header = { HelloBeAction(label = headerLabel, onClick = {}) }
                ) {
                    Row {
                        HelloBeAction(
                            label = SAFE,
                            onClick = {},
                            modifier = Modifier.focusRequester(entry)
                        )
                        HelloBeAction(label = OTHER, onClick = { attempts++ })
                    }
                }
            }
        }

        composeTestRule.onNodeWithText(SAFE).assertIsFocused()

        composeTestRule.onNodeWithText(SAFE).performKeyInput { pressKey(Key.DirectionRight) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(OTHER).assertIsFocused()

        // Genuinely mutate stage state: this recomposes the header, which previously would have
        // been the moment a naive implementation yanked focus back to the entry action.
        composeTestRule.onNodeWithText(OTHER).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("$PROMPT 1").assertExists()

        composeTestRule.onNodeWithText(OTHER).assertIsFocused()
    }

    private fun assertSlotsAreInsideSafeArea(
        density: Float,
        canvasWidth: androidx.compose.ui.unit.Dp = HelloBeLayout.referenceWidth,
        canvasHeight: androidx.compose.ui.unit.Dp = HelloBeLayout.referenceHeight
    ) {
        setStage(density = density, canvasWidth = canvasWidth, canvasHeight = canvasHeight)

        val canvas = boundsOf(CANVAS)
        listOf(HEADER, CONTENT, SUPPORT).forEach { tag ->
            val slot = boundsOf(tag)
            assertThat(slot.left.value)
                .isAtLeast((canvas.left + HelloBeLayout.safeHorizontal).value - TOLERANCE)
            assertThat(slot.right.value)
                .isAtMost((canvas.right - HelloBeLayout.safeHorizontal).value + TOLERANCE)
            assertThat(slot.top.value)
                .isAtLeast((canvas.top + HelloBeLayout.safeVertical).value - TOLERANCE)
            assertThat(slot.bottom.value)
                .isAtMost((canvas.bottom - HelloBeLayout.safeVertical).value + TOLERANCE)
        }
    }

    private fun setStage(
        density: Float,
        canvasWidth: androidx.compose.ui.unit.Dp = HelloBeLayout.referenceWidth,
        canvasHeight: androidx.compose.ui.unit.Dp = HelloBeLayout.referenceHeight
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, 1f)) {
                HelloBeTheme {
                    Box(
                        modifier = Modifier
                            .size(canvasWidth, canvasHeight)
                            .testTag(CANVAS)
                    ) {
                        StorybookScaffold(
                            scenery = { Box(Modifier.fillMaxSize().testTag(SCENERY)) },
                            header = { Box(Modifier.fillMaxWidth().testTag(HEADER)) },
                            support = { Box(Modifier.fillMaxWidth().testTag(SUPPORT)) }
                        ) {
                            Box(Modifier.fillMaxSize().testTag(CONTENT))
                        }
                    }
                }
            }
        }
    }

    private fun boundsOf(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private companion object {
        const val CANVAS = "canvas"
        const val SCENERY = "scenery"
        const val HEADER = "header"
        const val CONTENT = "content"
        const val SUPPORT = "support"
        const val TOLERANCE = 0.5f

        // The three profiles differ by raster density, not by dp layout.
        const val DENSITY_720P = 1.0f
        const val DENSITY_1080P = 2.0f
        const val DENSITY_4K = 4.0f

        val CRAMPED_WIDTH = androidx.compose.ui.unit.Dp(480f)
        val CRAMPED_HEIGHT = androidx.compose.ui.unit.Dp(270f)

        const val SAFE = "Keep learning"
        const val OTHER = "Free play"
        const val PROMPT = "attempts"
    }
}
