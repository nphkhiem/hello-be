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
 * A card is taller than its words on purpose: `childChoiceMinHeight` is the smallest target a
 * child can reliably aim at with a D-pad, and the rest of the space is where artwork will go.
 *
 * The slack has to sit around the content rather than under it. `Surface` builds its content box
 * with `propagateMinConstraints = true`, so the column inside is stretched to the full card height
 * and arranges from the top unless it is told otherwise, which left every label riding high.
 */
@RunWith(AndroidJUnit4::class)
class CardContentCentringTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenAChoiceCardTallerThanItsWord_whenDrawn_thenTheSlackSitsAboveAndBelowEqually() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(modifier = Modifier.testTag(CARD).width(CARD_WIDTH).height(CARD_HEIGHT)) {
                    ChoiceCard(label = APPLE, onClick = {}, modifier = Modifier.fillMaxSize())
                }
            }
        }

        assertCentred(label = APPLE)
    }

    @Test
    fun givenALearningObjectCardTallerThanItsWord_whenDrawn_thenTheSlackSitsAboveAndBelowEqually() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(modifier = Modifier.testTag(CARD).width(CARD_WIDTH).height(CARD_HEIGHT)) {
                    LearningObjectCard(label = APPLE, modifier = Modifier.fillMaxSize())
                }
            }
        }

        assertCentred(label = APPLE)
    }

    /**
     * Stated as "the gap above equals the gap below" rather than "the label is near the middle",
     * because the second passes for a label that is merely somewhere in the upper half.
     *
     * The card is measured through a wrapper of known height rather than by tagging the card
     * itself: a choice card's focus clearance falls outside its own measured node, so tagging it
     * would compare two cards against differently defined edges.
     */
    private fun assertCentred(label: String) {
        val card = composeTestRule.onNodeWithTag(CARD).getUnclippedBoundsInRoot()
        // Unmerged, because a choice card merges its descendants and a merged lookup would return
        // the card itself, making the comparison trivially true.
        val text = composeTestRule.onNodeWithText(label, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        val above = (text.top - card.top).value
        val below = (card.bottom - text.bottom).value

        assertThat(above).isWithin(TOLERANCE).of(below)
    }

    private companion object {
        const val CARD = "card"
        const val APPLE = "Apple"
        val CARD_WIDTH = 268.dp
        val CARD_HEIGHT = 240.dp

        /** Rounding only. A top-aligned label misses by roughly the whole slack, not by a pixel. */
        const val TOLERANCE = 2f
    }
}
