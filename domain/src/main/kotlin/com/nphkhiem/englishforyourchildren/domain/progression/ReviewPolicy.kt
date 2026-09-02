package com.nphkhiem.englishforyourchildren.domain.progression

import com.nphkhiem.englishforyourchildren.domain.model.SkillId
import com.nphkhiem.englishforyourchildren.domain.model.SkillProgress

/** Which of the things a child has met are worth coming back to. */
fun interface ReviewPolicy {
    fun select(
        availableSkills: Set<SkillId>,
        progress: Map<SkillId, SkillProgress>,
        limit: Int
    ): List<SkillId>
}

/**
 * The things a child needed Pip's help with the last time they came up.
 *
 * Not everything they have ever found hard. A skill they needed help with once and then got is not
 * still on the list, because a list that only grows is streak pressure with the number filed off,
 * and the brief refuses that as firmly as it refuses scores.
 *
 * Nothing here is random and nothing weights recency. The same history always produces the same
 * list, which is what lets a review lesson be checked against its content rather than observed.
 *
 * [courseOrder] arrives at construction rather than in [select], because which skill comes first is
 * a fact about a course and [ReviewPolicy] takes none. A skill the course does not name sorts last,
 * by id, so content moving under a child's history cannot make this throw.
 */
class LastTimeItNeededHelpReviewPolicy(private val courseOrder: List<SkillId>) : ReviewPolicy {

    override fun select(
        availableSkills: Set<SkillId>,
        progress: Map<SkillId, SkillProgress>,
        limit: Int
    ): List<SkillId> {
        if (limit <= 0) return emptyList()

        val position = courseOrder.withIndex().associate { (index, skill) -> skill to index }

        return availableSkills
            .filter { progress[it]?.reviewNeeded == true }
            .sortedWith(
                compareBy(
                    { position[it] ?: courseOrder.size },
                    { it.value }
                )
            )
            .take(limit)
    }
}
