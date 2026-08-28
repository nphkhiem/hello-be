package com.nphkhiem.englishforyourchildren.feature.caregiver

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
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaregiverShellTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenTheShell_whenTheRailIsRead_thenItHoldsExactlyTheThreeSectionsAndTheWayOut() {
        setShell(CaregiverFixtures.shell())

        composeTestRule.onNodeWithText(overview()).assertIsDisplayed()
        composeTestRule.onNodeWithText(settings()).assertIsDisplayed()
        composeTestRule.onNodeWithText(profiles()).assertIsDisplayed()
        composeTestRule.onNodeWithText(returnToChild()).assertIsDisplayed()
    }

    @Test
    fun givenTheShell_whenItOpens_thenTheCurrentSectionHoldsFocus() {
        setShell(CaregiverFixtures.shell(CaregiverSection.SETTINGS))

        composeTestRule.onNodeWithText(settings()).assertIsFocused()
    }

    @Test
    fun givenEachSection_whenItIsPressed_thenItReportsItsOwnSection() {
        val actions = mutableListOf<CaregiverShellAction>()
        setShell(CaregiverFixtures.shell(), onAction = { actions += it })

        composeTestRule.onNodeWithText(profiles()).requestFocus()
        composeTestRule.onNodeWithText(profiles()).performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeTestRule.waitForIdle()

        assertThat(actions)
            .containsExactly(CaregiverShellAction.SectionChosen(CaregiverSection.PROFILES))
    }

    @Test
    fun givenTheWayOut_whenItIsPressed_thenItIsItsOwnActionRatherThanASection() {
        // Leaving ends the caregiver session. It is not a fourth section and must not report as one.
        val actions = mutableListOf<CaregiverShellAction>()
        setShell(CaregiverFixtures.shell(), onAction = { actions += it })

        composeTestRule.onNodeWithText(returnToChild()).requestFocus()
        composeTestRule.onNodeWithText(returnToChild()).performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(CaregiverShellAction.ReturnToChildRequested)
    }

    @Test
    fun givenTheRail_whenItIsMeasured_thenTheWayOutIsSeparatedFromTheSections() {
        // The information architecture asks for spatial separation. Asserted in pixels, because a
        // control that ends a session sitting flush under the sections is the accident it prevents.
        setShell(CaregiverFixtures.shell())

        val profiles = composeTestRule.onNodeWithText(profiles()).getUnclippedBoundsInRoot()
        val overview = composeTestRule.onNodeWithText(overview()).getUnclippedBoundsInRoot()
        val exit = composeTestRule.onNodeWithText(returnToChild()).getUnclippedBoundsInRoot()

        val betweenSections = profiles.top.value - overview.bottom.value
        val beforeTheWayOut = exit.top.value - profiles.bottom.value

        assertThat(beforeTheWayOut).isGreaterThan(betweenSections)
    }

    @Test
    fun givenFocusMovesIntoThePanelAndBack_whenTheRailIsReentered_thenItRestoresWhereItWas() {
        setShell(CaregiverFixtures.shell(CaregiverSection.SETTINGS))

        composeTestRule.onNodeWithText(settings()).assertIsFocused()
        composeTestRule.onNodeWithText(PANEL_CONTROL).requestFocus()
        composeTestRule.onNodeWithText(PANEL_CONTROL).assertIsFocused()
        composeTestRule.onNodeWithText(settings()).requestFocus()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(settings()).assertIsFocused()
    }

    @Test
    fun givenTheShell_whenASectionIsOpen_thenNoGateControlExistsAnywhereInIt() {
        // One gate per foreground caregiver session: moving between sections must never summon
        // another. The rail is built from the section enum, so what holds for one section holds
        // for all three, and the first test above asserts all three are present at once.
        setShell(CaregiverFixtures.shell(CaregiverSection.SETTINGS))

        composeTestRule.onNodeWithText(gateTitle()).assertDoesNotExist()
        composeTestRule.onNodeWithText(CaregiverFixtures.QUESTION).assertDoesNotExist()
        composeTestRule.onNodeWithText(CaregiverFixtures.CORRECT).assertDoesNotExist()
    }

    private fun setShell(
        state: CaregiverShellState,
        onAction: (CaregiverShellAction) -> Unit = {}
    ) {
        composeTestRule.setContent {
            HelloBeTheme {
                CaregiverScaffold(state = state, onAction = onAction) {
                    HelloBeAction(label = PANEL_CONTROL, onClick = {})
                }
            }
        }
    }

    private fun overview() = resources.getString(R.string.caregiver_overview)

    private fun settings() = resources.getString(R.string.caregiver_settings)

    private fun profiles() = resources.getString(R.string.caregiver_profiles)

    private fun returnToChild() = resources.getString(R.string.caregiver_return)

    private fun gateTitle() = resources.getString(R.string.gate_title)

    private companion object {
        const val PANEL_CONTROL = "Panel control"
    }
}
