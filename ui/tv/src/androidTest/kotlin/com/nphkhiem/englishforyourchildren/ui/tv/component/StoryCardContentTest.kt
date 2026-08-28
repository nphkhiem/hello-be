package com.nphkhiem.englishforyourchildren.ui.tv.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A story card holds a block: an optional picture, a title, and a supporting line under it. Left
 * and top is right for that, and it is what a profile or a unit card wants.
 *
 * It is wrong for a card holding a single mark, such as an age. That case centres.
 */
@RunWith(AndroidJUnit4::class)
class StoryCardContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenACardHoldingOneGlyph_whenCentred_thenTheSlackSitsEvenlyAround() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(modifier = Modifier.testTag(CARD).width(CARD_SIZE).height(CARD_SIZE)) {
                    StoryCard(
                        title = GLYPH,
                        onClick = {},
                        centerContent = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        val card = composeTestRule.onNodeWithTag(CARD).getUnclippedBoundsInRoot()
        val text = composeTestRule.onNodeWithText(GLYPH, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertThat((text.top - card.top).value)
            .isWithin(TOLERANCE).of((card.bottom - text.bottom).value)
        assertThat((text.left - card.left).value)
            .isWithin(TOLERANCE).of((card.right - text.right).value)
    }

    @Test
    fun givenACardOfRunningText_whenLeftAsItIs_thenItStaysAtTheTopLeft() {
        // The default must not move. Profile and unit cards read as a block and are meant to.
        composeTestRule.setContent {
            HelloBeTheme {
                Box(modifier = Modifier.testTag(CARD).width(CARD_SIZE).height(CARD_SIZE)) {
                    StoryCard(
                        title = TITLE,
                        supportingText = SUPPORTING,
                        onClick = {},
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        val card = composeTestRule.onNodeWithTag(CARD).getUnclippedBoundsInRoot()
        val text = composeTestRule.onNodeWithText(TITLE, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        val above = (text.top - card.top).value
        val below = (card.bottom - text.bottom).value
        val left = (text.left - card.left).value

        assertThat(above).isLessThan(below)
        assertThat(left).isLessThan((card.right - text.right).value)
    }

    private companion object {
        const val CARD = "card"
        const val GLYPH = "3"
        const val TITLE = "Unit one"
        const val SUPPORTING = "Five lessons"
        val CARD_SIZE = 240.dp
        const val TOLERANCE = 2f
    }
}
