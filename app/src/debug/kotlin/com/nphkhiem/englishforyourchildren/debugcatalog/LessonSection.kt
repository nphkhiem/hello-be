package com.nphkhiem.englishforyourchildren.debugcatalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.nphkhiem.englishforyourchildren.R
import com.nphkhiem.englishforyourchildren.feature.learning.LessonFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.LetterAndSoundActivity
import com.nphkhiem.englishforyourchildren.feature.learning.LetterAndSoundFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.ListenAndChooseActivity
import com.nphkhiem.englishforyourchildren.feature.learning.PictureMatchingActivity
import com.nphkhiem.englishforyourchildren.feature.learning.PictureMatchingFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.ReviewActivity
import com.nphkhiem.englishforyourchildren.feature.learning.ReviewFixtures
import com.nphkhiem.englishforyourchildren.feature.learning.SayWithPipActivity
import com.nphkhiem.englishforyourchildren.feature.learning.SayWithPipFixtures

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

/**
 * The same shell again, carrying the letter pair. Three families in one scroll: if any of them
 * stops looking like the others, this is where it shows.
 */
@Composable
internal fun LetterAndSoundSection() {
    val states = remember { LetterAndSoundFixtures.reviewStates() }

    LessonStateWalker(
        label = stringResource(R.string.theme_catalog_letter_label),
        states = states
    ) { state, onAction ->
        LetterAndSoundActivity(state = state, onAction = onAction)
    }
}

/**
 * The fourth family. Worth walking with the sound off: nothing on this screen may suggest the app
 * heard anything.
 */
@Composable
internal fun SayWithPipSection() {
    val states = remember { SayWithPipFixtures.reviewStates() }

    LessonStateWalker(
        label = stringResource(R.string.theme_catalog_say_label),
        states = states
    ) { state, onAction ->
        SayWithPipActivity(state = state, onAction = onAction)
    }
}

/**
 * The fifth and last family. Its whole requirement is a feeling, so it is the one section that
 * only means anything read against the four above it.
 */
@Composable
internal fun ReviewSection() {
    val states = remember { ReviewFixtures.reviewStates() }

    LessonStateWalker(
        label = stringResource(R.string.theme_catalog_review_label),
        states = states
    ) { state, onAction ->
        ReviewActivity(state = state, onAction = onAction)
    }
}
