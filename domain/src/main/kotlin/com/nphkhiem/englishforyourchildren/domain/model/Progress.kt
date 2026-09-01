package com.nphkhiem.englishforyourchildren.domain.model

/** Whether a child is still working through a lesson or has reached the end of it. */
enum class LessonStatus {
    IN_PROGRESS,
    COMPLETED
}

/**
 * How an activity ended.
 *
 * None of these is a failure, and that is deliberate. A wrong answer is a supportive retry, which
 * the child meets as a calm second invitation. A question that could not be asked fairly, because
 * the sound would not play, is an unscored skip and costs nothing. There is no value here that a
 * caregiver could read as a mark against a three-year-old, because there is no such thing in this
 * product to record.
 */
enum class AttemptOutcome {
    CORRECT,
    SUPPORTIVE_RETRY,
    UNSCORED_SKIP,

    /**
     * A guided repetition the child worked through.
     *
     * Distinct from [UNSCORED_SKIP], which says a child went past something. This says they did it.
     * Both are unscored, and recording one as the other would put a skip in a child's history for
     * an activity they actually took part in.
     */
    PRACTISED
}

/**
 * One sitting at a lesson.
 *
 * The status and the activity in hand have to agree: a session in progress is a child in front of
 * something and can say what, and a completed one is not pointing at anything. Allowing both would
 * allow a lesson to be counted as finished and then resumed into, which would count it twice.
 */
data class LessonSession(
    val id: SessionId,
    val profileId: ProfileId,
    val courseVersion: CourseVersion,
    val lessonId: LessonId,
    val currentActivity: ActivityInstanceId?,
    val status: LessonStatus
) {
    init {
        when (status) {
            LessonStatus.IN_PROGRESS -> require(currentActivity != null) {
                "A session in progress has to say which activity the child is on"
            }

            LessonStatus.COMPLETED -> require(currentActivity == null) {
                "A completed session cannot still be on an activity"
            }
        }
    }
}

/**
 * The last thing a child finished that is safe to come back to.
 *
 * [lastCompletedActivity] is null for a lesson that was opened and left before anything was
 * confirmed, which is the honest way to say it rather than a sentinel activity nobody did. The
 * course version travels with it so that progress made under one published version is never counted
 * against another.
 */
data class LessonCheckpoint(
    val profileId: ProfileId,
    val courseVersion: CourseVersion,
    val lessonId: LessonId,
    val lastCompletedActivity: ActivityId?,
    val sessionId: SessionId,
    val updatedAt: EpochMillis
)

/**
 * What happened when a child met one activity, once.
 *
 * There is nowhere here for a recording, a transcript, or anything a child said or typed, and there
 * is no remote identifier. The no-microphone rule and the no-child-analytics rule are both easier to
 * keep as a shape than as something a reviewer has to check.
 */
data class ActivityAttempt(
    val sessionId: SessionId,
    val activityInstance: ActivityInstanceId,
    val ordinal: Int,
    val outcome: AttemptOutcome,
    val at: EpochMillis
) {
    init {
        require(ordinal >= 0) { "An attempt cannot come before the first one" }
    }
}

/**
 * How well a child knows one thing, across every lesson that touched it.
 *
 * [supportedSuccesses] counts the times they got it right with whatever help Pip was giving at the
 * time, which is why it is not called correct answers. It cannot exceed [exposures]: succeeding at a
 * word more often than meeting it is a counting bug, and it should stop here rather than reach a
 * caregiver dressed as progress.
 */
data class SkillProgress(
    val skillId: SkillId,
    val exposures: Int,
    val supportedSuccesses: Int,
    val reviewNeeded: Boolean,
    val lastPractisedAt: EpochMillis?
) {
    init {
        require(exposures >= 0) { "A skill cannot have been met a negative number of times" }
        require(supportedSuccesses >= 0) { "A skill cannot have a negative number of successes" }
        require(supportedSuccesses <= exposures) {
            "A child cannot have succeeded at ${skillId.value} more often than they have met it"
        }
    }
}
