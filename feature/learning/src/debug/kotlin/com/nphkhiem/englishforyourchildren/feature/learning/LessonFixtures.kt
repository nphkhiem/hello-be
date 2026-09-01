package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback

/**
 * Review states for previews, the debug catalog and tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data. The
 * debug catalog and the instrumented tests both compile against the debug variant, so they share
 * exactly the states each other exercises, and a release build cannot reach any of it.
 *
 * These are fixtures, not content: real activities arrive from packaged curriculum later.
 */
object LessonFixtures {

    fun preparing(): LessonUiState = answering().copy(
        phase = LessonPhase.PREPARING,
        answers = emptyList()
    )

    fun prompting(): LessonUiState = answering().copy(phase = LessonPhase.PROMPTING)

    fun answering(): LessonUiState = LessonUiState(
        kind = LessonActivityKind.LISTEN_AND_CHOOSE,
        unitName = "My Home",
        activityTitle = "Listen and choose",
        prompt = "Where is the chair?",
        caption = null,
        activityNumber = 2,
        activityCount = 4,
        phase = LessonPhase.ANSWERING,
        support = SupportLevel.NONE,
        learningObject = null,
        answers = listOf(
            AnswerOption(id = CHAIR, label = "chair"),
            AnswerOption(id = LAMP, label = "lamp"),
            AnswerOption(id = BED, label = "bed")
        ),
        audioAvailable = true,
        pendingSave = false,
        stopForNowVisible = false,
        pauseProgress = null
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

    fun stoppingForNow(): LessonUiState = answering().copy(stopForNowVisible = true)

    fun stoppingForNowPendingSave(): LessonUiState =
        answering().copy(stopForNowVisible = true, pendingSave = true)

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
        "stop for now, progress saved" to stoppingForNow(),
        "stop for now, progress pending" to stoppingForNowPendingSave(),
        "no answers" to answering().copy(answers = emptyList())
    )

    private const val CHAIR = "chair"
    private const val LAMP = "lamp"
    private const val BED = "bed"
}
