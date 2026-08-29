package com.nphkhiem.englishforyourchildren.feature.learning

import com.google.common.truth.Truth.assertThat
import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability
import org.junit.jupiter.api.Test

class FreePlayRulesTest {

    @Test
    fun givenNoShelvesAndNothingOpen_whenTheLibraryIsRead_thenItIsEmpty() {
        assertThat(isLibraryEmpty(state(shelves = emptyList()))).isTrue()
    }

    @Test
    fun givenShelves_whenTheLibraryIsRead_thenItIsNotEmpty() {
        assertThat(isLibraryEmpty(state(shelves = listOf(shelf(HOME))))).isFalse()
    }

    @Test
    fun givenAnOpenShelf_whenTheLibraryIsRead_thenItIsNotEmptyEvenWithNoPeers() {
        // Reachable when a shelf is opened from a page that is then the only one.
        val open = shelf(HOME)

        assertThat(isLibraryEmpty(state(shelves = emptyList(), openShelf = open))).isFalse()
    }

    @Test
    fun givenSoundWorks_whenAWordIsConsidered_thenItCanBePressed() {
        assertThat(objectAvailability(audioAvailable = true))
            .isEqualTo(HelloBeAvailability.ENABLED)
    }

    @Test
    fun givenNoSound_whenAWordIsConsidered_thenItKeepsFocusToSayWhy() {
        // Not skipped: a child can still look at what they have learned.
        assertThat(objectAvailability(audioAvailable = false))
            .isEqualTo(HelloBeAvailability.UNAVAILABLE)
    }

    @Test
    fun givenAShelfWasPlayedLast_whenEntryFocusIsChosen_thenItOpensThere() {
        val shelves = listOf(shelf(BODY), shelf(HOME, lastPlayed = true), shelf(FAMILY))

        assertThat(focusedShelfId(shelves)).isEqualTo(HOME)
    }

    @Test
    fun givenNoShelfWasPlayedLast_whenEntryFocusIsChosen_thenItOpensOnTheFirst() {
        val shelves = listOf(shelf(BODY), shelf(HOME))

        assertThat(focusedShelfId(shelves)).isEqualTo(BODY)
    }

    @Test
    fun givenNoShelvesAtAll_whenEntryFocusIsChosen_thenThereIsNone() {
        assertThat(focusedShelfId(emptyList())).isNull()
    }

    @Test
    fun givenARememberedWordStillOnTheShelf_whenEntryFocusIsChosen_thenItOpensThere() {
        val open = shelf(HOME, objects = listOf(CHAIR, LAMP), lastPlayedObjectId = LAMP)

        assertThat(focusedObjectId(open)).isEqualTo(LAMP)
    }

    @Test
    fun givenARememberedWordThatHasGone_whenEntryFocusIsChosen_thenItFallsToTheFirst() {
        // The brief asks for the most recently played valid item. Aiming at a word that is no
        // longer here would strand focus on nothing.
        val open = shelf(HOME, objects = listOf(CHAIR, LAMP), lastPlayedObjectId = "rug")

        assertThat(focusedObjectId(open)).isEqualTo(CHAIR)
    }

    @Test
    fun givenNothingRemembered_whenEntryFocusIsChosen_thenItOpensOnTheFirstWord() {
        val open = shelf(HOME, objects = listOf(CHAIR, LAMP))

        assertThat(focusedObjectId(open)).isEqualTo(CHAIR)
    }

    @Test
    fun givenAnEmptyShelf_whenEntryFocusIsChosen_thenThereIsNoWord() {
        assertThat(focusedObjectId(shelf(HOME, objects = emptyList()))).isNull()
    }

    @Test
    fun givenAnEmptyLibrary_whenTheScreenOpens_thenFocusGoesToTheWayOut() {
        assertThat(freePlayFocusTarget(state(shelves = emptyList())))
            .isEqualTo(FreePlayFocusTarget.EMPTY_LIBRARY)
    }

    @Test
    fun givenAnOpenShelf_whenTheScreenOpens_thenFocusGoesToAWord() {
        val open = shelf(HOME)

        assertThat(freePlayFocusTarget(state(shelves = listOf(open), openShelf = open)))
            .isEqualTo(FreePlayFocusTarget.OPEN_OBJECT)
    }

    @Test
    fun givenShelvesAndNothingOpen_whenTheScreenOpens_thenFocusGoesToAShelf() {
        assertThat(freePlayFocusTarget(state(shelves = listOf(shelf(HOME)))))
            .isEqualTo(FreePlayFocusTarget.OPEN_SHELF)
    }

    private fun shelf(
        id: String,
        objects: List<String> = listOf(CHAIR),
        lastPlayed: Boolean = false,
        lastPlayedObjectId: String? = null
    ): Shelf = Shelf(
        id = id,
        name = id,
        objects = objects.map { LearnedObject(id = it, label = it) },
        lastPlayed = lastPlayed,
        lastPlayedObjectId = lastPlayedObjectId
    )

    private fun state(shelves: List<Shelf>, openShelf: Shelf? = null): FreePlayUiState =
        FreePlayUiState(
            profileName = "Minh",
            profileAvatar = "M",
            shelves = shelves,
            previousShelf = null,
            nextShelf = null,
            openShelf = openShelf,
            speakingObjectId = null,
            audioAvailable = true
        )

    private companion object {
        const val HOME = "my-home"
        const val BODY = "my-body"
        const val FAMILY = "my-family"
        const val CHAIR = "chair"
        const val LAMP = "lamp"
    }
}
