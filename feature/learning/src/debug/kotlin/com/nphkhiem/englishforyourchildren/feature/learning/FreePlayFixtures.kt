package com.nphkhiem.englishforyourchildren.feature.learning

/**
 * Free play states, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 */
object FreePlayFixtures {

    fun shelves(): FreePlayUiState = FreePlayUiState(
        profileName = PROFILE,
        profileAvatar = "M",
        shelves = listOf(
            shelf(BODY, "My Body", BODY_WORDS),
            shelf(FAMILY, "My Family", FAMILY_WORDS),
            shelf(HOME, "My Home", HOME_WORDS, lastPlayed = true)
        ),
        previousShelf = null,
        nextShelf = ShelfSummary(id = "my-day", name = "My Day"),
        openShelf = null,
        speakingObjectId = null,
        audioAvailable = true
    )

    /** A middle page, where shelves exist on both sides. */
    fun shelvesMidLibrary(): FreePlayUiState = shelves().copy(
        previousShelf = ShelfSummary(id = "my-garden", name = "My Garden")
    )

    /** The words inside the shelf that was opened. Eight fills the stage without scrolling. */
    fun openShelf(): FreePlayUiState = shelves().copy(
        openShelf = shelf(HOME, "My Home", HOME_WORDS, lastPlayed = true)
    )

    /** More words than the stage holds, which is where focus-driven scrolling starts. */
    fun openShelfThatScrolls(): FreePlayUiState = shelves().copy(
        openShelf = shelf(HOME, "My Home", HOME_WORDS + MORE_HOME_WORDS)
    )

    /** A word is being said, so the card a child pressed is visibly the active one. */
    fun speaking(): FreePlayUiState = openShelf().copy(speakingObjectId = "chair")

    /** Focus opens on the word played last rather than the first. */
    fun resumesOnLastWord(): FreePlayUiState = shelves().copy(
        openShelf = shelf(HOME, "My Home", HOME_WORDS, lastPlayedObjectId = "lamp")
    )

    /** No sound, so the words can be looked at but not heard. */
    fun noSound(): FreePlayUiState = openShelf().copy(audioAvailable = false)

    /** Before the first lesson there is nothing here, which is explained rather than hidden. */
    fun emptyLibrary(): FreePlayUiState = shelves().copy(
        shelves = emptyList(),
        previousShelf = null,
        nextShelf = null
    )

    /** Every approved free play state, in the order a reviewer would walk them. */
    fun reviewStates(): List<Pair<String, FreePlayUiState>> = listOf(
        "shelves" to shelves(),
        "shelves, middle of the library" to shelvesMidLibrary(),
        "one shelf open" to openShelf(),
        "a shelf that scrolls" to openShelfThatScrolls(),
        "saying a word" to speaking(),
        "resumes on the last word" to resumesOnLastWord(),
        "no sound" to noSound(),
        "empty library" to emptyLibrary()
    )

    private fun shelf(
        id: String,
        name: String,
        words: List<String>,
        lastPlayed: Boolean = false,
        lastPlayedObjectId: String? = null
    ): Shelf = Shelf(
        id = id,
        name = name,
        objects = words.map { LearnedObject(id = it, label = it) },
        lastPlayed = lastPlayed,
        lastPlayedObjectId = lastPlayedObjectId
    )

    const val PROFILE = "Minh"
    const val BODY = "my-body"
    const val FAMILY = "my-family"
    const val HOME = "my-home"
    const val HOME_NAME = "My Home"
    const val BODY_NAME = "My Body"
    const val NEXT_SHELF = "My Day"
    const val PREVIOUS_SHELF = "My Garden"
    val HOME_WORDS = listOf("chair", "bed", "door", "lamp", "cup", "spoon", "window", "rug")
    val MORE_HOME_WORDS = listOf("clock", "plate", "towel", "shelf")
    private val BODY_WORDS = listOf("eyes", "ears", "nose", "mouth", "hands")
    private val FAMILY_WORDS = listOf("mother", "father", "baby")
}
