package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

/**
 * What focus looks like is a claim about pixels, so it is checked in pixels rather than by reading
 * the source. Both halves of the treatment matter: the fill is what a child sees from across the
 * room, and the stroke is what carves it out of the background.
 */
@RunWith(AndroidJUnit4::class)
class FocusAppearanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenAFocusedControl_whenRendered_thenItTakesTheFocusFillAndStroke() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(Modifier.size(HOST).background(BACKDROP_COLOR).testTag(HOST_TAG)) {
                    HelloBeAction(label = LABEL, onClick = {})
                }
            }
        }
        composeTestRule.onNodeWithText(LABEL).requestFocus()
        composeTestRule.waitForIdle()

        val colors = helloBeColors(HelloBeThemeMode.DAY)
        val painted = paintedColours()

        assertThat(painted).contains(colors.focusFill.toArgb())
        assertThat(painted).contains(colors.focusRing.toArgb())
    }

    @Test
    fun givenAQuietActionAtRest_whenRendered_thenItPaintsNoBackgroundOfItsOwn() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(Modifier.size(HOST).background(BACKDROP_COLOR).testTag(HOST_TAG)) {
                    HelloBeAction(
                        label = LABEL,
                        onClick = {},
                        tone = HelloBeActionTone.QUIET
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val colors = helloBeColors(HelloBeThemeMode.DAY)
        val painted = paintedColours()

        // Nothing but the backdrop and the label's ink: no container of its own, and in
        // particular not the secondary action's fill, which is what it would inherit by mistake.
        assertThat(painted).doesNotContain(colors.actionSecondary.toArgb())
        assertThat(painted).doesNotContain(colors.actionPrimary.toArgb())
        assertThat(painted).contains(BACKDROP_COLOR.toArgb())
    }

    @Test
    fun givenAQuietActionAtRest_whenRendered_thenItIsOutlinedLikeAnyOtherControl() {
        // Without an outline a quiet action read as a caption on a television, not as something
        // to press. It keeps no fill, so the safe choice beside it is still the heavier one.
        composeTestRule.setContent {
            HelloBeTheme {
                Box(Modifier.size(HOST).background(BACKDROP_COLOR).testTag(HOST_TAG)) {
                    HelloBeAction(label = LABEL, onClick = {}, tone = HelloBeActionTone.QUIET)
                }
            }
        }
        composeTestRule.waitForIdle()

        // Every pixel: the outline is a hairline that a sampled scan can step over.
        assertThat(paintedColours(stride = 1))
            .contains(helloBeColors(HelloBeThemeMode.DAY).borderSecondary.toArgb())
    }

    private fun paintedColours(stride: Int = STRIDE): Set<Int> {
        val image = composeTestRule.onNodeWithTag(HOST_TAG).captureToImage()
        val map = image.toPixelMap()
        return buildSet {
            for (y in 0 until map.height step stride) {
                for (x in 0 until map.width step stride) {
                    add(map[x, y].toArgb())
                }
            }
        }
    }

    private companion object {
        const val LABEL = "Keep learning"
        const val HOST_TAG = "host"
        const val STRIDE = 3
        val HOST = 280.dp
        val BACKDROP_COLOR = androidx.compose.ui.graphics.Color(0xFF00FF00)
    }
}
