package com.nphkhiem.englishforyourchildren.tools.pipeline

import com.nphkhiem.englishforyourchildren.content.validation.AttributionsDto
import com.nphkhiem.englishforyourchildren.content.validation.CourseDto
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import com.nphkhiem.englishforyourchildren.content.validation.UnitDto

/**
 * One course, read the way the app reads a bundle: the same layout and the same parser.
 *
 * [files] maps a bundle path, such as `curriculum/course.json`, to its bytes. That lets an authored
 * folder and an already published pack be read alike, which is what the append-only guard needs.
 */
class CourseBundle(val files: Map<String, ByteArray>) {
    private val parser = CurriculumJsonParser()

    val course: CourseDto = parser.parseCourse(text(COURSE))
    val units: List<UnitDto> = course.units.map { parser.parseUnit(text("$CURRICULUM/${it.file}")) }
    val attributions: AttributionsDto = parser.parseAttributions(text(ATTRIBUTIONS))

    /** Every media file, by the asset id the content refers to it by. */
    val mediaIds: Set<String> = files.keys
        .filter { it.startsWith("$MEDIA/") }
        .map { it.substringAfterLast('/').substringBeforeLast('.') }
        .toSet()

    private fun text(path: String): String =
        requireNotNull(files[path]) { "The bundle has no $path" }.decodeToString()

    companion object {
        const val CURRICULUM = "curriculum"
        const val COURSE = "$CURRICULUM/course.json"
        const val ATTRIBUTIONS = "attribution/attributions.json"
        const val MEDIA = "media"
    }
}
