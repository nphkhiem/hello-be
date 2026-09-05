package com.nphkhiem.englishforyourchildren.data.curriculum

import com.nphkhiem.englishforyourchildren.domain.model.Activity
import com.nphkhiem.englishforyourchildren.domain.model.ActivityContent
import com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.AnswerChoice
import com.nphkhiem.englishforyourchildren.domain.model.AssetId
import com.nphkhiem.englishforyourchildren.domain.model.CoPlayIdea
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.CourseId
import com.nphkhiem.englishforyourchildren.domain.model.CourseUnit
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.UnitId
import javax.inject.Inject
import kotlinx.serialization.json.Json

/**
 * Turns the files on disk into the domain, or says why it cannot.
 *
 * Nothing here decides whether the content is sensible; that is [CurriculumValidator]'s job, and it
 * runs first.
 */
class CurriculumJsonParser @Inject constructor() {
    /**
     * Lenient about fields it does not know, so content written for a later app does not crash an
     * older one, and strict about everything it does read.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun parseCourse(source: String): CourseDto = json.decodeFromString(source)

    fun parseUnit(source: String): UnitDto = json.decodeFromString(source)

    fun parseAttributions(source: String): AttributionsDto = json.decodeFromString(source)

    /**
     * The domain course, content and all.
     *
     * The validator runs before this and refuses anything malformed, so a family this app has no
     * screen for, or an answer that is not among the choices, never reaches here. What is left is
     * a mapping, and it fails loudly rather than guessing if that assumption is ever wrong.
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
        word = word,
        lessons = lessons.sortedBy { it.ordinal }.map { it.toDomain(id) }
    )

    private fun LessonDto.toDomain(unitId: String) = Lesson(
        id = LessonId(id),
        unitId = UnitId(unitId),
        ordinal = ordinal,
        teaches = teaches.map { SkillId(it) },
        activities = activities.sortedBy { it.ordinal }.map { it.toDomain() },
        coPlay = coPlay?.toDomain()
    )

    private fun CoPlayDto.toDomain() = CoPlayIdea(
        title = title,
        instruction = instruction,
        titleVietnamese = titleVi,
        instructionVietnamese = instructionVi
    )

    private fun ActivityDto.toDomain(): Activity {
        val activityFamily = ActivityFamily.valueOf(family)
        return Activity(
            id = ActivityId(id),
            ordinal = ordinal,
            family = activityFamily,
            content = toContent(activityFamily)
        )
    }

    private fun ActivityDto.toContent(family: ActivityFamily): ActivityContent {
        val offered = choices.map { it.toChoice() }
        return when (family) {
            ActivityFamily.LISTEN_AND_CHOOSE -> ActivityContent.ListeningSelection(
                prompt = prompt,
                promptAsset = promptAsset.asAsset(),
                choices = offered,
                correct = SkillId(requireAnswer())
            )

            ActivityFamily.PICTURE_MATCHING -> ActivityContent.PictureMatching(
                prompt = prompt,
                promptAsset = promptAsset.asAsset(),
                choices = offered,
                correct = SkillId(requireAnswer())
            )

            ActivityFamily.LETTER_AND_SOUND -> ActivityContent.LetterAndSound(
                prompt = prompt,
                promptAsset = promptAsset.asAsset(),
                choices = offered,
                correct = SkillId(requireAnswer()),
                letter = SkillId(
                    requireNotNull(letterSkillId) { "$id is a letter activity naming no letter" }
                ),
                letterAsset = letterAsset.asAsset()
            )

            // Unscored by construction: whatever the file says about a correct answer, there is
            // nowhere in this variant to put one.
            ActivityFamily.SAY_WITH_PIP -> ActivityContent.GuidedRepetition(
                prompt = prompt,
                promptAsset = promptAsset.asAsset(),
                words = offered
            )

            ActivityFamily.REVIEW -> ActivityContent.ReviewQuestion(
                prompt = prompt,
                promptAsset = promptAsset.asAsset(),
                choices = offered,
                correct = SkillId(requireAnswer())
            )
        }
    }

    private fun ActivityDto.requireAnswer(): String =
        requireNotNull(correctSkillId) { "$id is a question with no right answer" }

    private fun ChoiceDto.toChoice() = AnswerChoice(
        skillId = SkillId(skillId),
        label = label,
        image = AssetId(image),
        audio = AssetId(audio)
    )

    /**
     * An asset reference becomes an id only when a file could exist for it.
     *
     * Blank means the content author has nothing to point at yet, which is every recording today.
     */
    private fun String?.asAsset(): AssetId? = this?.takeIf { it.isNotBlank() }?.let { AssetId(it) }
}
