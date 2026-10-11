package com.nphkhiem.englishforyourchildren.tools.pipeline

import com.nphkhiem.englishforyourchildren.content.validation.AttributionLedger
import com.nphkhiem.englishforyourchildren.content.validation.ContentProblem
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumValidator
import com.nphkhiem.englishforyourchildren.content.validation.ManifestDto
import com.nphkhiem.englishforyourchildren.content.validation.PackRefDto
import com.nphkhiem.englishforyourchildren.content.validation.SUPPORTED_SCHEMA_VERSION
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json

/** What a build produced, or why it produced nothing. */
sealed interface BuildResult {
    /** [notes] are things worth a reviewer's eye that do not stop a publish, such as unmade media. */
    data class Built(val manifest: ManifestDto, val notes: List<String>) : BuildResult

    data class Refused(val problems: List<String>) : BuildResult
}

/**
 * Turns an authored course into what a television downloads: packs, and the manifest naming them.
 *
 * Nothing is signed here. The signature is the record that a person approved the result, so it is
 * applied afterwards by [Signing], once someone has looked at what this wrote.
 *
 * The packs are a course pack (the course file and its credits), one pack per unit, and one pack of
 * media. Unpacked into one folder they are exactly the bundle that was authored. Every archive is
 * byte-for-byte reproducible: the same course always produces the same checksums, so a reviewer can
 * rebuild and compare rather than trust.
 */
object Publisher {
    private val json = Json { prettyPrint = true }

    fun build(source: File, output: File, previousPublished: File?): BuildResult {
        val bundle = CourseBundle(readFolder(source))
        val problems = check(bundle) +
            previousPublished?.let { PublishedGuard.check(readPublished(it), bundle) }.orEmpty()
        if (problems.isNotEmpty()) return BuildResult.Refused(problems)

        val version = bundle.course.courseVersion
        val packs = packsOf(bundle).map { (id, files) ->
            val bytes = zip(files)
            val url = "$PACKS/$version/$id.zip"
            File(output, url).apply {
                parentFile.mkdirs()
                writeBytes(bytes)
            }
            PackRefDto(id = id, url = url, sizeBytes = bytes.size.toLong(), sha256 = sha256(bytes))
        }
        val manifest = ManifestDto(
            schemaVersion = MANIFEST_SCHEMA,
            courseVersion = version,
            minAppSchemaVersion = bundle.course.schemaVersion,
            packs = packs
        )
        File(output, MANIFEST).writeText(json.encodeToString(ManifestDto.serializer(), manifest))
        return BuildResult.Built(manifest, notes(bundle))
    }

    /** Everything a television would refuse, plus what the licences require of a publish. */
    private fun check(bundle: CourseBundle): List<String> {
        val graph = CurriculumValidator()
            .validate(bundle.course, bundle.units, bundle.mediaIds)
            .filter { it.kind != ContentProblem.Kind.MISSING_ASSET }
        val licences = AttributionLedger().checkShipping(
            bundle.mediaIds,
            bundle.attributions.entries
        )
        val schema = if (bundle.course.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            listOf("schema ${bundle.course.schemaVersion} is newer than any app reads")
        } else {
            emptyList()
        }
        return (graph + licences).map { it.toString() } + schema
    }

    /** Media the content names that nobody has made yet. The app runs without it; a person decides. */
    private fun notes(bundle: CourseBundle): List<String> = CurriculumValidator()
        .validate(bundle.course, bundle.units, bundle.mediaIds)
        .filter { it.kind == ContentProblem.Kind.MISSING_ASSET }
        .map { it.toString() }

    private fun packsOf(bundle: CourseBundle): List<Pair<String, Map<String, ByteArray>>> {
        val unitFiles = bundle.course.units.associate {
            it.id to
                "${CourseBundle.CURRICULUM}/${it.file}"
        }
        return buildList {
            add("course" to bundle.files.filterKeys { it in COURSE_FILES })
            unitFiles.forEach { (id, path) -> add(id to bundle.files.filterKeys { it == path }) }
            val media = bundle.files.filterKeys { it.startsWith("${CourseBundle.MEDIA}/") }
            if (media.isNotEmpty()) add("media" to media)
        }
    }

    /** The previously published version, read back out of its own manifest and packs. */
    fun readPublished(published: File): CourseBundle {
        val manifest = CurriculumJsonParser().parseManifest(File(published, MANIFEST).readText())
        val files = manifest.packs.flatMap { unzip(File(published, it.url).readBytes()).entries }
            .associate { it.key to it.value }
        return CourseBundle(files)
    }

    private fun readFolder(source: File): Map<String, ByteArray> = source.walkTopDown()
        .filter { it.isFile && !it.name.startsWith('.') }
        .associate { it.relativeTo(source).invariantSeparatorsPath to it.readBytes() }

    /** Sorted entries and a fixed timestamp, so the same files always zip to the same bytes. */
    private fun zip(files: Map<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                files.toSortedMap().forEach { (path, bytes) ->
                    zip.putNextEntry(ZipEntry(path).apply { setTimeLocal(FIXED_TIME) })
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }.toByteArray()

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> =
        ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }
                .filterNot { it.isDirectory }
                .associate { it.name to zip.readBytes() }
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    const val MANIFEST = "manifest.json"
    private const val PACKS = "packs"
    private const val MANIFEST_SCHEMA = 1
    private val COURSE_FILES = setOf(CourseBundle.COURSE, CourseBundle.ATTRIBUTIONS)
    private val FIXED_TIME: LocalDateTime = LocalDateTime.of(1980, 1, 1, 0, 0)
}
