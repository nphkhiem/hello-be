package com.nphkhiem.englishforyourchildren.feature.learning

import androidx.compose.runtime.Immutable

/** One word a child has already met in a lesson. Free play never contains anything else. */
@Immutable
data class LearnedObject(
    val id: String,
    val label: String,
    /** The picture for this word, or null where none has been drawn. A library of written words is
     * no use to a child who cannot read, which is the whole audience. */
    val image: String? = null
)

/**
 * One story shelf: a unit, or an approved semantic group, holding the words learned from it.
 *
 * [lastPlayedObjectId] may name an object that is no longer here, which is why the focus rule
 * validates it rather than trusting it. The design brief asks entry focus to go to the most
 * recently played *valid* item, and a shelf can lose an object between sessions.
 */
@Immutable
data class Shelf(
    val id: String,
    val name: String,
    val objects: List<LearnedObject>,
    val lastPlayed: Boolean = false,
    val lastPlayedObjectId: String? = null
)

/** Enough of a shelf just off the edge of the view to name it on the control that reaches it. */
@Immutable
data class ShelfSummary(val id: String, val name: String)

/**
 * Everything free play needs to draw itself.
 *
 * [openShelf] is null on the shelves view and is the shelf itself on the objects view. A nullable
 * value rather than an id plus a lookup, so an objects view with no shelf cannot be represented.
 * One destination with two levels, per `INFORMATION_ARCHITECTURE.md`, rather than two screens.
 *
 * [shelves] is one viewport's worth, at most three. Paging is the host's: this screen renders the
 * page it is handed and names the shelves either side of it.
 *
 * [speakingObjectId] is the word currently being said. It exists so that pressing a word does
 * something a child can see, not only something they hear, per ADR 0004.
 */
@Immutable
data class FreePlayUiState(
    val profileName: String,
    val profileAvatar: String,
    val shelves: List<Shelf>,
    val previousShelf: ShelfSummary?,
    val nextShelf: ShelfSummary?,
    val openShelf: Shelf?,
    val speakingObjectId: String?,
    val audioAvailable: Boolean
)

/** What free play reports upward. */
sealed interface FreePlayAction {
    data class ShelfChosen(val shelfId: String) : FreePlayAction

    /** A word was pressed, which asks for its pronunciation and nothing else. */
    data class ObjectChosen(val objectId: String) : FreePlayAction

    data object PreviousShelvesRequested : FreePlayAction

    data object NextShelvesRequested : FreePlayAction

    /** Back out of a shelf, to the shelves. Never leaves free play. */
    data object ShelvesRequested : FreePlayAction

    /** The way out when there is nothing here yet. */
    data object HomeRequested : FreePlayAction

    data object SwitchProfileRequested : FreePlayAction
}
