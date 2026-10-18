package com.nphkhiem.englishforyourchildren.data.pack

import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The starter course that ships in the APK, as files on disk. */
val SEED = File("../content/starter/src/main/assets")

val SEED_VERSION = CourseVersion("2026.09")

/** The shipped seed, read off disk as the app reads it from the APK. */
class SeedFromDisk(private val loader: CurriculumLoader) : CurriculumRepository {
    private val course = loader.course({ File(SEED, it).readText() }, emptySet())

    override fun observeCourse(): Flow<DomainResult<Course>> = flowOf(course)

    override suspend fun getLesson(id: LessonId, version: CourseVersion): DomainResult<Lesson> {
        val loaded = course as? DomainResult.Success ?: return course as DomainResult.Failure
        if (loaded.value.version != version) {
            return DomainResult.Failure(DomainError.LessonNotFound)
        }
        return loaded.value.lesson(id)
    }

    override suspend fun getAttributions(): DomainResult<List<Attribution>> =
        loader.attributions { File(SEED, it).readText() }
}
