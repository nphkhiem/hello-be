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
     * The version as one directory name. It arrives in a signed manifest, but a value that could
     * step out of [root] is refused anyway: it costs one check, and the alternative is trusting that
     * nothing else ever writes this key.
     */
    private fun pathSegment(version: CourseVersion): String = version.value.also {
        require(it != "." && it != ".." && '/' !in it && '\\' !in it) {
            "A course version must be a single directory name"
        }
    }

    private companion object {
        val ACTIVE = stringPreferencesKey("active_course_version")
    }
}
