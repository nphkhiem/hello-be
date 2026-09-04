package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdultGateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenTheGate_whenItOpens_thenFocusRestsOnAnAnswerThatIsNotTheCorrectOne() {
        setGate(CaregiverFixtures.gate())

        composeTestRule.onNodeWithText(CaregiverFixtures.WRONG_LOW).assertIsFocused()
        composeTestRule.onNodeWithText(CaregiverFixtures.CORRECT).assertIsNotFocused()
    }

    @Test
    fun givenTheCorrectAnswerIsFirst_whenTheGateOpens_thenFocusStepsPastIt() {
        // The case a fixed "focus the first control" would have opened the gate on one press.
        setGate(CaregiverFixtures.gateCorrectFirst())

        composeTestRule.onNodeWithText(CaregiverFixtures.CORRECT).assertIsNotFocused()
        composeTestRule.onNodeWithText(CaregiverFixtures.WRONG_LOW).assertIsFocused()
    }

    @Test
    fun givenSelectIsPressedRepeatedly_whenFocusNeverMoves_thenTheCorrectAnswerIsNeverSent() {
        // The stop condition, asserted as the behaviour rather than as the arrangement behind it.
        val actions = mutableListOf<AdultGateAction>()
        setGate(CaregiverFixtures.gate(), onAction = { actions += it })

        repeat(POUNDING) {
            composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
            composeTestRule.waitForIdle()
        }

        assertThat(actions).isNotEmpty()
        assertThat(actions).doesNotContain(AdultGateAction.AnswerChosen(CORRECT_INDEX))
    }

    @Test
    fun givenAnAnswerIsPressed_whenItIsReported_thenItIsNamedByPosition() {
        val actions = mutableListOf<AdultGateAction>()
        setGate(CaregiverFixtures.gate(), onAction = { actions += it })

        // Focus has to move first, because a key press goes to whatever holds focus rather than
        // to the node it is addressed to. That is the same mechanism the gate relies on.
        composeTestRule.onNodeWithText(CaregiverFixtures.WRONG_HIGH).requestFocus()
        composeTestRule.onNodeWithText(CaregiverFixtures.WRONG_HIGH).performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(AdultGateAction.AnswerChosen(2))
    }

    @Test
    fun givenTheGateIsRead_whenTheAnswersAreCompared_thenTheCorrectOneLooksLikeTheOthers() {
        // Nothing may single the correct answer out. Its bounds are the tell that would be easiest
        // to get wrong, since a taller or wider control is a visible hint.
        setGate(CaregiverFixtures.gate())

        val low = composeTestRule.onNodeWithText(CaregiverFixtures.WRONG_LOW)
            .fetchSemanticsNode().size
        val correct = composeTestRule.onNodeWithText(CaregiverFixtures.CORRECT)
            .fetchSemanticsNode().size
        val high = composeTestRule.onNodeWithText(CaregiverFixtures.WRONG_HIGH)
            .fetchSemanticsNode().size

        assertThat(correct.height).isEqualTo(low.height)
        assertThat(correct.height).isEqualTo(high.height)
        // Width too. Answers sized to their own digits would let the correct one be found by
        // shape on a screen where nothing about it may differ.
        //
        // Within a pixel rather than exactly equal: three equal weights across an odd number of
        // pixels leave one column a pixel wider, and asserting exact equality failed on that
        // rounding while proving nothing about whether an answer stands out.
        assertThat(correct.width).isWithin(ROUNDING).of(low.width)
        assertThat(correct.width).isWithin(ROUNDING).of(high.width)
    }

    @Test
    fun givenAPreviousWrongAnswer_whenTheGateIsRead_thenItIsNeutralAndTheQuestionHasChanged() {
        setGate(CaregiverFixtures.gateAfterWrongAnswer())

        composeTestRule.onNodeWithText(retry()).assertIsDisplayed()
        composeTestRule.onNodeWithText(CaregiverFixtures.SECOND_QUESTION).assertIsDisplayed()
        composeTestRule.onNodeWithText(CaregiverFixtures.QUESTION).assertDoesNotExist()
    }

    @Test
    fun givenAChallengeThatCannotBeStoodOn_whenTheGateOpens_thenThereIsNothingToPress() {
        // Fails closed: its only answer would be the correct one, so no answer is drawn at all.
        val actions = mutableListOf<AdultGateAction>()
        setGate(CaregiverFixtures.gateUnusable(), onAction = { actions += it })

        composeTestRule.onNodeWithText(unavailable()).assertIsDisplayed()
        composeTestRule.onNodeWithText(CaregiverFixtures.CORRECT).assertDoesNotExist()
        // And nothing still asks for a solution to a question that is not there.
        composeTestRule.onNodeWithText(instruction()).assertDoesNotExist()

        repeat(POUNDING) {
            composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
            composeTestRule.waitForIdle()
        }

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenTheGate_whenItIsRead_thenItExplainsItselfAndBackInBothLanguages() {
        // Bilingual, per the approved draft, which is what makes this a caregiver surface. It used
        // to be bilingual because the strings themselves carried both languages joined by a middle
        // dot, which meant a caregiver who reads only one of them could never be given just that
        // one. It is now the default of `LocalCaregiverLanguage`, and the joining happens here.
        setGate(CaregiverFixtures.gate())

        composeTestRule.onNodeWithText(title()).assertIsDisplayed()
        composeTestRule.onNodeWithText(backHint()).assertIsDisplayed()
        assertThat(title()).contains("·")
        assertThat(backHint()).contains("·")
    }

    @Test
    fun givenACaregiverWhoReadsOneLanguage_whenTheGateIsShown_thenItSpeaksOnlyThatOne() {
        // The point of the setting. English here, because Vietnamese for this screen exists; a
        // string with no translation yet would fall back to English and prove nothing.
        composeTestRule.setContent {
            HelloBeTheme {
                CompositionLocalProvider(
                    LocalCaregiverLanguage provides CaregiverLanguage.ENGLISH
                ) {
                    AdultGateScreen(state = CaregiverFixtures.gate(), onAction = {})
                }
            }
        }

        composeTestRule.onNodeWithText(ENGLISH_TITLE).assertIsDisplayed()
    }

    private fun title() = context.caregiverText(CaregiverLanguage.BOTH, R.string.gate_title)

    private fun backHint() = context.caregiverText(CaregiverLanguage.BOTH, R.string.gate_back_hint)

    private fun setGate(state: AdultGateUiState, onAction: (AdultGateAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                AdultGateScreen(state = state, onAction = onAction)
            }
        }
    }

    private fun instruction() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.gate_instruction)

    private fun retry() = context.caregiverText(CaregiverLanguage.BOTH, R.string.gate_retry)

    private fun unavailable() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.gate_unavailable)

    private companion object {
        const val ENGLISH_TITLE = "Grown-ups only"
        const val POUNDING = 8

        /** One pixel of weight-distribution rounding, far below anything an eye resolves. */
        const val ROUNDING = 1
        const val CORRECT_INDEX = 1
    }
}
