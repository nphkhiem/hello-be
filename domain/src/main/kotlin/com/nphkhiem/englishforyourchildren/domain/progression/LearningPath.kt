package com.nphkhiem.englishforyourchildren.domain.progression

import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.SkillId

/**
 * Where a lesson stands for one child.
 *
 * There is no value here for locked, and there never will be. A lesson a child has not reached yet
 * is [FUTURE], which the path draws as later rather than as refused, and nothing a child does can
 * take a lesson away from them. Incorrect answers change what is suggested for review and never
 * what is reachable.
 */
enum class LessonStanding {
    COMPLETED,
    RECOMMENDED,
    FUTURE
}

/**
 * The course as one child's way through it.
 *
 * Deliberately not a copy of the course. It carries the judgement, which is what a child's progress
 * decides, and leaves the shape of the content to the content: a screen already walking the units
 * asks this what each lesson's standing is rather than being handed the course a second time.
 */
data class LearningPath(
    val standings: Map<LessonId, LessonStanding>,
    /** The one lesson to point at, or null once there is nothing left a child has not done. */
    val recommended: LessonId?,
    val review: List<SkillId>
) {
    /**
     * A lesson the path has never heard of is [LessonStanding.FUTURE].
     *
     * Content can move under a child's history, and the safe answer for a lesson nobody can place
     * is the one that draws as later rather than as finished.
     */
    fun standingOf(lessonId: LessonId): LessonStanding =
        standings[lessonId] ?: LessonStanding.FUTURE
}
