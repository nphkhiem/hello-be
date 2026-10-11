package com.nphkhiem.englishforyourchildren.data.pack

import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import com.nphkhiem.englishforyourchildren.content.validation.ManifestDto
import com.nphkhiem.englishforyourchildren.content.validation.SUPPORTED_SCHEMA_VERSION
import com.nphkhiem.englishforyourchildren.content.validation.isNewerThan
import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader
import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader.Companion.MEDIA
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Where an update stands, as a caregiver's Settings row will show it.
 *
 * ```text
 * Idle -> Checking -> UpToDate
 *                  -> Available -> Downloading -> Verifying -> Staged
 * ```
 *
 * Going live is not a state here. It is [PackActivation]'s, because it depends on the children
 * using the television rather than on the download, and it has to work after a restart, when this
 * object remembers nothing.
 *
 * [Refused] is the only failure with a state of its own, because it is the only kind a caregiver can
 * act on or must be told about. Everything else, an unreachable server, a dropped connection, a
 * checksum that does not match, returns to the state before it and is simply tried again later.
 */
sealed interface ContentUpdate {
    data object Idle : ContentUpdate

    data object Checking : ContentUpdate

    data object UpToDate : ContentUpdate

    data class Available(val version: CourseVersion, val sizeBytes: Long) : ContentUpdate

    data class Downloading(val version: CourseVersion, val packsDone: Int, val packsTotal: Int) :
        ContentUpdate

    data class Verifying(val version: CourseVersion) : ContentUpdate

    /** On disk and complete, waiting for a moment no child is in a lesson. */
    data class Staged(val version: CourseVersion) : ContentUpdate

    data class Refused(val reason: Refusal) : ContentUpdate
}

enum class Refusal {
    /** The manifest is not the one the owner signed. Could be tampering, so a caregiver is told. */
    SIGNATURE_INVALID,

    /** This version needs a content schema the installed app cannot read. */
    APP_UPDATE_NEEDED,

    /** Signed and intact, and still not a course that can be taught. Retrying will not change it. */
    CONTENT_INVALID
}

/**
 * Fetches, checks and stages new lessons, without ever touching the ones a child is using.
 *
 * Downloads land in a workspace, are checked there, and move into place in one rename; going live
 * is one pointer write, made later by [PackActivation]. So whichever step a power cut or a dropped connection interrupts, the live
 * pack is exactly as it was and the application still works.
 *
 * Only a version newer than the live one is offered. The signature proves the owner published a
 * manifest, not that they published it last: someone controlling the hosting could serve an old,
 * genuinely signed manifest and roll every television back. Refusing anything not newer closes that.
 *
 * [liveVersion] is the version a child is being taught now, or null if even the seed cannot be read.
 */
class ContentUpdater(
    private val fetcher: ContentFetcher,
    private val verifier: PackVerifier,
    private val parser: CurriculumJsonParser,
    private val loader: CurriculumLoader,
    private val packs: ContentPackStore,
    private val liveVersion: suspend () -> CourseVersion?
) {
    private val current = MutableStateFlow<ContentUpdate>(ContentUpdate.Idle)
    val state: StateFlow<ContentUpdate> = current.asStateFlow()

    // One operation at a time: a check landing during a download would otherwise replace the
    // manifest the download is working from.
    private val oneAtATime = Mutex()

    /** The verified manifest behind the current [ContentUpdate.Available], held for [download]. */
    private var offered: ManifestDto? = null

    /** Asks the server what is published. A signed, newer version becomes [ContentUpdate.Available]. */
    suspend fun check() = oneAtATime.withLock {
        withContext(Dispatchers.IO) {
            val before = current.value
            current.value = ContentUpdate.Checking
            current.value = try {
                decide(fetchManifest())
            } catch (_: IOException) {
                before
            }
        }
    }

    /** Fetches and stages the [ContentUpdate.Available] version. Anything else is left alone. */
    suspend fun download() = oneAtATime.withLock {
        withContext(Dispatchers.IO) {
            val available = current.value as? ContentUpdate.Available ?: return@withContext
            val manifest = offered ?: return@withContext
            val staging = packs.stage(available.version)
            current.value = try {
                assemble(manifest, available.version, staging)
            } catch (_: IOException) {
                packs.discard(staging)
                available
            } catch (_: UnusablePack) {
                packs.discard(staging)
                ContentUpdate.Refused(Refusal.CONTENT_INVALID)
            }
        }
    }

    /**
     * Looks, and fetches anything newer, without anyone asking: the prefetch that runs while a child
     * is in a lesson. Every failure is as silent as the steps it is made of.
     */
    suspend fun refresh() {
        check()
        download()
    }

    private fun fetchManifest(): ContentUpdate {
        val bytes = fetch(MANIFEST, MAX_MANIFEST_BYTES)
        val signature = fetch(SIGNATURE, MAX_SIGNATURE_BYTES)
        val signed = verifier.verify(bytes, signature)
            ?: return ContentUpdate.Refused(Refusal.SIGNATURE_INVALID)
        // Signed and unreadable is the owner's mistake rather than a fault in transit, and
        // fetching it again will not change it.
        val manifest = runCatching { parser.parseManifest(signed.bytes.decodeToString()) }
            .getOrNull()
            ?: return ContentUpdate.Refused(Refusal.CONTENT_INVALID)
        return offer(manifest)
    }

    private suspend fun decide(update: ContentUpdate): ContentUpdate = when (update) {
        is ContentUpdate.Available -> {
            val live = liveVersion()
            when {
                live != null && !update.version.isNewerThan(live) -> ContentUpdate.UpToDate
                packs.directory(update.version) != null -> ContentUpdate.Staged(update.version)
                else -> update
            }
        }

        else -> update
    }

    private fun offer(manifest: ManifestDto): ContentUpdate {
        if (manifest.minAppSchemaVersion > SUPPORTED_SCHEMA_VERSION) {
            return ContentUpdate.Refused(Refusal.APP_UPDATE_NEEDED)
        }
        val version = runCatching { CourseVersion(manifest.courseVersion) }.getOrNull()
        if (version == null || manifest.packs.isEmpty()) {
            return ContentUpdate.Refused(Refusal.CONTENT_INVALID)
        }
        offered = manifest
        return ContentUpdate.Available(version, manifest.packs.sumOf { it.sizeBytes })
    }

    private fun assemble(
        manifest: ManifestDto,
        version: CourseVersion,
        staging: ContentPackStore.Staging
    ): ContentUpdate {
        val archives = manifest.packs.mapIndexed { index, pack ->
            current.value = ContentUpdate.Downloading(version, index, manifest.packs.size)
            File(staging.archives, "$index.zip").also { save(pack.url, pack.sizeBytes, it) }
        }

        current.value = ContentUpdate.Verifying(version)
        manifest.packs.zip(archives).forEach { (pack, archive) ->
            // A mismatch is a download gone wrong, not a bad pack: retried, never refused.
            if (sha256(archive) != pack.sha256.lowercase()) throw IOException("Checksum mismatch")
        }
        archives.forEach { unpack(it, staging.bundle) }

        val course = loader.course({ File(staging.bundle, it).readText() }, mediaIn(staging.bundle))
        if (course !is DomainResult.Success || course.value.version != version) {
            throw UnusablePack()
        }

        packs.commit(staging, version)
        return ContentUpdate.Staged(version)
    }

    private fun fetch(path: String, limit: Long): ByteArray =
        fetcher.open(path).use { it.readCapped(limit) }

    private fun save(path: String, limit: Long, into: File) {
        fetcher.open(path).use { input ->
            into.outputStream().use { output -> input.copyCapped(output, limit) }
        }
    }

    /**
     * Unpacks [archive] into [bundle], refusing any entry that would land outside it. Signed and
     * checksummed archives are the owner's, but an entry named `../` is a pipeline bug that would
     * write over the app's own files, and a pack that does that is no pack.
     */
    private fun unpack(archive: File, bundle: File) {
        val root = bundle.canonicalFile
        var unpacked = 0L
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val target = File(root, entry.name).canonicalFile
                if (!target.path.startsWith(root.path + File.separator)) throw UnusablePack()
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    unpacked += try {
                        target.outputStream().use {
                            zip.copyCapped(it, MAX_UNPACKED_BYTES - unpacked)
                        }
                    } catch (_: TooLarge) {
                        // Past every honest pack size: the archive is wrong, not the connection.
                        throw UnusablePack()
                    }
                }
            }
        }
    }

    private fun mediaIn(bundle: File): Set<String> =
        File(bundle, MEDIA).walk().filter { it.isFile }.map { it.nameWithoutExtension }.toSet()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            generateSequence { input.read(buffer).takeIf { it >= 0 } }
                .forEach { digest.update(buffer, 0, it) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** A pack that arrived intact and still cannot be taught. */
    private class UnusablePack : Exception()

    private companion object {
        const val MANIFEST = "manifest.json"
        const val SIGNATURE = "manifest.json.sig"
        const val MAX_MANIFEST_BYTES = 1L shl 20
        const val MAX_SIGNATURE_BYTES = 1L shl 10
        const val MAX_UNPACKED_BYTES = 1L shl 30
        const val BUFFER_BYTES = 1 shl 16
    }
}

private fun InputStream.readCapped(limit: Long): ByteArray =
    java.io.ByteArrayOutputStream().also { copyCapped(it, limit) }.toByteArray()

/** More bytes than were declared or allowed: transient while downloading, fatal while unpacking. */
private class TooLarge : IOException("More bytes than declared")

/** Copies at most [limit] bytes, and fails rather than truncating when there are more. */
private fun InputStream.copyCapped(output: java.io.OutputStream, limit: Long): Long {
    val buffer = ByteArray(1 shl 16)
    var copied = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) return copied
        copied += read
        if (copied > limit) throw TooLarge()
        output.write(buffer, 0, read)
    }
}
