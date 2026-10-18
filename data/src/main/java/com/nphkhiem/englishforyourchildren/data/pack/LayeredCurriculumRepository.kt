package com.nphkhiem.englishforyourchildren.data.pack

import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader
import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader.Companion.MEDIA
import com.nphkhiem.englishforyourchildren.data.curriculum.lesson
import com.nphkhiem.englishforyourchildren.domain.model.Attribution
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * The live course: a downloaded pack when one is active and sound, the [seed] otherwise.
 *
 * A pack that will not load is passed over rather than reported. The seed is a complete course, so
 * the honest thing a child can be given is the lesson that works, and a bad pack is the update
 * flow's to explain to a caregiver, never the lesson's to explain to a child.
 *
 * Lessons are read at the version a session pinned, not at whichever version is live now. A pack
 * activating while a child is mid-lesson must not change the lesson under them, and two versions
 * must never mix within one session.
 */
class LayeredCurriculumRepository(
    private val seed: CurriculumRepository,
    private val packs: ContentPackStore,
    private val loader: CurriculumLoader
) : CurriculumRepository {

    // A course version is immutable, so a pack read once never needs reading again.
    private val loaded = ConcurrentHashMap<CourseVersion, DomainResult<Course>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCourse(): Flow<DomainResult<Course>> = packs.observeActiveVersion()
        .flatMapLatest { active ->
            val pack = active?.let(::pack)
            if (pack is DomainResult.Success) flowOf(pack) else seed.observeCourse()
        }
        .flowOn(Dispatchers.IO)

    override suspend fun getLesson(id: LessonId, version: CourseVersion): DomainResult<Lesson> =
        withContext(Dispatchers.IO) {
            val pack = pack(version)
            if (pack is DomainResult.Success) pack.value.lesson(id) else seed.getLesson(id, version)
        }

    /** The credits for the course a child is being shown, which is the live one. */
    override suspend fun getAttributions(): DomainResult<List<Attribution>> =
        withContext(Dispatchers.IO) {
            val active = packs.observeActiveVersion().first()
            val directory = active?.let(packs::directory)
            if (active != null && directory != null && pack(active) is DomainResult.Success) {
                loader.attributions { File(directory, it).readText() }
            } else {
                seed.getAttributions()
            }
        }

    /** The pack for [version], or null when there is none on disk. */
    private fun pack(version: CourseVersion): DomainResult<Course>? {
        val directory = packs.directory(version) ?: return null
        return loaded.getOrPut(version) { load(directory, version) }
    }

    private fun load(directory: File, version: CourseVersion): DomainResult<Course> {
        val course = loader.course({ File(directory, it).readText() }, mediaIn(directory))
        // The directory is the version a session pins and comes back to. A pack claiming another
        // version inside would let a child's checkpoint name one course and their lesson come from
        // a different one.
        return if (course is DomainResult.Success && course.value.version != version) {
            DomainResult.Failure(DomainError.InvalidContent)
        } else {
            course
        }
    }

    private fun mediaIn(directory: File): Set<String> =
        File(directory, MEDIA).walk().filter { it.isFile }.map { it.nameWithoutExtension }.toSet()
}
