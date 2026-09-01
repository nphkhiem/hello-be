package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The one place that decides which activity a lesson is showing.
 *
 * Every renderer takes the same three things, so this is a `when` and nothing more. What matters is
 * that it has no `else`: adding a sixth family will not compile until somebody has decided what it
 * looks like, which is the failure this replaces. Until now the host drew listen-and-choose for all
 * five, so a child met the same shape whatever the lesson asked of them.
 */
@Composable
fun LessonActivity(
    state: LessonUiState,
    onAction: (LessonAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state.kind) {
        LessonActivityKind.LISTEN_AND_CHOOSE ->
            ListenAndChooseActivity(state = state, onAction = onAction, modifier = modifier)

        LessonActivityKind.PICTURE_MATCHING ->
            PictureMatchingActivity(state = state, onAction = onAction, modifier = modifier)

        LessonActivityKind.LETTER_AND_SOUND ->
            LetterAndSoundActivity(state = state, onAction = onAction, modifier = modifier)

        LessonActivityKind.SAY_WITH_PIP ->
            SayWithPipActivity(state = state, onAction = onAction, modifier = modifier)

        LessonActivityKind.REVIEW ->
            ReviewActivity(state = state, onAction = onAction, modifier = modifier)
    }
}
