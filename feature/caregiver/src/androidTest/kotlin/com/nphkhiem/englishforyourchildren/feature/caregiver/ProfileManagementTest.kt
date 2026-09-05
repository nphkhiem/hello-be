package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
class ProfileManagementTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Strings are read the way the screens read them, in both languages. */
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenTwoProfiles_whenTheListIsRead_thenEachChildIsNamedWithTheirOwnDetail() {
        setManagement(CaregiverFixtures.profiles())

        composeTestRule.onNodeWithText(CaregiverFixtures.SECOND_PROFILE).assertIsDisplayed()
        composeTestRule.onNodeWithText(CaregiverFixtures.LAN_DETAIL).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(CaregiverFixtures.MINH_DETAIL)
            .fetchSemanticsNodes()
            .also { assertThat(it).isNotEmpty() }
    }

    @Test
    fun givenTwoProfiles_whenCapacityIsRead_thenItSaysHowManyOfFourAreUsed() {
        setManagement(CaregiverFixtures.profiles())

        composeTestRule.onNodeWithText(capacity(2)).assertIsDisplayed()
    }

    @Test
    fun givenTheLimitIsReached_whenAddingIsAttempted_thenItSaysWhyAndAsksNothing() {
        // Focusable so the reason can be read, never clickable. The same rule the profile picker
        // applies to its own full state, arrived at independently rather than imported.
        val actions = mutableListOf<ProfileManagementAction>()
        setManagement(CaregiverFixtures.profilesAtLimit(), onAction = { actions += it })

        // Focus then a key press, not performClick: a TV surface answers key events, and a touch
        // click is a no-op on it, so asserting emptiness after one would have proved nothing.
        composeTestRule.onNodeWithText(add()).requestFocus()
        composeTestRule.onNodeWithText(add()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
        composeTestRule.onNode(hasStateDescription(atLimit())).assertIsDisplayed()
        composeTestRule.onNodeWithText(capacity(4)).assertIsDisplayed()
    }

    @Test
    fun givenRoomForAnotherChild_whenAddIsPressed_thenItAsksToAddOne() {
        val actions = mutableListOf<ProfileManagementAction>()
        setManagement(CaregiverFixtures.profiles(), onAction = { actions += it })

        composeTestRule.onNodeWithText(add()).requestFocus()
        composeTestRule.onNodeWithText(add()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ProfileManagementAction.AddProfileRequested)
    }

    @Test
    fun givenAnotherProfile_whenItIsPressed_thenItAsksToSelectThatChild() {
        val actions = mutableListOf<ProfileManagementAction>()
        setManagement(CaregiverFixtures.profiles(), onAction = { actions += it })

        composeTestRule.onNodeWithText(CaregiverFixtures.SECOND_PROFILE).requestFocus()
        composeTestRule.onNodeWithText(CaregiverFixtures.SECOND_PROFILE)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ProfileManagementAction.ProfileSelected("lan"))
    }

    @Test
    fun givenTheSelectedChild_whenEachActionIsPressed_thenItNamesThatChild() {
        val actions = mutableListOf<ProfileManagementAction>()
        setManagement(CaregiverFixtures.profiles(), onAction = { actions += it })

        listOf(editName(), changePicture(), reset(), delete()).forEach { label ->
            composeTestRule.onNodeWithText(label).requestFocus()
            composeTestRule.onNodeWithText(label).performKeyInput { pressKey(Key.DirectionCenter) }
            composeTestRule.waitForIdle()
        }

        assertThat(actions).containsExactly(
            ProfileManagementAction.EditNameRequested("minh"),
            ProfileManagementAction.ChangeAvatarRequested("minh"),
            ProfileManagementAction.ResetProgressRequested("minh"),
            ProfileManagementAction.DeleteProfileRequested("minh")
        ).inOrder()
    }

    @Test
    fun givenTheSecondChildIsSelected_whenAnActionIsPressed_thenItNamesThatChildAndNotTheFirst() {
        // The first fixture selects the first child, so an action hardcoded to that child would
        // have looked correct. This one selects the second.
        val actions = mutableListOf<ProfileManagementAction>()
        setManagement(CaregiverFixtures.profilesSecondSelected(), onAction = { actions += it })

        composeTestRule.onNodeWithText(reset()).requestFocus()
        composeTestRule.onNodeWithText(reset()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ProfileManagementAction.ResetProgressRequested("lan"))
    }

    @Test
    fun givenTheDetailPane_whenItIsMeasured_thenDeleteIsSeparatedFromTheOtherActions() {
        // "Visually and spatially separated", per the information architecture. A caregiver
        // reaching for Reset progress must not be one press away from removing the child.
        setManagement(CaregiverFixtures.profiles())

        val edit = composeTestRule.onNodeWithText(editName()).getUnclippedBoundsInRoot()
        val picture = composeTestRule.onNodeWithText(changePicture()).getUnclippedBoundsInRoot()
        val resetRow = composeTestRule.onNodeWithText(reset()).getUnclippedBoundsInRoot()
        val deleteRow = composeTestRule.onNodeWithText(delete()).getUnclippedBoundsInRoot()

        val betweenActions = picture.top.value - edit.bottom.value
        val beforeDelete = deleteRow.top.value - resetRow.bottom.value

        assertThat(beforeDelete).isGreaterThan(betweenActions)
    }

    @Test
    fun givenTheSelectionHasGone_whenTheDetailIsDrawn_thenItShowsAChildThatExists() {
        // A caregiver can delete the profile that was selected. The detail pane must not offer
        // actions for a child who is no longer here.
        setManagement(CaregiverFixtures.profilesStaleSelection())

        composeTestRule
            .onAllNodesWithText(CaregiverFixtures.PROFILE)
            .fetchSemanticsNodes()
            .also { assertThat(it).isNotEmpty() }
        composeTestRule.onNodeWithText(delete()).assertIsDisplayed()
    }

    @Test
    fun givenNoProfilesAtAll_whenTheScreenIsRead_thenItExplainsRatherThanShowingActions() {
        setManagement(CaregiverFixtures.profilesNone())

        composeTestRule.onNodeWithText(emptyTitle()).assertIsDisplayed()
        composeTestRule.onNodeWithText(delete()).assertDoesNotExist()
        composeTestRule.onNodeWithText(reset()).assertDoesNotExist()
        composeTestRule.onNodeWithText(capacity(0)).assertIsDisplayed()
    }

    @Test
    fun givenChangesAreNotSaving_whenTheScreenIsRead_thenItSaysSo() {
        setManagement(CaregiverFixtures.profilesPersistenceFailed())

        composeTestRule.onNodeWithText(persistenceFailed()).assertIsDisplayed()
    }

    @Test
    fun givenOneProfile_whenTheScreenIsRead_thenItStillOffersEveryAction() {
        // The brief's floor. A single child is not a degenerate case with controls missing.
        setManagement(CaregiverFixtures.profilesOnlyOne())

        composeTestRule.onNodeWithText(delete()).assertIsDisplayed()
        composeTestRule.onNodeWithText(add()).assertIsDisplayed()
        composeTestRule.onNodeWithText(capacity(1)).assertIsDisplayed()
    }

    private fun setManagement(
        state: ProfileManagementUiState,
        onAction: (ProfileManagementAction) -> Unit = {}
    ) {
        composeTestRule.setContent {
            HelloBeTheme {
                ProfileManagementScreen(state = state, onAction = onAction)
            }
        }
    }

    private fun capacity(used: Int) =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.profiles_capacity, used, 4)

    private fun add() = context.caregiverText(CaregiverLanguage.BOTH, R.string.profiles_add)

    private fun atLimit() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.profiles_at_limit)

    private fun editName() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.profiles_edit_name)

    private fun changePicture() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.profiles_change_picture)

    private fun reset() = context.caregiverText(CaregiverLanguage.BOTH, R.string.profiles_reset)

    private fun delete() = context.caregiverText(CaregiverLanguage.BOTH, R.string.profiles_delete)

    private fun emptyTitle() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.profiles_empty_title)

    private fun persistenceFailed() =
        context.caregiverText(CaregiverLanguage.BOTH, R.string.profiles_persistence_failed)
}
