package com.nphkhiem.englishforyourchildren.audit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverFixtures
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverOverviewScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The caregiver overview across every font scale a television offers.
 *
 * The design brief asks caregiver content to reflow or reduce under a large font scale and never to
 * shrink text below its approved role size. The overview is the surface at risk, because it is the
 * densest and, at ordinary scale, deliberately does not scroll.
 *
 * The audit that produced these numbers found a real gap: at 1.5 the instruction under the
 * suggestion measured zero high, and at 2.0 it did too. One test per scale, because a compose rule
 * accepts setContent exactly once.
 */
@RunWith(AndroidJUnit4::class)
class CaregiverFontScaleAuditTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenOrdinaryText_whenTheOverviewIsDrawn_thenTheInstructionKeepsItsRoleSize() {
        assertInstructionKeepsRoleSize(composeTestRule, scale = 1.0f)
    }

    @Test
    fun givenSlightlyLargerText_whenTheOverviewIsDrawn_thenTheInstructionKeepsItsRoleSize() {
        assertInstructionKeepsRoleSize(composeTestRule, scale = 1.3f)
    }

    @Test
    fun givenLargeText_whenTheOverviewIsDrawn_thenTheInstructionKeepsItsRoleSize() {
        // The scale that used to collapse it to nothing.
        assertInstructionKeepsRoleSize(composeTestRule, scale = 1.5f)
    }

    @Test
    fun givenTheLargestText_whenTheOverviewIsDrawn_thenTheInstructionKeepsItsRoleSize() {
        assertInstructionKeepsRoleSize(composeTestRule, scale = 2.0f)
    }

    @Test
    fun givenOrdinaryText_whenTheOverviewIsDrawn_thenItAlreadyScrollsSoTheRestCanBeReached() {
        // This used to assert the opposite: that at ordinary scale everything fits, so a scroll
        // nothing could drive would put the lower half out of reach rather than out of sight. The
        // fitting stopped being true when the suggestion got a source, and the driving stopped
        // being a problem when the cards took focus. Reflow at every scale, never truncation.
        setOverview(composeTestRule, scale = 1.0f)

        assertThat(composeTestRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes())
            .isNotEmpty()
    }

    @Test
    fun givenLargeText_whenTheOverviewIsDrawn_thenItStillScrolls() {
        setOverview(composeTestRule, scale = 1.5f)

        assertThat(composeTestRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes())
            .isNotEmpty()
    }
}

private fun assertInstructionKeepsRoleSize(rule: ComposeContentTestRule, scale: Float) {
    var roleSize = 0f
    rule.setContent {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, scale)) {
            HelloBeTheme {
                roleSize = with(LocalDensity.current) {
                    HelloBeTheme.typography.bodyMedium.fontSize.toPx()
                }
                Overview()
            }
        }
    }

    val instruction = rule
        .onAllNodesWithText(CaregiverFixtures.SUGGESTION_HINT, useUnmergedTree = true)
        .fetchSemanticsNodes()

    assertThat(instruction).isNotEmpty()
    assertThat(instruction.first().size.height.toFloat()).isAtLeast(roleSize)
}

private fun setOverview(rule: ComposeContentTestRule, scale: Float) {
    rule.setContent {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, scale)) {
            HelloBeTheme { Overview() }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Overview() {
    Box(modifier = Modifier.size(HelloBeLayout.referenceWidth, HelloBeLayout.referenceHeight)) {
        CaregiverScaffold(state = CaregiverFixtures.shell(), onAction = {}) {
            CaregiverOverviewScreen(state = CaregiverFixtures.overview())
        }
    }
}
