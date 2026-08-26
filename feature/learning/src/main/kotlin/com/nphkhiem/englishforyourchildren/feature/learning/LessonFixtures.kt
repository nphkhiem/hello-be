package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback

/**
 * Review states for previews, the debug catalog and tests.
 *
 * Kept in main sources on purpose so previews and the catalog share exactly the states the tests
 * assert against. They are fixtures, not content: no production code path reads them, and real
 * activities arrive from packaged curriculum later.
 */
object LessonFixtures {

    fun preparing(): LessonUiState = answering().copy(
        phase = LessonPhase.PREPARING,
        answers = emptyList()
    )

    fun prompting(): LessonUiState = answering().copy(phase = LessonPhase.PROMPTING)

    fun answering(): LessonUiState = LessonUiState(
        unitName = "My Home",
        activityTitle = "Listen and choose",
        prompt = "Where is the chair?",
        caption = null,
        activityNumber = 2,
        activityCount = 4,
        phase = LessonPhase.ANSWERING,
        support = SupportLevel.NONE,
        answers = listOf(
            AnswerOption(id = CHAIR, label = "chair"),
            AnswerOption(id = LAMP, label = "lamp"),
            AnswerOption(id = BED, label = "bed")
        ),
        audioAvailable = true,
        pendingSave = false
    )

    fun supportiveRetry(level: SupportLevel): LessonUiState = answering().copy(
        support = level,
        answers = answering().answers.map { option ->
            if (option.id == LAMP) {
                option.copy(feedback = HelloBeChoiceFeedback.SUPPORTIVE_RETRY)
            } else {
                option
            }
        }
    )

    fun correct(): LessonUiState = answering().copy(
        phase = LessonPhase.CORRECT,
        answers = answering().answers.map { option ->
            if (option.id ==
                CHAIR
            ) {
                option.copy(feedback = HelloBeChoiceFeedback.CORRECT)
            } else {
                option
            }
        }
    )

    fun audioUnavailable(): LessonUiState = answering().copy(audioAvailable = false)

    fun captioned(): LessonUiState = answering().copy(caption = "Where is the chair?")

    /** Every approved lesson state, in the order a reviewer would walk them. */
    fun reviewStates(): List<Pair<String, LessonUiState>> = listOf(
        "preparing" to preparing(),
        "prompting" to prompting(),
        "answering" to answering(),
        "captioned" to captioned(),
        "retry one, calm repeat" to supportiveRetry(SupportLevel.REPEAT),
        "retry two, slower" to supportiveRetry(SupportLevel.SLOWER),
        "Vietnamese support" to supportiveRetry(SupportLevel.VIETNAMESE),
        "correct" to correct(),
        "audio unavailable" to audioUnavailable(),
        "pending save" to answering().copy(pendingSave = true),
        "answering, no sound, not saved" to audioUnavailable().copy(pendingSave = true),
        "no answers" to answering().copy(answers = emptyList())
    )

    private const val CHAIR = "chair"
    private const val LAMP = "lamp"
    private const val BED = "bed"
}
