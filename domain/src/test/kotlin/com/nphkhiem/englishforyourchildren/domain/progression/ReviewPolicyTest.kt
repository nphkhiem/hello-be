package com.nphkhiem.englishforyourchildren.domain.progression

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.SkillProgress
import org.junit.jupiter.api.Test

/**
 * Which things come back, and in what order.
 *
 * Every case here is a fact about determinism as much as about selection: the same history has to
 * produce the same list, so that a review lesson can be checked against its content rather than
 * watched to see what it does.
 */
class ReviewPolicyTest {
    private val policy = LastTimeItNeededHelpReviewPolicy(
        courseOrder = listOf(SkillId(EYES), SkillId(EARS), SkillId(NOSE))
    )

    @Test
    fun givenSomethingWentFineLastTime_whenReviewIsChosen_thenItIsLeftAlone() {
        val chosen = policy.select(
            availableSkills = setOf(SkillId(EYES)),
            progress = mapOf(SkillId(EYES) to skill(EYES, wantsReview = false)),
            limit = 4
        )

        assertThat(chosen).isEmpty()
    }

    @Test
    fun givenPipHadToHelpLastTime_whenReviewIsChosen_thenItComesBack() {
        val chosen = policy.select(
            availableSkills = setOf(SkillId(EYES)),
            progress = mapOf(SkillId(EYES) to skill(EYES, wantsReview = true)),
            limit = 4
        )

        assertThat(chosen).containsExactly(SkillId(EYES))
    }

    @Test
    fun givenAThingTheChildHasNeverMet_whenReviewIsChosen_thenItIsNotIntroducedByAReview() {
        // A review gathers up what came before. Putting something new in one would make it a test
        // of something nobody taught.
        val chosen = policy.select(
            availableSkills = setOf(SkillId(EYES)),
            progress = mapOf(
                SkillId(EYES) to skill(EYES, wantsReview = false),
                SkillId(NOSE) to skill(NOSE, wantsReview = true)
            ),
            limit = 4
        )

        assertThat(chosen).isEmpty()
    }

    @Test
    fun givenSeveralWantReview_whenTheyAreChosen_thenTheyComeBackInTheOrderTheCourseTeachesThem() {
        val chosen = policy.select(
            availableSkills = setOf(SkillId(NOSE), SkillId(EYES), SkillId(EARS)),
            progress = wantingReview(EYES, EARS, NOSE),
            limit = 4
        )

        assertThat(chosen)
            .containsExactly(SkillId(EYES), SkillId(EARS), SkillId(NOSE))
            .inOrder()
    }

    @Test
    fun givenSomethingTheCourseDoesNotName_whenItIsChosen_thenItSortsLastRatherThanBreaking() {
        // Content moves under a child's history, and a skill nobody can place is not a crash.
        val stray = "word-elbow"

        val chosen = policy.select(
            availableSkills = setOf(SkillId(stray), SkillId(EARS)),
            progress = wantingReview(stray, EARS),
            limit = 4
        )

        assertThat(chosen).containsExactly(SkillId(EARS), SkillId(stray)).inOrder()
    }

    @Test
    fun givenMoreWantReviewThanWillFit_whenTheyAreChosen_thenTheEarliestOnesGoFirst() {
        val chosen = policy.select(
            availableSkills = setOf(SkillId(NOSE), SkillId(EYES), SkillId(EARS)),
            progress = wantingReview(EYES, EARS, NOSE),
            limit = 2
        )

        assertThat(chosen).containsExactly(SkillId(EYES), SkillId(EARS)).inOrder()
    }

    @Test
    fun givenAReviewWithNoRoomInIt_whenSkillsAreChosen_thenNoneIsPromised() {
        val chosen = policy.select(
            availableSkills = setOf(SkillId(EYES)),
            progress = wantingReview(EYES),
            limit = 0
        )

        assertThat(chosen).isEmpty()
    }

    private fun wantingReview(vararg skills: String) =
        skills.associate { SkillId(it) to skill(it, wantsReview = true) }

    private fun skill(id: String, wantsReview: Boolean) = SkillProgress(
        skillId = SkillId(id),
        exposures = 1,
        supportedSuccesses = 1,
        reviewNeeded = wantsReview,
        lastPractisedAt = EpochMillis(1)
    )

    private companion object {
        const val EYES = "word-eyes"
        const val EARS = "word-ears"
        const val NOSE = "word-nose"
    }
}
