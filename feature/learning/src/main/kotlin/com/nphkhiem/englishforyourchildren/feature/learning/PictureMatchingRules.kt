package com.nphkhiem.englishforyourchildren.feature.learning

/** Destinations per row in the matching grid. Four or more pair off; fewer stay in one row. */
private const val GRID_COLUMNS = 2

/**
 * How the destination pictures are arranged.
 *
 * Two and three share a single row, which is the arrangement a child already learned in listen and
 * choose and which needs only Left and Right. Four pair off into two rows, because four cards do
 * not fit across the destination half of the board and because a three-card two-column grid would
 * leave a ragged row with an undefined Down press.
 *
 * The brief allows two to four. Anything larger still pairs off rather than failing, so a
 * malformed activity degrades into a grid instead of overflowing the stage.
 */
internal fun destinationRows(answers: List<AnswerOption>): List<List<AnswerOption>> = when {
    answers.isEmpty() -> emptyList()
    answers.size < 4 -> listOf(answers)
    else -> answers.chunked(GRID_COLUMNS)
}
