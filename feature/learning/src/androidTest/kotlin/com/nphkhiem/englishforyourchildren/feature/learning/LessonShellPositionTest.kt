package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That the prompt sits in the same place whichever activity family is asking.
 *
 * The brief's contract for the lesson shell: prompt, Pip, replay, captions, progress and answer area
 * keep predictable positions across all five families, so a child learns the stage once. The header
 * and Pip are drawn by the shared scaffold and never moved. The prompt is drawn by each family inside
 * its own centred column, so it landed wherever that family's answers pushed it.
 */
@RunWith(AndroidJUnit4::class)
class LessonShellPositionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenEveryActivityFamily_whenItAsks_thenThePromptSitsInTheSamePlace() {
        val families = listOf(
            "listen and choose" to LessonFixtures.answering(),
            "picture match" to PictureMatchingFixtures.answering(),
            "letter and sound" to LetterAndSoundFixtures.answering(),
            "say with Pip" to SayWithPipFixtures.modelling(),
            "review" to ReviewFixtures.answering()
        )
        var state by mutableStateOf(families.first().second)
        composeTestRule.setContent { HelloBeTheme { LessonActivity(state = state, onAction = {}) } }

        val tops = families.map { (name, family) ->
            state = family
            composeTestRule.waitForIdle()
            val top = composeTestRule.onNodeWithText(family.prompt)
                .getUnclippedBoundsInRoot().top.value
            name to top
        }

        val (firstName, firstTop) = tops.first()
        tops.drop(1).forEach { (name, top) ->
            assertWithMessage("prompt top for $name against $firstName")
                .that(top).isWithin(TOLERANCE_DP).of(firstTop)
        }
    }

    private companion object {
        /** Sub-pixel rounding only. Anything larger is the prompt moving. */
        const val TOLERANCE_DP = 0.5f
    }
}
