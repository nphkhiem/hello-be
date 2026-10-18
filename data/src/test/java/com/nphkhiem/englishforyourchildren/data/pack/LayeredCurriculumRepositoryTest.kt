package com.nphkhiem.englishforyourchildren.data.pack

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumValidator
import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader
import com.nphkhiem.englishforyourchildren.domain.model.Attribution
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.Lesson
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

/**
 * Which course a child gets once downloaded packs exist.
 *
 * Both sides are real content. The seed is the starter course that ships, read off disk through the
 * same loader the app uses, and a pack is that same course copied to a new version with one prompt
 * reworded, so every assertion can tell exactly which of the two answered.
 */
class LayeredCurriculumRepositoryTest {

    @TempDir
    lateinit var temp: File

    private val loader = CurriculumLoader(CurriculumJsonParser(), CurriculumValidator())
    private val seed = SeedFromDisk(loader)

    private val packsRoot by lazy { File(temp, "packs") }
    private val store by lazy {
        ContentPackStore(
            packsRoot,
            PreferenceDataStoreFactory.create(produceFile = { File(temp, "test.preferences_pb") })
        )
    }
    private val repository by lazy { LayeredCurriculumRepository(seed, store, loader) }

    @Test
    fun givenNoPackHasBeenActivated_whenTheCourseIsObserved_thenItIsTheSeed() {
        assertThat(observedVersion()).isEqualTo(SEED_VERSION)
    }

    @Test
    fun givenAnActivePack_whenTheCourseIsObserved_thenItIsThePack() {
        writePack()
        activate(PACK_VERSION)

        assertThat(observedVersion()).isEqualTo(PACK_VERSION)
    }

    @Test
    fun givenAnActivePackThatWillNotParse_whenTheCourseIsObserved_thenTheSeedStillTeaches() {
        writePack()
        File(packsRoot, "${PACK_VERSION.value}/curriculum/course.json").writeText("{ not json")
        activate(PACK_VERSION)

        assertThat(observedVersion()).isEqualTo(SEED_VERSION)
    }

    @Test
    fun givenAPackClaimingAnotherVersionInside_whenActive_thenItIsNotTrusted() {
        // Directory says one version, course.json another. Serving it would let a session pin a
        // version whose lessons come from a different course.
        writePack(declaring = "2027.01")
        activate(PACK_VERSION)

        assertThat(observedVersion()).isEqualTo(SEED_VERSION)
        assertThat(runBlocking { repository.getLesson(FIRST_LESSON, PACK_VERSION) })
            .isEqualTo(DomainResult.Failure(DomainError.LessonNotFound))
    }

    @Test
    fun givenAChildMidLessonOnTheSeed_whenAPackActivates_thenTheirLessonDoesNotChange() {
        writePack()
        activate(PACK_VERSION)

        val pinned = runBlocking { repository.getLesson(FIRST_LESSON, SEED_VERSION) }

        assertThat(firstPrompt(pinned)).isEqualTo(SEED_PROMPT)
    }

    @Test
    fun givenAnActivePack_whenALessonIsAskedForAtItsVersion_thenThePackAnswers() {
        writePack()
        activate(PACK_VERSION)

        val lesson = runBlocking { repository.getLesson(FIRST_LESSON, PACK_VERSION) }

        assertThat(firstPrompt(lesson)).isEqualTo(PACK_PROMPT)
    }

    @Test
    fun givenAVersionNothingOnThisTelevisionHas_whenALessonIsAskedFor_thenItIsNotFound() {
        val lesson = runBlocking { repository.getLesson(FIRST_LESSON, CourseVersion("1999.01")) }

        assertThat(lesson).isEqualTo(DomainResult.Failure(DomainError.LessonNotFound))
    }

    @Test
    fun givenAnActivePack_whenCreditsAreAskedFor_thenTheyAreThePacks() {
        // The seed ships with no credits yet, so the pack carries one to tell the two apart.
        writePack()
        File(packsRoot, "${PACK_VERSION.value}/attribution/attributions.json").writeText(
            """
            {"schemaVersion":2,"entries":[{"assetId":"img-eyes","kind":"image",
            "provenance":"licensed","source":"Wikimedia Commons","licence":"CC BY-SA 4.0",
            "licenceEvidence":"commons/File:Eyes.webp","attributionText":"Eyes, by a contributor"}]}
            """.trimIndent()
        )
        activate(PACK_VERSION)

        val credits = runBlocking { repository.getAttributions() }

        assertThat((credits as? DomainResult.Success)?.value?.map { it.assetId.value })
            .containsExactly("img-eyes")
    }

    @Test
    fun givenAVersionWithNoPackOnDisk_whenActivated_thenItIsRefused() {
        // A pointer naming nothing would read as a lost download on the next launch.
        assertThrows<IllegalArgumentException> { activate(PACK_VERSION) }
    }

    @Test
    fun givenAnActivePackThatHasSinceGone_whenTheCourseIsObserved_thenItIsTheSeed() {
        writePack()
        activate(PACK_VERSION)

        File(packsRoot, PACK_VERSION.value).deleteRecursively()

        assertThat(runBlocking { store.observeActiveVersion().first() }).isNull()
        assertThat(observedVersion()).isEqualTo(SEED_VERSION)
    }

    @Test
    fun givenAPackGoesLive_whenMediaIsLookedUpWithoutWaiting_thenItsFolderIsAlreadyKnown() {
        // Lookups for pictures and recordings cannot suspend, so the store has to know already.
        writePack()

        activate(PACK_VERSION)

        assertThat(store.liveDirectory()).isEqualTo(File(packsRoot, PACK_VERSION.value))
    }

    @Test
    fun givenTheLivePackHasGone_whenTheCourseIsReadAgain_thenMediaComesFromTheApkAgain() {
        writePack()
        activate(PACK_VERSION)
        File(packsRoot, PACK_VERSION.value).deleteRecursively()

        observedVersion()

        assertThat(store.liveDirectory()).isNull()
    }

    @Test
    fun givenAVersionThatWouldLeaveThePacksDirectory_whenUsed_thenItIsRefused() {
        assertThrows<IllegalArgumentException> { store.directory(CourseVersion("..")) }
        assertThrows<IllegalArgumentException> { store.directory(CourseVersion("a/b")) }
    }

    private fun observedVersion(): CourseVersion? = (
        runBlocking {
            repository.observeCourse().first()
        } as? DomainResult.Success
        )?.value?.version

    private fun activate(version: CourseVersion) = runBlocking { store.activate(version) }

    /** The starter course, copied as a pack at [PACK_VERSION], one prompt reworded. */
    private fun writePack(declaring: String = PACK_VERSION.value) {
        val pack = File(packsRoot, PACK_VERSION.value)
        SEED.copyRecursively(pack)
        File(pack, "curriculum/course.json").apply {
            writeText(readText().replace("\"${SEED_VERSION.value}\"", "\"$declaring\""))
        }
        File(pack, "curriculum/units/u01-my-body.json").apply {
            writeText(readText().replaceFirst(SEED_PROMPT, PACK_PROMPT))
        }
    }

    private fun firstPrompt(lesson: DomainResult<Lesson>): String? =
        (lesson as? DomainResult.Success)?.value?.activities?.first()?.content?.prompt

    private companion object {
        val PACK_VERSION = CourseVersion("2026.10")
        val FIRST_LESSON = LessonId("u01-my-body-l1")
        const val SEED_PROMPT = "Where are the eyes?"
        const val PACK_PROMPT = "Where are your eyes?"
    }
}
