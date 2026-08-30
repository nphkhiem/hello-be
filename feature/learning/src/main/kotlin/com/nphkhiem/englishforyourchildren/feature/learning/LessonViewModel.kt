package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.lifecycle.ViewModel
import com.nphkhiem.englishforyourchildren.domain.model.Activity
import com.nphkhiem.englishforyourchildren.domain.model.ActivityInstanceId
import com.nphkhiem.englishforyourchildren.domain.model.Answerable
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CompleteSession
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.repository.StartSession
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.domain.session.LessonAction
import com.nphkhiem.englishforyourchildren.domain.session.LessonEffect
import com.nphkhiem.englishforyourchildren.domain.session.LessonPhase as DomainPhase
import com.nphkhiem.englishforyourchildren.domain.session.LessonReducer
import com.nphkhiem.englishforyourchildren.domain.session.LessonReduction
import com.nphkhiem.englishforyourchildren.domain.session.LessonSessionState
import com.nphkhiem.englishforyourchildren.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What a child can do to a lesson. Named for the press, not for what it will cause. */
sealed interface LessonUiAction {
    /**
     * [activityNumber] is the one the screen was showing when the child pressed.
     *
     * Without it a press that arrives just after the lesson moved on would answer the next
     * question with the button meant for the last one, which is what a held Select key does on a
     * television: it would walk a child through a lesson without them choosing anything.
     */
    data class AnswerChosen(val skillId: String, val activityNumber: Int) : LessonUiAction

    data object PromptReplayRequested : LessonUiAction

    data object SaveRetryRequested : LessonUiAction

    data object ContinueUnsaved : LessonUiAction

    data object StopRequested : LessonUiAction
}

/**
 * A lesson, running.
 *
 * It owns nothing about how a lesson behaves: that is the reducer, which is a function and has its
 * own table of tests. What lives here is everything the reducer refuses to touch, namely time,
 * storage and order.
 *
 * **Order is the point.** Actions are handled one at a time under a lock, so a press that arrives
 * while a write is in flight waits for it rather than racing it. A television remote makes this
 * ordinary: a button held a moment too long sends two presses, and the second must be judged
 * against the state the first produced, not beside it.
 */
@HiltViewModel
class LessonViewModel @Inject constructor(
    private val curriculum: CurriculumRepository,
    private val progress: ProgressRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _state = MutableStateFlow(LessonUiMapper.preparing())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    /** The lesson could not be opened at all. The host turns this into a recovery. */
    private val _unavailable = MutableStateFlow(false)
    val unavailable: StateFlow<Boolean> = _unavailable.asStateFlow()

    private val reducer = LessonReducer()
    private val lock = Mutex()
    private var session: LessonSessionState? = null

    /** The theme of the unit this lesson belongs to. A slug is not a thing to show anybody. */
    private var unitTheme: String = ""

    suspend fun start(profileId: ProfileId, lessonId: LessonId, courseVersion: CourseVersion) {
        val lesson = curriculum.getLesson(lessonId, courseVersion)
        if (lesson !is DomainResult.Success) {
            _unavailable.value = true
            return
        }

        val started = progress.startSession(
            StartSession(profileId = profileId, lessonId = lessonId, courseVersion = courseVersion)
        )
        if (started !is DomainResult.Success) {
            _unavailable.value = true
            return
        }

        var state = LessonReducer.start(
            sessionId = started.value.id,
            profileId = profileId,
            courseVersion = courseVersion,
            lesson = lesson.value,
            firstInstance = instanceOf(lesson.value.activities.first()),
            promptAsset = lesson.value.activities.first().content?.promptAsset,
            startedAt = timeProvider.now()
        )

        // No recording exists for any prompt yet. Rather than waiting for a sound that will never
        // arrive, the lesson enters the state this app was designed to have: the words on screen,
        // and an answer that costs nothing if it was never really askable.
        if (state.promptAsset == null) {
            state =
                reducer.reduce(state, LessonAction.MediaUnavailable(state.currentInstance)).state
        }

        unitTheme = themeOf(lesson.value)
        session = state
        publish(state)
    }

    private suspend fun themeOf(
        lesson: com.nphkhiem.englishforyourchildren.domain.model.Lesson
    ): String {
        val course = curriculum.observeCourse().first()
        if (course !is DomainResult.Success) return ""
        return course.value.units.firstOrNull { it.id == lesson.unitId }?.theme.orEmpty()
    }

    suspend fun onAction(action: LessonUiAction) = lock.withLock {
        val current = session ?: return@withLock
        val domainAction = action.toDomain(current) ?: return@withLock
        apply(reducer.reduce(current, domainAction))
    }

    /**
     * Applies a reduction and everything it asked for, in order.
     *
     * Effects are run one at a time and their answers are fed straight back in, so a checkpoint's
     * confirmation is handled before anything else a child does. That is what makes "nothing moves
     * until the write lands" true in practice and not only in the reducer's table.
     */
    private suspend fun apply(reduction: LessonReduction) {
        var state = reduction.state
        session = state
        publish(state)

        for (effect in reduction.effects) {
            when (effect) {
                is LessonEffect.Persist -> {
                    val written = progress.persistCheckpoint(effect.command)
                    val answer = if (written is DomainResult.Success) {
                        LessonAction.CheckpointConfirmed(
                            expectedActivityInstanceId = state.currentInstance,
                            nextInstance = nextInstance(state),
                            at = timeProvider.now()
                        )
                    } else {
                        LessonAction.CheckpointFailed(state.currentInstance)
                    }
                    val next = reducer.reduce(state, answer)
                    state = next.state
                    session = state
                    publish(state)
                    for (more in next.effects) run(more)
                }

                else -> run(effect)
            }
        }
    }

    private suspend fun run(effect: LessonEffect) {
        when (effect) {
            is LessonEffect.Complete -> progress.completeSession(effect.command)

            // Playback has no module yet, and no recording exists to play. Doing nothing here is
            // the honest behaviour: the lesson already knows it is running without sound.
            is LessonEffect.Play, LessonEffect.PausePlayback -> Unit

            is LessonEffect.Persist -> progress.persistCheckpoint(effect.command)
        }
    }

    private fun LessonUiAction.toDomain(state: LessonSessionState): LessonAction? = when (this) {
        is LessonUiAction.AnswerChosen -> if (activityNumber != state.activityIndex + 1) {
            // A press for a question the child has already left.
            null
        } else {
            LessonAction.AnswerChosen(
                expectedActivityInstanceId = state.currentInstance,
                // Whether it was right is decided here and never reaches the screen, so the screen
                // cannot give it away through focus or ordering.
                correct = (state.currentActivity.content as? Answerable)?.correct?.value == skillId,
                at = timeProvider.now()
            )
        }

        LessonUiAction.PromptReplayRequested ->
            LessonAction.PromptReplayRequested(state.currentInstance)

        LessonUiAction.SaveRetryRequested -> LessonAction.SaveRetryRequested(state.currentInstance)

        LessonUiAction.ContinueUnsaved -> LessonAction.ContinueUnsaved(
            expectedActivityInstanceId = state.currentInstance,
            nextInstance = nextInstance(state)
        )

        LessonUiAction.StopRequested -> LessonAction.StopRequested(state.currentInstance)
    }

    private fun publish(state: LessonSessionState) {
        _state.value = LessonUiMapper.map(state, unitTheme)
        if (state.phase == DomainPhase.Finished) {
            session = state
        }
    }

    /**
     * The encounter that follows this one.
     *
     * Derived from the activity's own id and its occurrence, which is the grammar
     * `CONTENT_ID_REGISTRY.md` sets out. Deterministic on purpose: a resumed lesson names the same
     * encounters as the first pass through it.
     */
    private fun nextInstance(state: LessonSessionState): ActivityInstanceId? {
        val next = state.lesson.activities.getOrNull(state.activityIndex + 1) ?: return null
        return instanceOf(next)
    }

    private fun instanceOf(activity: Activity) = ActivityInstanceId("${activity.id.value}-1")
}
