package com.nphkhiem.englishforyourchildren.feature.learning

/**
 * Child home states, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 */
object ChildHomeFixtures {

    fun returning(): ChildHomeUiState = ChildHomeUiState(
        profileName = "Minh",
        profileAvatar = "M",
        greeting = GREETING,
        greetingHint = WAITING_HINT,
        primary = HomePrimary.Resume(context = CONTEXT),
        pendingSave = false
    )

    fun newLearner(): ChildHomeUiState = returning().copy(primary = HomePrimary.StartFirstAdventure)

    fun courseComplete(): ChildHomeUiState = returning().copy(primary = HomePrimary.CourseComplete)

    fun checkpointUnavailable(): ChildHomeUiState =
        returning().copy(primary = HomePrimary.ResumeUnavailable(context = CONTEXT))

    fun pendingSave(): ChildHomeUiState = returning().copy(pendingSave = true)

    /** Every approved home state, in the order a reviewer would walk them. */
    fun reviewStates(): List<Pair<String, ChildHomeUiState>> = listOf(
        "returning learner" to returning(),
        "new learner" to newLearner(),
        "course complete" to courseComplete(),
        "checkpoint will not open" to checkpointUnavailable(),
        "progress pending" to pendingSave()
    )

    const val CONTEXT = "My Home, Lesson 3"
    private const val GREETING = "Let us find words at home"
    const val WAITING_HINT = "Pip has a little adventure ready for you."
}
