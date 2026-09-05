package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaregiverOverviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Strings are read the way the screens read them, in both languages. */
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenAWeekWithPractice_whenTheOverviewIsRead_thenItDescribesItInPlainWords() {
        setOverview(CaregiverFixtures.overview())

        composeTestRule.onNodeWithText(title()).assertIsDisplayed()
        composeTestRule.onNodeWithText(CaregiverFixtures.NOT_A_SCORE).assertIsDisplayed()
        composeTestRule.onNodeWithText(CaregiverFixtures.PERIOD).assertIsDisplayed()
    }

    @Test
    fun givenTheOverview_whenItIsRead_thenItSaysTheDataStaysOnThisTelevision() {
        // The local-data explanation the information architecture asks for, and the one claim on
        // this screen that a caregiver cannot verify for themselves.
        setOverview(CaregiverFixtures.overview())

        composeTestRule.onNodeWithText(localNote()).assertIsDisplayed()
    }

    @Test
    fun givenMoreHistoryThanFits_whenTheOverviewIsDrawn_thenItRefusesToGrow() {
        // The stop condition: no infinite history, and no fourth metric turning this into a
        // dashboard. Both bounds are the screen's, not the caller's.
        setOverview(CaregiverFixtures.overviewLongHistory())

        composeTestRule.onNodeWithText(CaregiverFixtures.OVERFLOWED_WORD).assertDoesNotExist()
        composeTestRule.onNodeWithText("Should not appear").assertDoesNotExist()
        CaregiverFixtures.RECENT_WORDS.forEach { word ->
            composeTestRule.onNodeWithText(word).assertIsDisplayed()
        }
    }

    @Test
    fun givenANewProfile_whenTheOverviewIsRead_thenItExplainsRatherThanShowingZeroes() {
        setOverview(CaregiverFixtures.overviewNewProfile())

        composeTestRule.onNodeWithText(newProfileTitle()).assertIsDisplayed()
        composeTestRule.onNodeWithText(CaregiverFixtures.NOT_A_SCORE).assertDoesNotExist()
        CaregiverFixtures.RECENT_WORDS.forEach { word ->
            composeTestRule.onNodeWithText(word).assertDoesNotExist()
        }
    }

    @Test
    fun givenAQuietWeek_whenTheOverviewIsRead_thenItSaysSoWithoutCallingItNew() {
        // A profile with history behind it is not a new profile, and telling a caregiver it is
        // would be untrue about their child.
        setOverview(CaregiverFixtures.overviewNothingRecent())

        composeTestRule.onNodeWithText(nothingRecentTitle()).assertIsDisplayed()
        composeTestRule.onNodeWithText(newProfileTitle()).assertDoesNotExist()
    }

    @Test
    fun givenASessionNotWrittenDownYet_whenTheOverviewIsRead_thenItSaysSo() {
        setOverview(CaregiverFixtures.overviewPendingSave())

        composeTestRule.onNodeWithText(pendingSave()).assertIsDisplayed()
    }

    @Test
    fun givenNoSuggestionAvailable_whenTheOverviewIsDrawn_thenTheRestStillStands() {
        setOverview(CaregiverFixtures.overviewNoSuggestion())

        composeTestRule.onNodeWithText(suggestionHeading()).assertDoesNotExist()
        composeTestRule.onNodeWithText(CaregiverFixtures.NOT_A_SCORE).assertIsDisplayed()
    }

    @Test
    fun givenASuggestion_whenItIsRead_thenItIsSomethingToDoAndNotSomewhereToGo() {
        setOverview(CaregiverFixtures.overview())

        composeTestRule.onNodeWithText(CaregiverFixtures.SUGGESTION).assertIsDisplayed()
        composeTestRule.onNodeWithText(CaregiverFixtures.SUGGESTION).assertHasNoClickAction()
    }

    @Test
    fun givenASuggestionOnAFullPanel_whenACaregiverLooksForIt_thenTheyCanReachIt() {
        // Everything here carries two languages, so a real suggestion under a real summary is
        // taller than the stage. What it may never be is unreachable: the instruction is the whole
        // of what a caregiver can act on, and it used to lose its height and be clipped away with
        // nothing to say that it had been.
        composeTestRule.setContent {
            HelloBeTheme {
                Box(
                    modifier = Modifier.size(
                        HelloBeLayout.referenceWidth,
                        HelloBeLayout.referenceHeight
                    )
                ) {
                    CaregiverOverviewScreen(state = CaregiverFixtures.overviewLongCopy())
                }
            }
        }

        composeTestRule.onNodeWithText(CaregiverFixtures.LONG_HINT)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun givenAnotherProfile_whenTheOverviewIsDrawn_thenEveryLineNamesThatProfile() {
        setOverview(CaregiverFixtures.overviewLongCopy())

        composeTestRule
            .onAllNodesWithText(CaregiverFixtures.LONG_PROFILE, substring = true)
            .fetchSemanticsNodes()
            .also { assertThat(it).isNotEmpty() }
        composeTestRule
            .onAllNodesWithText(CaregiverFixtures.PROFILE, substring = true)
            .fetchSemanticsNodes()
            .also { assertThat(it).isEmpty() }
    }

    @Test
    fun givenLongBilingualCopy_whenTheOverviewIsDrawn_thenNothingLeavesTheStage() {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(
                    modifier = Modifier.size(
                        HelloBeLayout.referenceWidth,
                        HelloBeLayout.referenceHeight
                    )
                ) {
                    CaregiverOverviewScreen(state = CaregiverFixtures.overviewLongCopy())
                }
            }
        }

        val hint = composeTestRule
            .onNodeWithText(CaregiverFixtures.LONG_HINT)
            .getUnclippedBoundsInRoot()

        assertThat(hint.right.value).isAtMost(HelloBeLayout.referenceWidth.value)
        assertThat(hint.left.value).isAtLeast(0f)
    }

    @Test
    fun givenTheFullestOverviewInsideItsChrome_whenACaregiverReadsDown_thenNothingIsStranded() {
        // This used to assert the opposite: that the panel fits the stage, because nothing on it
        // took focus and a scrolling container no remote can move strands whatever is below the
        // fold. It stopped fitting when the suggestion got a source, and it cannot be made to fit
        // again at caregiver density with every label in two languages. So the cards take focus
        // instead, and the rule this holds is the one that always mattered: a caregiver can reach
        // the last line.
        composeTestRule.setContent {
            HelloBeTheme {
                Box(
                    modifier = Modifier.size(
                        HelloBeLayout.referenceWidth,
                        HelloBeLayout.referenceHeight
                    )
                ) {
                    CaregiverScaffold(state = CaregiverFixtures.shell(), onAction = {}) {
                        CaregiverOverviewScreen(state = CaregiverFixtures.overviewLongCopy())
                    }
                }
            }
        }

        composeTestRule.onNodeWithText(CaregiverFixtures.LONG_HINT)
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setOverview(state: CaregiverOverviewUiState) {
        composeTestRule.setContent {
            HelloBeTheme {
                CaregiverOverviewScreen(state = state)
            }
        }
    }

    private fun title() = context.caregiverText(
        CaregiverLanguage.BOTH,
        R.string.overview_title,
        CaregiverFixtures.PROFILE
    )

    private fun localNote() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.overview_local_note)

    private fun suggestionHeading() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.overview_suggestion)

    private fun newProfileTitle() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.overview_new_profile_title)

    private fun nothingRecentTitle() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.overview_nothing_recent_title)

    private fun pendingSave() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.overview_pending_save)
}
