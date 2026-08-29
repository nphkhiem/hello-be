package com.nphkhiem.englishforyourchildren.audit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeScreen
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathScreen
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every palette and motion setting a television can be in, over the surfaces most likely to break.
 *
 * These are the settings that change what every screen looks like without changing a line of any
 * screen, which is exactly the kind of thing nothing else in the suite would catch. The claims are
 * deliberately modest and structural: content is present, focus lands where it should, and no
 * setting removes either. Contrast ratios belong to the token tests in `:ui:tv`.
 */
@RunWith(AndroidJUnit4::class)
class PaletteAndMotionAuditTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun givenNightMode_whenChildHomeIsDrawn_thenItIsStillReadableAndFocused() {
        setContent(mode = HelloBeThemeMode.NIGHT) {
            ChildHomeScreen(state = ChildHomeFixtures.returning(), onAction = {})
        }

        composeTestRule.onNodeWithText(continueLabel()).assertIsDisplayed()
        composeTestRule.onNodeWithText(continueLabel()).assertIsFocused()
    }

    @Test
    fun givenHighContrast_whenChildHomeIsDrawn_thenNothingIsLost() {
        setContent(highContrast = true) {
            ChildHomeScreen(state = ChildHomeFixtures.returning(), onAction = {})
        }

        composeTestRule.onNodeWithText(continueLabel()).assertIsDisplayed()
        composeTestRule.onNodeWithText(continueLabel()).assertIsFocused()
    }

    @Test
    fun givenNightModeAndHighContrastTogether_whenTheLearningPathIsDrawn_thenItHolds() {
        // The two override layers at once, which is the combination no screen was built against.
        setContent(mode = HelloBeThemeMode.NIGHT, highContrast = true) {
            LearningPathScreen(state = LearningPathFixtures.midUnit(), onAction = {})
        }

        composeTestRule.onNodeWithText(RECOMMENDED_LESSON).assertIsDisplayed()
        composeTestRule.onNodeWithText(RECOMMENDED_LESSON).assertIsFocused()
    }

    @Test
    fun givenReducedMotion_whenTheLearningPathIsDrawn_thenEveryLessonIsStillThere() {
        // Motion may not be load-bearing: with it off, the same content is present and reachable.
        setContent(reduceMotion = true) {
            LearningPathScreen(state = LearningPathFixtures.midUnit(), onAction = {})
        }

        listOf(
            LearningPathFixtures.EYES,
            LearningPathFixtures.NOSE,
            LearningPathFixtures.HANDS,
            LearningPathFixtures.MOVE,
            LearningPathFixtures.REVIEW
        ).forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun givenNightMode_whenTheAdultGateIsDrawn_thenFocusStillAvoidsTheCorrectAnswer() {
        // The protection is focus placement, and a palette change must not disturb it.
        setContent(mode = HelloBeThemeMode.NIGHT) {
            com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateScreen(
                state = CaregiverFixtures.gate(),
                onAction = {}
            )
        }

        composeTestRule.onNodeWithText(CaregiverFixtures.WRONG_LOW).assertIsFocused()
        assertThat(CaregiverFixtures.gate().challenge.correctIndex).isEqualTo(1)
    }

    private fun setContent(
        mode: HelloBeThemeMode = HelloBeThemeMode.DAY,
        highContrast: Boolean = false,
        reduceMotion: Boolean = false,
        screen: @Composable () -> Unit
    ) {
        composeTestRule.setContent {
            HelloBeTheme(
                themeMode = mode,
                highContrast = highContrast,
                reduceMotion = reduceMotion
            ) {
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

    private fun continueLabel(): String {
        val id = resources.getIdentifier(
            "home_continue",
            "string",
            "com.nphkhiem.englishforyourchildren"
        )
        return resources.getString(id)
    }

    private companion object {
        const val RECOMMENDED_LESSON = "Hands and feet"
    }
}
