package com.nphkhiem.englishforyourchildren.feature.learning

/**
 * Review states for say with Pip, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 *
 * No state here carries [LessonPhase.CORRECT], and a test asserts that. This family cannot be
 * correct, so a fixture that quietly offered a correct state would be describing a screen the
 * child must never see.
 */
object SayWithPipFixtures {

    fun modelling(): LessonUiState = LessonUiState(
        unitName = "My Home",
        activityTitle = "Say it with Pip",
        prompt = "\"This is a chair.\"",
        caption = null,
        activityNumber = 4,
        activityCount = 4,
        phase = LessonPhase.PROMPTING,
        support = SupportLevel.NONE,
        learningObject = LearningObject(id = CHAIR, label = CHAIR),
        answers = emptyList(),
        audioAvailable = true,
        pendingSave = false,
        stopForNowVisible = false,
        pauseProgress = null
    )

    fun preparing(): LessonUiState = modelling().copy(phase = LessonPhase.PREPARING)

    fun pauseBeginning(): LessonUiState =
        modelling().copy(phase = LessonPhase.RESPONDING, pauseProgress = 0f)

    fun pauseHalfway(): LessonUiState =
        modelling().copy(phase = LessonPhase.RESPONDING, pauseProgress = 0.5f)

    fun pauseEnding(): LessonUiState =
        modelling().copy(phase = LessonPhase.RESPONDING, pauseProgress = 1f)

    /** A host that has not wired timing yet still shows a usable invitation. */
    fun pauseWithoutTiming(): LessonUiState =
        modelling().copy(phase = LessonPhase.RESPONDING, pauseProgress = null)

    fun captioned(): LessonUiState = pauseHalfway().copy(caption = "This is a chair.")

    fun audioUnavailable(): LessonUiState = modelling().copy(audioAvailable = false)

    fun completed(): LessonUiState = modelling().copy(phase = LessonPhase.COMPLETED)

    /** Every approved say-with-Pip state, in the order a reviewer would walk them. */
    fun reviewStates(): List<Pair<String, LessonUiState>> = listOf(
        "preparing" to preparing(),
        "Pip is modelling" to modelling(),
        "pause, just begun" to pauseBeginning(),
        "pause, halfway" to pauseHalfway(),
        "pause, ending" to pauseEnding(),
        "pause, no timing wired" to pauseWithoutTiming(),
        "captioned" to captioned(),
        "audio unavailable" to audioUnavailable(),
        "pending save" to pauseHalfway().copy(pendingSave = true),
        "completed" to completed(),
        "no target object" to pauseHalfway().copy(learningObject = null)
    )

    const val CHAIR = "chair"
}
