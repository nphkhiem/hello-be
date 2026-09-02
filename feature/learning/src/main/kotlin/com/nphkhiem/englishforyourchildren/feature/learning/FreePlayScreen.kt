package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.nphkhiem.englishforyourchildren.ui.tv.component.PackagedPicture
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipGuide
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipPose
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.StoryHeader
import com.nphkhiem.englishforyourchildren.ui.tv.component.StorybookScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.component.helloBeFocusGroup
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberPackagedPicture
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The library of words a child has already met, and nothing else.
 *
 * One destination with two levels, per the information architecture: at most three story shelves
 * in a viewport, then a bounded grid of the words inside the shelf that was opened. There is no
 * search, no score, no lock and no endless row, because everything here is already learned and
 * there is nothing to unlock or fail.
 *
 * Back returns to the shelves when a shelf is open and is otherwise left alone, so the host's
 * return to child home still happens. That is the same division `LearningPathScreen` makes: this
 * screen intercepts Back only when it has somewhere of its own to go.
 */
@Composable
fun FreePlayScreen(
    state: FreePlayUiState,
    onAction: (FreePlayAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val entryFocus = remember { FocusRequester() }
    val openShelf = state.openShelf

    BackHandler(enabled = openShelf != null) { onAction(FreePlayAction.ShelvesRequested) }

    StorybookScaffold(
        modifier = modifier,
        entryFocus = entryFocus,
        header = {
            // On a shelf the shelf is what the page is about, so it takes the title and the
            // brand becomes the context above it. Titling it "Free play" over "My Home" read as
            // though My Home were the section and free play the page inside it.
            StoryHeader(
                modifier = Modifier.fillMaxWidth(),
                title = openShelf?.name ?: stringResource(R.string.free_play_brand),
                contextLabel = openShelf?.let { stringResource(R.string.free_play_brand) },
                action = {
                    HelloBeAction(
                        label = state.profileName,
                        onClick = { onAction(FreePlayAction.SwitchProfileRequested) },
                        tone = HelloBeActionTone.QUIET,
                        supportingText = state.profileAvatar
                    )
                }
            )
        }
    ) {
        when (freePlayFocusTarget(state)) {
            FreePlayFocusTarget.EMPTY_LIBRARY ->
                EmptyLibrary(onAction = onAction, entryFocus = entryFocus)

            FreePlayFocusTarget.OPEN_OBJECT ->
                ObjectGrid(
                    shelf = requireNotNull(openShelf),
                    state = state,
                    onAction = onAction,
                    entryFocus = entryFocus
                )

            FreePlayFocusTarget.OPEN_SHELF ->
                ShelfBoard(state = state, onAction = onAction, entryFocus = entryFocus)
        }
    }
}

@Composable
private fun ShelfBoard(
    state: FreePlayUiState,
    onAction: (FreePlayAction) -> Unit,
    entryFocus: FocusRequester
) {
    val focusedShelf = focusedShelfId(state.shelves)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
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
                    text = stringResource(R.string.free_play_kicker),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.textTertiary
                )
                Text(
                    text = stringResource(R.string.free_play_title),
                    style = HelloBeTheme.typography.headlineMedium,
                    color = HelloBeTheme.colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.free_play_subtitle, state.profileName),
                    style = HelloBeTheme.typography.bodyMedium,
                    color = HelloBeTheme.colors.textSecondary
                )
            }

            ShelfPager(state = state, onAction = onAction)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .helloBeFocusGroup(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
        ) {
            state.shelves.forEach { shelf ->
                VocabularyShelf(
                    shelf = shelf,
                    onAction = onAction,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (shelf.id == focusedShelf) {
                                Modifier.focusRequester(entryFocus)
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
 * The shelves either side of this viewport, named.
 *
 * The information architecture asks for horizontal pagination "with clear edge context", which is
 * the same requirement the learning path's unit stepper answers, so it is the same control: the
 * neighbour is named rather than hidden behind a bare chevron, and a direction that leads nowhere
 * is absent rather than dead.
 */
@Composable
private fun ShelfPager(state: FreePlayUiState, onAction: (FreePlayAction) -> Unit) {
    val previous = state.previousShelf
    val next = state.nextShelf

    Row(
        modifier = Modifier.helloBeFocusGroup(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)
    ) {
        if (previous != null) {
            // The arrow is part of the label rather than a line beneath it. Naming the shelf and
            // then naming it again under an arrow said everything twice.
            HelloBeAction(
                label = stringResource(R.string.free_play_previous_shelf, previous.name),
                onClick = { onAction(FreePlayAction.PreviousShelvesRequested) },
                tone = HelloBeActionTone.QUIET
            )
        }
        if (next != null) {
            HelloBeAction(
                label = stringResource(R.string.free_play_next_shelf, next.name),
                onClick = { onAction(FreePlayAction.NextShelvesRequested) },
                tone = HelloBeActionTone.QUIET
            )
        }
    }
}

/** One story shelf, with how many words are inside it and whether it was the last one played. */
@Composable
private fun VocabularyShelf(
    shelf: Shelf,
    onAction: (FreePlayAction) -> Unit,
    modifier: Modifier = Modifier
) {
    StoryCard(
        title = shelf.name,
        onClick = { onAction(FreePlayAction.ShelfChosen(shelf.id)) },
        modifier = modifier,
        supportingText = if (shelf.lastPlayed) {
            stringResource(R.string.free_play_shelf_last)
        } else {
            stringResource(R.string.free_play_shelf_hint)
        },
        centerContent = true,
        illustration = {
            // A count of words a child owns, never a score and never out of a total. The draft
            // puts it on the shelf because it is inventory, not achievement.
            Text(
                text = stringResource(R.string.free_play_word_count, shelf.objects.size),
                style = HelloBeTheme.typography.labelSmall,
                color = HelloBeTheme.colors.successContent
            )
        }
    )
}

/**
 * The words inside one shelf.
 *
 * Deliberately not a lazy grid. "Bounded" is the requirement, and a shelf holds a set of words a
 * child has actually met, so there is nothing to recycle and nothing to page in. Laying every word
 * out at once makes an endless feed impossible to build here by accident, and it lets entry focus
 * aim at a word that is definitely composed: a requester inside a lazy item is not attached when
 * the scaffold asks for focus, which left focus on nothing at all.
 *
 * Scrolling is focus-driven because that is all a scrollable column does on a television: moving
 * focus onto a word below the fold brings it into view. Nothing scrolls on its own.
 *
 * Four columns at `cardFourColumnSet` with the grid gutter is exactly the content width, and two
 * rows fit the stage, so a shelf larger than eight words scrolls rather than shrinking.
 */
@Composable
private fun ObjectGrid(
    shelf: Shelf,
    state: FreePlayUiState,
    onAction: (FreePlayAction) -> Unit,
    entryFocus: FocusRequester
) {
    val focusedObject = focusedObjectId(shelf)
    val availability = objectAvailability(state.audioAvailable)

    // The scroll lives on the inner column while the box around it stays the full stage, so a
    // shelf that fits is centred and one that does not takes the whole height and scrolls. Putting
    // the scroll on the outer box instead gives it an unbounded height, and then nothing can be
    // centred: eight words sat against the top of the stage with a third of it empty below.
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .helloBeFocusGroup(),
            verticalArrangement = Arrangement.spacedBy(HelloBeLayout.gutter)
        ) {
            shelf.objects.chunked(OBJECT_COLUMNS).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HelloBeLayout.gutter)
                ) {
                    row.forEach { learned ->
                        VocabularyObjectCard(
                            learned = learned,
                            speaking = learned.id == state.speakingObjectId,
                            availability = availability,
                            onAction = onAction,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (learned.id == focusedObject) {
                                        Modifier.focusRequester(entryFocus)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                    // A short last row keeps its cards the width of every other row's, rather
                    // than letting two leftover words stretch into half the stage each.
                    repeat(OBJECT_COLUMNS - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * One learned word, pressed to hear it again.
 *
 * [speaking] is carried on the card's selection, because on this screen the word being said is
 * exactly the word that is currently active. Pressing a word has to do something a child can see
 * and not only something they hear, per ADR 0004, and the card they just pressed is where they
 * are already looking.
 */
@Composable
private fun VocabularyObjectCard(
    learned: LearnedObject,
    speaking: Boolean,
    availability: HelloBeAvailability,
    onAction: (FreePlayAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val picture = rememberPackagedPicture(learned.image)

    StoryCard(
        title = learned.label,
        onClick = { onAction(FreePlayAction.ObjectChosen(learned.id)) },
        modifier = modifier,
        selected = speaking,
        availability = availability,
        centerContent = true,
        // The word keeps its place beside the picture here, unlike a lesson answer. Free play asks
        // nothing, so naming the thing gives nothing away, and a caregiver alongside can read it.
        illustration = picture?.let { { PackagedPicture(it) } },
        stateDescription = when {
            availability == HelloBeAvailability.UNAVAILABLE ->
                stringResource(R.string.free_play_no_sound)

            speaking -> stringResource(R.string.free_play_speaking, learned.label)

            else -> null
        }
    )
}

/** What a child sees before their first lesson. An explanation, not a failure. */
@Composable
private fun EmptyLibrary(onAction: (FreePlayAction) -> Unit, entryFocus: FocusRequester) {
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
            contentDescription = stringResource(R.string.free_play_pip),
            modifier = Modifier.size(HelloBeLayout.pipMinSize)
        )
        Text(
            text = stringResource(R.string.free_play_empty_title),
            style = HelloBeTheme.typography.headlineMedium,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = stringResource(R.string.free_play_empty_hint),
            style = HelloBeTheme.typography.bodyLarge,
            color = HelloBeTheme.colors.textSecondary
        )
        HelloBeAction(
            label = stringResource(R.string.free_play_empty_action),
            onClick = { onAction(FreePlayAction.HomeRequested) },
            tone = HelloBeActionTone.PRIMARY,
            modifier = Modifier.focusRequester(entryFocus)
        )
    }
}

/** Four at `cardFourColumnSet` plus the gutter is exactly the content width. */
private const val OBJECT_COLUMNS = 4
