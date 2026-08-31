package com.nphkhiem.englishforyourchildren.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackagedAssetLocatorTest {
    private val locator =
        PackagedAssetLocator(InstrumentationRegistry.getInstrumentation().context)

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
}
