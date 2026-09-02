package com.nphkhiem.englishforyourchildren.domain.model

/**
 * Everything one child has done so far, as the learning path and the caregiver overview need it.
 *
 * [openCheckpoint] is the lesson they can be put back into, or null when there is nothing to
 * resume. It is the whole reason a child can leave a lesson mid-way and be met where they left off
 * rather than at the beginning.
 */
data class ProfileProgress(
    val profileId: ProfileId,
    val lessonsCompleted: Set<LessonId>,
    /**
     * What the child actually did, rather than what it adds up to.
     *
     * Storage reports attempts and the progression package concludes what they mean, so a count of
     * how well a thing is known can never disagree with the attempts underneath it. This replaced a
     * `skills` list that no writer ever filled and no reader ever read.
     */
    val attempts: List<ActivityAttempt>,
    val openCheckpoint: LessonCheckpoint?
)

/**
 * Storage saying yes.
 *
 * A write that returned nothing would leave the caller to assume it worked, and "pending save" is
 * the vocabulary this product uses precisely because nothing may claim progress is saved until
 * storage has confirmed it. This is that confirmation, and it is the only thing that turns a
 * pending save into a saved one.
 */
data class ConfirmedCheckpoint(
    val sessionId: SessionId,
    val lessonId: LessonId,
    val lastCompletedActivity: ActivityId,
    val confirmedAt: EpochMillis
)

/**
 * What finishing a lesson produced.
 *
 * [learnedSkills] is what the celebration screen lists back to the child, which is why it is here
 * rather than being recomputed by the screen: the words a child sees at the end are the words the
 * write actually recorded.
 */
data class LessonCompletion(
    val sessionId: SessionId,
    val lessonId: LessonId,
    val learnedSkills: List<SkillId>,
    val unitCompleted: Boolean,
    val completedAt: EpochMillis
)
