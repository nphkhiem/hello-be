package com.nphkhiem.englishforyourchildren.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeAction
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeScreen
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeViewModel
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathAction
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathScreen
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathViewModel
import com.nphkhiem.englishforyourchildren.feature.learning.LessonAction
import com.nphkhiem.englishforyourchildren.feature.learning.LessonActivity
import com.nphkhiem.englishforyourchildren.feature.learning.LessonCelebrationScreen
import com.nphkhiem.englishforyourchildren.feature.learning.LessonCelebrationViewModel
import com.nphkhiem.englishforyourchildren.feature.learning.LessonPhase
import com.nphkhiem.englishforyourchildren.feature.learning.LessonUiAction
import com.nphkhiem.englishforyourchildren.feature.learning.LessonViewModel
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileScreen
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfileAction
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfilePickerScreen
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfileViewModel
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The same screens, looking at real storage.
 *
 * They are separate composables rather than a condition inside the host for one concrete reason:
 * [hiltViewModel] needs a Hilt entry point, and the debug catalog's activity is not one. The
 * catalog passes fixtures and never reaches this file; an installed build passes none and never
 * reaches the fixture path. Keeping them apart means neither can accidentally take the other's
 * route.
 */
@Composable
internal fun LiveProfilePicker(
    onChosen: (ProfileId) -> Unit,
    onAddProfile: () -> Unit,
    onCaregiverEntry: () -> Unit,
    onUnreadable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model: ProfileViewModel = hiltViewModel()
    val state by model.picker.collectAsStateWithLifecycle()
    val unreadable by model.storageUnreadable.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Storage failing while the picker is open is the same situation as storage failing at launch,
    // and it has the same answer: tell an adult rather than show a child an empty shelf.
    LaunchedEffect(unreadable) {
        if (unreadable) onUnreadable()
    }

    ProfilePickerScreen(
        state = state,
        onAction = { action ->
            scope.launch { model.onPickerAction(action) }
            when (action) {
                is ProfileAction.ProfileChosen -> onChosen(ProfileId(action.profileId))
                ProfileAction.AddProfileRequested -> onAddProfile()
                ProfileAction.CaregiverEntryRequested -> onCaregiverEntry()
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun LiveProfileCreate(onCreated: (ProfileId) -> Unit, modifier: Modifier = Modifier) {
    val model: ProfileViewModel = hiltViewModel()
    val state by model.create.collectAsStateWithLifecycle()
    val created by model.lastCreated.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Moving on only once the write has reported back. Navigating optimistically would open a home
    // for a child that storage may have refused.
    LaunchedEffect(created) {
        created?.let(onCreated)
    }

    CreateProfileScreen(
        state = state,
        onAction = { action -> scope.launch { model.onCreateAction(action) } },
        modifier = modifier
    )
}

@Composable
internal fun LiveChildHome(
    profileId: ProfileId,
    onLearningPath: () -> Unit,
    onFreePlay: () -> Unit,
    onSwitchProfile: () -> Unit,
    onCaregiverEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model: ChildHomeViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()

    LaunchedEffect(profileId) { model.start(profileId) }

    ChildHomeScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ChildHomeAction.ContinueRequested,
                ChildHomeAction.LearningPathRequested -> onLearningPath()

                ChildHomeAction.FreePlayRequested -> onFreePlay()

                ChildHomeAction.SwitchProfileRequested -> onSwitchProfile()

                ChildHomeAction.CaregiverEntryRequested -> onCaregiverEntry()
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun LiveLearningPath(
    profileId: ProfileId,
    onLessonChosen: (LessonId) -> Unit,
    onHome: () -> Unit,
    onSwitchProfile: () -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model: LearningPathViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val unavailable by model.unavailable.collectAsStateWithLifecycle()

    LaunchedEffect(profileId) { model.start(profileId) }
    LaunchedEffect(unavailable) { if (unavailable) onUnavailable() }

    LearningPathScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is LearningPathAction.LessonChosen -> onLessonChosen(LessonId(action.lessonId))
                LearningPathAction.HomeRequested -> onHome()
                LearningPathAction.SwitchProfileRequested -> onSwitchProfile()
                else -> Unit
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun LiveLessonCelebration(
    profileId: ProfileId,
    lessonId: LessonId,
    courseVersion: CourseVersion,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model: LessonCelebrationViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()

    LaunchedEffect(lessonId) { model.start(profileId, lessonId, courseVersion) }

    // The reveal is a clock, and ADR 0003 keeps clocks out of the screen. It lives here with the
    // motion token that names the budget, the way the stop-for-now dialog's visibility does.
    val revealBudget = HelloBeTheme.motion.durations.celebrationMax.toLong()
    var revealed by remember(lessonId) { mutableStateOf(false) }
    LaunchedEffect(lessonId) {
        delay(revealBudget)
        revealed = true
    }

    LessonCelebrationScreen(
        state = state.copy(revealed = revealed),
        // Done, Maybe later and Play together all leave the same way. Only the host counts a
        // decline, and there is nothing to decline until play-together content exists.
        onAction = { onDone() },
        modifier = modifier
    )
}

@Composable
internal fun LiveLesson(
    profileId: ProfileId,
    lessonId: LessonId,
    courseVersion: CourseVersion,
    onFinished: () -> Unit,
    onStopConfirmed: () -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model: LessonViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val unavailable by model.unavailable.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(lessonId) { model.start(profileId, lessonId, courseVersion) }
    LaunchedEffect(unavailable) { if (unavailable) onUnavailable() }

    // Finishing is the host's business, not the screen's: the lesson says it is over and the
    // celebration is a destination rather than a state of this one.
    LaunchedEffect(state.phase) {
        if (state.phase == LessonPhase.COMPLETED) onFinished()
    }

    LessonActivity(
        state = state,
        onAction = { action ->
            scope.launch {
                when (action) {
                    is LessonAction.AnswerChosen -> model.onAction(
                        LessonUiAction.AnswerChosen(
                            skillId = action.answerId,
                            activityNumber = state.activityNumber
                        )
                    )

                    LessonAction.ReplayRequested ->
                        model.onAction(LessonUiAction.PromptReplayRequested)

                    LessonAction.BackRequested ->
                        model.onAction(LessonUiAction.StopRequested)

                    LessonAction.SkipRequested ->
                        model.onAction(LessonUiAction.SkipRequested)

                    LessonAction.KeepLearningRequested ->
                        model.onAction(LessonUiAction.KeepLearningRequested)

                    LessonAction.StopForNowConfirmed -> onStopConfirmed()

                    // Only Say with Pip offers Continue, and only one renderer is live, so this
                    // cannot arrive yet. It is named rather than swallowed by an else, so that
                    // wiring that renderer has to come back here and decide.
                    LessonAction.ContinueRequested -> Unit
                }
            }
        },
        modifier = modifier
    )
}
