package com.nphkhiem.englishforyourchildren.feature.learning

/**
 * Celebration states, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 */
object CelebrationFixtures {

    /** The draft's own state: four words, revealed, saved. */
    fun settled(): CelebrationUiState = CelebrationUiState(
        unitWord = UNIT_WORD,
        words = listOf(
            LearnedWord(id = "chair", label = CHAIR),
            LearnedWord(id = "bed", label = BED),
            LearnedWord(id = "door", label = DOOR),
            LearnedWord(id = "lamp", label = LAMP)
        ),
        revealed = true,
        saveConfirmed = true
    )

    /** The first frame, before the reveal window has closed. */
    fun revealing(): CelebrationUiState = settled().copy(revealed = false)

    /** Room has not confirmed the checkpoint, so nothing may claim it is stored. */
    fun pendingSave(): CelebrationUiState = settled().copy(saveConfirmed = false)

    /** The brief's floor. */
    fun threeWords(): CelebrationUiState = settled().copy(words = settled().words.take(3))

    /** The brief's ceiling, and what the row is built for. */
    fun fiveWords(): CelebrationUiState = settled().copy(
        words = settled().words + LearnedWord(id = "rug", label = RUG)
    )

    /** Every approved celebration state, in the order a reviewer would walk them. */
    fun reviewStates(): List<Pair<String, CelebrationUiState>> = listOf(
        "revealing" to revealing(),
        "settled" to settled(),
        "progress pending" to pendingSave(),
        "three words" to threeWords(),
        "five words" to fiveWords()
    )

    const val UNIT_WORD = "home"
    const val CHAIR = "chair"
    const val BED = "bed"
    const val DOOR = "door"
    const val LAMP = "lamp"
    const val RUG = "rug"
}
