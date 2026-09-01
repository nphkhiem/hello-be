package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That a lesson draws the activity it is actually on.
 *
 * Until this mapping existed the host drew listen-and-choose for all five families, so a child met
 * the same shape whatever the lesson asked of them. The `when` inside [LessonActivity] has no
 * `else`, so a sixth family will not compile until somebody decides what it looks like; these two
 * cover that it dispatches rather than merely compiles.
 */
@RunWith(AndroidJUnit4::class)
class LessonActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenASpeakingActivity_whenTheLessonDrawsIt_thenPipAsksForATurn() {
        setActivity(SayWithPipFixtures.pauseHalfway())

        composeTestRule.onNodeWithText(yourTurn()).assertIsDisplayed()
    }

    @Test
    fun givenAListeningActivity_whenTheLessonDrawsIt_thenItIsNotTheSpeakingOne() {
        // The half that would have passed before this mapping existed, and still has to.
        setActivity(LessonFixtures.answering())

        composeTestRule.onNodeWithText(yourTurn()).assertDoesNotExist()
        composeTestRule.onNodeWithText(LessonFixtures.answering().answers.first().label)
            .assertIsDisplayed()
    }

    private fun setActivity(state: LessonUiState) {
        composeTestRule.setContent {
            HelloBeTheme {
                LessonActivity(state = state, onAction = {})
            }
        }
    }

    private fun yourTurn(): String {
        val id = resources.getIdentifier(
            "say_your_turn",
            "string",
            InstrumentationRegistry.getInstrumentation().targetContext.packageName
        )
        return resources.getString(id)
    }
}
