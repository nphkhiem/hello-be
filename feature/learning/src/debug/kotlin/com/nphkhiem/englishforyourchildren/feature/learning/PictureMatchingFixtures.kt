package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback

/**
 * Review states for the matching activity, shared by the debug catalog and the instrumented tests
 * so a reviewer walks exactly what is asserted.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 *
 * The source and the correct destination are both a bed, as in the approved draft. They stay
 * distinguishable in tests because the source draws its word while destinations do not: the source
 * is found by text, a destination by content description.
 */
object PictureMatchingFixtures {

    fun answering(): LessonUiState = LessonUiState(
        unitName = "My Home",
        activityTitle = "Find the same picture",
        prompt = "Match the bed",
        caption = null,
        activityNumber = 3,
        activityCount = 4,
        phase = LessonPhase.ANSWERING,
        support = SupportLevel.NONE,
        learningObject = LearningObject(id = BED, label = BED),
        answers = listOf(
            AnswerOption(id = CHAIR, label = CHAIR),
            AnswerOption(id = BED_PICTURE, label = BED),
            AnswerOption(id = DOOR, label = DOOR),
            AnswerOption(id = LAMP, label = LAMP)
        ),
        audioAvailable = true,
        pendingSave = false
    )

    fun prompting(): LessonUiState = answering().copy(phase = LessonPhase.PROMPTING)

    fun twoDestinations(): LessonUiState = answering().let { base ->
        base.copy(answers = base.answers.take(2))
    }

    fun supportiveRetry(level: SupportLevel): LessonUiState = answering().let { base ->
        base.copy(
            support = level,
            answers = base.answers.map { answer ->
                if (answer.id == CHAIR) {
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
                if (answer.id == BED_PICTURE) {
                    answer.copy(feedback = HelloBeChoiceFeedback.CORRECT)
                } else {
                    answer
                }
            }
        )
    }

    const val BED = "bed"
    const val CHAIR = "chair"
    const val DOOR = "door"
    const val LAMP = "lamp"
    const val BED_PICTURE = "bed-picture"
}
