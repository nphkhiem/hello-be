package com.nphkhiem.englishforyourchildren.feature.profiles

/**
 * Create-profile states, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 *
 * The draft arrives already named and pictured, which is the point of the screen: the only
 * required decision left is the age.
 */
object CreateProfileFixtures {

    fun ready(): CreateProfileUiState = CreateProfileUiState(
        draft = ProfileDraft(nickname = NICKNAME, avatarId = TIGER, age = null),
        ages = listOf(3, 4, 5),
        avatarChoices = listOf(TIGER, RABBIT, BEAR),
        choosingAvatar = false,
        capacityReached = false,
        saveFailed = false
    )

    fun ageChosen(): CreateProfileUiState = ready().let { base ->
        base.copy(draft = base.draft.copy(age = 3))
    }

    fun choosingAvatar(): CreateProfileUiState = ready().copy(choosingAvatar = true)

    fun capacityReached(): CreateProfileUiState = ageChosen().copy(capacityReached = true)

    /** The draft is kept, never cleared: losing choices because a write failed is worse. */
    fun saveFailed(): CreateProfileUiState = ageChosen().copy(saveFailed = true)

    fun noAvatarChoices(): CreateProfileUiState = ready().copy(avatarChoices = emptyList())

    /** Every approved create state, in the order a reviewer would walk them. */
    fun reviewStates(): List<Pair<String, CreateProfileUiState>> = listOf(
        "ready, no age yet" to ready(),
        "age chosen" to ageChosen(),
        "choosing a picture" to choosingAvatar(),
        "no pictures to choose" to noAvatarChoices(),
        "television already full" to capacityReached(),
        "that did not save" to saveFailed()
    )

    const val NICKNAME = "Bé 1"
    const val TIGER = "Tiger"
    const val RABBIT = "Rabbit"
    const val BEAR = "Bear"
}
