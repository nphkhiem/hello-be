package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberHelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That opening the catalog does not strand a reviewer inside a dialog.
 *
 * The catalog opened with entry focus on the delete confirmation's safe action, seven sections
 * down, and neither the D-pad nor Back escaped it. A dialog contains focus and requests it on first
 * composition, both correct when one is raised over a stage; the confirmation specimen was composed
 * unasked, so it took the catalog's entry focus and would not give it back.
 *
 * `DialogSection` already states the rule this restores: a section holds the opener, and the dialog
 * is hosted at the catalog root.
 *
 * What the confirmation does once open is not asserted here. `CaregiverConfirmationTest` already
 * covers it at the component level, where it belongs, including that the safe choice holds initial
 * focus and that pressing Select repeatedly destroys nothing.
 */
@RunWith(AndroidJUnit4::class)
class CatalogEntryFocusTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenTheCatalogOpens_whenItFirstComposes_thenNoRecoveryIsShowing() {
        composeTestRule.setContent { ThemeCatalogScreen() }

        composeTestRule.onNodeWithText(RECOVERY_ACTION, substring = true).assertDoesNotExist()
    }

    @Test
    fun givenTheCatalogOpens_whenItFirstComposes_thenNoConfirmationIsShowing() {
        composeTestRule.setContent { ThemeCatalogScreen() }

        composeTestRule.onNodeWithText(SAFE_ACTION, substring = true).assertDoesNotExist()
    }

    @Test
    fun givenTheCatalogOpens_whenItFirstComposes_thenTheConfirmationIsStillOffered() {
        composeTestRule.setContent { ThemeCatalogScreen() }

        // Gating the specimen must not remove it: the section exists to demonstrate the
        // confirmation, and an opener nobody can find would be the same as deleting it.
        composeTestRule.onNodeWithText(OPENER, substring = true).assertExists()
    }

    @Test
    fun givenTheOpener_whenSelectIsPressed_thenItAsksForTheConfirmation() {
        var opened = false
        composeTestRule.setContent {
            HelloBeTheme {
                CaregiverConfirmationCatalogSection(
                    index = 0,
                    lastAction = null,
                    restorer = rememberHelloBeFocusRestorer(),
                    onNextState = {},
                    onOpen = { opened = true }
                )
            }
        }

        // The section alone, not the whole catalog: seven sections down a scrolling column the
        // opener lands flush against the screen edge and never takes focus, which measures the
        // harness rather than the opener. Select is a key press because `performClick` is a no-op
        // on a TV surface.
        composeTestRule.onNodeWithText(OPENER, substring = true).requestFocus()
        composeTestRule.onNodeWithText(OPENER, substring = true).assertIsFocused()
        composeTestRule.onNodeWithText(OPENER, substring = true)
            .performKeyInput { pressKey(Key.DirectionCenter) }

        assertThat(opened).isTrue()
    }

    private companion object {
        const val SAFE_ACTION = "Keep profile"
        const val OPENER = "Show confirmation"
        const val RECOVERY_ACTION = "Try sound again"
    }
}
