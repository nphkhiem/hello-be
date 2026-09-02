package com.nphkhiem.englishforyourchildren.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackagedAssetLocatorTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val locator = PackagedAssetLocator.forAudio(context)

    private val pictures = PackagedAssetLocator.forImages(context)

    @Test
    fun givenARecordingNobodyHasMade_whenItIsLocated_thenThereIsNoFile() {
        assertThat(locator.locate(AssetId("aud-en-prompt-where-is"))).isNull()
    }

    @Test
    fun givenAPackagedRecording_whenItIsLocated_thenItsAssetUriIsFound() {
        val located = locator.locate(AssetId("aud-test-tone"))

        assertThat(located).isNotNull()
        assertThat(located.toString()).isEqualTo("asset:///media/audio/aud-test-tone.m4a")
    }

    @Test
    fun givenAPictureNobodyHasDrawn_whenItIsLocated_thenThereIsNoFile() {
        assertThat(pictures.locate(AssetId("img-eyes"))).isNull()
    }

    @Test
    fun givenAPackagedPicture_whenItIsLocated_thenItsAssetUriIsFound() {
        val located = pictures.locate(AssetId("img-test-shape"))

        assertThat(located).isNotNull()
        assertThat(located.toString()).isEqualTo("asset:///media/image/img-test-shape.webp")
    }

    @Test
    fun givenAnIdOfOneKind_whenTheOtherKindIsAsked_thenItIsNotFound() {
        // An id is opaque, so neither locator reads it to work out what it names. Asking the wrong
        // one is answered with a missing file rather than with the right file by accident.
        assertThat(pictures.locate(AssetId("aud-test-tone"))).isNull()
        assertThat(locator.locate(AssetId("img-test-shape"))).isNull()
    }
}
