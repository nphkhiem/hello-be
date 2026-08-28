package com.nphkhiem.englishforyourchildren.feature.learning

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CelebrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenTheRevealHasNotFinished_whenTheCelebrationOpens_thenDoneAlreadyHoldsFocus() {
        // The draft has the reveal accept input immediately, so Done is live on the first frame
        // rather than arriving when the reveal ends.
        setCelebration(CelebrationFixtures.revealing())

        composeTestRule.onNodeWithText(done()).assertIsFocused()
    }

    @Test
    fun givenTheRevealHasNotFinished_whenDoneIsPressed_thenItCompletesRatherThanSettlingThePage() {
        val actions = mutableListOf<CelebrationAction>()
        setCelebration(CelebrationFixtures.revealing(), onAction = { actions += it })

        composeTestRule.onNodeWithText(done()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(CelebrationAction.DoneRequested)
    }

    @Test
    fun givenTheCelebration_whenBackIsPressed_thenItCompletesTheSameWayDoneDoes() {
        // Without this, Back pops into the lesson the child just finished.
        val actions = mutableListOf<CelebrationAction>()
        var backDispatched = false
        composeTestRule.setContent {
            HelloBeTheme {
                BackHandler(enabled = true) { backDispatched = true }
                LessonCelebrationScreen(
                    state = CelebrationFixtures.settled(),
                    onAction = { actions += it }
                )
            }
        }

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(CelebrationAction.DoneRequested)
        // The screen's own handler took it, so nothing behind the celebration saw the press.
        assertThat(backDispatched).isFalse()
    }

    @Test
    fun givenAnyNumberOfWords_whenTheHeadlineIsRead_thenItCountsTheWordsOnThePage() {
        // The headline used to be free text a caller wrote, which let it say "four" above five
        // cards. The count is formatted from the list now, so the two cannot disagree.
        setCelebration(CelebrationFixtures.fiveWords())

        composeTestRule.onNodeWithText(headlineFor(5)).assertIsDisplayed()
        composeTestRule.onNodeWithText(headlineFor(4)).assertDoesNotExist()
    }

    @Test
    fun givenTheBriefsFloorOfThreeWords_whenTheHeadlineIsRead_thenItCountsThoseThree() {
        setCelebration(CelebrationFixtures.threeWords())

        composeTestRule.onNodeWithText(headlineFor(3)).assertIsDisplayed()
    }

    @Test
    fun givenTheSaveIsConfirmed_whenTheCelebrationIsRead_thenItSaysTheWordsAreStored() {
        setCelebration(CelebrationFixtures.settled())

        composeTestRule.onNodeWithText(saved()).assertIsDisplayed()
    }

    @Test
    fun givenTheSaveIsPending_whenTheCelebrationIsRead_thenNothingClaimsTheWordsAreStored() {
        // The invariant the design brief actually states, asserted as an absence so a future edit
        // cannot quietly reintroduce the claim.
        setCelebration(CelebrationFixtures.pendingSave())

        composeTestRule.onNodeWithText(saved()).assertDoesNotExist()
        composeTestRule.onNodeWithText(saving()).assertIsDisplayed()
    }

    @Test
    fun givenStandardMotion_whenTheRevealHasNotFinished_thenTheWordsAreNotOnThePageYet() {
        setCelebration(CelebrationFixtures.revealing())

        composeTestRule.onNodeWithText(CelebrationFixtures.CHAIR).assertDoesNotExist()
        composeTestRule.onNodeWithText(CelebrationFixtures.LAMP).assertDoesNotExist()
    }

    @Test
    fun givenStandardMotion_whenTheRevealHasFinished_thenTheWordsAreOnThePage() {
        setCelebration(CelebrationFixtures.settled())

        composeTestRule.onNodeWithText(CelebrationFixtures.CHAIR).assertIsDisplayed()
        composeTestRule.onNodeWithText(CelebrationFixtures.LAMP).assertIsDisplayed()
    }

    @Test
    fun givenReducedMotion_whenTheRevealHasNotFinished_thenTheWordsAreAlreadyThere() {
        // The static success page: no movement to wait for, so nothing is withheld.
        setCelebration(CelebrationFixtures.revealing(), reduceMotion = true)

        composeTestRule.onNodeWithText(CelebrationFixtures.CHAIR).assertIsDisplayed()
        composeTestRule.onNodeWithText(CelebrationFixtures.LAMP).assertIsDisplayed()
    }

    @Test
    fun givenTheBriefsMaximumOfFiveWords_whenTheRowIsDrawn_thenEachStaysAboveTheColumnToken() {
        var fiveColumn = 0f
        var cardGap = 0f
        composeTestRule.setContent {
            HelloBeTheme {
                fiveColumn = HelloBeTheme.layout.cardFiveColumnSet.value
                cardGap = HelloBeTheme.spacing.cardGap.value
                Box(
                    modifier = Modifier.size(
                        HelloBeLayout.referenceWidth,
                        HelloBeLayout.referenceHeight
                    )
                ) {
                    LessonCelebrationScreen(
                        state = CelebrationFixtures.fiveWords(),
                        onAction = {}
                    )
                }
            }
        }

        // Measured from the spacing between words rather than from a card node. The learning
        // object card has no semantics of its own, so selecting by text hands back the word, and
        // asserting on that measured the length of "chair" instead of the card holding it.
        val centres = listOf(
            CelebrationFixtures.CHAIR,
            CelebrationFixtures.BED,
            CelebrationFixtures.DOOR,
            CelebrationFixtures.LAMP,
            CelebrationFixtures.RUG
        ).map { word ->
            val node = composeTestRule.onNodeWithText(word).fetchSemanticsNode()
            (node.positionInRoot.x + node.size.width / 2f).toDp()
        }

        centres.zipWithNext { left, right ->
            // One card plus one gap, so the card itself clears the five column token.
            assertThat(right - left).isAtLeast(fiveColumn + cardGap)
        }
    }

    @Test
    fun givenTheReferenceCanvas_whenTheCelebrationIsDrawn_thenNoWordIsSqueezedFlat() {
        // The defect HB-D13 shipped and had to fix: cards whose bounds fit while their content was
        // sliced. A word allowed less height than its own font size is a word a child cannot read.
        var fontSize = 0f
        composeTestRule.setContent {
            HelloBeTheme {
                fontSize = with(androidx.compose.ui.platform.LocalDensity.current) {
                    HelloBeTheme.typography.titleMedium.fontSize.toPx()
                }
                Box(
                    modifier = Modifier.size(
                        HelloBeLayout.referenceWidth,
                        HelloBeLayout.referenceHeight
                    )
                ) {
                    LessonCelebrationScreen(
                        state = CelebrationFixtures.fiveWords(),
                        onAction = {}
                    )
                }
            }
        }

        listOf(
            CelebrationFixtures.CHAIR,
            CelebrationFixtures.BED,
            CelebrationFixtures.DOOR,
            CelebrationFixtures.LAMP,
            CelebrationFixtures.RUG
        ).forEach { word ->
            val node = composeTestRule
                .onAllNodesWithText(word, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .single()
            assertThat(node.size.height.toFloat()).isAtLeast(fontSize)
        }
    }

    @Test
    fun givenTheCelebration_whenItIsRead_thenNothingCountsAnythingButTheWords() {
        // No score, no streak, no percentage. The stop condition, asserted rather than assumed.
        setCelebration(CelebrationFixtures.settled())

        listOf("%", "points", "score", "streak", "star", "/").forEach { pressure ->
            composeTestRule
                .onAllNodesWithText(pressure, substring = true)
                .fetchSemanticsNodes()
                .also { assertThat(it).isEmpty() }
        }
    }

    private fun setCelebration(
        state: CelebrationUiState,
        onAction: (CelebrationAction) -> Unit = {},
        reduceMotion: Boolean = false
    ) {
        composeTestRule.setContent {
            HelloBeTheme(reduceMotion = reduceMotion) {
                LessonCelebrationScreen(state = state, onAction = onAction)
            }
        }
    }

    private fun headlineFor(count: Int) = resources.getString(
        R.string.celebration_headline,
        count,
        CelebrationFixtures.UNIT_WORD
    )

    private fun done() = resources.getString(R.string.celebration_done)

    private fun saved() = resources.getString(R.string.celebration_saved)

    private fun saving() = resources.getString(R.string.celebration_saving)

    private fun Float.toDp(): Float = this / InstrumentationRegistry.getInstrumentation()
        .targetContext.resources.displayMetrics.density
}
