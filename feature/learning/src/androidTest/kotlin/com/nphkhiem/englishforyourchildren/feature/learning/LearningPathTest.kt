package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
class LearningPathTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenAUnit_whenThePathIsDrawn_thenLessonsRunInOrderAcrossTheStage() {
        setPath(LearningPathFixtures.midUnit())

        val left = listOf(
            LearningPathFixtures.EYES,
            LearningPathFixtures.NOSE,
            LearningPathFixtures.HANDS,
            LearningPathFixtures.MOVE,
            LearningPathFixtures.REVIEW
        ).map { composeTestRule.onNodeWithText(it).getUnclippedBoundsInRoot().left.value }

        assertThat(left).isInOrder()
        assertThat(left).containsNoDuplicates()
    }

    @Test
    fun givenALessonStillAhead_whenThePathIsDrawn_thenFocusCannotLandOnIt() {
        setPath(LearningPathFixtures.midUnit())

        // Asserted by walking, not by reading semantics: a disabled TV Surface still defines the
        // Focused key, so an absent-key assertion here passed while proving nothing.
        //
        // The claim is only that focus never enters a lesson still ahead. Pressing on from the
        // last reachable lesson leaves the row for the stepper above it, which is the ordinary
        // focus search and is fine; what must not happen is landing on "Later".
        composeTestRule.onNodeWithText(LearningPathFixtures.HANDS).assertIsFocused()
        repeat(2) {
            composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithText(LearningPathFixtures.MOVE).assertIsNotFocused()
        composeTestRule.onNodeWithText(LearningPathFixtures.REVIEW).assertIsNotFocused()
        composeTestRule.onNodeWithText(LearningPathFixtures.MOVE).assertIsNotEnabled()
        composeTestRule.onNodeWithText(LearningPathFixtures.REVIEW).assertIsNotEnabled()
    }

    @Test
    fun givenALessonStillAhead_whenItIsPressedAnyway_thenNothingIsAsked() {
        val actions = mutableListOf<LearningPathAction>()
        setPath(LearningPathFixtures.midUnit(), onAction = { actions += it })

        composeTestRule.onNodeWithText(LearningPathFixtures.MOVE).performClick()
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenALessonStillAhead_whenThePathIsRead_thenItIsShownAsLaterRatherThanHidden() {
        setPath(LearningPathFixtures.midUnit())

        composeTestRule.onNodeWithText(LearningPathFixtures.MOVE).assertIsDisplayed()
        composeTestRule.onNodeWithText(LearningPathFixtures.REVIEW).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(later()).assertCountEquals(2)
    }

    @Test
    fun givenARecommendedLesson_whenThePathOpens_thenFocusStartsThereAndNotAtTheFirstLesson() {
        // Also the Back restoration claim: a child returning from the third lesson arrives in a
        // state that names it, so focus lands where they left rather than at the top of the unit.
        setPath(LearningPathFixtures.midUnit())

        composeTestRule.onNodeWithText(LearningPathFixtures.HANDS).assertIsFocused()
        // A real assertion here only because this card is reachable and so does define the key.
        composeTestRule.onNodeWithText(LearningPathFixtures.EYES).assertIsNotFocused()
    }

    @Test
    fun givenARecommendedLesson_whenThePathIsDrawn_thenPipStandsOverThatLessonAndNoOther() {
        // The brief has Pip indicate the recommendation. Indication by position is the only kind a
        // child who cannot read can follow, so it is asserted in pixels rather than by wording.
        setPath(LearningPathFixtures.midUnit())

        val pip = composeTestRule.onNodeWithContentDescription(pipDescription())
            .getUnclippedBoundsInRoot()
        val pipCentre = (pip.left.value + pip.right.value) / 2
        val recommended = composeTestRule.onNodeWithText(LearningPathFixtures.HANDS)
            .getUnclippedBoundsInRoot()
        val neighbour = composeTestRule.onNodeWithText(LearningPathFixtures.NOSE)
            .getUnclippedBoundsInRoot()

        assertThat(pipCentre).isAtLeast(recommended.left.value)
        assertThat(pipCentre).isAtMost(recommended.right.value)
        assertThat(pipCentre).isGreaterThan(neighbour.right.value)
        assertThat(pip.bottom.value).isAtMost(recommended.top.value)
    }

    @Test
    fun givenNoLessonIsRecommended_whenThePathIsDrawn_thenPipPointsAtNothing() {
        // A cue standing over nothing in particular is a cue that means nothing.
        setPath(LearningPathFixtures.nothingReachable())

        composeTestRule.onNodeWithContentDescription(pipDescription()).assertDoesNotExist()
    }

    @Test
    fun givenAFinishedUnit_whenThePathOpens_thenFocusFallsToTheFirstLessonItCanReach() {
        setPath(LearningPathFixtures.unitFinished())

        composeTestRule.onNodeWithText(LearningPathFixtures.EYES).assertIsFocused()
    }

    @Test
    fun givenNothingReachableYet_whenThePathOpens_thenFocusFallsToTheStepper() {
        setPath(LearningPathFixtures.nothingReachable())

        composeTestRule.onNodeWithText(PREVIOUS_THEME).assertIsFocused()
    }

    @Test
    fun givenALessonThatWillNotOpen_whenItIsFocusedAndPressed_thenItSaysSoAndAsksNothing() {
        val actions = mutableListOf<LearningPathAction>()
        setPath(LearningPathFixtures.lessonWillNotOpen(), onAction = { actions += it })

        composeTestRule.onNodeWithText(LearningPathFixtures.HANDS).assertIsFocused()
        composeTestRule.onNodeWithText(LearningPathFixtures.HANDS)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenARecommendedLesson_whenItIsPressed_thenItAsksForThatLessonById() {
        val actions = mutableListOf<LearningPathAction>()
        setPath(LearningPathFixtures.midUnit(), onAction = { actions += it })

        composeTestRule.onNodeWithText(LearningPathFixtures.HANDS)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions)
            .containsExactly(LearningPathAction.LessonChosen("hands-and-feet"))
    }

    @Test
    fun givenTheFirstUnit_whenThePathIsDrawn_thenOnlyTheWayForwardIsOffered() {
        setPath(LearningPathFixtures.firstUnit())

        composeTestRule.onNodeWithText(nextControl(2)).assertIsDisplayed()
        composeTestRule.onNodeWithText(previousControl(1)).assertDoesNotExist()
    }

    @Test
    fun givenTheLastUnit_whenThePathIsDrawn_thenOnlyTheWayBackIsOffered() {
        setPath(LearningPathFixtures.lastUnit())

        composeTestRule.onNodeWithText(previousControl(11)).assertIsDisplayed()
        composeTestRule.onNodeWithText(nextControl(3)).assertDoesNotExist()
    }

    @Test
    fun givenTheStepper_whenEachSideIsPressed_thenItAsksToPageThatWay() {
        val actions = mutableListOf<LearningPathAction>()
        setPath(LearningPathFixtures.midUnit(), onAction = { actions += it })

        composeTestRule.onNodeWithText(PREVIOUS_THEME).requestFocus()
        composeTestRule.onNodeWithText(PREVIOUS_THEME)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(NEXT_THEME).requestFocus()
        composeTestRule.onNodeWithText(NEXT_THEME)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(
            LearningPathAction.PreviousUnitRequested,
            LearningPathAction.NextUnitRequested
        ).inOrder()
    }

    @Test
    fun givenFocusOnTheStepper_whenTheUnitChanges_thenFocusStaysOnTheStepper() {
        val state = mutableStateOf(LearningPathFixtures.midUnit())
        composeTestRule.setContent {
            HelloBeTheme {
                LearningPathScreen(state = state.value, onAction = {})
            }
        }

        composeTestRule.onNodeWithText(NEXT_THEME).requestFocus()
        composeTestRule.runOnIdle { state.value = LearningPathFixtures.lastUnit() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(LAST_PREVIOUS_THEME).assertIsFocused()
    }

    @Test
    fun givenFocusOnPrevious_whenPagingReachesTheFirstUnit_thenFocusIsNotDropped() {
        // Paging back to the first unit removes the control the child is standing on, which is
        // exactly where they are most likely to still be pressing.
        val state = mutableStateOf(LearningPathFixtures.midUnit())
        composeTestRule.setContent {
            HelloBeTheme {
                LearningPathScreen(state = state.value, onAction = {})
            }
        }

        composeTestRule.onNodeWithText(PREVIOUS_THEME).requestFocus()
        composeTestRule.runOnIdle { state.value = LearningPathFixtures.firstUnit() }
        composeTestRule.waitForIdle()

        // The first unit's only remaining control, which the stepper must have moved focus onto.
        composeTestRule.onNodeWithText(LearningPathFixtures.THEME).assertIsFocused()
    }

    @Test
    fun givenATwelveUnitCourse_whenThePathIsDrawn_thenOnlyThisUnitsLessonsAreOnTheStage() {
        // The stop condition: twelve units without an all-course grid, a search, or a feed.
        setPath(LearningPathFixtures.midUnit())

        composeTestRule.onNodeWithText(LearningPathFixtures.THEME).assertIsDisplayed()
        composeTestRule.onNodeWithText(kicker()).assertIsDisplayed()
        // The neighbouring units reach the stage as paging controls and nothing else. Two of
        // twelve units are named here; the other nine are not on screen in any form.
        composeTestRule.onNodeWithText(previousControl(1)).assertIsDisplayed()
        composeTestRule.onNodeWithText(nextControl(3)).assertIsDisplayed()
        composeTestRule.onNodeWithText(LAST_PREVIOUS_THEME).assertDoesNotExist()
    }

    @Test
    fun givenTheReferenceCanvas_whenThePathIsDrawn_thenNothingIsPushedOffTheStage() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(
                    modifier = Modifier.size(
                        HelloBeLayout.referenceWidth,
                        HelloBeLayout.referenceHeight
                    )
                ) {
                    LearningPathScreen(state = LearningPathFixtures.midUnit(), onAction = {})
                }
            }
        }

        val card = composeTestRule.onNodeWithText(LearningPathFixtures.EYES)
            .getUnclippedBoundsInRoot()

        assertThat(card.bottom.value)
            .isAtMost(HelloBeLayout.referenceHeight.value)
        assertThat(card.top.value).isGreaterThan(0f)
    }

    @Test
    fun givenTheReferenceCanvas_whenEveryLessonIsDrawn_thenNoStatusWordIsSqueezedFlat() {
        // The status word under each title was drawn sliced in half, and neither a bounds check nor
        // the text layout's overflow flag saw it: the word is not spilling past the card, it is
        // being squeezed: its node measured 20px against the 85px it gets when the layout is
        // healthy. So the claim is that each word is allowed at least the height of its own font
        // size, which a squeezed word cannot reach and a healthy one clears comfortably.
        var fontSize = 0f
        composeTestRule.setContent {
            HelloBeTheme {
                with(LocalDensity.current) {
                    fontSize = HelloBeTheme.typography.bodyMedium.fontSize.toPx()
                }
                Box(
                    modifier = Modifier.size(
                        HelloBeLayout.referenceWidth,
                        HelloBeLayout.referenceHeight
                    )
                ) {
                    LearningPathScreen(state = LearningPathFixtures.midUnit(), onAction = {})
                }
            }
        }

        listOf(
            LearningPathFixtures.EYES to FINISHED,
            LearningPathFixtures.NOSE to FINISHED,
            LearningPathFixtures.HANDS to CONTINUE,
            LearningPathFixtures.MOVE to LATER,
            LearningPathFixtures.REVIEW to LATER
        ).forEach { (title, status) ->
            val card = composeTestRule.onNodeWithText(title).fetchSemanticsNode()
            // Unmerged and matched by column: the card merges its descendants, so asking the merged
            // tree for the status word hands back the card itself.
            val word = composeTestRule
                .onAllNodesWithText(status, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .single { it.positionInRoot.x in card.columnRange() }

            assertThat(word.size.height.toFloat()).isAtLeast(fontSize)
        }
    }

    private fun SemanticsNode.columnRange(): ClosedFloatingPointRange<Float> =
        positionInRoot.x..(positionInRoot.x + size.width)

    @Test
    fun givenAUnitThatWillNotLoad_whenThePathOpens_thenThereIsAWayHomeHoldingFocus() {
        val actions = mutableListOf<LearningPathAction>()
        setPath(LearningPathFixtures.recovering(), onAction = { actions += it })

        composeTestRule.onNodeWithText(recoveryAction()).assertIsFocused()
        composeTestRule.onNodeWithText(recoveryAction())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LearningPathAction.HomeRequested)
    }

    @Test
    fun givenAUnitThatLoadedEmpty_whenThePathOpens_thenItIsTheSameDeadEndAndSaysSo() {
        setPath(LearningPathFixtures.emptyUnit())

        composeTestRule.onNodeWithText(recoveryTitle()).assertIsDisplayed()
        composeTestRule.onNodeWithText(recoveryAction()).assertIsFocused()
    }

    @Test
    fun givenProgressIsNotWrittenDownYet_whenThePathOpens_thenItSaysSo() {
        setPath(LearningPathFixtures.pendingSave())

        composeTestRule.onNodeWithText(pendingSave()).assertIsDisplayed()
    }

    private fun setPath(state: LearningPathUiState, onAction: (LearningPathAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                LearningPathScreen(state = state, onAction = onAction)
            }
        }
    }

    private fun later() = resources.getString(R.string.path_lesson_later)

    private fun kicker() = resources.getString(
        R.string.path_unit_kicker,
        2,
        LearningPathFixtures.UNIT_COUNT
    )

    private fun recoveryTitle() = resources.getString(R.string.path_recovery_title)

    private fun recoveryAction() = resources.getString(R.string.path_recovery_action)

    private fun pendingSave() = resources.getString(R.string.lesson_pending_save)

    private fun pipDescription() = resources.getString(R.string.path_pip_recommended)

    private fun previousControl(unitNumber: Int) =
        resources.getString(R.string.path_previous_unit, unitNumber)

    private fun nextControl(unitNumber: Int) =
        resources.getString(R.string.path_next_unit, unitNumber)

    private companion object {
        const val PREVIOUS_THEME = "My Home"
        const val NEXT_THEME = "My Toys"
        const val LAST_PREVIOUS_THEME = "My Garden"
        const val FINISHED = "Finished"
        const val CONTINUE = "Continue"
        const val LATER = "Later"
    }
}
