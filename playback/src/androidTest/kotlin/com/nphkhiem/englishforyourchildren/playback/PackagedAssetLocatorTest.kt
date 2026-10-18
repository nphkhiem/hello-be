package com.nphkhiem.englishforyourchildren.playback

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import java.io.File
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackagedAssetLocatorTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    /** Where a downloaded pack would be, present only once a test puts something in it. */
    private val pack = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        .resolve("locator-test-pack")
    private val live = DownloadedMedia { pack.takeIf { it.isDirectory } }

    private val locator = PackagedAssetLocator.forAudio(context, live)

    private val pictures = PackagedAssetLocator.forImages(context, live)

    @After
    fun forgetThePack() {
        pack.deleteRecursively()
    }

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
    fun givenALivePackCarryingARecording_whenItIsLocated_thenThePacksFileWins() {
        // The pack may bring a better take of a word the APK already has.
        val file = download("media/audio/aud-test-tone.m4a")

        assertThat(locator.locate(AssetId("aud-test-tone"))).isEqualTo(Uri.fromFile(file))
    }

    @Test
    fun givenALivePackWithoutThatRecording_whenItIsLocated_thenTheApksIsFound() {
        download("media/audio/aud-test-word.m4a")

        assertThat(locator.locate(AssetId("aud-test-tone")).toString())
            .isEqualTo("asset:///media/audio/aud-test-tone.m4a")
    }

    @Test
    fun givenAPackBringingAPictureTheApkNeverHad_whenItIsLocated_thenItIsFound() {
        val file = download("media/image/img-eyes.webp")

        assertThat(pictures.locate(AssetId("img-eyes"))).isEqualTo(Uri.fromFile(file))
    }

    @Test
    fun givenAPackRecordingFetchedFromCommons_whenItIsLocated_thenItsOwnContainerIsFound() {
        // Kept as uploaded, usually Ogg, so the file is the licensed work rather than a copy.
        val ogg = download("media/audio/aud-en-eyes.ogg")
        val mp3 = download("media/audio/aud-en-ears.mp3")

        assertThat(locator.locate(AssetId("aud-en-eyes"))).isEqualTo(Uri.fromFile(ogg))
        assertThat(locator.locate(AssetId("aud-en-ears"))).isEqualTo(Uri.fromFile(mp3))
    }

    @Test
    fun givenARecordingInTwoContainers_whenItIsLocated_thenTheCoursesOwnM4aWins() {
        // A person's recording replaces a fetched one by being added beside it.
        val m4a = download("media/audio/aud-en-eyes.m4a")
        download("media/audio/aud-en-eyes.ogg")

        assertThat(locator.locate(AssetId("aud-en-eyes"))).isEqualTo(Uri.fromFile(m4a))
    }

    @Test
    fun givenAnIdOfOneKind_whenTheOtherKindIsAsked_thenItIsNotFound() {
        // An id is opaque, so neither locator reads it to work out what it names. Asking the wrong
        // one is answered with a missing file rather than with the right file by accident.
        assertThat(pictures.locate(AssetId("aud-test-tone"))).isNull()
        assertThat(locator.locate(AssetId("img-test-shape"))).isNull()
    }

    /** A file in the live pack at [path]. Its bytes do not matter to a locator. */
    private fun download(path: String): File = pack.resolve(path).apply {
        parentFile?.mkdirs()
        writeText("downloaded")
    }
}
