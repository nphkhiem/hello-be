package com.nphkhiem.englishforyourchildren.feature.learning

import com.nphkhiem.englishforyourchildren.ui.tv.component.HelloBeAvailability

/** Where entry focus goes when free play opens. */
internal enum class FreePlayFocusTarget {
    OPEN_OBJECT,
    OPEN_SHELF,
    EMPTY_LIBRARY
}

/** True when the child has not met any words yet, which is a state and not a failure. */
internal fun isLibraryEmpty(state: FreePlayUiState): Boolean =
    state.openShelf == null && state.shelves.isEmpty()

/**
 * Whether a word can be pressed.
 *
 * Without sound there is nothing a press could do, so the word stays focusable and says why
 * rather than being skipped. A child can still look at what they have learned, which is most of
 * why the shelf exists.
 */
internal fun objectAvailability(audioAvailable: Boolean): HelloBeAvailability =
    if (audioAvailable) HelloBeAvailability.ENABLED else HelloBeAvailability.UNAVAILABLE

/**
 * The shelf entry focus opens on: the one last played, else the first.
 *
 * Null only when there are no shelves, which is the empty library.
 */
internal fun focusedShelfId(shelves: List<Shelf>): String? =
    (shelves.firstOrNull { it.lastPlayed } ?: shelves.firstOrNull())?.id

/**
 * The word entry focus opens on inside a shelf.
 *
 * The remembered word is checked against what is actually here before it is used. The design
 * brief asks for the most recently played *valid* item, and a shelf can lose an object between
 * sessions; aiming focus at a word that is gone would strand it.
 */
internal fun focusedObjectId(shelf: Shelf): String? {
    val remembered = shelf.lastPlayedObjectId?.takeIf { id -> shelf.objects.any { it.id == id } }
    return remembered ?: shelf.objects.firstOrNull()?.id
}

internal fun freePlayFocusTarget(state: FreePlayUiState): FreePlayFocusTarget = when {
    isLibraryEmpty(state) -> FreePlayFocusTarget.EMPTY_LIBRARY
    state.openShelf != null -> FreePlayFocusTarget.OPEN_OBJECT
    else -> FreePlayFocusTarget.OPEN_SHELF
}
