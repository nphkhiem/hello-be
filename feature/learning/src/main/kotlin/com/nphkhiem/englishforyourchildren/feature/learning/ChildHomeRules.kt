package com.nphkhiem.englishforyourchildren.feature.learning

/** Which destination is dominant, and which sit beside it. */
internal data class HomeLayout(val dominant: HomeTarget, val secondaries: List<HomeTarget>)

/**
 * What goes in each slot.
 *
 * A finished course promotes free play into the dominant slot and drops it from the secondaries,
 * so it appears exactly once. That promotion is the honest answer: there is no next lesson, and
 * free play is what is left. Learning path stays so finished adventures remain visitable.
 */
internal fun homeDestinations(primary: HomePrimary): HomeLayout = when (primary) {
    is HomePrimary.CourseComplete -> HomeLayout(
        dominant = HomeTarget.FREE_PLAY,
        secondaries = listOf(HomeTarget.LEARNING_PATH)
    )

    else -> HomeLayout(
        dominant = HomeTarget.CONTINUE,
        secondaries = listOf(HomeTarget.LEARNING_PATH, HomeTarget.FREE_PLAY)
    )
}

/** Whether the dominant action can be pressed. A broken checkpoint is stated, not hidden. */
internal fun isDominantAvailable(primary: HomePrimary): Boolean =
    primary !is HomePrimary.ResumeUnavailable
