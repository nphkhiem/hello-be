package com.nphkhiem.englishforyourchildren.tools.pipeline

import com.nphkhiem.englishforyourchildren.content.validation.isNewerThan
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion

/**
 * What a new version may not do to the one before it, per ADR 0017.
 *
 * A child's saved place names a lesson by id. If a lesson they rest on disappears or is moved, they
 * are stranded, so a published lesson and its unit keep their id, their unit and their place for
 * good. Content may be added, and published lessons corrected in place. And a version a television
 * would ignore, because it is not newer than the last one, is refused here rather than discovered
 * there.
 */
object PublishedGuard {
    fun check(previous: CourseBundle, next: CourseBundle): List<String> = buildList {
        val before = CourseVersion(previous.course.courseVersion)
        val after = CourseVersion(next.course.courseVersion)
        if (!after.isNewerThan(before)) {
            add("version ${after.value} is not newer than the published ${before.value}")
        }

        val nextUnits = next.units.associateBy { it.id }
        previous.units.forEach { unit ->
            val kept = nextUnits[unit.id]
            when {
                kept == null -> add("unit ${unit.id} was published and has been removed")

                kept.ordinal != unit.ordinal ->
                    add("unit ${unit.id} moved from place ${unit.ordinal} to ${kept.ordinal}")
            }
        }

        val nextLessons = next.units.flatMap { unit ->
            unit.lessons.map {
                it.id to
                    (unit.id to it.ordinal)
            }
        }
            .toMap()
        previous.units.forEach { unit ->
            unit.lessons.forEach { lesson ->
                val kept = nextLessons[lesson.id]
                when {
                    kept == null -> add("lesson ${lesson.id} was published and has been removed")

                    kept != (unit.id to lesson.ordinal) -> add(
                        "lesson ${lesson.id} moved from ${unit.id} place ${lesson.ordinal} " +
                            "to ${kept.first} place ${kept.second}"
                    )
                }
            }
        }
    }
}
