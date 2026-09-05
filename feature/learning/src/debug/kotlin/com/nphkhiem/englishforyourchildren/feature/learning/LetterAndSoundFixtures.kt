package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback

/**
 * Review states for letter and sound, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 *
 * The letter arrives lowercase on purpose. The activity derives both cases, so a fixture that
 * happened to store "A a" would hide the very thing the casing test exists to prove.
 */
object LetterAndSoundFixtures {

    fun answering(): LessonUiState = LessonUiState(
        lessonId = "u03-my-home-l2",
        kind = LessonActivityKind.LETTER_AND_SOUND,
        unitName = "Letters with Pip",
        activityTitle = "Listen for the sound",
        prompt = "Which starts with /ae/?",
        caption = null,
        activityNumber = 2,
        activityCount = 4,
        phase = LessonPhase.ANSWERING,
        support = SupportLevel.NONE,
        learningObject = LearningObject(id = LETTER, label = LETTER),
        answers = listOf(
            AnswerOption(id = APPLE, label = APPLE),
            AnswerOption(id = CAT, label = CAT),
            AnswerOption(id = SUN, label = SUN)
        ),
        audioAvailable = true,
        pendingSave = false,
        stopForNowVisible = false,
        pauseProgress = null
    )

    fun preparing(): LessonUiState = answering().copy(
        phase = LessonPhase.PREPARING,
        answers = emptyList()
    )

    fun prompting(): LessonUiState = answering().copy(phase = LessonPhase.PROMPTING)

    fun captioned(): LessonUiState = answering().copy(caption = "A says /ae/")

    fun supportiveRetry(level: SupportLevel): LessonUiState = answering().let { base ->
        base.copy(
            support = level,
            answers = base.answers.map { answer ->
                if (answer.id == CAT) {
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
                if (answer.id == APPLE) {
                    answer.copy(feedback = HelloBeChoiceFeedback.CORRECT)
                } else {
                    answer
                }
            }
        )
    }

    fun audioUnavailable(): LessonUiState = answering().copy(audioAvailable = false)

    /** Every approved letter-and-sound state, in the order a reviewer would walk them. */
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
        "no letter" to answering().copy(learningObject = null),
        "no choices" to answering().copy(answers = emptyList())
    )

    const val LETTER = "a"
    const val APPLE = "apple"
    const val CAT = "cat"
    const val SUN = "sun"
}
