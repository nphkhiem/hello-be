package com.nphkhiem.englishforyourchildren.data.curriculum

import android.content.res.AssetManager
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.Attribution
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * The course that shipped inside the app.
 *
 * Read once and kept, because packaged content cannot change while the app is running. A bundle
 * that will not parse or will not validate is [DomainError.InvalidContent], which the recovery
 * family already knows how to explain, rather than an exception halfway through a lesson.
 */
class PackagedCurriculumRepository @Inject constructor(
    private val assets: AssetManager,
    private val parser: CurriculumJsonParser = CurriculumJsonParser(),
    private val validator: CurriculumValidator = CurriculumValidator()
) : CurriculumRepository {

    private val loaded: DomainResult<Course> by lazy { load() }

    override fun observeCourse(): Flow<DomainResult<Course>> = flow { emit(loaded) }

    override suspend fun getLesson(id: LessonId, version: CourseVersion): DomainResult<Lesson> {
        val course = loaded as? DomainResult.Success ?: return loaded as DomainResult.Failure
        if (course.value.version != version) return DomainResult.Failure(DomainError.LessonNotFound)

        val lesson = course.value.units
            .flatMap { it.lessons }
            .firstOrNull { it.id == id }
            ?: return DomainResult.Failure(DomainError.LessonNotFound)
        return DomainResult.Success(lesson)
    }

    override suspend fun getAttributions(): DomainResult<List<Attribution>> = runCatching {
        val entries = parser.parseAttributions(read(ATTRIBUTIONS)).entries.map {
            Attribution(
                assetId = AssetId(it.assetId),
                source = it.source,
                licence = it.licence,
                attributionText = it.attributionText
            )
        }
        DomainResult.Success(entries)
    }.getOrElse { DomainResult.Failure(DomainError.InvalidContent) }

    private fun load(): DomainResult<Course> = runCatching {
        val course = parser.parseCourse(read(COURSE))
        val units = course.units.map { parser.parseUnit(read("$CURRICULUM/${it.file}")) }
        val problems = validator.validate(course, units, availableAssets())
        if (problems.isNotEmpty()) return DomainResult.Failure(DomainError.InvalidContent)
        DomainResult.Success(parser.toDomain(course, units))
    }.getOrElse { DomainResult.Failure(DomainError.InvalidContent) }

    /** Every media file that actually shipped, by the id the content refers to it by. */
    private fun availableAssets(): Set<String> =
        runCatching { assets.list(MEDIA)?.map { it.substringBeforeLast('.') }?.toSet() }
            .getOrNull()
            .orEmpty()

    private fun read(path: String) = assets.open(path).bufferedReader().use { it.readText() }

    private companion object {
        const val CURRICULUM = "curriculum"
        const val COURSE = "$CURRICULUM/course.json"
        const val ATTRIBUTIONS = "attribution/attributions.json"
        const val MEDIA = "media"
    }
}
