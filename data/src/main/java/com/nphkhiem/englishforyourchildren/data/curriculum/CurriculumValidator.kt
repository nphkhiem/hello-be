package com.nphkhiem.englishforyourchildren.data.curriculum

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
 */
class CurriculumValidator @Inject constructor() {

    fun validate(
        course: CourseDto,
        units: List<UnitDto>,
        availableAssets: Set<String>
    ): List<ContentProblem> {
        val problems = mutableListOf<ContentProblem>()

        if (course.schemaVersion != SUPPORTED_SCHEMA) {
            problems += ContentProblem(
                course.id,
                "schema version ${course.schemaVersion} is not the $SUPPORTED_SCHEMA this app reads"
            )
        }
        if (course.supportedLocales.isEmpty()) {
            problems += ContentProblem(course.id, "a course nobody can be spoken to in")
        }

        val declared = course.units.map { it.id }
        val delivered = units.map { it.id }
        (declared - delivered.toSet()).forEach {
            problems += ContentProblem(course.id, "unit $it is declared but its file is missing")
        }

        units.forEach { unit -> problems += validateUnit(unit, availableAssets) }
        problems += duplicates(units.flatMap { u -> u.lessons.map { it.id } }, "lesson")
        return problems
    }

    private fun validateUnit(unit: UnitDto, assets: Set<String>): List<ContentProblem> {
        val problems = mutableListOf<ContentProblem>()
        problems += ordinalsInOrder(unit.lessons.map { it.ordinal }, unit.id, "lesson")

        unit.lessons.forEach { lesson ->
            problems += ordinalsInOrder(
                lesson.activities.map { it.ordinal },
                lesson.id,
                "activity"
            )
            problems += duplicates(lesson.activities.map { it.id }, "activity")

            if (lesson.activities.isEmpty()) {
                problems += ContentProblem(lesson.id, "a lesson with nothing in it")
            }

            lesson.activities.forEach { activity ->
                problems += validateActivity(activity, assets)
            }
        }
        return problems
    }

    private fun validateActivity(activity: ActivityDto, assets: Set<String>): List<ContentProblem> {
        val problems = mutableListOf<ContentProblem>()

        if (activity.family !in FAMILIES) {
            problems +=
                ContentProblem(
                    activity.id,
                    "'${activity.family}' is not a family this app has a screen for"
                )
        }
        if (activity.prompt.isBlank()) {
            problems += ContentProblem(activity.id, "an activity that asks nothing")
        }

        // An answer that is not on offer is a question no child can get right.
        val offered = activity.choices.map { it.skillId }.toSet()
        activity.correctSkillId?.let {
            if (it !in offered) {
                problems +=
                    ContentProblem(activity.id, "the correct answer $it is not one of the choices")
            }
        }
        if (activity.correctSkillId == null && activity.family !in FAMILIES_WITHOUT_AN_ANSWER) {
            problems += ContentProblem(activity.id, "a question with no right answer")
        }

        problems += duplicates(activity.choices.map { it.skillId }, "choice")

        // The check that matters today. Every reference is a file somebody still has to make.
        (
            listOfNotNull(activity.promptAsset, activity.letterAsset) +
                activity.choices.flatMap { listOf(it.image, it.audio) }
            )
            .distinct()
            .filterNot { it in assets }
            .forEach {
                problems += ContentProblem(
                    where = activity.id,
                    what = "asset $it has no file",
                    kind = ContentProblem.Kind.MISSING_ASSET
                )
            }

        return problems
    }

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
        val FAMILIES = setOf(
            "LISTEN_AND_CHOOSE",
            "PICTURE_MATCHING",
            "LETTER_AND_SOUND",
            "SAY_WITH_PIP",
            "REVIEW"
        )

        /** Saying a word with Pip is modelled and unscored, so it has nothing to be right about. */
        val FAMILIES_WITHOUT_AN_ANSWER = setOf("SAY_WITH_PIP")
    }
}
