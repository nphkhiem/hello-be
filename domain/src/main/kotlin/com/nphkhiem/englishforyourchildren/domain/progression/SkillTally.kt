package com.nphkhiem.englishforyourchildren.domain.progression

import com.nphkhiem.englishforyourchildren.domain.model.Activity
import com.nphkhiem.englishforyourchildren.domain.model.ActivityAttempt
import com.nphkhiem.englishforyourchildren.domain.model.ActivityContent
import com.nphkhiem.englishforyourchildren.domain.model.ActivityId
import com.nphkhiem.englishforyourchildren.domain.model.Answerable
import com.nphkhiem.englishforyourchildren.domain.model.AttemptOutcome
import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.EpochMillis
import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.SkillProgress

/**
 * How much of each thing a child knows, counted from what they actually did.
 *
 * Derived rather than stored. A count worked out from the attempts cannot drift from them, there is
 * no second write to keep inside the same transaction as the first, and nothing has to decide what
 * to believe when the two disagree. The `skill_progress` table this replaces never had a row
 * written to it.
 *
 * An unscored skip counts for nothing at all. It happens because a recording would not play, so the
 * child was never actually asked, and counting it as a meeting they failed would put the app's
 * missing audio into their record as apparent difficulty. [AttemptOutcome] says outright that there
 * is no value in this product a caregiver could read as a mark against a three-year-old.
 */
fun tallySkills(course: Course, attempts: List<ActivityAttempt>): Map<SkillId, SkillProgress> {
    val activities = course.units
        .flatMap { it.lessons }
        .flatMap { it.activities }
        .associateBy { it.id }

    val counted = mutableMapOf<SkillId, SkillProgress>()

    // Oldest first, so "the last time this came up" is whatever the fold ends on.
    attempts.sortedBy { it.at.value }.forEach { attempt ->
        if (!attempt.outcome.wasMet) return@forEach
        val activity = activities[attempt.activityId] ?: return@forEach

        activity.skills().forEach { skill ->
            counted[skill] = counted[skill].plus(skill, attempt.outcome, attempt.at)
        }
    }

    return counted
}

/**
 * Whether the child met the thing at all.
 *
 * Every other outcome is a child in front of a question doing something about it, including a
 * guided repetition, which has nothing to be right about but is not nothing.
 */
private val AttemptOutcome.wasMet: Boolean
    get() = this != AttemptOutcome.UNSCORED_SKIP

/** What an activity is about: the answer it wants, or the words it asks a child to say. */
private fun Activity.skills(): List<SkillId> = when (val content = content) {
    is Answerable -> listOf(content.correct)
    is ActivityContent.GuidedRepetition -> content.words.map { it.skillId }
    null -> emptyList()
}

/**
 * One more meeting with a thing.
 *
 * [SkillProgress.reviewNeeded] is set by the latest outcome rather than accumulated, so a child who
 * needed Pip once and then got it is not still carrying it. It describes where they are now, not
 * everything they have ever found hard, which is the difference between help and a running tally.
 */
private fun SkillProgress?.plus(
    skill: SkillId,
    outcome: AttemptOutcome,
    at: EpochMillis
): SkillProgress = SkillProgress(
    skillId = skill,
    exposures = (this?.exposures ?: 0) + 1,
    supportedSuccesses = (this?.supportedSuccesses ?: 0) + 1,
    reviewNeeded = outcome == AttemptOutcome.SUPPORTIVE_RETRY,
    lastPractisedAt = at
)
