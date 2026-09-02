package com.nphkhiem.englishforyourchildren.data.curriculum

import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.CourseId
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.IdentifierRead
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import com.nphkhiem.englishforyourchildren.domain.model.readIdentifier
import javax.inject.Inject

/**
 * What is wrong with a bundle, in words a person can act on.
 *
 * Not an exception, because the point is to report every problem at once rather than the first one.
 * A content author fixing thirty activities should be told about all thirty.
 */
data class ContentProblem(val where: String, val what: String, val kind: Kind = Kind.BROKEN_GRAPH) {
    /**
     * Two different kinds of wrong.
     *
     * A broken graph is content that cannot be taught: a question naming an answer it does not
     * offer, an activity out of order. Nothing can run on it, so loading refuses.
     *
     * A missing file is work nobody has done yet. The app was designed to run without sound, and a
     * word can be shown before it can be heard, so a lesson still runs. It is a release gate, not a
     * runtime one: shipping is what these must block.
     *
     * An unlicensed asset is the same kind of gate seen from the other end: a file the project may
     * not be entitled to ship, or a claim to be entitled that nothing stands behind.
     */
    enum class Kind { BROKEN_GRAPH, MISSING_ASSET, UNLICENSED_ASSET }

    override fun toString() = "$where: $what"
}

/**
 * Checks a packaged course before anybody depends on it.
 *
 * The graph checks are the ones a lesson cannot survive: an activity that names an answer it does
 * not offer, ordinals that do not match their order, an id used twice. The asset check is the one
 * that matters today, because it is the difference between a lesson that runs and a silent screen.
 *
 * Every name is read here before anything builds one, which is ADR 0011's parse helper at the
 * boundary it was promised for. A blank id used to reach a value class and throw out of the middle
 * of the mapping, and the whole bundle came back as invalid content with nothing a content author
 * could act on.
 */
class CurriculumValidator @Inject constructor() {

    fun validate(
        course: CourseDto,
        units: List<UnitDto>,
        availableAssets: Set<String>
    ): List<ContentProblem> {
        val problems = mutableListOf<ContentProblem>()
        val where = course.id.orUnnamed("course")

        problems += named(where, "course id", course.id) { CourseId(it) }
        problems += named(where, "course version", course.courseVersion) { CourseVersion(it) }
        course.units.forEach { ref ->
            problems += named(where, "declared unit id", ref.id) { UnitId(it) }
        }

        if (course.schemaVersion != SUPPORTED_SCHEMA) {
            problems += ContentProblem(
                where,
                "schema version ${course.schemaVersion} is not the $SUPPORTED_SCHEMA this app reads"
            )
        }
        if (course.supportedLocales.isEmpty()) {
            problems += ContentProblem(where, "a course nobody can be spoken to in")
        }

        val declared = course.units.map { it.id }
        val delivered = units.map { it.id }
        (declared - delivered.toSet()).forEach {
            problems += ContentProblem(where, "unit $it is declared but its file is missing")
        }

        units.forEach { unit -> problems += validateUnit(unit, availableAssets) }
        problems += duplicates(units.flatMap { u -> u.lessons.map { it.id } }, "lesson")
        return problems
    }

    private fun validateUnit(unit: UnitDto, assets: Set<String>): List<ContentProblem> {
        val problems = mutableListOf<ContentProblem>()
        val where = unit.id.orUnnamed("unit")

        problems += named(where, "unit id", unit.id) { UnitId(it) }
        problems += ordinalsInOrder(unit.lessons.map { it.ordinal }, where, "lesson")

        unit.lessons.forEach { lesson ->
            val lessonWhere = lesson.id.orUnnamed("lesson")

            problems += named(lessonWhere, "lesson id", lesson.id) { LessonId(it) }
            problems += lesson.teaches.flatMap { taught ->
                named(lessonWhere, "taught skill id", taught) { SkillId(it) }
            }
            problems += ordinalsInOrder(
                lesson.activities.map { it.ordinal },
                lessonWhere,
                "activity"
            )
            problems += duplicates(lesson.activities.map { it.id }, "activity")

            if (lesson.activities.isEmpty()) {
                problems += ContentProblem(lessonWhere, "a lesson with nothing in it")
            }

            lesson.activities.forEach { activity ->
                problems += validateActivity(activity, assets)
            }
        }
        return problems
    }

    private fun validateActivity(activity: ActivityDto, assets: Set<String>): List<ContentProblem> {
        val problems = mutableListOf<ContentProblem>()
        val where = activity.id.orUnnamed("activity")

        problems += named(where, "activity id", activity.id) { ActivityId(it) }
        if (activity.family !in FAMILIES) {
            problems +=
                ContentProblem(
                    where,
                    "'${activity.family}' is not a family this app has a screen for"
                )
        }
        if (activity.prompt.isBlank()) {
            problems += ContentProblem(where, "an activity that asks nothing")
        }

        // An answer that is not on offer is a question no child can get right.
        val offered = activity.choices.map { it.skillId }.toSet()
        activity.correctSkillId?.let { correct ->
            problems += named(where, "correct answer skill id", correct) { SkillId(it) }
            if (correct !in offered) {
                problems +=
                    ContentProblem(where, "the correct answer $correct is not one of the choices")
            }
        }
        if (activity.correctSkillId == null && activity.family !in FAMILIES_WITHOUT_AN_ANSWER) {
            problems += ContentProblem(where, "a question with no right answer")
        }

        // A letter activity is the one family that names something besides its answer, and the
        // mapping cannot build one without it.
        if (activity.family == LETTER_AND_SOUND) {
            problems += named(where, "letter skill id", activity.letterSkillId) { SkillId(it) }
        }

        activity.choices.forEach { choice ->
            problems += named(where, "choice skill id", choice.skillId) { SkillId(it) }
            // A choice is drawn and spoken, so both of its files are named rather than optional.
            // A prompt recording may legitimately be blank: nobody has made one yet.
            problems += named(where, "choice picture", choice.image) { AssetId(it) }
            problems += named(where, "choice recording", choice.audio) { AssetId(it) }
        }

        problems += duplicates(activity.choices.map { it.skillId }, "choice")

        // The check that matters today. Every reference is a file somebody still has to make.
        (
            listOfNotNull(activity.promptAsset, activity.letterAsset) +
                activity.choices.flatMap { listOf(it.image, it.audio) }
            )
            .filter { it.isNotBlank() }
            .distinct()
            .filterNot { it in assets }
            .forEach {
                problems += ContentProblem(
                    where = where,
                    what = "asset $it has no file",
                    kind = ContentProblem.Kind.MISSING_ASSET
                )
            }

        return problems
    }

    /**
     * One name, read before anything builds an identifier out of it.
     *
     * Empty when the value is a name, which is what lets every call site add its answer to the list
     * without asking whether there was anything wrong.
     */
    private fun named(
        where: String,
        field: String,
        value: String?,
        into: (String) -> Any
    ): List<ContentProblem> = when (val read = readIdentifier(value, field, into)) {
        is IdentifierRead.Usable -> emptyList()
        is IdentifierRead.Unusable -> listOf(ContentProblem(where, read.reason))
    }

    /** Something with no name still has to be reported as somewhere. */
    private fun String.orUnnamed(what: String) = ifBlank { "an unnamed $what" }

    private fun ordinalsInOrder(ordinals: List<Int>, owner: String, kind: String) =
        ordinals.mapIndexedNotNull { index, ordinal ->
            if (ordinal == index) {
                null
            } else {
                ContentProblem(owner, "$kind at position $index says it is number $ordinal")
            }
        }

    private fun duplicates(ids: List<String>, kind: String) = ids.groupingBy { it }.eachCount()
        .filterValues { it > 1 }
        .map { ContentProblem(it.key, "the same $kind id appears ${it.value} times") }

    private companion object {
        const val SUPPORTED_SCHEMA = 2
        const val LETTER_AND_SOUND = "LETTER_AND_SOUND"
        val FAMILIES = setOf(
            "LISTEN_AND_CHOOSE",
            "PICTURE_MATCHING",
            LETTER_AND_SOUND,
            "SAY_WITH_PIP",
            "REVIEW"
        )

        /** Saying a word with Pip is modelled and unscored, so it has nothing to be right about. */
        val FAMILIES_WITHOUT_AN_ANSWER = setOf("SAY_WITH_PIP")
    }
}
