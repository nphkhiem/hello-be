package com.nphkhiem.englishforyourchildren.data.curriculum

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * What a bundle is told about its own names.
 *
 * `StarterContentTest` holds the shipped files to this validator; these are the malformed bundles
 * nobody would ever write on purpose and somebody eventually writes by accident. Every case here
 * used to pass validation and then throw out of the middle of the mapping, which reached a
 * caregiver as "the content is invalid" and reached the person who wrote the file as nothing at
 * all. See ADR 0011.
 */
class CurriculumValidatorTest {
    private val validator = CurriculumValidator()

    @Test
    fun givenAWellFormedBundle_whenItIsChecked_thenNothingIsWrongWithIt() {
        val problems = validator.validate(course(), listOf(unit()), REFERENCED_ASSETS)

        assertThat(problems).isEmpty()
    }

    @Test
    fun givenAnActivityWithNoId_whenItIsChecked_thenItIsNamedAsSomethingNothingCanRunOn() {
        val nameless = unit(activity(id = ""))

        val problems = validator.validate(course(), listOf(nameless), REFERENCED_ASSETS)

        assertThat(problems.map { it.toString() })
            .contains("an unnamed activity: activity id is blank")
        assertThat(problems.map { it.kind }).contains(ContentProblem.Kind.BROKEN_GRAPH)
    }

    @Test
    fun givenALetterActivityNamingNoLetter_whenItIsChecked_thenTheMissingFieldIsNamed() {
        // The one family that names something besides its answer. Without it there is no letter to
        // teach, and the mapping has nothing to build a letter activity out of.
        val letterless = unit(
            activity(family = "LETTER_AND_SOUND", letterSkillId = null)
        )

        val problems = validator.validate(course(), listOf(letterless), REFERENCED_ASSETS)

        assertThat(problems.map { it.toString() })
            .contains("$ACTIVITY: letter skill id is missing")
    }

    @Test
    fun givenAChoiceWithNoPicture_whenItIsChecked_thenItIsAMissingNameRatherThanAMissingFile() {
        // A blank reference is not a file nobody has made yet; it is a card with nothing to show.
        val unshowable = unit(
            activity(choices = listOf(ChoiceDto(SKILL, "nose", image = "", audio = AUDIO)))
        )

        val problems = validator.validate(course(), listOf(unshowable), REFERENCED_ASSETS)

        assertThat(problems.map { it.toString() }).contains("$ACTIVITY: choice picture is blank")
        assertThat(problems.map { it.what }).doesNotContain("asset  has no file")
    }

    @Test
    fun givenABundleWrongInSeveralPlaces_whenItIsChecked_thenAllOfItComesBackAtOnce() {
        // The whole reason these are values rather than exceptions: somebody fixing a bundle should
        // be told everything, not handed the first thing and asked to run it again.
        val broken = unit(
            activity(id = "", correctSkillId = ""),
            lessonId = ""
        )

        val problems = validator.validate(course(courseId = ""), listOf(broken), REFERENCED_ASSETS)

        assertThat(problems.map { it.toString() }).containsAtLeast(
            "an unnamed course: course id is blank",
            "an unnamed lesson: lesson id is blank",
            "an unnamed activity: activity id is blank",
            "an unnamed activity: correct answer skill id is blank"
        )
    }

    private fun course(courseId: String = COURSE) = CourseDto(
        schemaVersion = SCHEMA,
        id = courseId,
        courseVersion = VERSION,
        supportedLocales = listOf("en"),
        units = listOf(UnitRefDto(id = UNIT, ordinal = 0, file = "units/$UNIT.json"))
    )

    private fun unit(
        activity: ActivityDto = activity(),
        lessonId: String = LESSON,
        coPlay: CoPlayDto? = null
    ) = UnitDto(
        schemaVersion = SCHEMA,
        id = UNIT,
        ordinal = 0,
        theme = "My Body",
        word = "body",
        lessons = listOf(
            LessonDto(
                id = lessonId,
                ordinal = 0,
                kind = "TEACHING",
                teaches = listOf(SKILL),
                activities = listOf(activity),
                coPlay = coPlay
            )
        )
    )

    @Test
    fun givenAPlayTogetherIdeaWithNothingToDo_whenItIsChecked_thenItIsReported() {
        // A lesson may offer nothing away from the screen. One that says it offers something and
        // then has nothing to say would reach a caregiver as an empty card.
        val empty = unit(coPlay = CoPlayDto(title = "Touch and name", instruction = " "))

        val problems = validator.validate(course(), listOf(empty), REFERENCED_ASSETS)

        assertThat(problems.map { it.toString() })
            .contains("$LESSON: a play-together idea with nothing to do")
    }

    @Test
    fun givenAPlayTogetherIdeaWithNoName_whenItIsChecked_thenItIsReported() {
        val nameless = unit(coPlay = CoPlayDto(title = "", instruction = "Touch your eyes."))

        val problems = validator.validate(course(), listOf(nameless), REFERENCED_ASSETS)

        assertThat(problems.map { it.toString() })
            .contains("$LESSON: a play-together idea with no name")
    }

    @Test
    fun givenALessonOfferingNothingAwayFromTheScreen_whenItIsChecked_thenThatIsNotAProblem() {
        // Optional means optional. The default bundle in every other test here has no idea at all.
        val problems = validator.validate(course(), listOf(unit()), REFERENCED_ASSETS)

        assertThat(problems).isEmpty()
    }

    private fun activity(
        id: String = ACTIVITY,
        family: String = "LISTEN_AND_CHOOSE",
        correctSkillId: String? = SKILL,
        letterSkillId: String? = SKILL,
        choices: List<ChoiceDto> = listOf(ChoiceDto(SKILL, "nose", IMAGE, AUDIO))
    ) = ActivityDto(
        id = id,
        ordinal = 0,
        family = family,
        prompt = "Where is the nose?",
        promptAsset = PROMPT,
        choices = choices,
        correctSkillId = correctSkillId,
        letterSkillId = letterSkillId,
        letterAsset = LETTER
    )

    private companion object {
        const val SCHEMA = 2
        const val COURSE = "starter"
        const val VERSION = "2026.09"
        const val UNIT = "u01-my-body"
        const val LESSON = "u01-my-body-l1"
        const val ACTIVITY = "u01-my-body-l1-a1"
        const val SKILL = "en-word-nose"
        const val IMAGE = "img-en-nose"
        const val AUDIO = "aud-en-nose"
        const val PROMPT = "aud-en-prompt-where-is"
        const val LETTER = "aud-en-letter-n"

        val REFERENCED_ASSETS = setOf(IMAGE, AUDIO, PROMPT, LETTER)
    }
}
