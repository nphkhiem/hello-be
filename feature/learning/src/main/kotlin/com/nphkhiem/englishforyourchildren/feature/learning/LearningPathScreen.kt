package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
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
 * One unit story page at a time, with the lessons inside it.
 *
 * The path never shows the whole course. A twelve unit course is twelve of these pages, reached
 * one step at a time, which is how the design brief keeps this a storybook rather than a catalog:
 * no all-course grid, no search, no endless horizontal feed.
 *
 * Pip sits in the recommended lesson's own column rather than pointing by colour or wording, so
 * the indication survives a child who cannot yet read and a television seen from across a room.
 *
 * Back is not handled. The information architecture has Back return to child home from here, and
 * that is host navigation, exactly as on [ChildHomeScreen].
 */
@Composable
fun LearningPathScreen(
    state: LearningPathUiState,
    onAction: (LearningPathAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val recommendedFocus = remember { FocusRequester() }
    val stepperFocus = remember { FocusRequester() }
    val recoveryFocus = remember { FocusRequester() }
    val unit = visibleUnit(state)

    StorybookScaffold(
        modifier = modifier,
        entryFocus = when (pathFocusTarget(state)) {
            PathFocusTarget.RECOMMENDED_LESSON -> recommendedFocus
            PathFocusTarget.UNIT_STEPPER -> stepperFocus
            PathFocusTarget.RECOVERY -> recoveryFocus
        },
        // Nothing here can take focus until the course has been read, and spending the one claim on
        // the view that is about to be replaced is what left a child focused on the profile chip.
        entryFocusReady = !state.loading,
        header = {
            StoryHeader(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.picker_brand_home),
                action = {
                    HelloBeAction(
                        label = state.profileName,
                        onClick = { onAction(LearningPathAction.SwitchProfileRequested) },
                        tone = HelloBeActionTone.QUIET,
                        supportingText = state.profileAvatar
                    )
                }
            )
        }
    ) {
        if (state.loading) {
            // Still reading the course. Not the recovery screen, which says something is wrong.
            StoryLoading(
                contentDescription = stringResource(R.string.path_loading),
                modifier = Modifier.fillMaxSize()
            )
        } else if (unit == null) {
            UnitRecovery(onAction = onAction, recoveryFocus = recoveryFocus)
        } else {
            UnitPage(
                unit = unit,
                state = state,
                onAction = onAction,
                recommendedFocus = recommendedFocus,
                stepperFocus = stepperFocus
            )
        }
    }
}

@Composable
private fun UnitPage(
    unit: UnitPageState,
    state: LearningPathUiState,
    onAction: (LearningPathAction) -> Unit,
    recommendedFocus: FocusRequester,
    stepperFocus: FocusRequester
) {
    val recommendedId = recommendedLessonId(unit)
    // Whether the child is currently navigating by unit rather than by lesson. Owned here so it
    // survives a stepper control being disposed, which is precisely when it is needed: paging to
    // the first or last unit removes the button the child is standing on, and a flag cleared by
    // that disposal would be false by the time anything could act on it.
    var steppingUnits by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        UnitCopy(
            unit = unit,
            state = state,
            onAction = onAction,
            stepperFocus = stepperFocus,
            steppingUnits = steppingUnits,
            onSteppingUnits = { steppingUnits = it }
        )
        LessonRow(
            unit = unit,
            recommendedId = recommendedId,
            onAction = onAction,
            recommendedFocus = recommendedFocus,
            onLessonFocused = { steppingUnits = false }
        )
    }
}

@Composable
private fun UnitCopy(
    unit: UnitPageState,
    state: LearningPathUiState,
    onAction: (LearningPathAction) -> Unit,
    stepperFocus: FocusRequester,
    steppingUnits: Boolean,
    onSteppingUnits: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space2)
        ) {
            Text(
                text = stringResource(
                    R.string.path_unit_kicker,
                    unit.unitNumber,
                    unit.unitCount
                ),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.textTertiary
            )
            Text(
                text = unit.theme,
                style = HelloBeTheme.typography.headlineMedium,
                color = HelloBeTheme.colors.textPrimary
            )
            Text(
                text = unit.objective,
                style = HelloBeTheme.typography.bodyMedium,
                color = HelloBeTheme.colors.textSecondary
            )
            if (state.pendingSave) {
                // The wording HB-D04 already uses, rather than a second phrasing for one fact.
                Text(
                    text = stringResource(R.string.lesson_pending_save),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.warningContent
                )
            }
        }

        UnitStepper(
            state = state,
            onAction = onAction,
            stepperFocus = stepperFocus,
            steppingUnits = steppingUnits,
            onSteppingUnits = onSteppingUnits
        )
    }
}

/**
 * Previous and next, grouped to the right of the unit copy.
 *
 * Grouped rather than flanking the lesson row, which was rejected by measurement: two rails plus
 * their gaps leave each lesson under the five column token. Grouping also keeps the unit copy
 * where the draft puts it, so the heading does not shift sideways when an arrow disappears at the
 * first or last unit.
 *
 * Each control names the unit it leads to, so previous/next reads as context rather than as a
 * bare chevron.
 */
@Composable
private fun UnitStepper(
    state: LearningPathUiState,
    onAction: (LearningPathAction) -> Unit,
    stepperFocus: FocusRequester,
    steppingUnits: Boolean,
    onSteppingUnits: (Boolean) -> Unit
) {
    val previous = state.previousUnit
    val next = state.nextUnit

    // Paging to the first or last unit removes the control the child is standing on. Without this
    // focus is dropped at the edges of the course, which is exactly where a child is most likely
    // to still be pressing.
    LaunchedEffect(previous == null, next == null) {
        if (steppingUnits) {
            stepperFocus.requestFocus()
        }
    }

    Row(
        modifier = Modifier
            .helloBeFocusGroup()
            .onFocusChanged { if (it.hasFocus) onSteppingUnits(true) },
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)
    ) {
        if (previous != null) {
            HelloBeAction(
                label = previous.theme,
                onClick = { onAction(LearningPathAction.PreviousUnitRequested) },
                tone = HelloBeActionTone.QUIET,
                supportingText = stringResource(R.string.path_previous_unit, previous.unitNumber),
                modifier = Modifier.focusRequester(stepperFocus)
            )
        }
        if (next != null) {
            HelloBeAction(
                label = next.theme,
                onClick = { onAction(LearningPathAction.NextUnitRequested) },
                tone = HelloBeActionTone.QUIET,
                supportingText = stringResource(R.string.path_next_unit, next.unitNumber),
                // Takes the requester only when previous is absent, so the stepper always has one
                // and it is always a control that exists.
                modifier = if (previous == null) {
                    Modifier.focusRequester(stepperFocus)
                } else {
                    Modifier
                }
            )
        }
    }
}

/**
 * The space above one lesson, holding Pip when this is the lesson being recommended.
 *
 * Pip travels in the lesson's own column rather than in a band of its own. A separate band divided
 * the whole stage into equal shares, which stopped agreeing with the card positions the moment the
 * cards took a fixed width and began to scroll, and left Pip standing over a gap.
 *
 * The perch is always occupied so every card in the row starts at the same height. It is
 * deliberately shorter than Pip: Pip is drawn at its full minimum size with `requiredSize` and
 * rises out of the perch into empty stage, so pointing costs the cards half its height rather than
 * all of it.
 *
 * Nothing is drawn when no lesson is recommended, because Pip standing over nothing in particular
 * is a cue that means nothing.
 */
@Composable
private fun PipPerch(present: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HelloBeLayout.pipMinSize * PIP_BAND_FRACTION),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (present) {
            PipGuide(
                pose = PipPose.GREETING,
                contentDescription = stringResource(R.string.path_pip_recommended),
                modifier = Modifier.requiredSize(HelloBeLayout.pipMinSize)
            )
        }
    }
}

@Composable
private fun ColumnScope.LessonRow(
    unit: UnitPageState,
    recommendedId: String?,
    onAction: (LearningPathAction) -> Unit,
    recommendedFocus: FocusRequester,
    onLessonFocused: () -> Unit
) {
    // Equal shares of the stage, not a scrolling row of fixed-width cards. A scroller was tried
    // and reverted: it made the cards larger but put the fifth lesson off stage, and a child who
    // cannot read has no way to discover a lesson that is not on screen. The draft draws five
    // columns because the page is the unit, and the unit is meant to be seen at once.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .helloBeFocusGroup()
            .onFocusChanged { if (it.hasFocus) onLessonFocused() },
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
    ) {
        unit.lessons.forEach { node ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PipPerch(present = node.id == recommendedId)

                LessonNode(
                    node = node,
                    recommended = node.id == recommendedId,
                    onAction = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(
                            if (node.id == recommendedId) {
                                Modifier.focusRequester(recommendedFocus)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}

/**
 * One lesson on the path.
 *
 * Built on [StoryCard] rather than as a new shared component. A lesson node needs a title, a
 * status line, selection, availability, a spoken state and a mark, which is every axis the story
 * card already has, so this stays inside the feature instead of widening `:ui:tv` again.
 *
 * The completed mark goes in the card's illustration slot rather than over the surface, so it
 * scales with focus instead of drifting off the card that grew underneath it.
 */
@Composable
private fun LessonNode(
    node: LessonNodeState,
    recommended: Boolean,
    onAction: (LearningPathAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val availability = lessonAvailability(node)

    StoryCard(
        title = node.title,
        onClick = { onAction(LearningPathAction.LessonChosen(node.id)) },
        modifier = modifier,
        selected = recommended,
        availability = availability,
        supportingText = statusLabel(node, recommended),
        stateDescription = if (availability == HelloBeAvailability.UNAVAILABLE) {
            stringResource(R.string.home_continue_unavailable)
        } else {
            null
        },
        centerContent = true,
        // Two lines whether the title needs them or not, so every card in the row puts its title,
        // and the word under it, at the same height. A wider card lets some titles fit on one line
        // and that alone was enough to make the row step up and down.
        titleMinLines = LESSON_TITLE_LINES,
        illustration = lessonMark(node)
    )
}

/**
 * The card's mark, and an empty space of the same height where there is no mark to draw.
 *
 * The slot is always occupied. Omitting it for a lesson with nothing to report made those cards
 * shorter than their neighbours, and a centred card that is shorter sits its title higher, so the
 * titles and the words under them stepped up and down across the row. Reserving the height costs
 * a little space on three cards in five and buys one baseline for all of them.
 */
@Composable
private fun lessonMark(node: LessonNodeState): (@Composable () -> Unit) {
    val completed = node.progress == LessonProgress.COMPLETED
    val review = node.kind == LessonKind.REVIEW
    val style = HelloBeTheme.typography.titleMedium
    val markHeight = with(LocalDensity.current) { style.lineHeight.toDp() }

    val text = when {
        completed -> stringResource(R.string.path_complete_mark)
        review -> stringResource(R.string.path_review_mark)
        else -> null
    }
    val color = if (completed) {
        HelloBeTheme.colors.successContent
    } else {
        HelloBeTheme.colors.textSecondary
    }

    return {
        Box(
            modifier = Modifier.height(markHeight),
            contentAlignment = Alignment.Center
        ) {
            if (text != null) {
                Text(text = text, style = style, color = color)
            }
        }
    }
}

@Composable
private fun statusLabel(node: LessonNodeState, recommended: Boolean): String = when {
    node.progress == LessonProgress.COMPLETED -> stringResource(R.string.path_lesson_finished)
    node.progress == LessonProgress.FUTURE -> stringResource(R.string.path_lesson_later)
    recommended -> stringResource(R.string.path_lesson_continue)
    else -> stringResource(R.string.path_lesson_start)
}

/**
 * What a child sees when the unit will not load.
 *
 * Offers the way home rather than an empty page. Leaving is the host's job, so this only reports
 * the request.
 */
@Composable
private fun UnitRecovery(onAction: (LearningPathAction) -> Unit, recoveryFocus: FocusRequester) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = HelloBeTheme.spacing.cardGap,
            alignment = Alignment.CenterVertically
        )
    ) {
        PipGuide(
            pose = PipPose.GREETING,
            contentDescription = stringResource(R.string.lesson_pip_waiting),
            modifier = Modifier.size(HelloBeLayout.pipMinSize)
        )
        Text(
            text = stringResource(R.string.path_recovery_title),
            style = HelloBeTheme.typography.headlineMedium,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = stringResource(R.string.path_recovery_hint),
            style = HelloBeTheme.typography.bodyLarge,
            color = HelloBeTheme.colors.textSecondary
        )
        HelloBeAction(
            label = stringResource(R.string.path_recovery_action),
            onClick = { onAction(LearningPathAction.HomeRequested) },
            tone = HelloBeActionTone.PRIMARY,
            modifier = Modifier.focusRequester(recoveryFocus)
        )
    }
}

/**
 * How much vertical band the Pip row claims, as a share of Pip's own minimum size. Pip overflows
 * the rest upward into empty stage, so pointing costs the lesson cards half its height, not all.
 */
private const val PIP_BAND_FRACTION = 0.5f

/** Every lesson title is drawn two lines tall, so the row shares one baseline. */
private const val LESSON_TITLE_LINES = 2
