package com.nphkhiem.englishforyourchildren.data.curriculum

import android.content.res.AssetManager
import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader.Companion.MEDIA
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
 * The course that shipped inside the app: the seed every television starts from, and the floor a
 * downloaded pack falls back to.
 *
 * Read once and kept, because packaged content cannot change while the app is running. A bundle
 * that will not parse or will not validate is [DomainError.InvalidContent], which the recovery
 * family already knows how to explain, rather than an exception halfway through a lesson.
 */
class PackagedCurriculumRepository @Inject constructor(
    private val assets: AssetManager,
    private val loader: CurriculumLoader
) : CurriculumRepository {

    private val loaded: DomainResult<Course> by lazy { loader.course(::read, availableAssets()) }

    override fun observeCourse(): Flow<DomainResult<Course>> = flow { emit(loaded) }

    override suspend fun getLesson(id: LessonId, version: CourseVersion): DomainResult<Lesson> {
        val course = loaded as? DomainResult.Success ?: return loaded as DomainResult.Failure
        if (course.value.version != version) return DomainResult.Failure(DomainError.LessonNotFound)
        return course.value.lesson(id)
    }

    override suspend fun getAttributions(): DomainResult<List<Attribution>> =
        loader.attributions(::read)

    /** Every media file that actually shipped, by the id the content refers to it by. */
    private fun availableAssets(): Set<String> =
        runCatching { assets.list(MEDIA)?.map { it.substringBeforeLast('.') }?.toSet() }
            .getOrNull()
            .orEmpty()

    private fun read(path: String) = assets.open(path).bufferedReader().use { it.readText() }
}
