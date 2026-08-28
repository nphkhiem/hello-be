package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipGuide
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipPose
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryHeader
import com.nphkhiem.englishforyourchildren.ui.tv.component.StorybookScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Where a child lands every session, and where one press resumes learning.
 *
 * The dominant action is wider, carries the primary tone, and holds entry focus, so it reads as the
 * next thing to do whether or not focus is on it. The two secondary destinations stay visible
 * without competing.
 *
 * There is no navigation rail and no child-facing settings menu. The only utilities are the profile
 * chip and the grown-up entry, both in the header.
 *
 * Back is not handled. Child home is the root, and the information architecture has Back exit to
 * the launcher from here, which is what it already does.
 */
@Composable
fun ChildHomeScreen(
    state: ChildHomeUiState,
    onAction: (ChildHomeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val dominantFocus = remember { FocusRequester() }
    val fallbackFocus = remember { FocusRequester() }
    val layout = homeDestinations(state.primary)
    val dominantAvailable = isDominantAvailable(state.primary)

    StorybookScaffold(
        modifier = modifier,
        // When the checkpoint will not open, focus goes to the nearest thing that still works
        // rather than resting on a control that cannot be pressed.
        entryFocus = if (dominantAvailable) dominantFocus else fallbackFocus,
        header = {
            StoryHeader(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.picker_brand_home),
                action = {
                    Row(
                        modifier = Modifier.helloBeFocusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HelloBeAction(
                            label = stringResource(R.string.home_grown_ups),
                            onClick = { onAction(ChildHomeAction.CaregiverEntryRequested) },
                            tone = HelloBeActionTone.QUIET
                        )
                        HelloBeAction(
                            label = state.profileName,
                            onClick = { onAction(ChildHomeAction.SwitchProfileRequested) },
                            tone = HelloBeActionTone.QUIET,
                            supportingText = state.profileAvatar
                        )
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(
                space = HelloBeTheme.spacing.sectionGap,
                alignment = Alignment.CenterVertically
            )
        ) {
            Greeting(state = state)
            Destinations(
                state = state,
                layout = layout,
                dominantAvailable = dominantAvailable,
                onAction = onAction,
                dominantFocus = dominantFocus,
                fallbackFocus = fallbackFocus
            )
        }
    }
}

@Composable
private fun Greeting(state: ChildHomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PipGuide(
            pose = PipPose.GREETING,
            contentDescription = stringResource(R.string.lesson_pip_waiting),
            modifier = Modifier.size(HelloBeLayout.pipMinSize)
        )
        Column(verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)) {
            Text(
                text = if (state.primary is HomePrimary.CourseComplete) {
                    stringResource(R.string.home_course_complete)
                } else {
                    state.greeting
                },
                style = HelloBeTheme.typography.headlineLarge,
                color = HelloBeTheme.colors.textPrimary
            )
            Text(
                text = state.greetingHint,
                style = HelloBeTheme.typography.bodyLarge,
                color = HelloBeTheme.colors.textSecondary
            )
            if (state.pendingSave) {
                // The same wording HB-D04 already uses, rather than a second phrasing for one fact.
                Text(
                    text = stringResource(R.string.lesson_pending_save),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.warningContent
                )
            }
        }
    }
}

@Composable
private fun Destinations(
    state: ChildHomeUiState,
    layout: HomeLayout,
    dominantAvailable: Boolean,
    onAction: (ChildHomeAction) -> Unit,
    dominantFocus: FocusRequester,
    fallbackFocus: FocusRequester
) {
    val rowHeight =
        HelloBeTheme.layout.childPrimaryActionMinHeight + HelloBeTheme.focus.clearance * 2

    Row(
        modifier = Modifier.fillMaxWidth().height(rowHeight).helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        HelloBeAction(
            label = dominantLabel(state.primary),
            onClick = { onAction(actionFor(layout.dominant)) },
            tone = HelloBeActionTone.PRIMARY,
            availability = if (dominantAvailable) {
                HelloBeAvailability.ENABLED
            } else {
                HelloBeAvailability.UNAVAILABLE
            },
            supportingText = dominantContext(state.primary),
            stateDescription = if (dominantAvailable) {
                null
            } else {
                stringResource(R.string.home_continue_unavailable)
            },
            modifier = Modifier
                .weight(DOMINANT_WEIGHT)
                .fillMaxHeight()
                .focusRequester(dominantFocus)
        )

        layout.secondaries.forEachIndexed { index, target ->
            HelloBeAction(
                label = secondaryLabel(target),
                onClick = { onAction(actionFor(target)) },
                tone = HelloBeActionTone.SECONDARY,
                supportingText = secondaryHint(target),
                modifier = Modifier
                    .weight(SECONDARY_WEIGHT)
                    .fillMaxHeight()
                    .then(if (index == 0) Modifier.focusRequester(fallbackFocus) else Modifier)
            )
        }
    }
}

@Composable
private fun dominantLabel(primary: HomePrimary): String = when (primary) {
    is HomePrimary.StartFirstAdventure -> stringResource(R.string.home_start)
    is HomePrimary.CourseComplete -> stringResource(R.string.home_free_play)
    else -> stringResource(R.string.home_continue)
}

@Composable
private fun dominantContext(primary: HomePrimary): String? = when (primary) {
    is HomePrimary.Resume -> primary.context.ifBlank { null }
    is HomePrimary.ResumeUnavailable -> primary.context.ifBlank { null }
    is HomePrimary.CourseComplete -> stringResource(R.string.home_free_play_hint)
    is HomePrimary.StartFirstAdventure -> null
}

@Composable
private fun secondaryLabel(target: HomeTarget): String = when (target) {
    HomeTarget.LEARNING_PATH -> stringResource(R.string.home_learning_path)
    HomeTarget.FREE_PLAY -> stringResource(R.string.home_free_play)
    HomeTarget.CONTINUE -> stringResource(R.string.home_continue)
}

@Composable
private fun secondaryHint(target: HomeTarget): String? = when (target) {
    HomeTarget.LEARNING_PATH -> stringResource(R.string.home_learning_path_hint)
    HomeTarget.FREE_PLAY -> stringResource(R.string.home_free_play_hint)
    HomeTarget.CONTINUE -> null
}

private fun actionFor(target: HomeTarget): ChildHomeAction = when (target) {
    HomeTarget.CONTINUE -> ChildHomeAction.ContinueRequested
    HomeTarget.LEARNING_PATH -> ChildHomeAction.LearningPathRequested
    HomeTarget.FREE_PLAY -> ChildHomeAction.FreePlayRequested
}

/** The draft's 1.6fr against 1fr, which is what makes the next action read as the next action. */
private const val DOMINANT_WEIGHT = 1.6f
private const val SECONDARY_WEIGHT = 1f
