package com.nphkhiem.englishforyourchildren.tools.pipeline

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Fetching the starter course's word recordings, against responses recorded once from
 * dictionaryapi.dev and Wikimedia Commons and checked in. Nothing here touches the network.
 */
class EnricherTest {

    @TempDir
    lateinit var temp: File

    private val web = RecordedWeb()

    @Test
    fun givenAWordWithAFreeRecording_whenEnriched_thenTheCommonsOriginalArrivesWithItsCredit() {
        val course = starterCopy()

        val result = Enricher(web).enrich(course)

        assertThat(
            File(course, "media/audio/aud-en-eyes.ogg").readText()
        ).isEqualTo("En-us-eyes.ogg")
        val credit = credits(course).single { it.assetId == "aud-en-eyes" }
        assertThat(credit.source).isEqualTo("Dvortygirl, via Wikimedia Commons")
        assertThat(credit.licence).isEqualTo("CC BY-SA 3.0")
        assertThat(credit.licenceEvidence)
            .isEqualTo("https://commons.wikimedia.org/wiki/File:En-us-eyes.ogg")
        assertThat(credit.attributionText)
            .isEqualTo("\"En-us-eyes.ogg\" by Dvortygirl, CC BY-SA 3.0, via Wikimedia Commons")
        assertThat(result.added).contains("aud-en-eyes from File:En-us-eyes.ogg")
    }

    @Test
    fun givenAUsAndAUkRecording_whenEnriched_thenTheUsOneIsTaken() {
        // The accent the rest of the course is voiced in.
        val course = starterCopy()

        Enricher(web).enrich(course)

        assertThat(
            File(course, "media/audio/aud-en-ears.ogg").readText()
        ).isEqualTo("En-us-ears.ogg")
    }

    @Test
    fun givenAWordTheDictionaryLacks_whenEnriched_thenItIsLeftForAPersonNotGuessed() {
        val course = starterCopy()

        val result = Enricher(web).enrich(course)

        assertThat(result.notes.joinToString()).contains("no dictionary entry")
        assertThat(File(course, "media/audio/aud-en-nose.ogg").exists()).isFalse()
    }

    @Test
    fun givenARecordingUnderANonFreeLicence_whenEnriched_thenItIsNotTaken() {
        web.relicense("1225585", "CC BY-NC-SA 3.0")
        val course = starterCopy()

        val result = Enricher(web).enrich(course)

        assertThat(File(course, "media/audio/aud-en-eyes.ogg").exists()).isFalse()
        assertThat(result.notes.joinToString()).contains("which is not free")
    }

    @Test
    fun givenARecordingAlreadyMade_whenEnriched_thenItIsLeftAloneAndNotLookedUp() {
        val course = starterCopy()
        File(course, "media/audio/aud-en-eyes.m4a").apply {
            parentFile.mkdirs()
            writeText("recorded by a person")
        }

        Enricher(web).enrich(course)

        assertThat(web.asked.any { it.endsWith("/eyes") }).isFalse()
        assertThat(File(course, "media/audio/aud-en-eyes.ogg").exists()).isFalse()
    }

    @Test
    fun givenAnEnrichedCourse_whenBuilt_thenEveryNewRecordingIsCredited() {
        // The ledger refuses to publish a file without a complete row, so this is the proof the
        // credits written are the ones publishing needs.
        val course = starterCopy()
        Enricher(web).enrich(course)

        val built = Publisher.build(course, File(temp, "out"), null)

        assertThat(built).isInstanceOf(BuildResult.Built::class.java)
    }

    private fun starterCopy(): File =
        File(temp, "course-${System.nanoTime()}").also { STARTER.copyRecursively(it) }

    private fun credits(course: File) = CurriculumJsonParser()
        .parseAttributions(File(course, "attribution/attributions.json").readText())
        .entries

    /**
     * The dictionary and Commons as they answered on October 18, 2026, for eyes and ears. Every
     * other word is a 404, and every audio download returns the file's own name as its bytes, so a
     * test can tell which recording landed.
     */
    private class RecordedWeb : WebSource {
        val asked = mutableListOf<String>()
        private val relicensed = mutableMapOf<String, String>()

        fun relicense(pageId: String, licence: String) {
            relicensed[pageId] = licence
        }

        override fun get(url: String): ByteArray? {
            asked += url
            return when {
                url.startsWith(
                    DICTIONARY
                ) -> recorded("dictionary-${url.substringAfterLast('/')}.json")

                "pageids=" in url -> url.substringAfter("pageids=").let { id ->
                    recorded("commons-$id.json")?.decodeToString()
                        ?.let { body ->
                            relicensed[id]?.let { body.replace("CC BY-SA 3.0", it) }
                                ?: body
                        }
                        ?.encodeToByteArray()
                }

                url.startsWith(UPLOADS) -> url.substringAfterLast('/').encodeToByteArray()

                else -> null
            }
        }

        private fun recorded(name: String): ByteArray? =
            javaClass.getResourceAsStream("/recorded/$name")?.use { it.readBytes() }
    }

    private companion object {
        val STARTER = File("../../content/starter/src/main/assets")
        const val DICTIONARY = "https://api.dictionaryapi.dev/api/v2/entries/en/"
        const val UPLOADS = "https://upload.wikimedia.org/"
    }
}
