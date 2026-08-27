package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import com.nphkhiem.englishforyourchildren.ui.tv.theme.helloBeTypography
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LetterAndSoundTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // The name is fixed by the task definition in TASKS.md, so it is kept verbatim rather than
    // shortened to fit the column limit.
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun givenLowercasePrompt_whenRendered_thenGlyphUsesLearningGlyphStyle() {
        // The fixture stores the letter lowercase. Both cases must appear regardless, drawn at the
        // learning-glyph size, which is what makes the letter dominate without the card borrowing
        // the focus grammar's gold.
        setActivity(LetterAndSoundFixtures.answering())

        // The style the text was actually laid out with, not a proxy for it. Compose exposes it
        // through the GetTextLayoutResult action, so this asserts the glyph rather than asserting
        // that something large happens to be on screen.
        val expected = helloBeTypography().learningGlyph
        val layouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(PAIR).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)

        val style = layouts.single().layoutInput.style
        assertThat(style.fontSize).isEqualTo(expected.fontSize)
        assertThat(style.fontWeight).isEqualTo(expected.fontWeight)
    }

    @Test
    fun givenALowercaseLetter_whenRendered_thenBothCasesAppearAndDiffer() {
        setActivity(LetterAndSoundFixtures.answering())

        composeTestRule.onNodeWithText(PAIR).assertIsDisplayed()
        assertThat(PAIR).isEqualTo("A a")
        assertThat(PAIR.first()).isNotEqualTo(PAIR.last())
    }

    @Test
    fun givenTheLetterCard_whenDpadMoves_thenFocusCannotReachIt() {
        setActivity(LetterAndSoundFixtures.answering())

        composeTestRule.onNodeWithText(APPLE).requestFocus()
        composeTestRule.onNodeWithText(APPLE).performKeyInput { pressKey(Key.DirectionLeft) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(APPLE).assertIsFocused()
    }

    @Test
    fun givenTheActivityAppears_whenFocusEnters_thenTheFirstChoiceTakesItNotTheCorrectOne() {
        setActivity(LetterAndSoundFixtures.answering())

        composeTestRule.onNodeWithText(APPLE).assertIsFocused()
    }

    @Test
    fun givenLetterAndSound_whenTheChoicesAreRead_thenTheirWordsAreDrawn() {
        // Unlike picture matching, the prompt asks about a sound rather than naming the target,
        // so a caption gives nothing away and the words help.
        setActivity(LetterAndSoundFixtures.answering())

        listOf(APPLE, CAT, SUN).forEach { word ->
            composeTestRule.onNodeWithText(word).assertIsDisplayed()
        }
    }

    @Test
    fun givenAChoiceIsChosen_whenSelectIsPressed_thenExactlyOneActionCarriesThatId() {
        val actions = mutableListOf<LessonAction>()
        setActivity(LetterAndSoundFixtures.answering(), onAction = { actions += it })

        composeTestRule.onNodeWithText(CAT).requestFocus()
        composeTestRule.onNodeWithText(CAT).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LessonAction.AnswerChosen(CAT))
    }

    @Test
    fun givenThreeChoices_whenTheBoardIsDrawn_thenNoneIsBelowTheChildMinimum() {
        setActivity(LetterAndSoundFixtures.answering())

        listOf(APPLE, CAT, SUN).forEach { word ->
            val card = composeTestRule.onNodeWithText(word).getUnclippedBoundsInRoot()

            assertThat((card.bottom - card.top).value)
                .isAtLeast(HelloBeLayout.childChoiceMinHeight.value)
        }
    }

    @Test
    fun givenNoLetter_whenTheBoardIsDrawn_thenTheChoicesStillStand() {
        setActivity(LetterAndSoundFixtures.answering().copy(learningObject = null))

        composeTestRule.onNodeWithText(PAIR).assertDoesNotExist()
        composeTestRule.onNodeWithText(APPLE).assertIsDisplayed()
    }

    private fun setActivity(state: LessonUiState, onAction: (LessonAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                LetterAndSoundActivity(state = state, onAction = onAction)
            }
        }
    }

    private companion object {
        const val PAIR = "A a"
        const val APPLE = LetterAndSoundFixtures.APPLE
        const val CAT = LetterAndSoundFixtures.CAT
        const val SUN = LetterAndSoundFixtures.SUN
    }
}
