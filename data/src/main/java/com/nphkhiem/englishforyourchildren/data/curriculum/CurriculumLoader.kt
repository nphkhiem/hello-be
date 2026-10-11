package com.nphkhiem.englishforyourchildren.data.curriculum

import com.nphkhiem.englishforyourchildren.content.validation.ContentProblem
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumValidator
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.Attribution
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import javax.inject.Inject

/**
 * Reads one course bundle and decides whether it can be taught, wherever its files are.
 *
 * The seed in the APK and a downloaded pack share a layout on purpose, so they share this too: the
 * same parser and the same validator stand between a child and either one, and a pack cannot be
 * held to a looser standard than the content that shipped.
 *
 * [read] opens a path inside the bundle, such as `curriculum/course.json`, and may throw: a bundle
 * that cannot be read is [DomainError.InvalidContent] like one that cannot be parsed.
 */
class CurriculumLoader @Inject constructor(
    private val parser: CurriculumJsonParser,
    private val validator: CurriculumValidator
) {
    /** [availableAssets] names every media file that came with the bundle, by asset id. */
    fun course(read: (String) -> String, availableAssets: Set<String>): DomainResult<Course> =
        runCatching {
            val course = parser.parseCourse(read(COURSE))
            val units = course.units.map { parser.parseUnit(read("$CURRICULUM/${it.file}")) }
            val problems = validator.validate(course, units, availableAssets)
            // Only a broken graph stops a lesson. A missing recording or picture is work still
            // owed, and the app was built to run without it: the words are on screen either way.
            // Blocking here would mean no lesson could open until every file existed.
            if (problems.any { it.kind == ContentProblem.Kind.BROKEN_GRAPH }) {
                return DomainResult.Failure(DomainError.InvalidContent)
            }
            DomainResult.Success(parser.toDomain(course, units))
        }.getOrElse { DomainResult.Failure(DomainError.InvalidContent) }

    fun attributions(read: (String) -> String): DomainResult<List<Attribution>> = runCatching {
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

    companion object {
        const val CURRICULUM = "curriculum"
        const val COURSE = "$CURRICULUM/course.json"
        const val ATTRIBUTIONS = "attribution/attributions.json"
        const val MEDIA = "media"
    }
}

/** The lesson [id] in this course, or [DomainError.LessonNotFound]. */
internal fun Course.lesson(id: LessonId): DomainResult<Lesson> = units.flatMap { it.lessons }
    .firstOrNull { it.id == id }
    ?.let { DomainResult.Success(it) }
    ?: DomainResult.Failure(DomainError.LessonNotFound)
