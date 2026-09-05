package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAction
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeActionTone
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusFrame
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.component.LearningObjectCard
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipGuide
import com.nphkhiem.englishforyourchildren.ui.tv.component.PipPose
import com.nphkhiem.englishforyourchildren.ui.tv.component.StorybookScaffold
import com.nphkhiem.englishforyourchildren.ui.tv.component.rememberHelloBeFocusRestorer
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * The page a child lands on when a lesson is finished.
 *
 * A completed storybook page holding the words they just met, one calm phrase, Pip, and a single
 * Done. There is no score, no streak, no count of anything except the words themselves, and no
 * countdown: the reveal is a fact the host sets, and Done is live from the first frame.
 *
 * Back completes the return rather than popping into the finished lesson, per the information
 * architecture, so it emits the same action Done does.
 */
@Composable
fun LessonCelebrationScreen(
    state: CelebrationUiState,
    onAction: (CelebrationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val doneFocus = remember { FocusRequester() }
    val promptRestorer = rememberHelloBeFocusRestorer()
    val activity = state.playTogether

    // Back is whatever the safe way out of what is on screen happens to be. While the activity is
    // being offered that is declining it, so Back never commits a caregiver to anything and a
    // second overlay cannot stack on the first. With nothing offered it completes the return,
    // which the information architecture asks for, and without which Back would pop back into the
    // lesson just finished.
    BackHandler {
        onAction(
            if (activity != null) {
                CelebrationAction.MaybeLaterRequested
            } else {
                CelebrationAction.DoneRequested
            }
        )
    }

    StorybookScaffold(
        modifier = modifier,
        entryFocus = doneFocus,
        scenery = {
            Box(modifier = Modifier.fillMaxSize().background(HelloBeTheme.colors.scenery))
        },
        // The overlay slot rather than the page, so the scrim covers the stage and the prompt is
        // bounded by the screen. An authored instruction in two languages does not fit a slot.
        overlay = {
            if (activity != null) {
                PlayTogetherPrompt(
                    activity = activity,
                    focusRestorer = promptRestorer,
                    onAction = onAction
                )
            }
        }
    ) {
        StoryPage(
            state = state,
            onAction = onAction,
            doneFocus = doneFocus,
            promptRestorer = promptRestorer
        )
    }
}

@Composable
private fun StoryPage(
    state: CelebrationUiState,
    onAction: (CelebrationAction) -> Unit,
    doneFocus: FocusRequester,
    promptRestorer: HelloBeFocusRestorer
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = HelloBeShapes.storyPage,
        // Raised rather than primary, because the learned words are primary. Page and words on
        // the same surface left the words as faint outlines on their own page, which the draft
        // avoids by tinting the words; the shared card owns its container, so the page moves.
        colors = SurfaceDefaults.colors(
            containerColor = HelloBeTheme.colors.surfaceRaised,
            contentColor = HelloBeTheme.colors.textPrimary
        ),
        border = HelloBeFocusFrame.resting(HelloBeShapes.storyPage)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(HelloBeTheme.spacing.sectionGap)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = HelloBeTheme.spacing.cardGap,
                    alignment = Alignment.CenterVertically
                )
            ) {
                Text(
                    text = stringResource(
                        R.string.celebration_headline,
                        state.words.size,
                        state.unitWord
                    ),
                    style = HelloBeTheme.typography.headlineLarge,
                    color = HelloBeTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    // Swapped rather than annotated. A stored phrase with a pending note under it
                    // is a page that contradicts itself, which is what HB-D12 shipped once.
                    text = if (state.saveConfirmed) {
                        stringResource(R.string.celebration_saved)
                    } else {
                        stringResource(R.string.celebration_saving)
                    },
                    style = HelloBeTheme.typography.bodyLarge,
                    color = HelloBeTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                LearnedWordRow(state = state)
            }

            PipGuide(
                pose = PipPose.CELEBRATING,
                contentDescription = stringResource(R.string.celebration_pip),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(HelloBeLayout.pipMinSize)
            )

            HelloBeAction(
                label = stringResource(R.string.celebration_done),
                onClick = { onAction(CelebrationAction.DoneRequested) },
                tone = HelloBeActionTone.PRIMARY,
                // Done is both where entry focus lands and the prompt's return target. Being the
                // only focusable control on the page, focus would fall here anyway today, so the
                // return target is wiring for correctness rather than something observable.
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .focusRequester(doneFocus)
                    .focusRequester(promptRestorer.returnTarget)
            )
        }
    }
}

/**
 * The words themselves, three to five of them in one row of equal widths.
 *
 * They are [LearningObjectCard]s because that is exactly what they are: things shown and named,
 * never chosen. That component has no click and no availability, so nothing here can later be
 * wired into something a child presses on a page whose only action is Done.
 */
@Composable
private fun LearnedWordRow(state: CelebrationUiState) {
    val motion = HelloBeTheme.motion
    val visible = wordsVisible(revealed = state.revealed, reduceMotion = motion.reduceMotion)

    // Composed rather than faded in place. A row held at zero alpha is still on the page as far as
    // a screen reader and a test are concerned, which would make "the words are not there yet"
    // a claim about pixels that nothing could check. AnimatedVisibility keeps the fade and makes
    // the absence real.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = motion.pageTurnTransitionMillis)),
        exit = fadeOut(animationSpec = tween(durationMillis = motion.pageTurnTransitionMillis))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap)
        ) {
            state.words.forEach { word ->
                // Width from the row, height from the content. Filling the height takes the
                // maximum rather than the row's own, which on a wrap-content row is the page.
                LearningObjectCard(
                    label = word.label,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
