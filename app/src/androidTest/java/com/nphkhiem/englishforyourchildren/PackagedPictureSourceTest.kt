package com.nphkhiem.englishforyourchildren

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.playback.PackagedAssetLocator
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That every way a picture can fail to arrive is answered with null.
 *
 * The card asking already knows what to do with that, and none of these is worth ending a child's
 * lesson over. The claim is worth a test because the code makes it defensively, in a path that
 * nothing exercises today: not one illustration this course names has been drawn.
 */
@RunWith(AndroidJUnit4::class)
class PackagedPictureSourceTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val pictures =
        PackagedPictureSource(context, PackagedAssetLocator.forImages(context))

    @Test
    fun givenAPackagedPicture_whenItIsAskedFor_thenItIsDecoded() = runTest {
        val picture = pictures.load("img-test-shape")

        assertThat(picture).isNotNull()
        assertThat(picture?.width).isEqualTo(1)
    }

    @Test
    fun givenAPictureNobodyHasDrawn_whenItIsAskedFor_thenThereIsNone() = runTest {
        assertThat(pictures.load("img-eyes")).isNull()
    }

    @Test
    fun givenSomethingThatIsNotAnIdentifier_whenItIsAskedFor_thenThereIsNoneRatherThanACrash() =
        runTest {
            // Ids reaching here come from packaged content and are already sound. This is the case
            // that would otherwise throw out of a coroutine rather than leaving a card its word.
            assertThat(pictures.load("Not An Id")).isNull()
        }
}
