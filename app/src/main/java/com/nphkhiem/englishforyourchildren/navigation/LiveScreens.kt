package com.nphkhiem.englishforyourchildren.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nphkhiem.englishforyourchildren.domain.model.AgeBand
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateViewModel
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmation
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationKind
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationViewModel
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverLanguageViewModel
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverOverviewScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverOverviewUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverOverviewViewModel
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverScaffold
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSection
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSettingsAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSettingsScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSettingsUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSettingsViewModel
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverShellAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverShellState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverShellViewModel
import com.nphkhiem.englishforyourchildren.feature.caregiver.GateChallenge
import com.nphkhiem.englishforyourchildren.feature.caregiver.LocalCaregiverLanguage
import com.nphkhiem.englishforyourchildren.feature.caregiver.ManagedProfile
import com.nphkhiem.englishforyourchildren.feature.caregiver.OverviewProgress
import com.nphkhiem.englishforyourchildren.feature.caregiver.OverviewSummary
import com.nphkhiem.englishforyourchildren.feature.caregiver.ProfileManagementAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.ProfileManagementScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.ProfileManagementUiState
import com.nphkhiem.englishforyourchildren.feature.caregiver.ProfileManagementViewModel
import com.nphkhiem.englishforyourchildren.feature.caregiver.R as CaregiverR
import com.nphkhiem.englishforyourchildren.feature.caregiver.SettingId
import com.nphkhiem.englishforyourchildren.feature.caregiver.caregiverText
import com.nphkhiem.englishforyourchildren.feature.caregiver.coPlaySuggestion
import com.nphkhiem.englishforyourchildren.feature.caregiver.languageName
import com.nphkhiem.englishforyourchildren.feature.caregiver.languageNamed
import com.nphkhiem.englishforyourchildren.feature.caregiver.settingRows
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeAction
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeScreen
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeViewModel
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayAction
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayScreen
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayViewModel
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
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberHelloBeFocusRestorer
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
internal fun LiveFreePlay(
    profileId: ProfileId,
    preferredShelfId: String?,
    onHome: () -> Unit,
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model: FreePlayViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()

    LaunchedEffect(profileId) { model.start(profileId, preferredShelfId) }

    FreePlayScreen(
        state = state,
        onAction = { action ->
            when (action) {
                // Leaving is the host's, because free play does not own where a child came from.
                FreePlayAction.HomeRequested -> onHome()

                FreePlayAction.SwitchProfileRequested -> onSwitchProfile()

                else -> model.onAction(action)
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

                    // Say with Pip's Next. It became reachable the moment every activity started
                    // rendering as itself, which is what the note here previously said would have
                    // to come back and be decided.
                    LessonAction.ContinueRequested ->
                        model.onAction(LessonUiAction.RepetitionFinished)
                }
            }
        },
        modifier = modifier
    )
}

/**
 * The caregiver area, in the language its reader chose.
 *
 * Wrapped around the caregiver destinations and nowhere else, which is the whole point of the
 * scoping: child mode is English-led whatever a caregiver sets, so the override must not be able to
 * reach it.
 */
@Composable
internal fun CaregiverLanguageScope(content: @Composable () -> Unit) {
    val model: CaregiverLanguageViewModel = hiltViewModel()
    val language by model.language.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalCaregiverLanguage provides language) { content() }
}

/**
 * The door, live.
 *
 * The question is composed here rather than in the ViewModel, because a sentence needs a string
 * resource and a language and the ViewModel has neither. The arithmetic comes up from below and the
 * grammar goes on here.
 *
 * Whether the gate opens is still the host's answer, not the screen's, which is what lets the
 * screen know the correct index without being the thing that lets anyone through.
 */
@Composable
internal fun LiveAdultGate(onOpened: () -> Unit, modifier: Modifier = Modifier) {
    val model: AdultGateViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.opened) { if (state.opened) onOpened() }

    AdultGateScreen(
        state = AdultGateUiState(
            challenge = GateChallenge(
                question = caregiverText(
                    CaregiverR.string.gate_question,
                    state.arithmetic.left,
                    state.arithmetic.right
                ),
                answers = state.arithmetic.answers.map { it.toString() },
                correctIndex = state.arithmetic.correctIndex
            ),
            previousAnswerWasWrong = state.previousAnswerWasWrong
        ),
        onAction = { model.onAction(it) },
        modifier = modifier
    )
}

/**
 * Settings, live.
 *
 * The rows are assembled here for the same reason the gate's question is: every title, every
 * consequence and every option name is a piece of writing.
 */
@Composable
internal fun LiveCaregiverSettings(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val model: CaregiverSettingsViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<SettingId?>(null) }

    val names = CaregiverLanguage.entries.associateWith { languageName(it) }

    CaregiverSettingsScreen(
        state = CaregiverSettingsUiState(
            rows = settingRows(state.settings),
            expandedRow = expanded,
            saveStatus = state.saveStatus,
            canRestoreDefaults = state.canRestoreDefaults
        ),
        onAction = { action ->
            when (action) {
                is CaregiverSettingsAction.SettingToggled -> model.onToggle(action.id)

                // Expansion is where the caregiver is looking, not what they have decided, so it
                // lives here and never reaches storage.
                is CaregiverSettingsAction.SettingExpanded ->
                    expanded = if (expanded == action.id) null else action.id

                is CaregiverSettingsAction.SettingChoiceChosen -> {
                    languageNamed(action.option, names)?.let { model.onLanguageChosen(it) }
                    expanded = null
                }

                CaregiverSettingsAction.RestoreDefaultsRequested -> model.onRestoreDefaults()
            }
        },
        modifier = modifier
    )

    BackHandler { onBack() }
}

/**
 * The frame every caregiver section sits in, live.
 *
 * Only the child's name comes from storage. The section is where the host has put the caregiver,
 * not something to be read back, which is why it is a parameter rather than state.
 */
@Composable
internal fun LiveCaregiverShell(
    profileId: ProfileId?,
    section: CaregiverSection,
    onSection: (CaregiverSection) -> Unit,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit
) {
    val model: CaregiverShellViewModel = hiltViewModel()
    val profileName by model.profileName.collectAsStateWithLifecycle()

    LaunchedEffect(profileId) { model.start(profileId) }

    CaregiverScaffold(
        state = CaregiverShellState(profileName = profileName, section = section),
        onAction = { action ->
            when (action) {
                is CaregiverShellAction.SectionChosen -> onSection(action.section)
                CaregiverShellAction.ReturnToChildRequested -> onReturn()
            }
        },
        modifier = modifier,
        content = body
    )
}

/**
 * The caregiver's view of their child's practice, live.
 *
 * Two summaries rather than the approved three. The third names a unit to come back to and nothing
 * derives one yet, so it is left out: showing what can be counted and leaving out what cannot be is
 * the honest version of this screen.
 *
 * The suggestion is the lesson's own, written by whoever wrote the lesson, and null where the child
 * has not practised or the lesson offers nothing. The state calls that the brief's
 * unavailable-content state rather than an error, which is what it is.
 */
@Composable
internal fun LiveCaregiverOverview(profileId: ProfileId?, modifier: Modifier = Modifier) {
    val model: CaregiverOverviewViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()

    LaunchedEffect(profileId) { profileId?.let { model.start(it) } }

    val counts = state.counts
    CaregiverOverviewScreen(
        state = CaregiverOverviewUiState(
            profileName = state.profileName,
            period = caregiverText(CaregiverR.string.overview_period),
            progress = when {
                counts == null -> OverviewProgress.NewProfile

                else -> OverviewProgress.Practiced(
                    summaries = listOf(
                        OverviewSummary(
                            label = caregiverText(CaregiverR.string.overview_adventures_label),
                            value = counts.lessonsFinished.toString(),
                            note = caregiverText(CaregiverR.string.overview_adventures_note)
                        ),
                        OverviewSummary(
                            label = caregiverText(CaregiverR.string.overview_words_label),
                            value = counts.wordsMet.toString(),
                            note = caregiverText(CaregiverR.string.overview_words_note)
                        )
                    ),
                    recentWords = state.recentWords
                )
            },
            suggestion = state.suggestion?.let { coPlaySuggestion(it) },
            pendingSave = state.pendingSave
        ),
        modifier = modifier
    )
}

/**
 * The children on this television, live.
 *
 * The detail line carries the age and not the count of finished adventures the approved draft also
 * puts there. That count needs each child's progress rather than this child's, and fetching one
 * flow per profile to fill in a subtitle is a larger change than this screen is. It is recorded as
 * owed rather than quietly dropped.
 */
@Composable
internal fun LiveProfileManagement(
    onAdd: () -> Unit,
    onDelete: (ProfileId) -> Unit,
    onReset: (ProfileId) -> Unit,
    modifier: Modifier = Modifier
) {
    val model: ProfileManagementViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()

    ProfileManagementScreen(
        state = ProfileManagementUiState(
            profiles = state.profiles.map { child ->
                ManagedProfile(
                    id = child.id.value,
                    name = child.nickname,
                    avatar = child.avatarId.value,
                    detail = caregiverText(
                        CaregiverR.string.profiles_detail_age,
                        child.ageBand.years()
                    )
                )
            },
            selectedId = state.selectedId?.value,
            persistenceFailed = state.persistenceFailed
        ),
        onAction = { action ->
            when (action) {
                is ProfileManagementAction.ProfileSelected -> model.onSelect(ProfileId(action.id))

                ProfileManagementAction.AddProfileRequested -> onAdd()

                is ProfileManagementAction.DeleteProfileRequested ->
                    onDelete(ProfileId(action.id))

                is ProfileManagementAction.ResetProgressRequested -> onReset(ProfileId(action.id))

                // Editing a name and changing a picture have no destination yet. Reported rather
                // than handled, so the gap is one place rather than a control that quietly does
                // nothing.
                is ProfileManagementAction.EditNameRequested -> Unit

                is ProfileManagementAction.ChangeAvatarRequested -> Unit
            }
        },
        modifier = modifier
    )
}

/** How a child's age reads on a line, which is a number rather than a band. */
private fun AgeBand.years(): Int = when (this) {
    AgeBand.THREE -> 3
    AgeBand.FOUR -> 4
    AgeBand.FIVE -> 5
}

/**
 * A thing that cannot be undone, live.
 *
 * The host leaves when it is done rather than the dialog drawing a finished state, which is why
 * completion is a flag on the ViewModel and not a fourth phase.
 */
@Composable
internal fun LiveCaregiverConfirmation(
    kind: CaregiverConfirmationKind,
    profileId: ProfileId,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model: CaregiverConfirmationViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val profileModel: ProfileManagementViewModel = hiltViewModel()
    val people by profileModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(kind, profileId) { model.start(kind, profileId) }
    LaunchedEffect(state.done) { if (state.done) onFinished() }

    val child = people.profiles.firstOrNull { it.id == profileId }
    CaregiverConfirmation(
        state = CaregiverConfirmationState(
            kind = state.kind,
            // Named on every one of them. A caregiver with four children must never be asked to
            // delete "this profile".
            profileName = child?.nickname.orEmpty(),
            profileAvatar = child?.avatarId?.value.orEmpty(),
            phase = state.phase
        ),
        focusRestorer = rememberHelloBeFocusRestorer(),
        onAction = { action ->
            when (action) {
                CaregiverConfirmationAction.Dismissed -> onFinished()
                CaregiverConfirmationAction.Confirmed -> model.onConfirm()
                CaregiverConfirmationAction.RetryRequested -> model.onRetry()
            }
        },
        modifier = modifier
    )
}
