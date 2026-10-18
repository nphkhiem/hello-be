package com.nphkhiem.englishforyourchildren.debugcatalog

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That Back reaches the app being walked.
 *
 * Back is dispatched to the most recently registered handler. The walk used to be the first section
 * on a page of twenty, so every lesson, free play and celebration specimen below registered a
 * handler after it and took the press: measured on a device, the celebration specimen answered the
 * first Back and the walk's own host answered later ones, both off screen. A reviewer pressing Back
 * saw nothing, while the adult gate on screen told them Back returns to the child screen.
 *
 * Raising the walk last makes its handler the newest, so Back reaches it. At the walked app's own
 * root Back leaves, which closes the walk.
 */
@RunWith(AndroidJUnit4::class)
class CatalogWalkBackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenTheCatalogOpens_whenItFirstComposes_thenTheWalkIsNotRunning() {
        composeTestRule.setContent { ThemeCatalogScreen() }

        composeTestRule.onNodeWithTag(WALK_TAG).assertDoesNotExist()
    }

    @Test
    fun givenTheWalkIsRunning_whenBackIsPressedAtItsRoot_thenItLeavesAndTheCatalogReturns() {
        composeTestRule.setContent { ThemeCatalogScreen() }
        openTheWalk()
        composeTestRule.onNodeWithTag(WALK_TAG).assertExists()

        pressBack()

        composeTestRule.onNodeWithTag(WALK_TAG).assertDoesNotExist()
    }

    private fun openTheWalk() {
        composeTestRule.onNodeWithText(OPENER, substring = true).requestFocus()
        composeTestRule.onNodeWithText(OPENER, substring = true)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        composeTestRule.waitForIdle()
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val OPENER = "Start the walk"
    }
}
