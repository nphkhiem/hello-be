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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaregiverOverviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

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
    fun givenTheFullestOverview_whenItIsDrawn_thenNothingSitsBelowTheStage() {
        // Nothing on this panel is focusable, so a scrolling container could never be moved by a
        // remote and anything below the fold would be unreachable rather than merely unseen. The
        // panel does not scroll, so it has to fit, and the fullest state is the one that proves it.
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

        val lastLine = composeTestRule
            .onNodeWithText(CaregiverFixtures.LONG_HINT)
            .getUnclippedBoundsInRoot()

        assertThat(lastLine.bottom.value).isAtMost(HelloBeLayout.referenceHeight.value)
    }

    private fun setOverview(state: CaregiverOverviewUiState) {
        composeTestRule.setContent {
            HelloBeTheme {
                CaregiverOverviewScreen(state = state)
            }
        }
    }

    private fun title() = resources.getString(R.string.overview_title, CaregiverFixtures.PROFILE)

    private fun localNote() = resources.getString(R.string.overview_local_note)

    private fun suggestionHeading() = resources.getString(R.string.overview_suggestion)

    private fun newProfileTitle() = resources.getString(R.string.overview_new_profile_title)

    private fun nothingRecentTitle() = resources.getString(R.string.overview_nothing_recent_title)

    private fun pendingSave() = resources.getString(R.string.overview_pending_save)
}
