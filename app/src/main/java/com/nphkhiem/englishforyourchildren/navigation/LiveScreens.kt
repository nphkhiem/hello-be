package com.nphkhiem.englishforyourchildren.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nphkhiem.englishforyourchildren.feature.learning.LessonPhase
import com.nphkhiem.englishforyourchildren.feature.learning.LessonUiAction
import com.nphkhiem.englishforyourchildren.feature.learning.LessonViewModel
import com.nphkhiem.englishforyourchildren.feature.learning.ListenAndChooseActivity
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileScreen
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfileAction
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfilePickerScreen
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfileViewModel
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

    // One renderer for now. Every family shares the lesson scaffold, and the four that differ do so
    // in what they draw inside it, which arrives when their content has pictures to draw.
    ListenAndChooseActivity(
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

                    LessonAction.StopForNowConfirmed -> onStopConfirmed()

                    else -> Unit
                }
            }
        },
        modifier = modifier
    )
}
