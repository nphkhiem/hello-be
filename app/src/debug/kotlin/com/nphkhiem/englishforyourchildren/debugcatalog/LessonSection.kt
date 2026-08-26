package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.feature.learning.LessonFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.ListenAndChooseActivity
import com.nphkhiem.englishforyourchildren.feature.learning.PictureMatchingActivity
import com.nphkhiem.englishforyourchildren.feature.learning.PictureMatchingFixtures

/** A real listen-and-choose lesson, in every state it can be in. */
@Composable
internal fun LessonSection() {
    val states = remember { LessonFixtures.reviewStates() }

    LessonStateWalker(
        label = stringResource(R.string.theme_catalog_lesson_label),
        states = states
    ) { state, onAction ->
        ListenAndChooseActivity(state = state, onAction = onAction)
    }
}

/**
 * The same lesson shell carrying picture matching, so the two families can be compared side by
 * side. If matching ever stops looking like the activity above it, that is the defect this section
 * exists to make obvious.
 */
@Composable
internal fun MatchingSection() {
    val states = remember { PictureMatchingFixtures.reviewStates() }

    LessonStateWalker(
        label = stringResource(R.string.theme_catalog_matching_label),
        states = states
    ) { state, onAction ->
        PictureMatchingActivity(state = state, onAction = onAction)
    }
}
