package com.nphkhiem.englishforyourchildren.data.pack

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumValidator
import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Downloading new lessons, and every way that can go wrong without a child noticing.
 *
 * Packs are real zips of the shipped starter course at a new version, the manifest is signed with a
 * real Ed25519 key, and the server is a map that can be told to drop the connection at any byte.
 * After every interruption the same question is asked: is the course a child is using still there,
 * unchanged?
 */
class ContentUpdaterTest {

    @TempDir
    lateinit var temp: File

    private val owner = KeyPairGenerator.getInstance(ED25519).generateKeyPair()
    private val parser = CurriculumJsonParser()
    private val loader = CurriculumLoader(parser, CurriculumValidator())
    private val server = FakeServer()

    private val store by lazy {
        ContentPackStore(
            File(temp, "packs"),
            PreferenceDataStoreFactory.create(produceFile = { File(temp, "t.preferences_pb") })
        )
    }
    private val repository by lazy {
        LayeredCurriculumRepository(SeedFromDisk(loader), store, loader)
    }
    private val updater by lazy { newUpdater() }

    @Test
    fun givenANewerSignedVersion_whenChecked_thenItIsAvailable() {
        val archives = publish(NEXT)

        check()

        assertThat(updater.state.value)
            .isEqualTo(ContentUpdate.Available(NEXT, archives.sumOf { it.size.toLong() }))
    }

    @Test
    fun givenTheVersionAlreadyBeingTaught_whenChecked_thenUpToDate() {
        publish(SEED_VERSION)

        check()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.UpToDate)
    }

    @Test
    fun givenAnOlderGenuinelySignedManifest_whenChecked_thenNothingIsOffered() {
        // The replay a signature alone cannot stop: real, signed, and a step backwards.
        publish(CourseVersion("2026.08"))

        check()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.UpToDate)
    }

    @Test
    fun givenAManifestAlteredAfterSigning_whenChecked_thenItIsRefusedAndNothingIsFetched() {
        publish(NEXT)
        server.files[MANIFEST] = server.files.getValue(MANIFEST).decodeToString()
            .replace("\"sizeBytes\":", "\"sizeBytes\": ").encodeToByteArray()

        check()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.Refused(Refusal.SIGNATURE_INVALID))
        assertThat(server.opened).containsExactly(MANIFEST, SIGNATURE)
    }

    @Test
    fun givenAManifestSignedBySomeoneElse_whenChecked_thenItIsRefused() {
        publish(NEXT, signedBy = KeyPairGenerator.getInstance(ED25519).generateKeyPair())

        check()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.Refused(Refusal.SIGNATURE_INVALID))
    }

    @Test
    fun givenAVersionNeedingANewerApp_whenChecked_thenTheCaregiverIsToldToUpdate() {
        publish(NEXT, minAppSchema = 99)

        check()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.Refused(Refusal.APP_UPDATE_NEEDED))
    }

    @Test
    fun givenTheServerCannotBeReached_whenChecked_thenTheStateIsWhereItWas() {
        check()
        assertThat(updater.state.value).isEqualTo(ContentUpdate.Idle)

        publish(NEXT)
        check()
        val available = updater.state.value
        server.files.clear()
        check()

        assertThat(updater.state.value).isEqualTo(available)
    }

    @Test
    fun givenAnAvailableVersion_whenDownloaded_thenItIsStagedButNotYetTaught() {
        publish(NEXT)
        check()

        download()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.Staged(NEXT))
        assertThat(liveVersion()).isEqualTo(SEED_VERSION)
    }

    @Test
    fun givenAStagedVersion_whenActivated_thenChildrenAreTaughtIt() {
        publish(NEXT)
        check()
        download()

        activate()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.Active(NEXT))
        assertThat(liveVersion()).isEqualTo(NEXT)
    }

    @Test
    fun givenANewerVersionActivated_whenTheOldPackIsNoLongerLive_thenItIsDeleted() {
        install(NEXT)
        publish(AFTER_NEXT)
        check()
        download()

        activate()

        assertThat(store.directory(NEXT)).isNull()
        assertThat(store.directory(AFTER_NEXT)).isNotNull()
    }

    @Test
    fun givenAPackThatArrivesDifferentFromItsChecksum_whenDownloaded_thenNothingIsStaged() {
        publish(NEXT)
        check()
        server.files[UNITS_PACK] = server.files.getValue(UNITS_PACK).copyOf().also { it[0] = 0 }

        download()

        assertThat(updater.state.value).isInstanceOf(ContentUpdate.Available::class.java)
        assertThat(store.directory(NEXT)).isNull()
    }

    @Test
    fun givenTheConnectionDropsAtAnyPoint_whenDownloading_thenTheLiveCourseIsUntouched() {
        // Every file, cut off at its first byte and halfway through, while a pack is already live.
        install(NEXT)
        publish(AFTER_NEXT)
        val liveBefore = snapshot(NEXT)
        val cuts = listOf(COURSE_PACK, UNITS_PACK).flatMap { listOf(it to 0, it to HALF) }

        cuts.forEach { (path, at) ->
            check()
            server.dropAt(path, at)

            download()

            assertThat(updater.state.value).isInstanceOf(ContentUpdate.Available::class.java)
            assertThat(store.directory(AFTER_NEXT)).isNull()
            assertThat(liveVersion()).isEqualTo(NEXT)
            assertThat(snapshot(NEXT)).isEqualTo(liveBefore)
            assertThat(File(temp, "packs/.staging").walk().filter { it.isFile }.toList()).isEmpty()
            server.dropNothing()
        }

        download()
        assertThat(updater.state.value).isEqualTo(ContentUpdate.Staged(AFTER_NEXT))
    }

    @Test
    fun givenTheConnectionDropsWhileChecking_whenItFails_thenTheLiveCourseIsUntouched() {
        install(NEXT)
        publish(AFTER_NEXT)
        val liveBefore = snapshot(NEXT)

        listOf(MANIFEST, SIGNATURE).forEach { path ->
            server.dropAt(path, 0)
            check()
            assertThat(updater.state.value).isEqualTo(ContentUpdate.Idle)
            assertThat(snapshot(NEXT)).isEqualTo(liveBefore)
        }
    }

    @Test
    fun givenAnAssemblyAPowerCutLeftHalfDone_whenDownloadedAgain_thenItCompletesCleanly() {
        publish(NEXT)
        check()
        File(temp, "packs/.staging/${NEXT.value}/bundle/curriculum/stale.json").apply {
            parentFile?.mkdirs()
            writeText("left behind")
        }

        val fresh = newUpdater()
        runBlocking {
            fresh.check()
            fresh.download()
        }

        assertThat(fresh.state.value).isEqualTo(ContentUpdate.Staged(NEXT))
        assertThat(File(store.directory(NEXT), "curriculum/stale.json").exists()).isFalse()
    }

    @Test
    fun givenAnArchiveReachingOutsideItsDirectory_whenUnpacked_thenThePackIsRefused() {
        publish(NEXT, extra = "../../escaped.txt" to "overwritten")
        check()

        download()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.Refused(Refusal.CONTENT_INVALID))
        assertThat(File(temp, "escaped.txt").exists()).isFalse()
        assertThat(store.directory(NEXT)).isNull()
    }

    @Test
    fun givenASignedPackThatCannotBeTaught_whenDownloaded_thenItIsRefused() {
        publish(NEXT, courseJson = "{ not a course")
        check()

        download()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.Refused(Refusal.CONTENT_INVALID))
        assertThat(store.directory(NEXT)).isNull()
    }

    @Test
    fun givenAPackWhoseCourseNamesAnotherVersion_whenDownloaded_thenItIsRefused() {
        // Staged under one version and claiming another would let a session pin one course and be
        // taught a different one.
        val course = File(SEED, "curriculum/course.json").readText()
            .replace("\"${SEED_VERSION.value}\"", "\"2027.01\"")
        publish(NEXT, courseJson = course)
        check()

        download()

        assertThat(updater.state.value).isEqualTo(ContentUpdate.Refused(Refusal.CONTENT_INVALID))
        assertThat(store.directory(NEXT)).isNull()
    }

    @Test
    fun givenAServerSendingMoreThanTheSignedSize_whenDownloaded_thenNothingIsStaged() {
        // The checksum would match these bytes; only the declared size says there are too many.
        publish(NEXT, understateUnitsBy = 1)
        check()

        download()

        assertThat(updater.state.value).isInstanceOf(ContentUpdate.Available::class.java)
        assertThat(store.directory(NEXT)).isNull()
    }

    @Test
    fun givenAVersionAlreadyDownloaded_whenCheckedAfterARestart_thenItIsStaged() {
        publish(NEXT)
        check()
        download()

        val fresh = newUpdater()
        runBlocking { fresh.check() }

        assertThat(fresh.state.value).isEqualTo(ContentUpdate.Staged(NEXT))
    }

    @Test
    fun givenDottedVersions_whenCompared_thenNumbersCountNotCharacters() {
        assertThat(CourseVersion("2026.10").isNewerThan(CourseVersion("2026.09"))).isTrue()
        assertThat(CourseVersion("1.10.0").isNewerThan(CourseVersion("1.9.0"))).isTrue()
        assertThat(CourseVersion("1.1").isNewerThan(CourseVersion("1.1.0"))).isFalse()
        assertThat(CourseVersion("2026.09").isNewerThan(CourseVersion("2026.09"))).isFalse()
        assertThat(CourseVersion("next").isNewerThan(CourseVersion("2026.09"))).isFalse()
    }

    private fun newUpdater() = ContentUpdater(
        fetcher = server,
        verifier = PackVerifier(owner.public.encoded.takeLast(RAW_KEY_BYTES).toByteArray()),
        parser = parser,
        loader = loader,
        packs = store,
        liveVersion = { liveVersion() }
    )

    private fun check() = runBlocking { updater.check() }

    private fun download() = runBlocking { updater.download() }

    private fun activate() = runBlocking { updater.activate() }

    private fun liveVersion(): CourseVersion? = (
        runBlocking {
            repository.observeCourse().first()
        } as? DomainResult.Success
        )?.value?.version

    /** [version] downloaded and live, through the same path a television takes. */
    private fun install(version: CourseVersion) {
        publish(version)
        val installer = newUpdater()
        runBlocking {
            installer.check()
            installer.download()
            installer.activate()
        }
        check(liveVersion() == version) { "the pack under test should be live" }
    }

    /** Every file of a live pack and its contents, to prove nothing touched it. */
    private fun snapshot(version: CourseVersion): Map<String, String> {
        val root = requireNotNull(store.directory(version))
        return root.walk().filter { it.isFile }
            .associate { it.relativeTo(root).path to it.readText() }
    }

    /**
     * Publishes the starter course as [version]: a course pack and a units pack, a manifest naming
     * both with their real sizes and checksums, and a detached signature over the manifest's bytes.
     */
    private fun publish(
        version: CourseVersion,
        signedBy: java.security.KeyPair = owner,
        minAppSchema: Int = 2,
        courseJson: String? = null,
        extra: Pair<String, String>? = null,
        understateUnitsBy: Int = 0
    ): List<ByteArray> {
        val course = courseJson
            ?: File(SEED, "curriculum/course.json").readText()
                .replace("\"${SEED_VERSION.value}\"", "\"${version.value}\"")
        val coursePack = zip(
            buildMap {
                put("curriculum/course.json", course)
                put(
                    "attribution/attributions.json",
                    File(SEED, "attribution/attributions.json").readText()
                )
                extra?.let { put(it.first, it.second) }
            }
        )
        val unitsPack = zip(
            File(SEED, "curriculum/units").listFiles().orEmpty()
                .associate { "curriculum/units/${it.name}" to it.readText() }
        )
        val packs = listOf(
            entry("course", COURSE_PACK, coursePack),
            entry("units", UNITS_PACK, unitsPack, understateBy = understateUnitsBy)
        ).joinToString(",")
        val manifest = (
            """{"schemaVersion":1,"courseVersion":"${version.value}",""" +
                """"minAppSchemaVersion":$minAppSchema,"packs":[$packs]}"""
            ).encodeToByteArray()

        server.files.clear()
        server.files[MANIFEST] = manifest
        server.files[SIGNATURE] = Signature.getInstance(ED25519).run {
            initSign(signedBy.private)
            update(manifest)
            sign()
        }
        server.files[COURSE_PACK] = coursePack
        server.files[UNITS_PACK] = unitsPack
        return listOf(coursePack, unitsPack)
    }

    private fun entry(id: String, url: String, bytes: ByteArray, understateBy: Int = 0): String {
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        val declared = bytes.size - understateBy
        return """{"id":"$id","url":"$url","sizeBytes":$declared,"sha256":"$sha256"}"""
    }

    private fun zip(files: Map<String, String>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip ->
            files.forEach { (path, text) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(text.encodeToByteArray())
                zip.closeEntry()
            }
        }
    }.toByteArray()

    /** A static file host that can be told to drop the connection partway through one file. */
    private class FakeServer : ContentFetcher {
        val files = mutableMapOf<String, ByteArray>()
        val opened = mutableListOf<String>()
        private var drop: Pair<String, Int>? = null

        fun dropAt(path: String, at: Int) {
            drop = path to at
        }

        fun dropNothing() {
            drop = null
        }

        override fun open(path: String): InputStream {
            opened += path
            val bytes = files[path] ?: throw IOException("404 $path")
            val cut =
                drop?.takeIf { it.first == path }?.second ?: return ByteArrayInputStream(bytes)
            val at = if (cut == HALF) bytes.size / 2 else cut
            return object : InputStream() {
                private var position = 0

                override fun read(): Int {
                    if (position >= at) throw IOException("connection dropped")
                    return bytes[position++].toInt() and 0xff
                }
            }
        }
    }

    private companion object {
        const val ED25519 = "Ed25519"
        const val RAW_KEY_BYTES = 32
        const val MANIFEST = "manifest.json"
        const val SIGNATURE = "manifest.json.sig"
        const val COURSE_PACK = "packs/course.zip"
        const val UNITS_PACK = "packs/units.zip"

        /** A drop point meaning halfway through whichever file it is applied to. */
        const val HALF = -1
        val NEXT = CourseVersion("2026.10")
        val AFTER_NEXT = CourseVersion("2026.11")
    }
}
