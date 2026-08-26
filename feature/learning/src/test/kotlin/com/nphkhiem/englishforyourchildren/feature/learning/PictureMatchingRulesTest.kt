package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PictureMatchingRulesTest {

    @Test
    fun givenTwoDestinations_whenRowsAreRead_thenTheyShareOneRow() {
        // Two and three stay in a row because that is the arrangement a child already met in
        // listen and choose, and it needs only Left and Right.
        assertThat(destinationRows(destinations(2))).hasSize(1)
        assertThat(destinationRows(destinations(2)).single()).hasSize(2)
    }

    @Test
    fun givenThreeDestinations_whenRowsAreRead_thenTheyStillShareOneRow() {
        // A three-card two-column grid would leave a ragged row and an undefined Down press from
        // the second column, which is the opposite of the predictable grid the draft calls for.
        assertThat(destinationRows(destinations(3))).hasSize(1)
        assertThat(destinationRows(destinations(3)).single()).hasSize(3)
    }

    @Test
    fun givenFourDestinations_whenRowsAreRead_thenTheyFormTwoRowsOfTwo() {
        // Four in a row does not fit: the destination area is roughly 467dp of the reference
        // canvas and four cards at the four-column width need 784dp.
        val rows = destinationRows(destinations(4))

        assertThat(rows).hasSize(2)
        assertThat(rows.map { it.size }).containsExactly(2, 2)
        assertThat(rows.flatten().map { it.id }).containsExactly("0", "1", "2", "3").inOrder()
    }

    @Test
    fun givenNoDestinations_whenRowsAreRead_thenThereAreNoRows() {
        // A malformed activity must not draw an empty row, per the same rule HB-D04 follows for
        // an empty answer list.
        assertThat(destinationRows(emptyList())).isEmpty()
    }

    private fun destinations(count: Int): List<AnswerOption> =
        List(count) { index -> AnswerOption(id = "$index", label = "picture $index") }
}
