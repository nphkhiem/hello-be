package com.nphkhiem.englishforyourchildren.data.curriculum

import com.nphkhiem.englishforyourchildren.domain.model.Activity
import com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseId
import com.nphkhiem.englishforyourchildren.domain.model.CourseUnit
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import kotlinx.serialization.json.Json

/**
 * Turns the files on disk into the domain, or says why it cannot.
 *
 * Lenient about fields it does not know, so content written for a later app does not crash an older
 * one, and strict about everything it does read. Nothing here decides whether the content is
 * sensible; that is [CurriculumValidator]'s job, and it runs first.
 */
class CurriculumJsonParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
) {
    fun parseCourse(source: String): CourseDto = json.decodeFromString(source)

    fun parseUnit(source: String): UnitDto = json.decodeFromString(source)

    fun parseAttributions(source: String): AttributionsDto = json.decodeFromString(source)

    /**
     * The domain course.
     *
     * The activity payload, what a question actually asks, stays in the DTO for now. `Activity` in
     * the domain carries identity, order and family, and giving it content is a change that should
     * happen when a screen is ready to read it rather than because a parser exists.
     */
    fun toDomain(course: CourseDto, units: List<UnitDto>): Course = Course(
        id = CourseId(course.id),
        version = CourseVersion(course.courseVersion),
        schemaVersion = course.schemaVersion,
        supportedLocales = course.supportedLocales.toSet(),
        units = units.sortedBy { it.ordinal }.map { it.toDomain(course.id) }
    )

    private fun UnitDto.toDomain(courseId: String) = CourseUnit(
        id = UnitId(id),
        courseId = CourseId(courseId),
        ordinal = ordinal,
        theme = theme,
        lessons = lessons.sortedBy { it.ordinal }.map { it.toDomain(id) }
    )

    private fun LessonDto.toDomain(unitId: String) = Lesson(
        id = LessonId(id),
        unitId = UnitId(unitId),
        ordinal = ordinal,
        activities = activities.sortedBy { it.ordinal }.map { it.toDomain() }
    )

    private fun ActivityDto.toDomain() = Activity(
        id = ActivityId(id),
        ordinal = ordinal,
        family = ActivityFamily.valueOf(family)
    )
}
