package com.nphkhiem.englishforyourchildren.tools.pipeline

import com.google.common.truth.Truth.assertThat
import com.google.crypto.tink.subtle.Ed25519Verify
import java.io.File
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

/**
 * Publishing a course, from the starter course that ships, without touching the network.
 *
 * Every test starts from a copy of the real authored course, changed in the one way the test is
 * about, so a pass means the tool handles the content that exists rather than a fixture shaped to
 * please it.
 */
class PublisherTest {

    @TempDir
    lateinit var temp: File

    @Test
    fun givenTheStarterCourse_whenBuilt_thenItsPacksUnpackToExactlyThatCourse() {
        val source = authored(NEXT)

        val built = build(source, out("next"))

        val unpacked = built.manifest.packs.flatMap { unzip(file("next", it.url)).entries }
            .associate { it.key to it.value.decodeToString() }
        // Hidden files, such as the folder's .gitkeep, are the author's housekeeping, not the course.
        val authored = source.walkTopDown().filter { it.isFile && !it.name.startsWith('.') }
            .associate { it.relativeTo(source).invariantSeparatorsPath to it.readText() }
        assertThat(unpacked).isEqualTo(authored)
    }

    @Test
    fun givenABuiltCourse_whenItsManifestIsRead_thenEveryPackMatchesItsSizeAndChecksum() {
        val built = build(authored(NEXT), out("next"))

        built.manifest.packs.forEach { pack ->
            val bytes = file("next", pack.url).readBytes()
            assertThat(bytes.size.toLong()).isEqualTo(pack.sizeBytes)
            assertThat(sha256(bytes)).isEqualTo(pack.sha256)
        }
        assertThat(built.manifest.courseVersion).isEqualTo(NEXT)
    }

    @Test
    fun givenTheSameCourse_whenBuiltTwice_thenEveryByteIsTheSame() {
        // So a reviewer can rebuild and compare, rather than take the checksums on trust.
        val source = authored(NEXT)
        build(source, out("first"))
        // Past the two-second tick a zip timestamp can record, so a build that stamped the time of
        // day could not pass by happening within the same tick.
        Thread.sleep(ZIP_CLOCK_TICK_MILLIS)
        build(source, out("second"))

        assertThat(file("second", Publisher.MANIFEST).readBytes())
            .isEqualTo(file("first", Publisher.MANIFEST).readBytes())
        assertThat(file("second", "packs/$NEXT/course.zip").readBytes())
            .isEqualTo(file("first", "packs/$NEXT/course.zip").readBytes())
    }

    @Test
    fun givenAPublishedCourse_whenTheNextIsNewerAndKeepsEveryLesson_thenItBuilds() {
        build(authored(SHIPPED), out("published"))

        val result = Publisher.build(authored(NEXT), out("next"), out("published"))

        assertThat(result).isInstanceOf(BuildResult.Built::class.java)
    }

    @Test
    fun givenAPublishedCourse_whenTheNextRemovesALesson_thenItIsRefused() {
        // A child resting on that lesson would be stranded.
        build(authored(SHIPPED), out("published"))
        val next = authored(NEXT).also { removeLastLessonOfFirstUnit(it) }

        val refused = refuse(Publisher.build(next, out("next"), out("published")))

        assertThat(refused.single()).contains("has been removed")
    }

    @Test
    fun givenAPublishedCourse_whenTheNextMovesALesson_thenItIsRefused() {
        build(authored(SHIPPED), out("published"))
        val next = authored(NEXT).also { swapFirstTwoLessonsOfFirstUnit(it) }

        val refused = refuse(Publisher.build(next, out("next"), out("published")))

        assertThat(refused).isNotEmpty()
        assertThat(refused.all { "moved from" in it }).isTrue()
    }

    @Test
    fun givenAPublishedCourse_whenTheNextIsNotANewerVersion_thenItIsRefused() {
        // A television would ignore it, so publishing it would only look like it worked.
        build(authored(SHIPPED), out("published"))

        val refused = refuse(Publisher.build(authored(SHIPPED), out("again"), out("published")))

        assertThat(refused.single()).contains("is not newer")
    }

    @Test
    fun givenAQuestionNoChildCanGetRight_whenBuilt_thenItIsRefused() {
        val source = authored(NEXT)
        unitFile(source).apply {
            writeText(
                readText().replaceFirst(
                    "\"correctSkillId\": \"word-eyes\"",
                    "\"correctSkillId\": \"word-nothing\""
                )
            )
        }

        assertThat(refuse(Publisher.build(source, out("next"), null))).isNotEmpty()
    }

    @Test
    fun givenMediaNobodyHasCredited_whenBuilt_thenItIsRefused() {
        // CC BY-SA requires credit, and the ledger is what proves it was given.
        val source = authored(NEXT)
        File(source, "media/image/img-eyes.webp").apply {
            parentFile.mkdirs()
            writeText("a picture")
        }

        val refused = refuse(Publisher.build(source, out("next"), null))

        assertThat(refused.joinToString()).contains("img-eyes")
    }

    @Test
    fun givenABuiltCourse_whenSignedWithTheOwnersKey_thenTheTelevisionsVerifierAcceptsIt() {
        val key = Signing.generate()
        build(authored(NEXT), out("next"))

        Signing.sign(out("next"), key.privateKeyBase64)

        // Tink's Ed25519, the one the app checks with, against the raw key the app is built with.
        val verifier = Ed25519Verify(Base64.getDecoder().decode(key.publicKeyBase64))
        val manifest = file("next", Publisher.MANIFEST).readBytes()
        val signature = file("next", Signing.SIGNATURE).readBytes()
        verifier.verify(signature, manifest)

        val tampered = manifest.copyOf().also { it[0] = (it[0] + 1).toByte() }
        assertThrows<GeneralSecurityException> { verifier.verify(signature, tampered) }
    }

    @Test
    fun givenANewKey_whenMade_thenThePublicHalfIsTheRawSizeTheAppPins() {
        assertThat(Base64.getDecoder().decode(Signing.generate().publicKeyBase64)).hasLength(32)
    }

    /** A copy of the shipped starter course, declaring [version]. */
    private fun authored(version: String): File {
        val copy = File(temp, "source-$version-${System.nanoTime()}")
        STARTER.copyRecursively(copy)
        File(copy, "curriculum/course.json").apply {
            writeText(
                readText().replace(
                    "\"courseVersion\": \"$SHIPPED\"",
                    "\"courseVersion\": \"$version\""
                )
            )
        }
        return copy
    }

    private fun build(source: File, output: File): BuildResult.Built =
        Publisher.build(source, output, null) as BuildResult.Built

    private fun refuse(result: BuildResult): List<String> = (result as BuildResult.Refused).problems

    private fun out(name: String) = File(temp, "out-$name")

    private fun file(name: String, path: String) = File(out(name), path)

    private fun unitFile(source: File) = File(source, "curriculum/units/u01-my-body.json")

    private fun removeLastLessonOfFirstUnit(source: File) = editFirstUnit(source) { lessons ->
        lessons.dropLast(1)
    }

    private fun swapFirstTwoLessonsOfFirstUnit(source: File) = editFirstUnit(source) { lessons ->
        listOf(
            lessons[1].replaceOrdinal(0),
            lessons[0].replaceOrdinal(1)
        ) + lessons.drop(2)
    }

    /**
     * Rewrites the first unit's lessons as JSON text. Done on the text rather than through the DTOs
     * so the test does not depend on how the tool itself reads the course.
     */
    private fun editFirstUnit(source: File, change: (List<JsonElement>) -> List<JsonElement>) {
        val file = unitFile(source)
        val unit = Json.parseToJsonElement(file.readText()).let {
            it as JsonObject
        }
        val lessons = (unit["lessons"] as JsonArray).toList()
        val edited = JsonObject(
            unit + ("lessons" to JsonArray(change(lessons)))
        )
        file.writeText(edited.toString())
    }

    private fun JsonElement.replaceOrdinal(ordinal: Int) = JsonObject(
        (this as JsonObject) +
            ("ordinal" to JsonPrimitive(ordinal))
    )

    private fun unzip(file: File): Map<String, ByteArray> =
        ZipInputStream(file.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.associate { it.name to zip.readBytes() }
        }

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        val STARTER = File("../../content/starter/src/main/assets")
        const val SHIPPED = "2026.09"
        const val NEXT = "2026.10"
        const val ZIP_CLOCK_TICK_MILLIS = 2_100L
    }
}
