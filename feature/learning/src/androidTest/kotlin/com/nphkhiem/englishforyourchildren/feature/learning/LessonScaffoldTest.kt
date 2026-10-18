package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeThemeMode
import com.nphkhiem.englishforyourchildren.ui.tv.theme.helloBeColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenALesson_whenRendered_thenTheChildSeesWhereTheyAreAndWhatTheyAreDoing() {
        setScaffold(LessonFixtures.answering())

        composeTestRule.onNodeWithText(UNIT).assertIsDisplayed()
        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(POSITION).assertExists()
    }

    @Test
    fun givenThePromptIsPlaying_whenTheStageAppears_thenFocusRestsOnReplay() {
        setScaffold(LessonFixtures.prompting())

        composeTestRule.onNodeWithText(REPLAY).assertIsFocused()
    }

    @Test
    fun givenTheChildShouldBeChoosing_whenTheStageAppears_thenFocusIsHandedToTheActivity() {
        // Replay comes first in the header, so without entry focus being claimed the child would
        // start on it rather than on the answers they are meant to be choosing between.
        setScaffold(LessonFixtures.answering())

        composeTestRule.onNodeWithText(ACTIVITY_CONTROL).assertIsFocused()
        composeTestRule.onNodeWithText(REPLAY).assertIsNotFocused()
    }

    @Test
    fun givenSkipIsAlsoAvailable_whenTheStageAppears_thenReplayWinsEntryFocusOverIt() {
        // With sound broken the skip is focusable too, so this is the case that actually proves
        // entry focus is being claimed rather than falling to whatever happens to come first.
        setScaffold(LessonFixtures.audioUnavailable().copy(phase = LessonPhase.PROMPTING))

        composeTestRule.onNodeWithText(REPLAY).assertIsFocused()
        composeTestRule.onNodeWithText(SKIP).assertIsNotFocused()
    }

    @Test
    fun givenReplayIsPressed_whenItIsChosen_thenExactlyOneReplayActionIsEmitted() {
        val actions = mutableListOf<LessonAction>()
        setScaffold(LessonFixtures.answering(), onAction = { actions += it })

        // Replay is not the entry focus while answering, so focus it the way a child would
        // before pressing it.
        composeTestRule.onNodeWithText(REPLAY).requestFocus()
        composeTestRule.onNodeWithText(REPLAY).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LessonAction.ReplayRequested)
    }

    @Test
    fun givenAudioUnavailable_whenReplayIsFocused_thenUnavailableSemanticsAreAnnounced() {
        setScaffold(LessonFixtures.audioUnavailable())

        // Replay stays on screen and says why it cannot help, rather than vanishing and leaving a
        // child wondering where the sound went.
        composeTestRule.onNodeWithText(REPLAY).assertIsDisplayed()
        composeTestRule.onNodeWithText(REPLAY).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, REPLAY_UNAVAILABLE)
        )
        composeTestRule.onNodeWithText(SKIP).assertIsDisplayed()
    }

    @Test
    fun givenAudioWorks_whenRendered_thenNoSkipIsOfferedAsAGeneralEscape() {
        setScaffold(LessonFixtures.answering())

        composeTestRule.onNodeWithText(SKIP).assertDoesNotExist()
    }

    @Test
    fun givenProgressNotYetWritten_whenRendered_thenItSaysSoRatherThanClaimingSaved() {
        setScaffold(LessonFixtures.answering().copy(pendingSave = true))

        composeTestRule.onNodeWithText(PENDING_SAVE).assertIsDisplayed()
    }

    @Test
    fun givenTheLessonIsStillPreparing_whenRendered_thenTheStageWaitsCalmlyInsteadOfEmpty() {
        setScaffold(LessonFixtures.preparing())

        composeTestRule.onNodeWithContentDescription(PREPARING).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ACTIVITY).assertDoesNotExist()
    }

    @Test
    fun givenALessonOpensWhilePreparing_whenThePromptStarts_thenReplayIsDrawnFocused() {
        // The order a lesson really opens in: a stage with nothing in it yet, then the question.
        // A first lesson opened like this held focus on Replay without drawing it, so the child saw
        // nothing focused and their first press seemed to start somewhere else.
        val state = mutableStateOf(LessonUiMapper.preparing())
        val opened = mutableStateOf(false)
        composeTestRule.setContent {
            HelloBeTheme {
                Box(Modifier.testTag(STAGE)) {
                    if (opened.value) {
                        ListenAndChooseActivity(state = state.value, onAction = {})
                    } else {
                        HelloBeAction(label = OPEN, onClick = { opened.value = true })
                    }
                }
            }
        }
        composeTestRule.onNodeWithText(OPEN).requestFocus()
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { opened.value = true }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { state.value = LessonFixtures.prompting() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(REPLAY).assertIsFocused()
        val drawn = composeTestRule.onNodeWithTag(STAGE).captureToImage().toPixelMap()
        val focusFill = helloBeColors(HelloBeThemeMode.DAY).focusFill.toArgb()
        val focused = (0 until drawn.height step STRIDE).any { y ->
            (0 until drawn.width step STRIDE).any { x -> drawn[x, y].toArgb() == focusFill }
        }
        assertThat(focused).isTrue()
    }

    private fun setScaffold(state: LessonUiState, onAction: (LessonAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                LessonScaffold(state = state, onAction = onAction) { choiceFocus ->
                    Box(Modifier.fillMaxSize().testTag(ACTIVITY)) {
                        HelloBeAction(
                            label = ACTIVITY_CONTROL,
                            onClick = {},
                            modifier = Modifier.lessonChoice(
                                choiceFocus,
                                ACTIVITY_CONTROL,
                                entry = true
                            )
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val UNIT = "My Home"
        const val TITLE = "Listen and choose"
        const val POSITION = "Activity 2 of 4"
        const val REPLAY = "Replay"
        const val REPLAY_UNAVAILABLE = "Sound is not working"
        const val SKIP = "Skip this one"
        const val PENDING_SAVE = "Not saved yet"
        const val PREPARING = "Getting your question ready"
        const val ACTIVITY = "activity"
        const val ACTIVITY_CONTROL = "chair"
        const val STAGE = "stage"
        const val OPEN = "Lesson 1"
        const val STRIDE = 3
    }
}
