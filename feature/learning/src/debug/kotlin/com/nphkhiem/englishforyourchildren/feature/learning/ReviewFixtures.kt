package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback

/**
 * Review states, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 *
 * "Mixed recall" lives here rather than in a field on the state: some items carry a learning
 * object and some do not, and that is the whole of the mixing. A type on the state would put a
 * branch in the one screen whose requirement is that it does not behave differently from the
 * others.
 *
 * The answer ids stay the same across item types on purpose, so the focus-preservation test can
 * change the prompt underneath a child without the row being rebuilt from scratch.
 */
object ReviewFixtures {

    fun answering(): LessonUiState = LessonUiState(
        lessonId = "u03-my-home-l5",
        kind = LessonActivityKind.REVIEW,
        unitName = "My Home review",
        activityTitle = "Remember our words",
        prompt = "Find something you sit on",
        caption = null,
        activityNumber = 4,
        activityCount = 4,
        phase = LessonPhase.ANSWERING,
        support = SupportLevel.NONE,
        learningObject = null,
        answers = listOf(
            AnswerOption(id = CHAIR, label = CHAIR),
            AnswerOption(id = SOFA, label = SOFA),
            AnswerOption(id = LAMP, label = LAMP)
        ),
        audioAvailable = true,
        pendingSave = false,
        stopForNowVisible = false,
        pauseProgress = null
    )

    /** A recall item that brings back a letter, so the row sits beside a learning object. */
    fun letterRecall(): LessonUiState = answering().copy(
        prompt = "Which one starts with /l/?",
        learningObject = LearningObject(id = LETTER, label = LETTER)
    )

    fun preparing(): LessonUiState = answering().copy(
        phase = LessonPhase.PREPARING,
        answers = emptyList()
    )

    fun prompting(): LessonUiState = answering().copy(phase = LessonPhase.PROMPTING)

    fun captioned(): LessonUiState = answering().copy(caption = "Find something you sit on.")

    fun supportiveRetry(level: SupportLevel): LessonUiState = answering().let { base ->
        base.copy(
            support = level,
            answers = base.answers.map { answer ->
                if (answer.id == LAMP) {
                    answer.copy(feedback = HelloBeChoiceFeedback.SUPPORTIVE_RETRY)
                } else {
                    answer
                }
            }
        )
    }

    fun correct(): LessonUiState = answering().let { base ->
        base.copy(
            phase = LessonPhase.CORRECT,
            answers = base.answers.map { answer ->
                if (answer.id == CHAIR) {
                    answer.copy(feedback = HelloBeChoiceFeedback.CORRECT)
                } else {
                    answer
                }
            }
        )
    }

    /** The last activity of the unit is over, which is what review being last means. */
    fun completed(): LessonUiState = correct().copy(phase = LessonPhase.COMPLETED)

    fun audioUnavailable(): LessonUiState = answering().copy(audioAvailable = false)

    /** Every approved review state, in the order a reviewer would walk them. */
    fun reviewStates(): List<Pair<String, LessonUiState>> = listOf(
        "preparing" to preparing(),
        "prompting" to prompting(),
        "answering, picture recall" to answering(),
        "answering, letter recall" to letterRecall(),
        "captioned" to captioned(),
        "retry one, calm repeat" to supportiveRetry(SupportLevel.REPEAT),
        "retry two, slower" to supportiveRetry(SupportLevel.SLOWER),
        "Vietnamese support" to supportiveRetry(SupportLevel.VIETNAMESE),
        "correct" to correct(),
        "final review complete" to completed(),
        "audio unavailable" to audioUnavailable(),
        "no answers" to answering().copy(answers = emptyList())
    )

    const val CHAIR = "chair"
    const val SOFA = "sofa"
    const val LAMP = "lamp"
    const val LETTER = "l"
}
