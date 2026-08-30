package com.nphkhiem.englishforyourchildren.domain.model

/**
 * The five kinds of activity a lesson is made of.
 *
 * One per built screen, and they share one interaction grammar: a prompt, a way to answer, and the
 * same feedback. A sixth family would be a new screen and a product decision, so the enum is the
 * place that decision has to be made.
 */
enum class ActivityFamily {
    LISTEN_AND_CHOOSE,
    PICTURE_MATCHING,
    LETTER_AND_SOUND,
    SAY_WITH_PIP,
    REVIEW
}

/**
 * One step of a lesson: which step it is, where it comes, and what kind it is.
 *
 * It deliberately carries nothing about what it asks. What a listen-and-choose says, which pictures
 * it offers and which asset it plays are curriculum content, and the blueprint that approves that
 * content does not exist yet. Modelling it here from the built screens would encode a rendering
 * decision as content. The payload arrives with the task that owns it.
 */
data class Activity(val id: ActivityId, val ordinal: Int, val family: ActivityFamily) {
    init {
        require(ordinal >= 0) { "An activity cannot come before the first one" }
    }
}

/**
 * A sitting's worth of activities, in the order a child meets them.
 *
 * Position in [activities] is that order, and the ordinals have to agree with it. A correct set of
 * activities in the wrong sequence is a content mistake, and sorting it here would hide it rather
 * than report it.
 */
data class Lesson(
    val id: LessonId,
    val unitId: UnitId,
    val ordinal: Int,
    val activities: List<Activity>
) {
    init {
        require(ordinal >= 0) { "A lesson cannot come before the first one" }
        requireOrderedSteps(activities, "lesson ${id.value}") { it.ordinal }
        requireDistinct(activities.map { it.id }, "lesson ${id.value}", "activity")
    }
}

/**
 * A themed group of lessons, such as My Home.
 *
 * The theme is what changes the scenery behind the stage. It is a name here and nothing more; how
 * it is drawn is the UI's business.
 */
data class CourseUnit(
    val id: UnitId,
    val courseId: CourseId,
    val ordinal: Int,
    val theme: String,
    val lessons: List<Lesson>
) {
    init {
        require(ordinal >= 0) { "A unit cannot come before the first one" }
        require(theme.isNotBlank()) { "A unit needs a theme" }
        requireOrderedSteps(lessons, "unit ${id.value}") { it.ordinal }
        requireDistinct(lessons.map { it.id }, "unit ${id.value}", "lesson")
    }
}

/**
 * Everything packaged for a child to learn, at one published version.
 *
 * [schemaVersion] describes the shape of the packaged content and [version] describes the content
 * itself. They move independently: a typo fix is a new content version against the same schema, and
 * progress recorded under one content version must never be counted against another.
 *
 * It is named `CourseUnit` rather than `Unit` above because a class called `Unit` in a Kotlin
 * package shadows `kotlin.Unit` inside it. The product still says unit everywhere a person can see.
 */
data class Course(
    val id: CourseId,
    val version: CourseVersion,
    val schemaVersion: Int,
    val supportedLocales: Set<String>,
    val units: List<CourseUnit>
) {
    init {
        require(schemaVersion > 0) { "A course needs a schema version" }
        require(supportedLocales.isNotEmpty()) {
            "A course nobody can be spoken to in cannot be taught"
        }
        requireOrderedSteps(units, "course ${id.value}") { it.ordinal }
        requireDistinct(units.map { it.id }, "course ${id.value}", "unit")
    }
}

/**
 * Holds the rule that an ordered collection is non-empty and its ordinals run 0, 1, 2 in the order
 * the list is written.
 */
private fun <T> requireOrderedSteps(steps: List<T>, owner: String, ordinal: (T) -> Int) {
    require(steps.isNotEmpty()) { "A $owner needs at least one step" }
    steps.forEachIndexed { index, step ->
        require(ordinal(step) == index) {
            "A $owner is out of order at position $index, which says it is step ${ordinal(step)}"
        }
    }
}

/** Holds the rule that nothing inside an ordered collection shares an identity with its siblings. */
private fun <T> requireDistinct(ids: List<T>, owner: String, kind: String) {
    require(ids.toSet().size == ids.size) { "A $owner names the same $kind twice" }
}
