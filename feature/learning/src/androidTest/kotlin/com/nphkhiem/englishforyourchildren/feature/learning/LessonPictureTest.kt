package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nphkhiem.englishforyourchildren.ui.tv.component.LocalPictureSource
import com.nphkhiem.englishforyourchildren.ui.tv.component.PictureSource
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a card shows when the picture it names exists, and when it does not.
 *
 * The second case is the whole build today: the content has named a picture per choice since the
 * course was packaged and not one file has been drawn. Picture matching and review drew nothing at
 * all, because they withhold the word for a picture that never arrived.
 */
@RunWith(AndroidJUnit4::class)
class LessonPictureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenAChoiceWhosePictureIsUndrawn_whenItIsRendered_thenTheChildSeesItsWord() {
        setActivity(withPictures(LessonFixtures.answering()), pictures = none)

        composeTestRule.onNodeWithText(CHAIR).assertIsDisplayed()
    }

    @Test
    fun givenAChoiceWhosePictureIsPackaged_whenItIsRendered_thenTheWordGivesWayToIt() {
        // What T1 asks for in one line: the card shows a picture rather than the word for it.
        setActivity(withPictures(LessonFixtures.answering()), pictures = drawn)

        composeTestRule.onNodeWithText(CHAIR).assertDoesNotExist()
    }

    @Test
    fun givenPictureMatchingWithNoPictureYet_whenItIsRendered_thenItStillWithholdsTheWord() {
        // The one family the word fallback is kept away from, and the reason is older than this
        // work: "Find the ears" beside a card captioned "ears" is solvable by reading rather than
        // by looking, and that is true whether or not the picture has been drawn. The card says
        // nothing on screen, and still announces its word to a screen reader.
        setActivity(withPictures(PictureMatchingFixtures.answering()), pictures = none)

        composeTestRule.onNodeWithText(CHAIR).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(CHAIR).assertIsDisplayed()
    }

    @Test
    fun givenAPictureMatchingSource_whenItsPictureIsPackaged_thenTheWordGivesWayToIt() {
        // The source may say its word, because it is the question rather than an answer. A picture
        // says it better, so the word stands in only until one exists.
        setActivity(withPictures(PictureMatchingFixtures.answering()), pictures = drawn)

        composeTestRule.onNodeWithText(BED).assertDoesNotExist()
    }

    private val none = PictureSource { null }

    private val drawn = PictureSource { ImageBitmap(width = 1, height = 1) }

    /** The fixtures predate pictures, so the ids the packaged content carries are added here. */
    private fun withPictures(state: LessonUiState) = state.copy(
        answers = state.answers.map { it.copy(image = "img-${it.label}") },
        learningObject = state.learningObject?.copy(image = "img-object")
    )

    private fun setActivity(state: LessonUiState, pictures: PictureSource) {
        composeTestRule.setContent {
            HelloBeTheme {
                CompositionLocalProvider(LocalPictureSource provides pictures) {
                    LessonActivity(state = state, onAction = {})
                }
            }
        }
    }

    private companion object {
        const val CHAIR = "chair"
        const val BED = "bed"
    }
}
