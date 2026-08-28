package com.nphkhiem.englishforyourchildren.feature.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryHeader
import com.nphkhiem.englishforyourchildren.ui.tv.component.StorybookScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * Making a profile, in two Select presses.
 *
 * The avatar and the name are already chosen when the screen opens, so the only required decision
 * is the age. Choosing it advances focus to Create, and one more press finishes.
 *
 * The screen renders a draft it is handed. It does not generate the name, does not persist
 * anything, and does not navigate.
 *
 * Renaming is not offered here. It needs a keyboard, a keyboard needs a text field, and a text
 * field is a reusable television mechanic that belongs in the shared module rather than inside
 * setup. Changing the picture needs no keyboard and is offered.
 */
@Composable
fun CreateProfileScreen(
    state: CreateProfileUiState,
    onAction: (CreateProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstAgeFocus = remember { FocusRequester() }
    val createFocus = remember { FocusRequester() }
    val chooserFocus = remember { FocusRequester() }

    // Keyed on the chosen age, so taking an age moves focus onward and changing it later brings
    // focus back rather than stranding it. This is a move the caregiver asked for by pressing
    // Select, which is not what ADR 0004 refused.
    LaunchedEffect(state.draft.age) {
        if (state.draft.age != null && !state.choosingAvatar) {
            runCatching { createFocus.requestFocus() }
        }
    }

    StorybookScaffold(
        modifier = modifier,
        // Entry focus goes where the screen already is, rather than always to the first age and
        // then being corrected by the effect above. A state that arrives with an age chosen has
        // already had that decision made.
        entryFocus = when {
            state.choosingAvatar -> chooserFocus
            state.draft.age != null -> createFocus
            else -> firstAgeFocus
        },
        header = {
            StoryHeader(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.picker_brand),
                contextLabel = stringResource(R.string.create_privacy)
            )
        }
    ) {
        if (state.choosingAvatar) {
            AvatarChooser(
                state = state,
                onAction = onAction,
                firstAvatarFocus = chooserFocus
            )
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.sectionGap)
            ) {
                Identity(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.width(HelloBeTheme.layout.cardThreeColumnSet)
                )
                Setup(
                    state = state,
                    onAction = onAction,
                    firstAgeFocus = firstAgeFocus,
                    createFocus = createFocus
                )
            }
        }
    }
}

/** Who the profile is, already decided, with the one thing that can be changed without typing. */
@Composable
private fun Identity(
    state: CreateProfileUiState,
    onAction: (CreateProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight().helloBeFocusGroup(),
        verticalArrangement = Arrangement.spacedBy(
            space = HelloBeTheme.spacing.cardGap,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = state.draft.avatarId,
            style = HelloBeTheme.typography.learningGlyph,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = state.draft.nickname,
            style = HelloBeTheme.typography.headlineMedium,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = stringResource(R.string.create_ready),
            style = HelloBeTheme.typography.bodyMedium,
            color = HelloBeTheme.colors.textSecondary
        )
        HelloBeAction(
            label = stringResource(R.string.create_change_picture),
            onClick = { onAction(CreateProfileAction.ChangePictureRequested) },
            tone = HelloBeActionTone.QUIET,
            availability = if (state.avatarChoices.isEmpty()) {
                // No pictures to choose from is not a chooser worth opening.
                HelloBeAvailability.DISABLED
            } else {
                HelloBeAvailability.ENABLED
            }
        )
        HelloBeAction(
            label = stringResource(R.string.create_change_name),
            onClick = { onAction(CreateProfileAction.ChangeNameRequested) },
            tone = HelloBeActionTone.QUIET
        )
    }
}

/** The one required decision, and the button it unlocks. */
@Composable
private fun Setup(
    state: CreateProfileUiState,
    onAction: (CreateProfileAction) -> Unit,
    firstAgeFocus: FocusRequester,
    createFocus: FocusRequester
) {
    val canCreate = canCreateProfile(state)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            space = HelloBeTheme.spacing.cardGap,
            alignment = Alignment.CenterVertically
        )
    ) {
        Text(
            text = stringResource(R.string.create_question),
            style = HelloBeTheme.typography.headlineLarge,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = stringResource(R.string.create_question_hint),
            style = HelloBeTheme.typography.bodyLarge,
            color = HelloBeTheme.colors.textSecondary
        )

        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
        ) {
            state.ages.forEachIndexed { index, age ->
                StoryCard(
                    title = age.toString(),
                    onClick = { onAction(CreateProfileAction.AgeChosen(age)) },
                    selected = state.draft.age == age,
                    // One digit, not a block of text.
                    centerContent = true,
                    modifier = Modifier
                        .width(HelloBeTheme.layout.cardFiveColumnSet)
                        .then(
                            if (index == 0) Modifier.focusRequester(firstAgeFocus) else Modifier
                        )
                )
            }
        }

        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.create_save_failed),
                style = HelloBeTheme.typography.bodyMedium,
                color = HelloBeTheme.colors.warningContent
            )
        }

        HelloBeAction(
            label = stringResource(R.string.create_submit),
            onClick = { onAction(CreateProfileAction.CreateRequested(state.draft)) },
            tone = HelloBeActionTone.PRIMARY,
            availability = if (canCreate) {
                HelloBeAvailability.ENABLED
            } else {
                HelloBeAvailability.UNAVAILABLE
            },
            // The hint the draft already puts beside this button. The age requirement is stated
            // before anyone presses, not after.
            stateDescription = if (canCreate) {
                null
            } else if (state.capacityReached) {
                stringResource(R.string.picker_add_child_full)
            } else {
                stringResource(R.string.create_needs_age)
            },
            supportingText = if (canCreate) null else stringResource(R.string.create_needs_age),
            modifier = Modifier.focusRequester(createFocus)
        )
    }
}

/** Pictures only. Choosing one needs no keyboard, which is why this half is offered here. */
@Composable
private fun AvatarChooser(
    state: CreateProfileUiState,
    onAction: (CreateProfileAction) -> Unit,
    firstAvatarFocus: FocusRequester
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(
            space = HelloBeTheme.spacing.cardGap,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.create_choose_picture),
            style = HelloBeTheme.typography.headlineLarge,
            color = HelloBeTheme.colors.textPrimary
        )
        Row(
            modifier = Modifier.helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
        ) {
            state.avatarChoices.forEachIndexed { index, avatar ->
                StoryCard(
                    title = avatar,
                    onClick = { onAction(CreateProfileAction.AvatarChosen(avatar)) },
                    selected = state.draft.avatarId == avatar,
                    centerContent = true,
                    modifier = Modifier
                        .width(HelloBeTheme.layout.cardFiveColumnSet)
                        .then(
                            if (index == 0) Modifier.focusRequester(firstAvatarFocus) else Modifier
                        )
                )
            }
        }
    }
}
