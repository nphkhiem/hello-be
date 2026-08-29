package com.nphkhiem.englishforyourchildren.audit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeScreen
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayScreen
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Meaningful content stays clear of the edges a television may eat.
 *
 * The design brief puts a number on it: at least five percent from every screen edge, with only
 * decorative backgrounds allowed to bleed. `StorybookScaffold` owns that inset, so this is really
 * an audit of whether every surface goes through it, which is the sort of thing that stays true
 * until one screen quietly does not.
 */
@RunWith(AndroidJUnit4::class)
class OverscanAuditTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenChildHome_whenItIsDrawn_thenItsHeadingClearsTheOverscanMargin() {
        setContent { ChildHomeScreen(state = ChildHomeFixtures.returning(), onAction = {}) }

        assertClearsOverscan(ChildHomeFixtures.WAITING_HINT)
    }

    @Test
    fun givenFreePlay_whenItIsDrawn_thenItsFirstShelfClearsTheOverscanMargin() {
        setContent { FreePlayScreen(state = FreePlayFixtures.shelves(), onAction = {}) }

        assertClearsOverscan(FreePlayFixtures.BODY_NAME)
    }

    private fun assertClearsOverscan(text: String) {
        val bounds = composeTestRule.onNodeWithText(text).getUnclippedBoundsInRoot()
        val horizontalMargin = HelloBeLayout.referenceWidth.value * EDGE_FRACTION
        val verticalMargin = HelloBeLayout.referenceHeight.value * EDGE_FRACTION

        assertThat(bounds.left.value).isAtLeast(horizontalMargin)
        assertThat(bounds.right.value)
            .isAtMost(HelloBeLayout.referenceWidth.value - horizontalMargin)
        assertThat(bounds.top.value).isAtLeast(verticalMargin)
    }

    private fun setContent(screen: @Composable () -> Unit) {
        composeTestRule.setContent {
            HelloBeTheme {
                Box(
                    modifier = Modifier.size(
                        HelloBeLayout.referenceWidth,
                        HelloBeLayout.referenceHeight
                    )
                ) {
                    screen()
                }
            }
        }
    }

    private companion object {
        /** The design brief's own number for how far meaningful content stays from an edge. */
        const val EDGE_FRACTION = 0.05f
    }
}
