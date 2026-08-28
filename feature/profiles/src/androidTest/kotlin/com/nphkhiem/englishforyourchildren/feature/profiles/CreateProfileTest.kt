package com.nphkhiem.englishforyourchildren.feature.profiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
class CreateProfileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    // The name is fixed by the task definition in TASKS.md, so it is kept verbatim rather than
    // shortened to fit the column limit.
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun givenDefaultProfileDraft_whenUserSelectsAgeAndCreate_thenProfileActionContainsGeneratedNameAndAvatar() {
        // Two presses, counted rather than inspected: the age that is already focused, then create.
        val actions = mutableListOf<CreateProfileAction>()
        var state by mutableStateOf(CreateProfileFixtures.ready())

        composeTestRule.setContent {
            HelloBeTheme {
                CreateProfileScreen(
                    state = state,
                    onAction = { action ->
                        actions += action
                        if (action is CreateProfileAction.AgeChosen) {
                            state = state.copy(draft = state.draft.copy(age = action.age))
                        }
                    }
                )
            }
        }

        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).hasSize(2)
        assertThat(actions.first()).isEqualTo(CreateProfileAction.AgeChosen(3))
        val created = actions.last() as CreateProfileAction.CreateRequested
        assertThat(created.draft.nickname).isEqualTo(CreateProfileFixtures.NICKNAME)
        assertThat(created.draft.avatarId).isEqualTo(CreateProfileFixtures.TIGER)
        assertThat(created.draft.age).isEqualTo(3)
    }

    @Test
    fun givenTheScreenOpens_whenFocusEnters_thenTheFirstAgeHasIt() {
        setScreen(CreateProfileFixtures.ready())

        composeTestRule.onNodeWithText("3").assertIsFocused()
    }

    @Test
    fun givenAnAgeIsChosen_whenTheStateUpdates_thenFocusAdvancesToCreate() {
        setScreen(CreateProfileFixtures.ageChosen())

        composeTestRule.onNodeWithText(create()).assertIsFocused()
    }

    @Test
    fun givenNoAgeYet_whenCreateIsPressed_thenNothingIsCreatedAndItSaysWhy() {
        val actions = mutableListOf<CreateProfileAction>()
        setScreen(CreateProfileFixtures.ready(), onAction = { actions += it })

        composeTestRule.onNodeWithText(needsAge()).assertIsDisplayed()
        composeTestRule.onNodeWithText(create()).requestFocus()
        composeTestRule.onNodeWithText(create()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenTheTelevisionIsFull_whenCreateIsPressed_thenNothingIsCreated() {
        val actions = mutableListOf<CreateProfileAction>()
        setScreen(CreateProfileFixtures.capacityReached(), onAction = { actions += it })

        composeTestRule.onNodeWithText(create()).requestFocus()
        composeTestRule.onNodeWithText(create()).performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).isEmpty()
    }

    @Test
    fun givenFocusIsOnCreate_whenUpIsPressed_thenTheAgesAreReachedFirst() {
        // The ages sit between Create and personalization, and focus follows what the eye follows.
        setScreen(CreateProfileFixtures.ageChosen())

        composeTestRule.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("3").assertIsFocused()
    }

    @Test
    fun givenTheChooserIsOpen_whenAPictureIsChosen_thenOneActionCarriesItAndNoKeyboardWasNeeded() {
        val actions = mutableListOf<CreateProfileAction>()
        setScreen(CreateProfileFixtures.choosingAvatar(), onAction = { actions += it })

        composeTestRule.onNodeWithText(CreateProfileFixtures.RABBIT).requestFocus()
        composeTestRule.onNodeWithText(CreateProfileFixtures.RABBIT)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(
            CreateProfileAction.AvatarChosen(CreateProfileFixtures.RABBIT)
        )
    }

    @Test
    fun givenTheChangeNameControl_whenPressed_thenItAsksElsewhereRatherThanOpeningAField() {
        // Renaming needs a keyboard, and a keyboard needs a text field this project does not have
        // yet. The screen asks profile management to do it.
        val actions = mutableListOf<CreateProfileAction>()
        setScreen(CreateProfileFixtures.ready(), onAction = { actions += it })

        composeTestRule.onNodeWithText(changeName()).requestFocus()
        composeTestRule.onNodeWithText(changeName())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(CreateProfileAction.ChangeNameRequested)
    }

    @Test
    fun givenAWriteFailed_whenTheScreenIsRead_thenTheChoicesAreStillThere() {
        setScreen(CreateProfileFixtures.saveFailed())

        composeTestRule.onNodeWithText(saveFailed()).assertIsDisplayed()
        composeTestRule.onNodeWithText(CreateProfileFixtures.NICKNAME).assertIsDisplayed()
    }

    private fun setScreen(
        state: CreateProfileUiState,
        onAction: (CreateProfileAction) -> Unit = {}
    ) {
        composeTestRule.setContent {
            HelloBeTheme {
                CreateProfileScreen(state = state, onAction = onAction)
            }
        }
    }

    private fun create() = resources.getString(R.string.create_submit)

    private fun needsAge() = resources.getString(R.string.create_needs_age)

    private fun changeName() = resources.getString(R.string.create_change_name)

    private fun saveFailed() = resources.getString(R.string.create_save_failed)
}
