package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeFocusFrame
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeLayout
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeShapes
import com.nphkhiem.englishforyourchildren.ui.tv.theme.HelloBeTheme

/**
 * What a caregiver reads about one child's practice.
 *
 * Everything here describes exposure and offers a next thing to do. Nothing ranks, scores, or
 * accumulates: there are at most three summaries and at most six recent words, both bounded by the
 * rules rather than by whoever fills the state, so this panel cannot grow into a dashboard or a
 * log. The stop condition names raw answer history, rankings, cloud analytics and infinite history,
 * and the honest way to hold those out is to make the screen incapable of drawing them.
 *
 * It carries no actions. The rail around it owns navigation, and the suggestion is a thing to do
 * with a real chair rather than a destination.
 */
@Composable
fun CaregiverOverviewScreen(state: CaregiverOverviewUiState, modifier: Modifier = Modifier) {
    // Deliberately not scrollable. Nothing on this panel is focusable, because nothing on it is
    // pressed, and a scrolling container that no remote can move puts its lower half out of reach
    // rather than out of sight. Everything a caregiver needs fits the stage at caregiver density,
    // and a test holds it there.
    // The design brief asks caregiver content to reflow or reduce under a large font scale, and
    // never to shrink text below its role size. Measured, this panel holds everything to a scale
    // of about 1.4 and then runs out of stage: the instruction under the suggestion was the first
    // thing to lose its height, which is the failure the brief names. Above that the recent words
    // stand down. They are a reminder rather than the point of the screen, and every one of them
    // is still in free play; the summaries and the one thing to try are what a caregiver came for.
    val reduced = LocalDensity.current.fontScale >= REDUCE_CONTENT_ABOVE

    // Above that scale the panel also has to reflow, because reducing alone is not enough: the
    // heading and the three summaries fill the stage on their own, and whatever came last lost its
    // height. It becomes a scrolling column, and it becomes focusable at the same time. On every
    // other surface a scroll is safe because its rows take focus; nothing here does, so the
    // container itself has to, or a caregiver with large text could see the lower half and never
    // reach it. At ordinary scale it stays exactly as it was: no scroll, and nothing focusable.
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (reduced) {
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .focusable()
                } else {
                    Modifier
                }
            ),
        verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space4)
    ) {
        PanelHead(state = state)

        when (val progress = state.progress) {
            is OverviewProgress.Practiced ->
                SummaryRow(summaries = summariesShown(progress.summaries))

            OverviewProgress.NewProfile -> Explanation(
                title = caregiverText(R.string.overview_new_profile_title),
                hint = caregiverText(R.string.overview_new_profile_hint, state.profileName)
            )

            OverviewProgress.NothingRecent -> Explanation(
                title = caregiverText(R.string.overview_nothing_recent_title),
                hint = caregiverText(R.string.overview_nothing_recent_hint, state.profileName)
            )
        }

        // Side by side, as the draft draws them. Stacked they were a panel taller than the stage,
        // and on a surface where nothing takes focus the overflow would have been unreachable.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (reduced) Modifier else Modifier.weight(1f)),
            horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space4)
        ) {
            val progress = state.progress
            if (progress is OverviewProgress.Practiced && !reduced) {
                RecentWords(
                    words = recentWordsShown(progress.recentWords),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            state.suggestion?.let {
                Suggestion(
                    suggestion = it,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (reduced) Modifier else Modifier.fillMaxHeight())
                )
            }
        }
    }
}

@Composable
private fun PanelHead(state: CaregiverOverviewUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.cardGap),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space2)
        ) {
            Text(
                text = caregiverText(R.string.overview_title, state.profileName),
                style = HelloBeTheme.typography.headlineLarge,
                color = HelloBeTheme.colors.textPrimary
            )
            Text(
                text = caregiverText(R.string.overview_local_note),
                style = HelloBeTheme.typography.bodyMedium,
                color = HelloBeTheme.colors.textSecondary
            )
            if (state.pendingSave) {
                Text(
                    text = caregiverText(R.string.overview_pending_save),
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.warningContent
                )
            }
        }

        Text(
            text = state.period,
            style = HelloBeTheme.typography.labelSmall,
            color = HelloBeTheme.colors.textTertiary
        )
    }
}

@Composable
private fun SummaryRow(summaries: List<OverviewSummary>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space4)
    ) {
        summaries.forEach { summary ->
            Panel(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.label,
                    style = HelloBeTheme.typography.labelSmall,
                    color = HelloBeTheme.colors.textTertiary
                )
                Text(
                    text = summary.value,
                    style = HelloBeTheme.typography.headlineMedium,
                    color = HelloBeTheme.colors.textPrimary
                )
                // The line that keeps a number from reading as a mark. The draft writes "Not a
                // test score" under a count for exactly this reason.
                Text(
                    text = summary.note,
                    style = HelloBeTheme.typography.bodyMedium,
                    color = HelloBeTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun RecentWords(words: List<String>, modifier: Modifier = Modifier) {
    Panel(modifier = modifier) {
        Text(
            text = caregiverText(R.string.overview_recent),
            style = HelloBeTheme.typography.titleSmall,
            color = HelloBeTheme.colors.textPrimary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3),
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space3)
        ) {
            words.forEach { word ->
                Text(
                    text = word,
                    style = HelloBeTheme.typography.bodyMedium,
                    color = HelloBeTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun Suggestion(suggestion: CoPlaySuggestion, modifier: Modifier = Modifier) {
    Panel(modifier = modifier) {
        Text(
            text = caregiverText(R.string.overview_suggestion),
            style = HelloBeTheme.typography.titleSmall,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = suggestion.title,
            style = HelloBeTheme.typography.bodyLarge,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = suggestion.instruction,
            style = HelloBeTheme.typography.bodyMedium,
            color = HelloBeTheme.colors.textSecondary
        )
    }
}

/** What a caregiver reads instead of a panel when there is nothing to describe. */
@Composable
private fun Explanation(title: String, hint: String) {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = HelloBeTheme.typography.titleSmall,
            color = HelloBeTheme.colors.textPrimary
        )
        Text(
            text = hint,
            style = HelloBeTheme.typography.bodyMedium,
            color = HelloBeTheme.colors.textSecondary
        )
    }
}

/** The one card shape this panel uses. Nothing here is focusable, because nothing here is pressed. */
@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = HelloBeShapes.large,
        colors = SurfaceDefaults.colors(
            containerColor = HelloBeTheme.colors.surfacePrimary,
            contentColor = HelloBeTheme.colors.textPrimary
        ),
        border = HelloBeFocusFrame.resting(HelloBeShapes.large)
    ) {
        Column(
            // Not fillMaxHeight. Inside a surface with no height of its own that takes the
            // maximum rather than the row's, and the summary cards grew to the whole stage and
            // pushed everything under them off it.
            modifier = Modifier.padding(HelloBeTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(HelloBeTheme.spacing.space2)
        ) {
            content()
        }
    }
}

/**
 * The font scale above which the overview shows less at once.
 *
 * Measured rather than chosen, and re-measured when the caregiver area became bilingual. It used to
 * be 1.4: everything fitted to about there and the instruction under the suggestion was the first
 * thing to lose its height. Every label on this panel now carries two languages joined by a middle
 * dot, so it runs out of stage sooner and the same failure arrives at 1.3.
 *
 * Measured against the bilingual mode on purpose, because that is the default and the longest a
 * caregiver can make this screen. A television set to one language alone has room to spare.
 */
private const val REDUCE_CONTENT_ABOVE = 1.3f
