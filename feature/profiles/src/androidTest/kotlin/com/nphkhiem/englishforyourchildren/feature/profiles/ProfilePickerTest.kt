package com.nphkhiem.englishforyourchildren.feature.profiles

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenARememberedChild_whenThePickerOpens_thenTheirCardHasFocus() {
        // The whole point of remembering: a returning child presses Select once.
        setPicker(ProfilePickerFixtures.twoProfiles())

        composeTestRule.onNodeWithText(MINH).assertIsFocused()
    }

    @Test
    fun givenNoChildrenYet_whenThePickerOpens_thenAddChildHasFocus() {
        setPicker(ProfilePickerFixtures.empty())

        composeTestRule.onNodeWithText(addChild()).assertIsFocused()
    }

    @Test
    fun givenTheRememberedChildCannotBeOpened_whenThePickerOpens_thenFocusMovesOn() {
        // Focus must never land on something that cannot be chosen.
        setPicker(ProfilePickerFixtures.rememberedUnavailable())

        composeTestRule.onNodeWithText(LAN).assertIsFocused()
    }

    @Test
    fun givenAChildIsChosen_whenSelectIsPressed_thenExactlyOneActionCarriesTheirId() {
        val actions = mutableListOf<ProfileAction>()
        setPicker(ProfilePickerFixtures.twoProfiles(), onAction = { actions += it })

        composeTestRule.onNodeWithText(LAN).requestFocus()
        composeTestRule.onNodeWithText(LAN).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ProfileAction.ProfileChosen(ProfilePickerFixtures.LAN))
    }

    @Test
    fun givenRoomForAnother_whenAddChildIsPressed_thenOneAddRequestIsMade() {
        val actions = mutableListOf<ProfileAction>()
        setPicker(ProfilePickerFixtures.twoProfiles(), onAction = { actions += it })

        composeTestRule.onNodeWithText(addChild()).requestFocus()
        composeTestRule.onNodeWithText(addChild()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ProfileAction.AddProfileRequested)
    }

    @Test
    fun givenFourChildren_whenAddChildIsReached_thenItExplainsItselfAndDoesNothing() {
        // Unavailable rather than hidden: a control that vanishes explains nothing, and the tokens
        // forbid a lock metaphor.
        val actions = mutableListOf<ProfileAction>()
        setPicker(ProfilePickerFixtures.full(), onAction = { actions += it })

        composeTestRule.onNodeWithText(addChild()).requestFocus()
        composeTestRule.onNodeWithText(addChild()).assertIsFocused()
        composeTestRule.onNodeWithText(addChild()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenAnUnavailableChild_whenTheirCardIsPressed_thenNothingIsChosen() {
        val actions = mutableListOf<ProfileAction>()
        setPicker(ProfilePickerFixtures.rememberedUnavailable(), onAction = { actions += it })

        composeTestRule.onNodeWithText(MINH).requestFocus()
        composeTestRule.onNodeWithText(MINH).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenTheRow_whenTheChildMovesUp_thenTheGrownUpEntryIsReachable() {
        val actions = mutableListOf<ProfileAction>()
        setPicker(ProfilePickerFixtures.twoProfiles(), onAction = { actions += it })

        composeTestRule.onNodeWithText(caregiver()).requestFocus()
        composeTestRule.onNodeWithText(caregiver()).performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(ProfileAction.CaregiverEntryRequested)
    }

    @Test
    fun givenAnyProfileRow_whenDrawn_thenNoCardIsBelowTheChildMinimum() {
        setPicker(ProfilePickerFixtures.full())

        listOf(MINH, LAN).forEach { name ->
            val card = composeTestRule.onNodeWithText(name).getUnclippedBoundsInRoot()

            assertThat((card.bottom - card.top).value)
                .isAtLeast(HelloBeLayout.childChoiceMinHeight.value)
        }
    }

    @Test
    fun givenProfilesAreStillLoading_whenThePickerOpens_thenNoCardIsOffered() {
        setPicker(ProfilePickerFixtures.loading())

        composeTestRule.onNodeWithText(MINH).assertDoesNotExist()
        composeTestRule.onNodeWithText(addChild()).assertDoesNotExist()
    }

    @Test
    fun givenTwoChildren_whenTheirCardsAreRead_thenTheProgressLineSupportsTheCaregiver() {
        setPicker(ProfilePickerFixtures.twoProfiles())

        composeTestRule.onNodeWithText("Continue learning").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 adventures").assertIsDisplayed()
    }

    private fun setPicker(state: ProfilePickerUiState, onAction: (ProfileAction) -> Unit = {}) {
        composeTestRule.setContent {
            HelloBeTheme {
                ProfilePickerScreen(state = state, onAction = onAction)
            }
        }
    }

    private fun addChild() = resources.getString(R.string.picker_add_child)

    private fun caregiver() = resources.getString(R.string.picker_caregiver)

    private companion object {
        const val MINH = "Minh"
        const val LAN = "Lan"
    }
}
