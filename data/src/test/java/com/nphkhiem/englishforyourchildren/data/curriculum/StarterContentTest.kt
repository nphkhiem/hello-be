package com.nphkhiem.englishforyourchildren.data.curriculum

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.ActivityContent
import com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily
import com.nphkhiem.englishforyourchildren.domain.model.Answerable
import java.io.File
import org.junit.jupiter.api.Test

/**
 * The bundle that actually ships, read off disk.
 *
 * Not a fixture. These tests open `content/starter/src/main/assets` and check the same files the
 * app will, so a content mistake fails here rather than on a television.
 */
class StarterContentTest {
    private val parser = CurriculumJsonParser()
    private val validator = CurriculumValidator()

    private val course = parser.parseCourse(read("curriculum/course.json"))
    private val units = course.units.map { parser.parseUnit(read("curriculum/${it.file}")) }

    @Test
    fun givenTheShippedCourse_whenItIsRead_thenItIsTheApprovedStarterAtItsPublishedVersion() {
        assertThat(course.id).isEqualTo("starter")
        assertThat(course.courseVersion).isEqualTo("2026.09")
        assertThat(course.supportedLocales).containsExactly("en", "vi")
    }

    @Test
    fun givenTheShippedUnit_whenItIsRead_thenItIsMyBodyWithItsFiveSessions() {
        val unit = units.single()

        assertThat(unit.id).isEqualTo("u01-my-body")
        assertThat(unit.theme).isEqualTo("My Body")
        assertThat(unit.lessons).hasSize(5)
    }

    @Test
    fun givenEveryTeachingLesson_whenItsShapeIsRead_thenItIsTheApprovedSpine() {
        // Six activities in one order, every session. Predictability over novelty, and a mistake in
        // one lesson is visible against four identical siblings.
        val teaching = units.single().lessons.filter { it.kind == "TEACHING" }

        assertThat(teaching).hasSize(4)
        teaching.forEach { lesson ->
            assertThat(lesson.activities.map { it.family }).containsExactly(
                "LISTEN_AND_CHOOSE",
                "LISTEN_AND_CHOOSE",
                "PICTURE_MATCHING",
                "LETTER_AND_SOUND",
                "SAY_WITH_PIP",
                "REVIEW"
            ).inOrder()
        }
    }

    @Test
    fun givenTheUnit_whenItsWordsAreCounted_thenThereAreSixteenAndNoneIsTaughtTwice() {
        // Four a session across four teaching sessions, which is the density the owner chose.
        val taught = units.single().lessons.flatMap { it.teaches }

        assertThat(taught).hasSize(16)
        assertThat(taught.toSet()).hasSize(16)
    }

    @Test
    fun givenTheReviewLesson_whenItIsRead_thenItTeachesNothingNew() {
        val review = units.single().lessons.single { it.kind == "REVIEW" }

        assertThat(review.teaches).isEmpty()
        assertThat(review.letters).isEmpty()
        assertThat(review.activities.map { it.family }.toSet()).containsExactly("REVIEW")
    }

    @Test
    fun givenTheShippedContent_whenItsGraphIsChecked_thenNothingIsWrongExceptMissingFiles() {
        // Everything that is this bundle's own fault has to be clean. The asset problems are a
        // different kind: they are work nobody has done yet, and they are counted separately below.
        val problems = validator.validate(course, units, availableAssets = allReferencedAssets())

        assertThat(problems).isEmpty()
    }

    @Test
    fun givenNoMediaHasBeenMade_whenTheContentIsChecked_thenEveryMissingFileIsNamed() {
        // This is the point of the whole exercise. Until an illustrator and a voice have been
        // commissioned, the app cannot teach anything, and this is the list of what is owed.
        val problems = validator.validate(course, units, availableAssets = emptySet())
        val missing = problems.filter { it.what.endsWith("has no file") }

        assertThat(missing).isNotEmpty()
        assertThat(
            missing.map {
                it.what.removePrefix("asset ").removeSuffix(" has no file")
            }.toSet()
        )
            .hasSize(EXPECTED_ASSET_COUNT)
    }

    @Test
    fun givenTheShippedBundle_whenItBecomesDomain_thenEveryActivityKnowsWhatItAsks() {
        // The reducer can drive a lesson without this; a screen cannot draw one. Thirty activities
        // with content is what stands between the two.
        val course = parser.toDomain(course, units)
        val activities = course.units.flatMap { it.lessons }.flatMap { it.activities }

        assertThat(activities).hasSize(30)
        assertThat(activities.all { it.content != null }).isTrue()
    }

    @Test
    fun givenTheShippedBundle_whenSpeakingPracticeIsRead_thenItHasNothingToBeRightAbout() {
        val speaking = parser.toDomain(course, units).units
            .flatMap { it.lessons }
            .flatMap { it.activities }
            .filter { it.family == ActivityFamily.SAY_WITH_PIP }

        assertThat(speaking).hasSize(4)
        assertThat(speaking.none { it.content is Answerable }).isTrue()
    }

    @Test
    fun givenTheShippedBundle_whenALetterActivityIsRead_thenItNamesItsLetter() {
        val letters = parser.toDomain(course, units).units
            .flatMap { it.lessons }
            .flatMap { it.activities }
            .mapNotNull { it.content as? ActivityContent.LetterAndSound }

        assertThat(letters).hasSize(4)
        assertThat(letters.map { it.letter.value })
            .containsExactly("letter-e", "letter-h", "letter-b", "letter-a")
    }

    @Test
    fun givenNoRecordingsExist_whenTheBundleBecomesDomain_thenEveryPromptIsStillSilent() {
        // Content referencing a recording nobody has made is content, not an error. This is the
        // state the app is in, and it has to be a state it can hold.
        val prompts = parser.toDomain(course, units).units
            .flatMap { it.lessons }
            .flatMap { it.activities }
            .mapNotNull { it.content?.promptAsset }

        assertThat(prompts).isNotEmpty()
    }

    @Test
    fun givenTheAttributionLedger_whenItIsRead_thenItIsHonestlyEmpty() {
        // An empty ledger stops a release. A ledger with invented evidence would not, which is why
        // this file is empty rather than filled in with plausible-looking rows.
        val attributions = parser.parseAttributions(read("attribution/attributions.json"))

        assertThat(attributions.entries).isEmpty()
    }

    @Test
    fun givenAnActivityNamingAnAnswerItDoesNotOffer_whenChecked_thenItIsReported() {
        // A question no child can get right. Proving the validator catches it matters more than
        // proving the current bundle is clean, because the bundle will change.
        val broken = units.single().let { unit ->
            unit.copy(
                lessons = unit.lessons.take(1).map { lesson ->
                    lesson.copy(
                        activities = lesson.activities.take(1).map {
                            it.copy(correctSkillId = "word-nothing")
                        }
                    )
                }
            )
        }

        val problems = validator.validate(course, listOf(broken), allReferencedAssets())

        assertThat(problems.map { it.what }).contains(
            "the correct answer word-nothing is not one of the choices"
        )
    }

    @Test
    fun givenActivitiesOutOfOrder_whenChecked_thenItIsReported() {
        val unit = units.single()
        val shuffled = unit.copy(
            lessons = unit.lessons.take(1).map { lesson ->
                lesson.copy(activities = lesson.activities.reversed())
            }
        )

        val problems = validator.validate(course, listOf(shuffled), allReferencedAssets())

        assertThat(problems.any { it.what.contains("says it is number") }).isTrue()
    }

    private fun allReferencedAssets(): Set<String> = units
        .flatMap { it.lessons }
        .flatMap { it.activities }
        .flatMap { activity ->
            listOfNotNull(activity.promptAsset, activity.letterAsset) +
                activity.choices.flatMap { listOf(it.image, it.audio) }
        }
        .toSet()

    private fun read(path: String): String = File(ASSETS, path).readText()

    private companion object {
        val ASSETS = File("../content/starter/src/main/assets")
        const val EXPECTED_ASSET_COUNT = 40
    }
}
