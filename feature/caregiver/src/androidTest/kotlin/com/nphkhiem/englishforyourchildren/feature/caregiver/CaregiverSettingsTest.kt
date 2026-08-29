package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
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
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaregiverSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenTheSettings_whenARowIsRead_thenItNamesItsEffectOnTheChild() {
        setSettings(CaregiverFixtures.settings())

        composeTestRule.onNodeWithText(CaregiverFixtures.CAPTIONS).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Show spoken instructions on screen", substring = true)
            .fetchSemanticsNodes()
            .also { assertThat(it).isNotEmpty() }
    }

    @Test
    fun givenAToggle_whenItsStateIsRead_thenItIsInWordsAndNotOnlyInColour() {
        // The approved draft keeps setting state in text and semantics rather than in the colour
        // of a toggle, which is the same rule this product applies to lesson feedback.
        setSettings(CaregiverFixtures.settings())

        // All of them, not one: several settings are on at once, and the claim is that each says
        // so rather than that exactly one does.
        composeTestRule.onAllNodes(hasStateDescription(on())).fetchSemanticsNodes()
            .also { assertThat(it).isNotEmpty() }
        composeTestRule.onAllNodesWithText(off(), substring = true).fetchSemanticsNodes()
            .also { assertThat(it).isNotEmpty() }
    }

    @Test
    fun givenAChoiceRow_whenItsValueIsRead_thenTheCurrentOptionIsNamed() {
        setSettings(CaregiverFixtures.settings())

        composeTestRule
            .onAllNodesWithText(CaregiverFixtures.BOTH_LANGUAGES, substring = true)
            .fetchSemanticsNodes()
            .also { assertThat(it).isNotEmpty() }
    }

    @Test
    fun givenTheSettings_whenTheyOpen_thenTheFirstRowHoldsFocus() {
        setSettings(CaregiverFixtures.settings())

        composeTestRule.onNodeWithText(CaregiverFixtures.VIETNAMESE_HELP).requestFocus()
        composeTestRule.onNodeWithText(CaregiverFixtures.VIETNAMESE_HELP).assertIsFocused()
    }

    @Test
    fun givenAToggleRow_whenItIsPressed_thenItAsksToToggleThatSetting() {
        val actions = mutableListOf<CaregiverSettingsAction>()
        setSettings(CaregiverFixtures.settings(), onAction = { actions += it })

        composeTestRule.onNodeWithText(CaregiverFixtures.CAPTIONS).requestFocus()
        composeTestRule.onNodeWithText(CaregiverFixtures.CAPTIONS)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions)
            .containsExactly(CaregiverSettingsAction.SettingToggled(SettingId.CAPTIONS))
    }

    @Test
    fun givenAChoiceRow_whenItIsPressed_thenItAsksToOpenRatherThanToChange() {
        // A choice cannot be advanced blindly by pressing: a caregiver picks from the options.
        val actions = mutableListOf<CaregiverSettingsAction>()
        setSettings(CaregiverFixtures.settings(), onAction = { actions += it })

        composeTestRule.onNodeWithText(CaregiverFixtures.CAREGIVER_LANGUAGE).requestFocus()
        composeTestRule.onNodeWithText(CaregiverFixtures.CAREGIVER_LANGUAGE)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(
            CaregiverSettingsAction.SettingExpanded(SettingId.CAREGIVER_LANGUAGE)
        )
    }

    @Test
    fun givenAnOpenChoiceRow_whenAnOptionIsPressed_thenItAsksForThatOption() {
        val actions = mutableListOf<CaregiverSettingsAction>()
        setSettings(CaregiverFixtures.settingsLanguageOpen(), onAction = { actions += it })

        composeTestRule.onNodeWithText("Tiếng Việt").requestFocus()
        composeTestRule.onNodeWithText("Tiếng Việt")
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(
            CaregiverSettingsAction.SettingChoiceChosen(
                SettingId.CAREGIVER_LANGUAGE,
                "Tiếng Việt"
            )
        )
    }

    @Test
    fun givenAClosedChoiceRow_whenTheListIsRead_thenItsOptionsAreNotOnScreen() {
        // Expanding in place keeps the list shallow. Closed means closed, not merely collapsed.
        setSettings(CaregiverFixtures.settings())

        composeTestRule.onNodeWithText("Tiếng Việt").assertDoesNotExist()
    }

    @Test
    fun givenNothingChanged_whenTheListIsRead_thenRestoringDefaultsIsNotOffered() {
        setSettings(CaregiverFixtures.settings())

        composeTestRule.onNodeWithText(restore()).assertDoesNotExist()
    }

    @Test
    fun givenSomethingChanged_whenRestoreIsPressed_thenItAsksToPutThingsBack() {
        val actions = mutableListOf<CaregiverSettingsAction>()
        setSettings(CaregiverFixtures.settingsChanged(), onAction = { actions += it })

        composeTestRule.onNodeWithText(restore()).requestFocus()
        composeTestRule.onNodeWithText(restore())
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()

        assertThat(actions)
            .containsExactly(CaregiverSettingsAction.RestoreDefaultsRequested)
    }

    @Test
    fun givenASaveInFlight_whenTheListIsRead_thenItSaysSoAndRestoringWaits() {
        setSettings(CaregiverFixtures.settingsSaving())

        composeTestRule.onNodeWithText(saving()).assertIsDisplayed()
        composeTestRule.onNodeWithText(restore()).assertDoesNotExist()
    }

    @Test
    fun givenASaveFailed_whenTheListIsRead_thenItSaysSoPlainly() {
        // A caregiver who changed something has to know whether it held.
        setSettings(CaregiverFixtures.settingsSaveFailed())

        composeTestRule.onNodeWithText(saveFailed()).assertIsDisplayed()
        // Present rather than on screen: the restore action sits under six rows, and this list
        // scrolls. That is safe here because every row takes focus, so moving down reaches it.
        composeTestRule.onNodeWithText(restore()).assertExists()
    }

    @Test
    fun givenAccessibilityOn_whenTheRowsAreRead_thenBothPreviewSettingsSayOn() {
        setSettings(CaregiverFixtures.settingsAccessible())

        listOf(CaregiverFixtures.REDUCED_MOTION, CaregiverFixtures.HIGH_CONTRAST)
            .forEach { title ->
                composeTestRule
                    .onAllNodesWithText("$title", substring = true)
                    .fetchSemanticsNodes()
                    .also { assertThat(it).isNotEmpty() }
            }
        composeTestRule.onAllNodesWithText(on(), substring = true).fetchSemanticsNodes()
            .also { assertThat(it.size).isAtLeast(3) }
    }

    @Test
    fun givenTheSettings_whenTheWholeListIsRead_thenNothingSwitchesTheChildsTheme() {
        // The stop condition. Night mode is not approved as a caregiver control in this phase.
        setSettings(CaregiverFixtures.settings())

        listOf("Night", "Dark", "Theme", "Colour", "Color").forEach { banned ->
            composeTestRule
                .onAllNodesWithText(banned, substring = true)
                .fetchSemanticsNodes()
                .also { assertThat(it).isEmpty() }
        }
    }

    private fun setSettings(
        state: CaregiverSettingsUiState,
        onAction: (CaregiverSettingsAction) -> Unit = {}
    ) {
        composeTestRule.setContent {
            HelloBeTheme {
                CaregiverSettingsScreen(state = state, onAction = onAction)
            }
        }
    }

    private fun on() = resources.getString(R.string.settings_on)

    private fun off() = resources.getString(R.string.settings_off)

    private fun saving() = resources.getString(R.string.settings_saving)

    private fun saveFailed() = resources.getString(R.string.settings_save_failed)

    private fun restore() = resources.getString(R.string.settings_restore)
}
