package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.domain.model.ActivityContent
import com.nphkhiem.englishforyourchildren.domain.model.Answerable
import com.nphkhiem.englishforyourchildren.domain.session.LessonPhase as DomainPhase
import com.nphkhiem.englishforyourchildren.domain.session.LessonSessionState
import com.nphkhiem.englishforyourchildren.domain.session.SaveStatus
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeChoiceFeedback

/**
 * A lesson as a screen sees it.
 *
 * Everything here is a translation and nothing is a decision. The one rule worth naming is what is
 * left out: the correct answer never crosses this line. Every choice arrives neutral, so the screen
 * cannot give an answer away through focus, ordering or selection, which is what the built lesson
 * screens were designed around.
 */
internal object LessonUiMapper {

    fun preparing(lessonId: String = "") = LessonUiState(
        lessonId = lessonId,
        // Nothing is being asked yet, so no shape is being drawn. Listen-and-choose is the arbitrary
        // one this never reaches: the host shows the loading surface while the phase is PREPARING.
        kind = LessonActivityKind.LISTEN_AND_CHOOSE,
        unitName = "",
        activityTitle = "",
        prompt = "",
        caption = null,
        activityNumber = 0,
        activityCount = 0,
        phase = LessonPhase.PREPARING,
        support = SupportLevel.NONE,
        learningObject = null,
        answers = emptyList(),
        audioAvailable = true,
        pendingSave = false,
        stopForNowVisible = false,
        pauseProgress = null
    )

    fun map(state: LessonSessionState, unitTheme: String = ""): LessonUiState {
        val content = state.currentActivity.content
        return LessonUiState(
            lessonId = state.lesson.id.value,
            unitName = unitTheme,
            activityTitle = title(state.currentActivity.family),
            prompt = content?.prompt.orEmpty(),
            // With no recording, the caption is the only way the question reaches a child. It is
            // shown whenever there is no sound rather than only when captions are switched on.
            caption = if (state.audioAvailable) null else content?.prompt,
            activityNumber = state.activityIndex + 1,
            activityCount = state.lesson.activities.size,
            phase = phase(state),
            support = support(state.supportLevel),
            learningObject = learningObject(content),
            answers = answers(content),
            audioAvailable = state.audioAvailable,
            pendingSave = state.saveStatus is SaveStatus.Unsaved,
            stopForNowVisible = state.stopRequested,
            // The speaking pause is a clock, and no clock runs yet. Null is the honest value.
            pauseProgress = null,
            kind = kind(state.currentActivity.family)
        )
    }

    private fun kind(family: com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily) =
        when (family) {
            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.LISTEN_AND_CHOOSE ->
                LessonActivityKind.LISTEN_AND_CHOOSE

            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.PICTURE_MATCHING ->
                LessonActivityKind.PICTURE_MATCHING

            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.LETTER_AND_SOUND ->
                LessonActivityKind.LETTER_AND_SOUND

            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.SAY_WITH_PIP ->
                LessonActivityKind.SAY_WITH_PIP

            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.REVIEW ->
                LessonActivityKind.REVIEW
        }

    private fun phase(state: LessonSessionState) = when (state.phase) {
        DomainPhase.Asking -> when {
            // The question is still being spoken, whichever family is asking it. See ADR 0004.
            state.soundingPrompt != null -> LessonPhase.PROMPTING

            state.currentActivity.family == ActivityFamilySpeaking -> LessonPhase.RESPONDING

            else -> LessonPhase.ANSWERING
        }

        // Deliberately not PROMPTING, even while a replay sounds. Hiding the correct answer's
        // treatment behind a prompting screen is a worse lie than not showing that a sound plays.
        DomainPhase.AwaitingCheckpoint -> LessonPhase.CORRECT

        DomainPhase.Finished -> LessonPhase.COMPLETED
    }

    private fun support(level: Int) = when (level) {
        0 -> SupportLevel.NONE
        1 -> SupportLevel.REPEAT
        2 -> SupportLevel.SLOWER
        else -> SupportLevel.VIETNAMESE
    }

    /**
     * The one thing an activity is about, where it has one.
     *
     * Listen and choose has none, per ADR 0005: the question is the sound, and naming a focal object
     * would put the answer on the stage.
     */
    private fun learningObject(content: ActivityContent?): LearningObject? = when (content) {
        is ActivityContent.PictureMatching, is ActivityContent.LetterAndSound ->
            (content as Answerable).choices
                .firstOrNull { it.skillId == content.correct }
                ?.let {
                    LearningObject(
                        id = it.skillId.value,
                        label = it.label,
                        image = it.image.value
                    )
                }

        else -> null
    }

    private fun answers(content: ActivityContent?): List<AnswerOption> = when (content) {
        is Answerable -> content.choices.map {
            AnswerOption(
                id = it.skillId.value,
                label = it.label,
                image = it.image.value,
                feedback = HelloBeChoiceFeedback.NEUTRAL
            )
        }

        is ActivityContent.GuidedRepetition -> emptyList()

        null -> emptyList()
    }

    private fun title(family: com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily) =
        when (family) {
            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.LISTEN_AND_CHOOSE ->
                "Listen and choose"

            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.PICTURE_MATCHING ->
                "Picture matching"

            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.LETTER_AND_SOUND ->
                "Letter and sound"

            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.SAY_WITH_PIP ->
                "Say with Pip"

            com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.REVIEW -> "Review"
        }

    private val ActivityFamilySpeaking =
        com.nphkhiem.englishforyourchildren.domain.model.ActivityFamily.SAY_WITH_PIP
}
