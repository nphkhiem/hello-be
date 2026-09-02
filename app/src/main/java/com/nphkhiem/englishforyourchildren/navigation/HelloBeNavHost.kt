package com.nphkhiem.englishforyourchildren.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.domain.model.CourseVersion
import com.nphkhiem.englishforyourchildren.domain.model.LessonId
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.AdultGateScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmation
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationKind
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverOverviewScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverRecovery
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverRecoveryAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverRecoveryState
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverScaffold
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSection
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverSettingsScreen
import com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverShellAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.ProfileManagementAction
import com.nphkhiem.englishforyourchildren.feature.caregiver.ProfileManagementScreen
import com.nphkhiem.englishforyourchildren.feature.learning.CelebrationAction
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeAction
import com.nphkhiem.englishforyourchildren.feature.learning.ChildHomeScreen
import com.nphkhiem.englishforyourchildren.feature.learning.ChildRecovery
import com.nphkhiem.englishforyourchildren.feature.learning.ChildRecoveryAction
import com.nphkhiem.englishforyourchildren.feature.learning.ChildRecoveryReason
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayAction
import com.nphkhiem.englishforyourchildren.feature.learning.FreePlayScreen
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathAction
import com.nphkhiem.englishforyourchildren.feature.learning.LearningPathScreen
import com.nphkhiem.englishforyourchildren.feature.learning.LessonAction
import com.nphkhiem.englishforyourchildren.feature.learning.LessonActivity
import com.nphkhiem.englishforyourchildren.feature.learning.LessonCelebrationScreen
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileAction
import com.nphkhiem.englishforyourchildren.feature.profiles.CreateProfileScreen
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfileAction
import com.nphkhiem.englishforyourchildren.feature.profiles.ProfilePickerScreen
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryLoading
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberHelloBeFocusRestorer

/**
 * The app's one navigation host.
 *
 * There is no route string anywhere in it. A destination is a [HelloBeKey] value, the stack is a
 * list of them, and every move is a typed action arriving from a screen. What a screen may ask for
 * is decided by its own sealed action type, and what the host may do about it is decided by the
 * key types, so an impossible navigation does not compile rather than failing at runtime.
 *
 * Overlays never enter the stack. The stop-for-now dialog, the play-together prompt and the
 * audio-unavailable overlay are state on the screens that own them, so Back reaches them before it
 * reaches this host: a screen's own handler is composed later and wins.
 *
 * The caregiver session is foreground-scoped, per the information architecture. Leaving the
 * foreground drops everything behind the gate, so a child who picks the remote up afterwards
 * cannot press Back into settings.
 */
@Composable
fun HelloBeNavHost(
    gateway: ProfileGateway,
    content: HelloBeContent?,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Null until storage has answered. The entry destination is not knowable before then, and
    // guessing one would mean showing a child a screen that the next frame takes away.
    var stack by remember(gateway) { mutableStateOf<List<HelloBeKey>?>(null) }

    LaunchedEffect(gateway) {
        stack = listOf(resolveEntry(gateway.snapshot()))
    }

    val resolved = stack ?: run {
        // Still is deliberate: this is the one screen that appears before anything else, and a
        // moving thing here would be motion a child has to wait out. StoryLoading has no
        // reduced-motion variant for the same reason.
        StoryLoading(
            contentDescription = stringResource(R.string.launch_loading),
            modifier = modifier
        )
        return
    }

    // The invoking child surface, remembered so closing the session can put it back. It is not a
    // key on the stack: the caregiver area replaces the child surface rather than covering it.
    var caregiverOrigin by remember { mutableStateOf(ChildReturnTarget.CHILD_HOME) }

    fun push(key: HelloBeKey) {
        stack = resolved + key
    }
    fun replaceAll(keys: List<HelloBeKey>) {
        stack = keys
    }
    fun pop() {
        if (resolved.size > 1) stack = resolved.dropLast(1)
    }

    val current = resolved.last()

    // The host's share of Back: pop, or leave. Screens that answer Back themselves are composed
    // deeper and take it first.
    BackHandler {
        when (backOutcome(resolved)) {
            BackOutcome.Pop -> pop()
            BackOutcome.ExitApp -> onExitApp()
        }
    }

    // Foreground-scoped caregiver session. ADR 0003 keeps lifecycle out of the feature modules,
    // so it lives here, which is the one place that owns the stack it has to clean up.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Read the stack when the event arrives, never the destination this effect happened to
            // see when it started. Capturing that one sent a closing session to the launch picker
            // instead of the child's own home, because at first composition the top of the stack
            // was still child home and had no caregiver profile on it.
            val live = stack
            if (event == Lifecycle.Event.ON_STOP &&
                live != null &&
                live.any { it.isCaregiver() }
            ) {
                val profileId = live.firstNotNullOfOrNull { profileIdOf(it) }
                stack = afterCaregiverSessionClosed(profileId, caregiverOrigin)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    when (val key = current) {
        HelloBeKey.ProfileCreate -> if (content == null) {
            LiveProfileCreate(
                onCreated = { replaceAll(afterProfileChosen(it)) },
                modifier = modifier
            )
        } else {
            content.let {
                CreateProfileScreen(
                    state = it.profileCreate(),
                    onAction = { action ->
                        when (action) {
                            // Creating a child replaces the entry destination with their home, so
                            // Back leaves the app rather than returning to the form they just filled.
                            is CreateProfileAction.CreateRequested ->
                                replaceAll(afterProfileChosen(ProfileId(CREATED_PROFILE)))

                            else -> Unit
                        }
                    },
                    modifier = modifier
                )
            } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)
        }

        is HelloBeKey.ProfilePicker -> if (content == null) {
            LiveProfilePicker(
                onChosen = { replaceAll(afterProfileChosen(it)) },
                onAddProfile = { push(HelloBeKey.ProfileCreate) },
                onCaregiverEntry = {
                    caregiverOrigin = ChildReturnTarget.CHILD_HOME
                    push(
                        HelloBeKey.CaregiverGate(
                            profileId = null,
                            returnTarget = ChildReturnTarget.CHILD_HOME
                        )
                    )
                },
                onUnreadable = {
                    replaceAll(
                        listOf(HelloBeKey.Recovery(reason = RecoveryReason.APP_NEEDS_GROWN_UP))
                    )
                },
                modifier = modifier
            )
        } else {
            content.let {
                ProfilePickerScreen(
                    state = it.profilePicker(key.mode),
                    onAction = { action ->
                        when (action) {
                            is ProfileAction.ProfileChosen ->
                                replaceAll(afterProfileChosen(ProfileId(action.profileId)))

                            ProfileAction.AddProfileRequested -> push(HelloBeKey.ProfileCreate)

                            ProfileAction.CaregiverEntryRequested -> {
                                caregiverOrigin = ChildReturnTarget.CHILD_HOME
                                push(
                                    HelloBeKey.CaregiverGate(
                                        profileId = null,
                                        returnTarget = ChildReturnTarget.CHILD_HOME
                                    )
                                )
                            }
                        }
                    },
                    modifier = modifier
                )
            } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)
        }

        is HelloBeKey.ChildHome -> if (content == null) {
            LiveChildHome(
                profileId = key.profileId,
                onLearningPath = { push(HelloBeKey.LearningPath(profileId = key.profileId)) },
                onFreePlay = { push(HelloBeKey.FreePlay(profileId = key.profileId)) },
                onSwitchProfile = {
                    push(HelloBeKey.ProfilePicker(mode = ProfilePickerMode.Switch(key.profileId)))
                },
                onCaregiverEntry = {
                    caregiverOrigin = ChildReturnTarget.CHILD_HOME
                    push(
                        HelloBeKey.CaregiverGate(
                            profileId = key.profileId,
                            returnTarget = ChildReturnTarget.CHILD_HOME
                        )
                    )
                },
                modifier = modifier
            )
        } else {
            content.let {
                ChildHomeScreen(
                    state = it.childHome(key.profileId),
                    onAction = { action ->
                        when (action) {
                            ChildHomeAction.ContinueRequested,
                            ChildHomeAction.LearningPathRequested ->
                                push(HelloBeKey.LearningPath(profileId = key.profileId))

                            ChildHomeAction.FreePlayRequested ->
                                push(HelloBeKey.FreePlay(profileId = key.profileId))

                            ChildHomeAction.SwitchProfileRequested -> push(
                                HelloBeKey.ProfilePicker(
                                    mode = ProfilePickerMode.Switch(key.profileId)
                                )
                            )

                            ChildHomeAction.CaregiverEntryRequested -> {
                                caregiverOrigin = ChildReturnTarget.CHILD_HOME
                                push(
                                    HelloBeKey.CaregiverGate(
                                        profileId = key.profileId,
                                        returnTarget = ChildReturnTarget.CHILD_HOME
                                    )
                                )
                            }
                        }
                    },
                    modifier = modifier
                )
            } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)
        }

        is HelloBeKey.LearningPath -> if (content == null) {
            LiveLearningPath(
                profileId = key.profileId,
                onLessonChosen = { lesson ->
                    push(HelloBeKey.Lesson(profileId = key.profileId, lessonId = lesson))
                },
                onHome = { replaceAll(afterProfileChosen(key.profileId)) },
                onSwitchProfile = {
                    push(HelloBeKey.ProfilePicker(mode = ProfilePickerMode.Switch(key.profileId)))
                },
                onUnavailable = {
                    replaceAll(
                        listOf(HelloBeKey.Recovery(reason = RecoveryReason.APP_NEEDS_GROWN_UP))
                    )
                },
                modifier = modifier
            )
        } else {
            content.let {
                LearningPathScreen(
                    state = it.learningPath(key.profileId, key.preferredUnitId),
                    onAction = { action ->
                        when (action) {
                            is LearningPathAction.LessonChosen -> push(
                                HelloBeKey.Lesson(
                                    profileId = key.profileId,
                                    lessonId = LessonId(action.lessonId)
                                )
                            )

                            LearningPathAction.HomeRequested -> pop()

                            LearningPathAction.SwitchProfileRequested -> push(
                                HelloBeKey.ProfilePicker(
                                    mode = ProfilePickerMode.Switch(key.profileId)
                                )
                            )

                            else -> Unit
                        }
                    },
                    modifier = modifier
                )
            } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)
        }

        is HelloBeKey.Lesson -> if (content == null) {
            LiveLesson(
                profileId = key.profileId,
                lessonId = key.lessonId,
                courseVersion = SHIPPED_COURSE_VERSION,
                onFinished = {
                    replaceAll(
                        resolved.dropLast(1) + HelloBeKey.LessonCelebration(
                            profileId = key.profileId,
                            lessonId = key.lessonId,
                            returnTarget = LessonReturnTarget.LEARNING_PATH
                        )
                    )
                },
                onStopConfirmed = { pop() },
                onUnavailable = {
                    replaceAll(
                        listOf(HelloBeKey.Recovery(reason = RecoveryReason.APP_NEEDS_GROWN_UP))
                    )
                },
                modifier = modifier
            )
        } else {
            content.let {
                // The stop-for-now dialog is local state, not a destination. It lives here rather than
                // on the stack so Back cannot reach it as a page, and so dismissing it never pops
                // anything: a child answering "keep learning" stays exactly where they were.
                var stopForNowVisible by remember(key) { mutableStateOf(false) }

                LessonActivity(
                    state = it.lesson(key.profileId, key.lessonId)
                        .copy(stopForNowVisible = stopForNowVisible),
                    onAction = { action ->
                        when (action) {
                            LessonAction.BackRequested -> stopForNowVisible = true

                            LessonAction.KeepLearningRequested -> stopForNowVisible = false

                            LessonAction.StopForNowConfirmed -> {
                                stopForNowVisible = false
                                pop()
                            }

                            LessonAction.ContinueRequested -> replaceAll(
                                resolved.dropLast(1) + HelloBeKey.LessonCelebration(
                                    profileId = key.profileId,
                                    lessonId = key.lessonId,
                                    returnTarget = LessonReturnTarget.LEARNING_PATH
                                )
                            )

                            else -> Unit
                        }
                    },
                    modifier = modifier
                )
            } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)
        }

        is HelloBeKey.LessonCelebration -> if (content == null) {
            LiveLessonCelebration(
                profileId = key.profileId,
                lessonId = key.lessonId,
                courseVersion = SHIPPED_COURSE_VERSION,
                onDone = { replaceAll(afterCelebrationDone(key.profileId, key.returnTarget)) },
                modifier = modifier
            )
        } else {
            LessonCelebrationScreen(
                state = content.celebration(key.profileId, key.lessonId),
                onAction = { action ->
                    when (action) {
                        CelebrationAction.DoneRequested,
                        CelebrationAction.MaybeLaterRequested,
                        CelebrationAction.PlayTogetherAccepted ->
                            replaceAll(afterCelebrationDone(key.profileId, key.returnTarget))
                    }
                },
                modifier = modifier
            )
        }

        is HelloBeKey.FreePlay -> if (content == null) {
            LiveFreePlay(
                profileId = key.profileId,
                preferredShelfId = key.preferredShelfId?.value,
                onHome = { pop() },
                onSwitchProfile = {
                    push(HelloBeKey.ProfilePicker(mode = ProfilePickerMode.Switch(key.profileId)))
                },
                modifier = modifier
            )
        } else {
            FreePlayScreen(
                state = content.freePlay(key.profileId, key.preferredShelfId),
                onAction = { action ->
                    when (action) {
                        FreePlayAction.HomeRequested -> pop()

                        FreePlayAction.SwitchProfileRequested -> push(
                            HelloBeKey.ProfilePicker(
                                mode = ProfilePickerMode.Switch(key.profileId)
                            )
                        )

                        else -> Unit
                    }
                },
                modifier = modifier
            )
        }

        is HelloBeKey.CaregiverGate -> content?.let {
            AdultGateScreen(
                state = it.adultGate(),
                onAction = { action ->
                    // The host judges the answer. The gate reports which position was pressed and
                    // never decides whether it opens, which is why the screen may know the correct
                    // index without being the thing that lets anyone through.
                    val challenge = it.adultGate().challenge
                    if (action is AdultGateAction.AnswerChosen &&
                        action.index == challenge.correctIndex
                    ) {
                        replaceAll(
                            resolved.dropLast(1) +
                                HelloBeKey.CaregiverDashboard(profileId = key.profileId)
                        )
                    }
                },
                modifier = modifier
            )
        } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)

        is HelloBeKey.CaregiverDashboard -> content?.let {
            CaregiverShell(
                content = it,
                profileId = key.profileId,
                section = CaregiverSection.OVERVIEW,
                onSection = { section -> push(sectionKey(section, key.profileId)) },
                onReturn = {
                    replaceAll(afterCaregiverSessionClosed(key.profileId, caregiverOrigin))
                },
                modifier = modifier
            ) { CaregiverOverviewScreen(state = it.caregiverOverview(key.profileId)) }
        } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)

        is HelloBeKey.CaregiverSettings -> content?.let {
            CaregiverShell(
                content = it,
                profileId = key.profileId,
                section = CaregiverSection.SETTINGS,
                onSection = { section ->
                    replaceCaregiver(::replaceAll, resolved, section, key.profileId)
                },
                onReturn = {
                    replaceAll(afterCaregiverSessionClosed(key.profileId, caregiverOrigin))
                },
                modifier = modifier
            ) {
                // Settings changes are the data layer's to apply. Navigation has nothing to do
                // when one arrives, and saying so with an empty handler is clearer than a check
                // that can only ever be true.
                CaregiverSettingsScreen(
                    state = it.caregiverSettings(),
                    onAction = {}
                )
            }
        } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)

        is HelloBeKey.ProfileManagement -> content?.let {
            CaregiverShell(
                content = it,
                profileId = key.selectedProfileId,
                section = CaregiverSection.PROFILES,
                onSection = { section ->
                    replaceCaregiver(
                        ::replaceAll,
                        resolved,
                        section,
                        key.selectedProfileId
                    )
                },
                onReturn = {
                    replaceAll(
                        afterCaregiverSessionClosed(key.selectedProfileId, caregiverOrigin)
                    )
                },
                modifier = modifier
            ) {
                ProfileManagementScreen(
                    state = it.profileManagement(key.selectedProfileId),
                    onAction = { action ->
                        when (action) {
                            is ProfileManagementAction.DeleteProfileRequested -> push(
                                HelloBeKey.DeleteProfileConfirmation(ProfileId(action.id))
                            )

                            is ProfileManagementAction.ResetProgressRequested -> push(
                                HelloBeKey.ResetProgressConfirmation(ProfileId(action.id))
                            )

                            else -> Unit
                        }
                    }
                )
            }
        } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)

        is HelloBeKey.DeleteProfileConfirmation -> content?.let {
            Confirmation(
                state = it.confirmation(CaregiverConfirmationKind.DELETE_PROFILE, key.profileId),
                onFinished = { pop() },
                modifier = modifier
            )
        } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)

        is HelloBeKey.ResetProgressConfirmation -> content?.let {
            Confirmation(
                state = it.confirmation(CaregiverConfirmationKind.RESET_PROGRESS, key.profileId),
                onFinished = { pop() },
                modifier = modifier
            )
        } ?: MissingContent(key, onSafeReturn = { pop() }, modifier)

        is HelloBeKey.Recovery -> Recovery(
            key = key,
            onRetry = { stack = null },
            onSafeReturn = { pop() },
            modifier = modifier
        )
    }
}

@Composable
private fun Recovery(
    key: HelloBeKey.Recovery,
    onRetry: () -> Unit,
    onSafeReturn: () -> Unit,
    modifier: Modifier
) {
    when (key.reason) {
        RecoveryReason.APP_NEEDS_GROWN_UP -> CaregiverRecovery(
            state = CaregiverRecoveryState(code = DATABASE_CODE),
            onAction = { action ->
                when (action) {
                    CaregiverRecoveryAction.RetryRequested -> onRetry()
                    CaregiverRecoveryAction.ResetReviewRequested -> Unit
                }
            },
            modifier = modifier
        )

        RecoveryReason.LESSON_UNAVAILABLE,
        RecoveryReason.NO_VALID_ROOT_CONTENT -> ChildRecovery(
            reason = if (key.reason == RecoveryReason.LESSON_UNAVAILABLE) {
                ChildRecoveryReason.LESSON_UNAVAILABLE
            } else {
                ChildRecoveryReason.EMPTY_LIBRARY
            },
            focusRestorer = rememberHelloBeFocusRestorer(),
            onAction = { action ->
                if (action is ChildRecoveryAction.LearningPathRequested) onSafeReturn()
            },
            modifier = modifier
        )
    }
}

@Composable
private fun Confirmation(
    state: com.nphkhiem.englishforyourchildren.feature.caregiver.CaregiverConfirmationState,
    onFinished: () -> Unit,
    modifier: Modifier
) {
    CaregiverConfirmation(
        state = state,
        focusRestorer = rememberHelloBeFocusRestorer(),
        onAction = { action ->
            when (action) {
                CaregiverConfirmationAction.Dismissed -> onFinished()
                CaregiverConfirmationAction.Confirmed -> onFinished()
                CaregiverConfirmationAction.RetryRequested -> Unit
            }
        },
        modifier = modifier
    )
}

@Composable
private fun CaregiverShell(
    content: HelloBeContent,
    profileId: ProfileId?,
    section: CaregiverSection,
    onSection: (CaregiverSection) -> Unit,
    onReturn: () -> Unit,
    modifier: Modifier,
    body: @Composable () -> Unit
) {
    CaregiverScaffold(
        state = content.caregiverShell(profileId).copy(section = section),
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

/** Sections replace one another rather than stacking, so the rail never grows a history. */
private fun replaceCaregiver(
    replaceAll: (List<HelloBeKey>) -> Unit,
    stack: List<HelloBeKey>,
    section: CaregiverSection,
    profileId: ProfileId?
) {
    replaceAll(stack.dropLast(1) + sectionKey(section, profileId))
}

private fun sectionKey(section: CaregiverSection, profileId: ProfileId?): HelloBeKey =
    when (section) {
        CaregiverSection.OVERVIEW -> HelloBeKey.CaregiverDashboard(profileId)
        CaregiverSection.SETTINGS -> HelloBeKey.CaregiverSettings(profileId)
        CaregiverSection.PROFILES -> HelloBeKey.ProfileManagement(profileId)
    }

private fun profileIdOf(key: HelloBeKey): ProfileId? = when (key) {
    is HelloBeKey.CaregiverDashboard -> key.profileId
    is HelloBeKey.CaregiverSettings -> key.profileId
    is HelloBeKey.ProfileManagement -> key.selectedProfileId
    is HelloBeKey.DeleteProfileConfirmation -> key.profileId
    is HelloBeKey.ResetProgressConfirmation -> key.profileId
    else -> null
}

/**
 * A destination with no live screen behind it.
 *
 * This was documented as never reached in practice. It is reached: free play and the adult gate
 * are one press from child home and neither has a live screen, so pressing either arrives here.
 *
 * This used to render the caregiver database recovery for every one of them, which said three
 * untrue things at once: that storage had failed, that a reset might be the answer, and, on a
 * destination a child can reach, it said both of those to a child. `CaregiverRecovery` documents
 * itself as the one recovery a child never sees, and free play is a press away on child home.
 *
 * A child is now told the page is resting, calmly and with no code. Behind the gate, where a code
 * is safe, it carries one that names this rather than the database, and offers no reset.
 */
@Composable
private fun MissingContent(key: HelloBeKey, onSafeReturn: () -> Unit, modifier: Modifier) {
    if (key.isCaregiver()) {
        CaregiverRecovery(
            state = CaregiverRecoveryState(code = UNBUILT_SCREEN_CODE),
            onAction = { action ->
                when (action) {
                    CaregiverRecoveryAction.RetryRequested -> onSafeReturn()

                    // Deliberately nothing. No storage fault happened here, so erasing a child's
                    // progress could not repair it.
                    CaregiverRecoveryAction.ResetReviewRequested -> Unit
                }
            },
            modifier = modifier
        )
    } else {
        ChildRecovery(
            reason = ChildRecoveryReason.LESSON_UNAVAILABLE,
            focusRestorer = rememberHelloBeFocusRestorer(),
            onAction = { action ->
                if (action is ChildRecoveryAction.LearningPathRequested) onSafeReturn()
            },
            modifier = modifier
        )
    }
}

/**
 * The content version an installed build carries.
 *
 * One published version ships inside the app, so the lesson a child opens is always this one. When
 * a second version exists, which one a child is working through becomes a stored fact rather than a
 * constant, and it moves to their progress.
 */
private val SHIPPED_COURSE_VERSION = CourseVersion("2026.09")

private const val DATABASE_CODE = "DB-OPEN-01"

/**
 * A destination with no screen behind it yet.
 *
 * Not `DB-OPEN-01`. Storage is fine, and a caregiver who reads a database code aloud to someone
 * helping them deserves one that names what actually happened.
 */
private const val UNBUILT_SCREEN_CODE = "SCREEN-MISSING-01"
private const val CREATED_PROFILE = "created"
