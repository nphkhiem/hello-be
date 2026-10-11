package com.nphkhiem.englishforyourchildren.data.pack

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Where downloaded lesson packs live, and which one the app reads.
 *
 * One directory per course version under [root], laid out exactly like the seed inside the APK, so
 * the same loader reads both. Content is immutable, so it is files rather than Room rows: Room owns
 * the learning data that changes, and nothing here ever does once written.
 *
 * Which pack is live is a single DataStore key. Activating one is therefore a single write, and
 * there is no moment at which a television has half switched from one course to the next.
 */
class ContentPackStore(private val root: File, private val preferences: DataStore<Preferences>) {

    /**
     * The live pack's version, or null when the seed is what a child should get.
     *
     * Null as well when the pointer names a pack that is no longer on disk, or when the preferences
     * cannot be read. Either way the seed in the APK is still there, and a child gets a lesson.
     */
    fun observeActiveVersion(): Flow<CourseVersion?> = preferences.data
        .map { stored -> stored[ACTIVE]?.let(::CourseVersion)?.takeIf { directory(it) != null } }
        .catch { cause -> if (cause is IOException) emit(null) else throw cause }

    /** The pack for [version], or null when there is none on disk. */
    fun directory(version: CourseVersion): File? =
        File(root, pathSegment(version)).takeIf { it.isDirectory }

    /**
     * Makes [version] the course every new session reads.
     *
     * Refused for a version with no pack on disk: the pointer must only ever name something that
     * can be read, or the next launch would quietly fall back to the seed and look like a lost
     * download.
     */
    suspend fun activate(version: CourseVersion) {
        requireNotNull(directory(version)) { "No pack on disk for ${version.value}" }
        preferences.edit { it[ACTIVE] = version.value }
    }

    /**
     * A fresh, empty workspace for assembling [version], away from anything the app reads.
     *
     * Anything a previous attempt left behind is cleared first, so an assembly killed halfway by a
     * power cut cannot leak into the next one.
     */
    fun stage(version: CourseVersion): Staging {
        val workspace = File(root, "$STAGING/${pathSegment(version)}")
        workspace.deleteRecursively()
        return Staging(File(workspace, "bundle"), File(workspace, "archives")).also {
            if (!it.bundle.mkdirs() || !it.archives.mkdirs()) {
                throw IOException("Could not prepare a workspace for ${version.value}")
            }
        }
    }

    /**
     * Moves an assembled bundle into place in a single rename, so a pack directory is only ever
     * complete: the reader never sees one half unpacked. The workspace goes with it.
     */
    fun commit(staging: Staging, version: CourseVersion) {
        val target = File(root, pathSegment(version))
        if (target.exists() || !staging.bundle.renameTo(target)) {
            throw IOException("Could not move ${version.value} into place")
        }
        File(root, STAGING).deleteRecursively()
    }

    /** Clears an abandoned workspace. The live pack is never in one. */
    fun discard(staging: Staging) {
        staging.bundle.parentFile?.deleteRecursively()
    }

    /**
     * Deletes every pack but [keep]. Only after [keep] is live: until then the old pack is what a
     * child is reading, and the seed in the APK stays underneath all of them regardless.
     */
    fun evictAllBut(keep: CourseVersion) {
        root.listFiles()?.filter {
            it.name != pathSegment(keep)
        }?.forEach { it.deleteRecursively() }
    }

    /** Where one version is assembled: [bundle] becomes the pack, [archives] holds the downloads. */
    class Staging(val bundle: File, val archives: File)

    /**
     * The version as one directory name. It arrives in a signed manifest, but a value that could
     * step out of [root] is refused anyway: it costs one check, and the alternative is trusting that
     * nothing else ever writes this key. Hidden names are refused too, which keeps every version
     * clear of the staging workspace.
     */
    private fun pathSegment(version: CourseVersion): String = version.value.also {
        require(!it.startsWith('.') && '/' !in it && '\\' !in it) {
            "A course version must be a single, visible directory name"
        }
    }

    private companion object {
        val ACTIVE = stringPreferencesKey("active_course_version")
        const val STAGING = ".staging"
    }
}
