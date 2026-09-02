package com.nphkhiem.englishforyourchildren.domain.progression

import com.nphkhiem.englishforyourchildren.domain.model.Course
import com.nphkhiem.englishforyourchildren.domain.model.ProfileProgress

/** The course, plus what one child has done, gives the way through it. */
fun interface ProgressionPolicy {
    fun build(course: Course, progress: ProfileProgress): LearningPath
}

/**
 * Lessons open in the order the course puts them in, and the first unfinished one is the offer.
 *
 * Derived rather than recorded, which is the whole of why "completing a lesson unlocks the next
 * exactly once" holds: there is no unlock to fire, only a set of finished lessons and a course to
 * read it against. Walking the same lesson twice leaves that set exactly as it was.
 *
 * A lesson finished out of order, which content moving under a child's history can produce, is
 * counted as finished and skips nothing: the recommendation is still the first one they have not
 * done. Nothing here can make a lesson unreachable.
 */
class CourseOrderProgressionPolicy : ProgressionPolicy {

    override fun build(course: Course, progress: ProfileProgress): LearningPath {
        val lessons = course.units.flatMap { it.lessons }
        val finished = progress.lessonsCompleted
        val recommended = lessons.firstOrNull { it.id !in finished }?.id

        val standings = lessons.associate { lesson ->
            lesson.id to when {
                lesson.id in finished -> LessonStanding.COMPLETED
                lesson.id == recommended -> LessonStanding.RECOMMENDED
                else -> LessonStanding.FUTURE
            }
        }

        val skills = tallySkills(course, progress.attempts)
        // Built here rather than injected, because which skills come first is a fact about this
        // course and the policy's own interface takes no course. See its KDoc.
        val review = LastTimeItNeededHelpReviewPolicy(courseOrder = lessons.flatMap { it.teaches })

        return LearningPath(
            standings = standings,
            recommended = recommended,
            review = review.select(
                availableSkills = skills.keys,
                progress = skills,
                limit = REVIEW_LIMIT
            )
        )
    }

    private companion object {
        /** As many as a review lesson has room for without becoming a test. */
        const val REVIEW_LIMIT = 4
    }
}
