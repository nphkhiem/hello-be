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
 * [content] is what it asks: the prompt, the choices, and which one is right where that means
 * anything. It is nullable because an activity existed as pure structure before the curriculum
 * blueprint did, and a test that only cares about ordering still builds one that way.
 */
data class Activity(
    val id: ActivityId,
    val ordinal: Int,
    val family: ActivityFamily,
    val content: ActivityContent? = null
) {
    init {
        require(ordinal >= 0) { "An activity cannot come before the first one" }
        // Two ways of saying the same thing can disagree, so the one place they meet checks them.
        require(content == null || content.family == family) {
            "An activity of $family cannot hold ${content?.family} content"
        }
    }
}

/**
 * One thing a caregiver and child can do away from the television, written in both languages.
 *
 * Content rather than copy. It names a real chair or a real pair of hands, so it cannot be written
 * in a string resource beside the buttons: it belongs to the lesson whose words it practises, and
 * a unit written next year brings its own.
 *
 * The Vietnamese is carried here rather than looked up because the person being addressed is the
 * caregiver, and the caregiver area speaks both languages. The child-facing prompts around it stay
 * English on purpose: being English is what they are for.
 */
data class CoPlayIdea(
    val title: String,
    val instruction: String,
    val titleVietnamese: String = "",
    val instructionVietnamese: String = ""
) {
    init {
        require(title.isNotBlank()) { "A play-together idea needs a name" }
        require(instruction.isNotBlank()) { "A play-together idea needs something to do" }
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
    /**
     * The words this lesson is for, which is what its celebration is about.
     *
     * Empty on a review lesson, which teaches nothing new and gathers up what came before.
     */
    val teaches: List<SkillId> = emptyList(),
    val activities: List<Activity>,
    /**
     * The off-screen activity offered once this lesson is finished, or null where none is written.
     *
     * Null is a real state and not a gap to be filled in later: the design brief makes co-play
     * optional, and a lesson with nothing worth doing away from the screen should offer nothing
     * rather than something generic.
     */
    val coPlay: CoPlayIdea? = null
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
    /**
     * The unit's own noun, as a child hears it: "body" gives "You found 4 body words!".
     *
     * Separate from [theme], which is a title. Trimming one into the other would be code inventing
     * copy a child reads, and the registry's posture is that nothing parses content strings.
     */
    val word: String,
    val lessons: List<Lesson>
) {
    init {
        require(ordinal >= 0) { "A unit cannot come before the first one" }
        require(theme.isNotBlank()) { "A unit needs a theme" }
        require(word.isNotBlank()) { "A unit needs a word a celebration can say" }
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
