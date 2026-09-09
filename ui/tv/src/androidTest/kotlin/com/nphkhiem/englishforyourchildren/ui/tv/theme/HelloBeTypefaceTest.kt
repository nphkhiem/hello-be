package com.nphkhiem.englishforyourchildren.ui.tv.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That the app draws in its own typeface, checked in pixels rather than by reading the source.
 *
 * Reading the declaration would prove nothing: `FontFamily.Default` and a real family are both
 * valid `FontFamily` values, and the app spent months rendering in the platform sans while the
 * tokens named Nunito Sans. Every screenshot taken in that period, including the approved design
 * reviews, showed a typeface nobody had chosen. So the claim under test is exactly the one that
 * was silently false, asserted the one way that cannot be satisfied by accident.
 *
 * The second test is why a variable font was chosen over static weights. The type ramp asks for
 * 450 and 750, which no static file carries; with static weights Compose rounds them to 500 and
 * 700, and two roles lose the distinction the tokens drew. Different pixels at 450 and 500 prove
 * the weight axis is resolved rather than rounded.
 *
 * Both samples are drawn in one composition because a Compose rule accepts content once.
 */
@RunWith(AndroidJUnit4::class)
class HelloBeTypefaceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenTheAppTypeface_whenTextIsDrawn_thenItIsNotThePlatformDefault() {
        composeTestRule.setContent {
            Column {
                Sample(FIRST, HelloBeFontFamily.primary, weight = 700)
                Sample(SECOND, FontFamily.Default, weight = 700)
            }
        }

        assertThat(pixelsOf(FIRST)).isNotEqualTo(pixelsOf(SECOND))
    }

    @Test
    fun givenAVariableWeight_whenDrawnAtFourFiftyAndFiveHundred_thenTheWeightsDiffer() {
        composeTestRule.setContent {
            Column {
                Sample(FIRST, HelloBeFontFamily.primary, weight = 450)
                Sample(SECOND, HelloBeFontFamily.primary, weight = 500)
            }
        }

        assertThat(pixelsOf(FIRST)).isNotEqualTo(pixelsOf(SECOND))
    }

    /** One glyph carrying a Vietnamese diacritic, drawn large on a known ground. */
    @Composable
    private fun Sample(tag: String, family: FontFamily, weight: Int) {
        BasicText(
            text = "Bé",
            modifier = Modifier
                .size(SAMPLE_SIZE.dp)
                .background(Color.White)
                .testTag(tag),
            style = TextStyle(
                fontFamily = family,
                fontWeight = FontWeight(weight),
                fontSize = SAMPLE_TEXT_SIZE.sp,
                color = Color.Black
            )
        )
    }

    private fun pixelsOf(tag: String): List<Int> =
        composeTestRule.onNodeWithTag(tag).captureToImage().toPixelMap().buffer.toList()

    private companion object {
        const val SAMPLE_SIZE = 120
        const val SAMPLE_TEXT_SIZE = 72
        const val FIRST = "typeface-sample-first"
        const val SECOND = "typeface-sample-second"
    }
}
