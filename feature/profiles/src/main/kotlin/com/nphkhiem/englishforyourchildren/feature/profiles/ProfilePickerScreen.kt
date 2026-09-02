package com.nphkhiem.englishforyourchildren.feature.profiles

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
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryHeader
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryLoading
import com.nphkhiem.englishforyourchildren.ui.tv.component.StorybookScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The first screen anyone sees: a child recognises their own picture and gets in with one press.
 *
 * Avatar recognition leads, because the child using this cannot read. The nickname and the progress
 * line beneath it are for the caregiver standing behind them.
 *
 * Not a lesson, so none of the lesson vocabulary is borrowed: this screen has its own state, its
 * own actions, and sits on `StorybookScaffold` directly.
 *
 * Back is deliberately not handled. The picker is the root, and the information architecture says
 * the app exits from here, which is what Back already does.
 */
@Composable
fun ProfilePickerScreen(
    state: ProfilePickerUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val rememberedFocus = remember { FocusRequester() }
    val firstProfileFocus = remember { FocusRequester() }
    val addFocus = remember { FocusRequester() }
    val focusTarget = pickerFocusTarget(state)

    StorybookScaffold(
        modifier = modifier,
        entryFocus = when (focusTarget) {
            PickerFocusTarget.REMEMBERED_PROFILE -> rememberedFocus
            PickerFocusTarget.FIRST_PROFILE -> firstProfileFocus
            PickerFocusTarget.ADD_PROFILE -> addFocus
        },
        // While loading there are no cards, so there is nothing for the claim to land on and the
        // screen would sit with nothing focused: a child pressing a direction would walk into the
        // header and find the adult gate before their own name.
        entryFocusReady = !state.loading,
        header = {
            StoryHeader(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.picker_brand),
                action = {
                    HelloBeAction(
                        label = stringResource(R.string.picker_caregiver),
                        onClick = { onAction(ProfileAction.CaregiverEntryRequested) },
                        tone = HelloBeActionTone.QUIET
                    )
                }
            )
        }
    ) {
        if (state.loading) {
            StoryLoading(
                contentDescription = stringResource(R.string.picker_loading),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    space = HelloBeTheme.spacing.sectionGap,
                    alignment = Alignment.CenterVertically
                )
            ) {
                Welcome()
                ProfileRow(
                    state = state,
                    onAction = onAction,
                    focusTarget = focusTarget,
                    rememberedFocus = rememberedFocus,
                    firstProfileFocus = firstProfileFocus,
                    addFocus = addFocus
                )
            }
        }
    }
}

/** Pip and the welcome, upper left, as the brief places them. */
@Composable
private fun Welcome() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PipGuide(
            pose = PipPose.GREETING,
            contentDescription = stringResource(R.string.picker_pip),
            modifier = Modifier.size(HelloBeLayout.pipMinSize)
        )
        Column(verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)) {
            Text(
                text = stringResource(R.string.picker_welcome),
                style = HelloBeTheme.typography.headlineLarge,
                color = HelloBeTheme.colors.textPrimary
            )
            Text(
                text = stringResource(R.string.picker_welcome_hint),
                style = HelloBeTheme.typography.bodyLarge,
                color = HelloBeTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun ProfileRow(
    state: ProfilePickerUiState,
    onAction: (ProfileAction) -> Unit,
    focusTarget: PickerFocusTarget,
    rememberedFocus: FocusRequester,
    firstProfileFocus: FocusRequester,
    addFocus: FocusRequester
) {
    val rowHeight = HelloBeTheme.layout.childChoiceMinHeight + HelloBeTheme.focus.clearance * 2
    val firstAvailable = remember(state.profiles) { state.profiles.firstOrNull { it.available } }
    val canAdd = canAddProfile(state)

    Row(
        modifier = Modifier.fillMaxWidth().height(rowHeight).helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        state.profiles.forEach { profile ->
            // Weighted equally whatever the count, so a child's card does not move as siblings are
            // added and the one they know stays where they left it.
            StoryCard(
                title = profile.nickname,
                supportingText = profile.summary,
                onClick = { onAction(ProfileAction.ProfileChosen(profile.id)) },
                availability = if (profile.available) {
                    HelloBeAvailability.ENABLED
                } else {
                    HelloBeAvailability.UNAVAILABLE
                },
                stateDescription = if (profile.available) {
                    null
                } else {
                    stringResource(R.string.picker_profile_unavailable)
                },
                illustration = { Avatar(profile.avatar) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // Both requesters are attached where they belong regardless of which one the
                    // scaffold ends up claiming. An attached requester that is never asked for
                    // focus does nothing, and this keeps the placement rule in one readable spot.
                    .then(
                        when {
                            profile.available && profile.id == state.rememberedProfileId ->
                                Modifier.focusRequester(rememberedFocus)

                            profile === firstAvailable ->
                                Modifier.focusRequester(firstProfileFocus)

                            else -> Modifier
                        }
                    )
            )
        }

        // An action rather than a card, because the brief requires it to read as secondary and a
        // StoryCard has no tone to make it so. It also says the truthful thing: the profiles are
        // things you pick, this is something you do.
        HelloBeAction(
            label = stringResource(R.string.picker_add_child),
            onClick = { onAction(ProfileAction.AddProfileRequested) },
            tone = HelloBeActionTone.SECONDARY,
            availability = if (canAdd) {
                HelloBeAvailability.ENABLED
            } else {
                HelloBeAvailability.UNAVAILABLE
            },
            // The label never becomes a sentence. The explanation is announced instead, the same
            // way an unavailable replay explains itself.
            stateDescription = if (canAdd) {
                null
            } else {
                stringResource(R.string.picker_add_child_full)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(addFocus)
        )
    }
}

@Composable
private fun Avatar(avatar: String) {
    Text(
        text = avatar,
        style = HelloBeTheme.typography.headlineLarge,
        color = HelloBeTheme.colors.textPrimary
    )
}
