package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
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
class StopForNowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenLessonBackPressed_whenDialogOpens_thenKeepLearningHasInitialFocus() {
        // A child pressing Back must never have the next press end the lesson.
        setLesson(LessonFixtures.stoppingForNow())

        composeTestRule.onNodeWithText(keepLearning()).assertIsFocused()
    }

    @Test
    fun givenProgressIsSaved_whenTheDialogOpens_thenItUsesTheApprovedSavedCopy() {
        setLesson(LessonFixtures.stoppingForNow())

        composeTestRule.onNodeWithText(title()).assertIsDisplayed()
        composeTestRule.onNodeWithText(savedDescription()).assertIsDisplayed()
        composeTestRule.onNodeWithText(pendingDescription()).assertDoesNotExist()
    }

    @Test
    fun givenProgressIsPending_whenTheDialogOpens_thenItNeverClaimsPipRemembers() {
        setLesson(LessonFixtures.stoppingForNowPendingSave())

        composeTestRule.onNodeWithText(pendingDescription()).assertIsDisplayed()
        composeTestRule.onNodeWithText(savedDescription()).assertDoesNotExist()
    }

    @Test
    fun givenTheDialogIsClosed_whenBackIsPressed_thenTheLessonAsksToStopRatherThanLeaving() {
        val actions = mutableListOf<LessonAction>()
        setLesson(LessonFixtures.answering(), onAction = { actions += it })

        pressBack()

        assertThat(actions).containsExactly(LessonAction.BackRequested)
    }

    @Test
    fun givenTheDialogIsOpen_whenBackIsPressed_thenItTakesTheSafePathAndCannotStackAnother() {
        val actions = mutableListOf<LessonAction>()
        setLesson(LessonFixtures.stoppingForNow(), onAction = { actions += it })

        pressBack()

        // Back and the safe action are the same press. A second BackRequested here would open a
        // dialog on top of a dialog.
        assertThat(actions).containsExactly(LessonAction.KeepLearningRequested)
    }

    @Test
    fun givenTheDialogIsOpen_whenEachActionIsChosen_thenExactlyOneMatchingActionIsEmitted() {
        val actions = mutableListOf<LessonAction>()
        setLesson(LessonFixtures.stoppingForNow(), onAction = { actions += it })

        composeTestRule.onNodeWithText(keepLearning()).performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stopForNow()).requestFocus()
        composeTestRule.onNodeWithText(stopForNow()).performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(
            LessonAction.KeepLearningRequested,
            LessonAction.StopForNowConfirmed
        ).inOrder()
    }

    @Test
    fun givenTheDialogIsOpen_whenTheChildPressesAround_thenFocusCannotReachTheStageBehind() {
        // The scrim says the lesson is unreachable, so D-pad movement has to agree with it.
        setLesson(LessonFixtures.stoppingForNow())

        listOf(Key.DirectionUp, Key.DirectionUp, Key.DirectionLeft, Key.DirectionRight)
            .forEach { key ->
                composeTestRule.onNodeWithText(keepLearning()).performKeyInput { pressKey(key) }
                composeTestRule.waitForIdle()
            }

        composeTestRule.onNodeWithText(replay()).assertIsNotFocused()
    }

    @Test
    fun givenTheChildHadMovedAlongTheAnswers_whenTheDialogCloses_thenFocusReturnsThere() {
        // Not the first answer: the one they were about to choose. Coming back somewhere else
        // makes declining to stop cost them their place.
        val state = LessonFixtures.answering()
        var open by mutableStateOf(false)

        composeTestRule.setContent {
            HelloBeTheme {
                ListenAndChooseActivity(
                    state = state.copy(stopForNowVisible = open),
                    onAction = { if (it is LessonAction.KeepLearningRequested) open = false }
                )
            }
        }

        composeTestRule.onNodeWithText(BED).requestFocus()
        composeTestRule.onNodeWithText(BED).assertIsFocused()

        open = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(keepLearning()).assertIsFocused()

        open = false
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(BED).assertIsFocused()
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
            )
        composeTestRule.waitForIdle()
    }

    private fun setLesson(state: LessonUiState, onAction: (LessonAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                ListenAndChooseActivity(state = state, onAction = onAction)
            }
        }
    }

    private fun title() = resources.getString(R.string.stop_for_now_title)

    private fun savedDescription() = resources.getString(R.string.stop_for_now_saved)

    private fun pendingDescription() = resources.getString(R.string.stop_for_now_pending)

    private fun keepLearning() = resources.getString(R.string.stop_for_now_keep)

    private fun stopForNow() = resources.getString(R.string.stop_for_now_stop)

    private fun replay() = resources.getString(R.string.lesson_replay)

    private companion object {
        const val BED = "bed"
    }
}
