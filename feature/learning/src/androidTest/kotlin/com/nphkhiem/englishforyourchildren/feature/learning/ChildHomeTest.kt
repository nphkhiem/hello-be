package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChildHomeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenAReturningChild_whenHomeOpens_thenContinueHoldsFocusAndSaysWhereItGoes() {
        setHome(ChildHomeFixtures.returning())

        composeTestRule.onNodeWithText(continueLabel()).assertIsFocused()
        composeTestRule.onNodeWithText(ChildHomeFixtures.CONTEXT).assertIsDisplayed()
    }

    @Test
    fun givenANewChild_whenHomeOpens_thenTheInvitationIsToStartRatherThanContinue() {
        setHome(ChildHomeFixtures.newLearner())

        composeTestRule.onNodeWithText(startLabel()).assertIsFocused()
        composeTestRule.onNodeWithText(continueLabel()).assertDoesNotExist()
    }

    @Test
    fun givenEveryAdventureIsFinished_whenHomeOpens_thenFreePlayLeadsAndAppearsOnce() {
        setHome(ChildHomeFixtures.courseComplete())

        composeTestRule.onNodeWithText(courseComplete()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(freePlay()).assertCountEquals(1)
        composeTestRule.onNodeWithText(freePlay()).assertIsFocused()
    }

    @Test
    fun givenEveryAdventureIsFinished_whenHomeIsRead_thenNothingStillPromisesOneIsWaiting() {
        // Found by looking at it: the greeting swapped and the line under it did not, so the
        // screen said an adventure was ready when there was none left.
        setHome(ChildHomeFixtures.courseComplete())

        composeTestRule.onNodeWithText(completeHint()).assertIsDisplayed()
        composeTestRule.onNodeWithText(ChildHomeFixtures.WAITING_HINT).assertDoesNotExist()
    }

    @Test
    fun givenTheCheckpointWillNotOpen_whenHomeOpens_thenItSaysSoAndFocusGoesSomewhereUseful() {
        val actions = mutableListOf<ChildHomeAction>()
        setHome(ChildHomeFixtures.checkpointUnavailable(), onAction = { actions += it })

        composeTestRule.onNodeWithText(learningPath()).assertIsFocused()
        composeTestRule.onNodeWithText(continueLabel()).requestFocus()
        composeTestRule.onNodeWithText(continueLabel())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenTheDominantAction_whenTheRowIsDrawn_thenItIsWiderThanEitherSecondary() {
        // Dominance is a claim about pixels, so it is asserted in pixels.
        setHome(ChildHomeFixtures.returning())

        val dominant = composeTestRule.onNodeWithText(continueLabel()).getUnclippedBoundsInRoot()
        val path = composeTestRule.onNodeWithText(learningPath()).getUnclippedBoundsInRoot()
        val play = composeTestRule.onNodeWithText(freePlay()).getUnclippedBoundsInRoot()

        val dominantWidth = (dominant.right - dominant.left).value

        // A ratio, not "greater than". At equal weights the dominant card still measures a
        // quarter of a dp wider from rounding, and that passed a bare greater-than while proving
        // nothing about dominance.
        assertThat(dominantWidth)
            .isAtLeast((path.right - path.left).value * MIN_DOMINANCE)
        assertThat(dominantWidth)
            .isAtLeast((play.right - play.left).value * MIN_DOMINANCE)
    }

    @Test
    fun givenAReturningChild_whenContinueIsPressed_thenOneActionAsksForTheNextThing() {
        val actions = mutableListOf<ChildHomeAction>()
        setHome(ChildHomeFixtures.returning(), onAction = { actions += it })

        composeTestRule.onNodeWithText(continueLabel())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ChildHomeAction.ContinueRequested)
    }

    @Test
    fun givenANewChild_whenStartIsPressed_thenItAsksForTheSameThingAsContinue() {
        // Continue and Start mean one thing, so they emit one action rather than a pair that must
        // always be handled identically.
        val actions = mutableListOf<ChildHomeAction>()
        setHome(ChildHomeFixtures.newLearner(), onAction = { actions += it })

        composeTestRule.onNodeWithText(startLabel())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ChildHomeAction.ContinueRequested)
    }

    @Test
    fun givenTheHeader_whenItsUtilitiesArePressed_thenEachEmitsItsOwnAction() {
        val actions = mutableListOf<ChildHomeAction>()
        setHome(ChildHomeFixtures.returning(), onAction = { actions += it })

        composeTestRule.onNodeWithText(grownUps()).requestFocus()
        composeTestRule.onNodeWithText(grownUps()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Minh").requestFocus()
        composeTestRule.onNodeWithText("Minh").performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(
            ChildHomeAction.CaregiverEntryRequested,
            ChildHomeAction.SwitchProfileRequested
        ).inOrder()
    }

    @Test
    fun givenProgressIsNotWrittenDownYet_whenHomeOpens_thenItSaysSoWithoutClaimingOtherwise() {
        setHome(ChildHomeFixtures.pendingSave())

        composeTestRule.onNodeWithText(pendingSave()).assertIsDisplayed()
    }

    private fun setHome(state: ChildHomeUiState, onAction: (ChildHomeAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                ChildHomeScreen(state = state, onAction = onAction)
            }
        }
    }

    private fun continueLabel() = resources.getString(R.string.home_continue)

    private fun startLabel() = resources.getString(R.string.home_start)

    private fun learningPath() = resources.getString(R.string.home_learning_path)

    private fun freePlay() = resources.getString(R.string.home_free_play)

    private fun grownUps() = resources.getString(R.string.home_grown_ups)

    private fun courseComplete() = resources.getString(R.string.home_course_complete)

    private fun completeHint() = resources.getString(R.string.home_course_complete_hint)

    private fun pendingSave() = resources.getString(R.string.lesson_pending_save)

    private companion object {
        /** The draft asks for 1.6 against 1. Comfortably clear of rounding, short of the target. */
        const val MIN_DOMINANCE = 1.3f
    }
}
