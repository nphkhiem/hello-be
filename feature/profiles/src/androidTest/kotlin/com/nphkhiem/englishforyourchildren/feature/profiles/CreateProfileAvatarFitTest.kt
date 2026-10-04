package com.nphkhiem.englishforyourchildren.feature.profiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That no avatar's name breaks across lines on the create-profile screen.
 *
 * "rabbit" was drawn as "rabbi" over "t". The preview borrowed the learning glyph type role, which
 * the tokens reserve for a single letter pair or numeral, and a six-letter word at that size does
 * not fit the column. The platform sans hid it by being narrower; Nunito Sans did not.
 *
 * Measured on line count rather than on pixels, and against every avatar the app ships rather than
 * a fixture's copy, so a longer avatar added later fails here instead of reaching a child.
 */
@RunWith(AndroidJUnit4::class)
class CreateProfileAvatarFitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenEveryShippedAvatar_whenItIsPreviewed_thenItsNameStaysOnOneLine() {
        var state by mutableStateOf(previewing(AVATARS.first()))
        composeTestRule.setContent {
            HelloBeTheme { CreateProfileScreen(state = state, onAction = {}) }
        }

        AVATARS.forEach { avatar ->
            state = previewing(avatar)
            composeTestRule.waitForIdle()

            val drawn = composeTestRule
                .onAllNodesWithText(avatar, useUnmergedTree = true)
                .fetchSemanticsNodes()
            // Guards against passing by finding nothing to measure.
            assertWithMessage("$avatar is drawn").that(drawn).isNotEmpty()

            drawn.forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                assertWithMessage("lines used by $avatar").that(layouts.single().lineCount)
                    .isEqualTo(1)
            }
        }
    }

    private fun previewing(avatar: String) = CreateProfileFixtures.ready().let { base ->
        base.copy(draft = base.draft.copy(avatarId = avatar))
    }
}
