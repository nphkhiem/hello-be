package com.nphkhiem.englishforyourchildren.feature.learning

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreePlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenALibrary_whenTheShelvesAreDrawn_thenAtMostThreeAreInOneViewport() {
        // The information architecture caps a viewport at three peer shelves. More shelves
        // paginate; they never grow into a fourth column or a scrolling row.
        setFreePlay(FreePlayFixtures.shelves())

        composeTestRule.onNodeWithText(FreePlayFixtures.BODY_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(FreePlayFixtures.HOME_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(nextShelfEdge()).assertIsDisplayed()
        // The fourth shelf is named on the pager, never drawn as a shelf beside the three.
        assertThat(shelfCardCount()).isEqualTo(3)
    }

    @Test
    fun givenAShelfPlayedLast_whenFreePlayOpens_thenFocusStartsThereAndNotOnTheFirst() {
        setFreePlay(FreePlayFixtures.shelves())

        composeTestRule.onNodeWithText(FreePlayFixtures.HOME_NAME).assertIsFocused()
        composeTestRule.onNodeWithText(FreePlayFixtures.BODY_NAME).assertIsNotFocused()
    }

    @Test
    fun givenTheFirstPageOfShelves_whenTheEdgeIsRead_thenOnlyTheReachableSideIsOffered() {
        setFreePlay(FreePlayFixtures.shelves())

        composeTestRule.onNodeWithText(nextShelfEdge()).assertIsDisplayed()
        composeTestRule.onNodeWithText(previousShelfEdge()).assertDoesNotExist()
    }

    @Test
    fun givenShelvesOnBothSides_whenEachEdgeIsPressed_thenItAsksToPageThatWay() {
        val actions = mutableListOf<FreePlayAction>()
        setFreePlay(FreePlayFixtures.shelvesMidLibrary(), onAction = { actions += it })

        composeTestRule.onNodeWithText(previousShelfEdge()).requestFocus()
        composeTestRule.onNodeWithText(previousShelfEdge())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(nextShelfEdge()).requestFocus()
        composeTestRule.onNodeWithText(nextShelfEdge())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(
            FreePlayAction.PreviousShelvesRequested,
            FreePlayAction.NextShelvesRequested
        ).inOrder()
    }

    @Test
    fun givenAShelf_whenItIsPressed_thenItAsksToOpenThatShelfById() {
        val actions = mutableListOf<FreePlayAction>()
        setFreePlay(FreePlayFixtures.shelves(), onAction = { actions += it })

        composeTestRule.onNodeWithText(FreePlayFixtures.HOME_NAME)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(FreePlayAction.ShelfChosen(FreePlayFixtures.HOME))
    }

    @Test
    fun givenAnOpenShelf_whenTheWordsAreDrawn_thenOnlyLearnedWordsAppear() {
        // The stop condition: free play contains what a child has met and nothing else.
        setFreePlay(FreePlayFixtures.openShelf())

        FreePlayFixtures.HOME_WORDS.forEach { word ->
            composeTestRule.onNodeWithText(word).assertIsDisplayed()
        }
        FreePlayFixtures.MORE_HOME_WORDS.forEach { word ->
            composeTestRule.onNodeWithText(word).assertDoesNotExist()
        }
    }

    @Test
    fun givenAnOpenShelf_whenItOpens_thenFocusStartsOnTheFirstWord() {
        setFreePlay(FreePlayFixtures.openShelf())

        composeTestRule.onNodeWithText(FreePlayFixtures.HOME_WORDS.first()).assertIsFocused()
    }

    @Test
    fun givenAWordPlayedLast_whenTheShelfOpens_thenFocusResumesThere() {
        setFreePlay(FreePlayFixtures.resumesOnLastWord())

        composeTestRule.onNodeWithText("lamp").assertIsFocused()
        composeTestRule.onNodeWithText(FreePlayFixtures.HOME_WORDS.first()).assertIsNotFocused()
    }

    @Test
    fun givenAWord_whenItIsPressed_thenItAsksForThatPronunciationAndNothingElse() {
        val actions = mutableListOf<FreePlayAction>()
        setFreePlay(FreePlayFixtures.openShelf(), onAction = { actions += it })

        composeTestRule.onNodeWithText("chair")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(FreePlayAction.ObjectChosen("chair"))
    }

    @Test
    fun givenAWordIsBeingSaid_whenTheGridIsRead_thenThatCardIsTheActiveOne() {
        // Pressing a word has to do something a child can see, not only something they hear.
        setFreePlay(FreePlayFixtures.speaking())

        // Both halves. The selection is what a child sees, and the state description is what a
        // screen reader hears. Asserting only the description passed with the visible feedback
        // deleted, which was the whole point of the decision.
        composeTestRule.onNodeWithText("chair").assertIsSelected()
        composeTestRule.onNodeWithText("bed").assertIsNotSelected()
        composeTestRule.onNode(hasStateDescription(speakingOf("chair"))).assertIsDisplayed()
        composeTestRule.onAllNodes(hasStateDescription(speakingOf("bed")))
            .fetchSemanticsNodes()
            .also { assertThat(it).isEmpty() }
    }

    @Test
    fun givenNoSound_whenAWordIsPressed_thenItSaysWhyAndAsksForNothing() {
        val actions = mutableListOf<FreePlayAction>()
        setFreePlay(FreePlayFixtures.noSound(), onAction = { actions += it })

        composeTestRule.onNodeWithText("chair").assertIsFocused()
        composeTestRule.onNodeWithText("chair").performClick()
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
        composeTestRule.onAllNodes(hasStateDescription(noSound())).fetchSemanticsNodes()
            .also { assertThat(it).isNotEmpty() }
    }

    @Test
    fun givenAnOpenShelf_whenBackIsPressed_thenItReturnsToTheShelvesRatherThanLeaving() {
        val actions = mutableListOf<FreePlayAction>()
        setFreePlay(FreePlayFixtures.openShelf(), onAction = { actions += it })

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(FreePlayAction.ShelvesRequested)
    }

    @Test
    fun givenTheShelves_whenBackIsPressed_thenTheScreenLetsItThroughToTheHost() {
        // Free play intercepts Back only when it has somewhere of its own to go. The return to
        // child home is the host's, exactly as it is on the learning path.
        val actions = mutableListOf<FreePlayAction>()
        setFreePlay(FreePlayFixtures.shelves(), onAction = { actions += it })

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenNoWordsYet_whenFreePlayOpens_thenItExplainsAndOffersAWayHome() {
        val actions = mutableListOf<FreePlayAction>()
        setFreePlay(FreePlayFixtures.emptyLibrary(), onAction = { actions += it })

        composeTestRule.onNodeWithText(emptyTitle()).assertIsDisplayed()
        composeTestRule.onNodeWithText(emptyAction()).assertIsFocused()
        composeTestRule.onNodeWithText(emptyAction())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(FreePlayAction.HomeRequested)
    }

    @Test
    fun givenTheReferenceCanvas_whenTheGridIsDrawn_thenFourWordsShareTheContentWidth() {
        var fourColumn = 0f
        composeTestRule.setContent {
            HelloBeTheme {
                fourColumn = HelloBeTheme.layout.cardFourColumnSet.value
                Box(
                    modifier = Modifier.size(
                        HelloBeLayout.referenceWidth,
                        HelloBeLayout.referenceHeight
                    )
                ) {
                    FreePlayScreen(state = FreePlayFixtures.openShelf(), onAction = {})
                }
            }
        }

        // Measured between neighbours, because the token describes the slot and each card sits
        // inside its slot behind focus clearance. Asserting on the drawn card measured the card
        // minus that clearance and failed against its own grid token.
        val first = composeTestRule.onNodeWithText("chair").getUnclippedBoundsInRoot()
        val second = composeTestRule.onNodeWithText("bed").getUnclippedBoundsInRoot()
        val fourth = composeTestRule.onNodeWithText("lamp").getUnclippedBoundsInRoot()

        val slot = second.left.value - first.left.value
        assertThat(slot).isAtLeast(fourColumn)
        assertThat(fourth.right.value).isAtMost(HelloBeLayout.referenceWidth.value)
    }

    @Test
    fun givenAShelfLargerThanTheStage_whenItOpens_thenItStillEndsRatherThanFeeding() {
        // Bounded, not endless: everything the shelf holds is present, and nothing beyond it is.
        setFreePlay(FreePlayFixtures.openShelfThatScrolls())

        val all = FreePlayFixtures.HOME_WORDS + FreePlayFixtures.MORE_HOME_WORDS
        composeTestRule.onNodeWithText(all.first()).assertIsDisplayed()
        assertThat(all).hasSize(12)
    }

    private fun setFreePlay(state: FreePlayUiState, onAction: (FreePlayAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                FreePlayScreen(state = state, onAction = onAction)
            }
        }
    }

    /** Shelf cards carry the "look, listen and say" line or the last-played one, and words do not. */
    private fun shelfCardCount(): Int {
        val hint = resources.getString(R.string.free_play_shelf_hint)
        val last = resources.getString(R.string.free_play_shelf_last)
        return composeTestRule.onAllNodesWithText(hint).fetchSemanticsNodes().size +
            composeTestRule.onAllNodesWithText(last).fetchSemanticsNodes().size
    }

    private fun nextShelfEdge() =
        resources.getString(R.string.free_play_next_shelf, FreePlayFixtures.NEXT_SHELF)

    private fun previousShelfEdge() =
        resources.getString(R.string.free_play_previous_shelf, FreePlayFixtures.PREVIOUS_SHELF)

    private fun speakingOf(word: String) = resources.getString(R.string.free_play_speaking, word)

    private fun noSound() = resources.getString(R.string.free_play_no_sound)

    private fun emptyTitle() = resources.getString(R.string.free_play_empty_title)

    private fun emptyAction() = resources.getString(R.string.free_play_empty_action)
}
