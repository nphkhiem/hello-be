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
    private val ledger = AttributionLedger()

    private val course = parser.parseCourse(read("curriculum/course.json"))
    private val units = course.units.map { parser.parseUnit(read("curriculum/${it.file}")) }

    @Test
    fun givenTheShippedCourse_whenItIsRead_thenItIsTheApprovedStarterAtItsPublishedVersion() {
        assertThat(course.id).isEqualTo("starter")
        assertThat(course.courseVersion).isEqualTo("2026.09")
        assertThat(course.supportedLocales).containsExactly("en", "vi")
    }

    @Test
    fun givenTheShippedUnits_whenTheyAreRead_thenTheyAreTheApprovedThemesInOrder() {
        assertThat(units.map { it.id })
            .containsExactly("u01-my-body", "u02-my-family", "u03-my-home")
            .inOrder()
        assertThat(units.map { it.theme })
            .containsExactly("My Body", "My Family", "My Home")
            .inOrder()
    }

    @Test
    fun givenTheWholeCourse_whenItIsCounted_thenItIsThreeUnitsAndFifteenSessions() {
        // The slice this phase is for, counted at the only place that can see all of it. A unit
        // that fails to load is a unit that quietly stops being taught, and nothing else here
        // would notice it was gone.
        assertThat(units).hasSize(3)
        assertThat(units.flatMap { it.lessons }).hasSize(15)
        assertThat(course.units.map { it.id }).isEqualTo(units.map { it.id })
    }

    @Test
    fun givenEveryShippedUnit_whenItIsRead_thenItHasItsFiveSessions() {
        // Four that teach and a fifth that reviews, every unit, which is the shape the blueprint
        // fixes for all twelve rather than for the one that happened to be written first.
        units.forEach { unit ->
            assertThat(unit.lessons).hasSize(5)
            assertThat(unit.lessons.count { it.kind == "TEACHING" }).isEqualTo(4)
            assertThat(unit.lessons.count { it.kind == "REVIEW" }).isEqualTo(1)
        }
    }

    @Test
    fun givenEveryTeachingLesson_whenItsShapeIsRead_thenItIsTheApprovedSpine() {
        // Six activities in one order, every session. Predictability over novelty, and a mistake in
        // one lesson is visible against four identical siblings.
        val teaching = units.flatMap { it.lessons }.filter { it.kind == "TEACHING" }

        assertThat(teaching).hasSize(4 * units.size)
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
    fun givenEveryUnit_whenItsWordsAreCounted_thenThereAreSixteenAndNoneIsTaughtTwice() {
        // Four a session across four teaching sessions, which is the density the owner chose.
        units.forEach { unit ->
            val taught = unit.lessons.flatMap { it.teaches }

            assertThat(taught).hasSize(16)
            assertThat(taught.toSet()).hasSize(16)
        }
    }

    @Test
    fun givenTheWholeBundle_whenItsWordsAreCounted_thenNoUnitTeachesAnothersWord() {
        // An id is permanent and names one thing, so a word taught in two units would be one skill
        // a child is credited with twice and a picture two units disagree about.
        val taught = units.flatMap { unit -> unit.lessons.flatMap { it.teaches } }

        assertThat(taught.toSet()).hasSize(taught.size)
    }

    @Test
    fun givenEveryReviewLesson_whenItIsRead_thenItTeachesNothingNew() {
        units.forEach { unit ->
            val review = unit.lessons.single { it.kind == "REVIEW" }

            assertThat(review.teaches).isEmpty()
            assertThat(review.letters).isEmpty()
            assertThat(review.activities.map { it.family }.toSet()).containsExactly("REVIEW")
        }
    }

    @Test
    fun givenEveryReviewLesson_whenItIsRead_thenItOnlyAsksAboutWordsAlreadyTaught() {
        // A review that asks about a word from a unit further on would be a question no child could
        // have been taught the answer to, and the further on the unit the longer it would take to
        // notice.
        units.forEachIndexed { index, unit ->
            val introduced = units.take(index + 1)
                .flatMap { earlier -> earlier.lessons.flatMap { it.teaches } }
                .toSet()
            val asked = unit.lessons
                .single { it.kind == "REVIEW" }
                .activities
                .flatMap { activity -> activity.choices.map { it.skillId } }

            assertThat(introduced).containsAtLeastElementsIn(asked.toSet())
        }
    }

    @Test
    fun givenEveryShippedSession_whenItEnds_thenThereIsSomethingToDoAwayFromTheTelevision() {
        // The information architecture puts one off-screen suggestion on the caregiver overview and
        // the brief offers one after every celebration. Both read the lesson, so a lesson without
        // one is a caregiver shown nothing.
        val ideas = units.flatMap { it.lessons }.map { it.coPlay }

        assertThat(ideas).hasSize(5 * units.size)
        assertThat(ideas.all { it != null }).isTrue()
    }

    @Test
    fun givenEveryShippedIdea_whenACaregiverReadsIt_thenItIsThereInBothLanguages() {
        // The person being addressed is the caregiver, and the caregiver area speaks both. An idea
        // in English alone would be a card a Vietnamese-speaking parent cannot act on.
        val ideas = units.flatMap { it.lessons }.mapNotNull { it.coPlay }

        ideas.forEach { idea ->
            assertThat(idea.titleVi).isNotEmpty()
            assertThat(idea.instructionVi).isNotEmpty()
            assertThat(idea.titleVi).isNotEqualTo(idea.title)
            assertThat(idea.instructionVi).isNotEqualTo(idea.instruction)
        }
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

        assertThat(activities).hasSize(30 * units.size)
        assertThat(activities.all { it.content != null }).isTrue()
    }

    @Test
    fun givenTheShippedBundle_whenSpeakingPracticeIsRead_thenItHasNothingToBeRightAbout() {
        val speaking = parser.toDomain(course, units).units
            .flatMap { it.lessons }
            .flatMap { it.activities }
            .filter { it.family == ActivityFamily.SAY_WITH_PIP }

        assertThat(speaking).hasSize(4 * units.size)
        assertThat(speaking.none { it.content is Answerable }).isTrue()
    }

    @Test
    fun givenEveryShippedUnit_whenItIsRead_thenItNamesTheWordACelebrationCanSay() {
        // "You found 4 body words!" needs a noun, and the theme is a title rather than one.
        // Deriving one by trimming "My Body" would be code inventing child-facing copy.
        val words = parser.toDomain(course, units).units.map { it.word }

        assertThat(words).containsExactly("body", "family", "home").inOrder()
    }

    @Test
    fun givenTheShippedBundle_whenALetterActivityIsRead_thenItNamesItsLetter() {
        val letters = parser.toDomain(course, units).units
            .flatMap { it.lessons }
            .flatMap { it.activities }
            .mapNotNull { it.content as? ActivityContent.LetterAndSound }

        assertThat(letters).hasSize(4 * units.size)
        assertThat(letters.map { it.letter.value })
            .containsExactly(
                "letter-e",
                "letter-h",
                "letter-b",
                "letter-a",
                "letter-s",
                "letter-g",
                "letter-u",
                "letter-w",
                "letter-d",
                "letter-l",
                "letter-r",
                "letter-p"
            )
            .inOrder()
    }

    @Test
    fun givenEveryTaughtLetter_whenItIsRead_thenItArrivesInsideAWordOfThatSession() {
        // The blueprint's rule, and the reason it is a rule: a letter met on its own is a shape,
        // and a letter met inside a word a child is learning that day is a sound they can use.
        units.flatMap { it.lessons }
            .filter { it.kind == "TEACHING" }
            .forEach { lesson ->
                val asked = lesson.activities.single { it.family == "LETTER_AND_SOUND" }
                val initial = asked.letterSkillId?.removePrefix("letter-")

                assertThat(lesson.letters).contains(asked.letterSkillId)
                assertThat(asked.correctSkillId?.removePrefix("word-")).startsWith(initial)
            }
    }

    @Test
    fun givenNoRecordingsExist_whenTheBundleBecomesDomain_thenEveryPromptIsStillSilent() {
        // Content referencing a recording nobody has made is content, not an error. This reads
        // the shipping bundle, which still has no media in it: the twenty-eight placeholder
        // recordings live in the debug source set, where a release build cannot reach them.
        val prompts = parser.toDomain(course, units).units
            .flatMap { it.lessons }
            .flatMap { it.activities }
            .mapNotNull { it.content?.promptAsset }

        assertThat(prompts).isNotEmpty()
    }

    @Test
    fun givenNoMediaExists_whenTheGraphIsChecked_thenNothingIsBrokenAndOnlyFilesAreOwed() {
        // The distinction that lets the app run today. Every shipping asset is missing, and none
        // of that is the content being wrong: a lesson opens, the words are on screen, and the
        // files are a release gate rather than a runtime one.
        val problems = validator.validate(course, units, availableAssets = emptySet())

        assertThat(problems.filter { it.kind == ContentProblem.Kind.BROKEN_GRAPH }).isEmpty()
        assertThat(problems.filter { it.kind == ContentProblem.Kind.MISSING_ASSET }).isNotEmpty()
    }

    @Test
    fun givenTheShippingLedger_whenItIsCheckedAgainstWhatIsPackaged_thenTheyAccountForEachOther() {
        // An empty ledger stops a release. A ledger with invented evidence would not, which is why
        // that file is empty rather than filled in with plausible-looking rows.
        //
        // This used to assert the emptiness itself, which was a fact about today rather than the
        // rule that made it right. Both sides are empty now and both will be full later, and what
        // has to hold either way is that they agree.
        val rows = parser.parseAttributions(read("attribution/attributions.json")).entries

        assertThat(ledger.checkShipping(packagedIn(ASSETS), rows)).isEmpty()
    }

    @Test
    fun givenTheDevelopmentLedger_whenItIsChecked_thenItAccountsForWhatDebugPackages() {
        // Standing-in audio lives in the debug source set, so a release build cannot contain it
        // whatever any row claims. It is still owed a row: a file nobody can name is a file nobody
        // can replace.
        val rows = parser
            .parseAttributions(File(DEBUG_ASSETS, DEVELOPMENT_LEDGER).readText())
            .entries

        assertThat(ledger.checkDevelopment(packagedIn(DEBUG_ASSETS), rows)).isEmpty()
    }

    /**
     * The asset ids of the files actually present, which is the inverse of the packaging convention
     * `PackagedAssetLocator` owns: `media/<kind>/<id>.<ext>`. The two cannot share code across the
     * module boundary, so `CONTENT_ID_REGISTRY.md` is the source of truth they both follow.
     */
    private fun packagedIn(assets: File): Set<String> = File(assets, "media")
        .walkTopDown()
        .filter { it.isFile }
        .map { it.nameWithoutExtension }
        .toSet()

    @Test
    fun givenAnActivityNamingAnAnswerItDoesNotOffer_whenChecked_thenItIsReported() {
        // A question no child can get right. Proving the validator catches it matters more than
        // proving the current bundle is clean, because the bundle will change.
        val broken = units.first().let { unit ->
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
        val unit = units.first()
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
        val DEBUG_ASSETS = File("../content/starter/src/debug/assets")
        const val DEVELOPMENT_LEDGER = "attribution/attributions-development.json"
        const val EXPECTED_ASSET_COUNT = 113
    }
}
