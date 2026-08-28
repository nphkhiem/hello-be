package com.nphkhiem.englishforyourchildren.feature.profiles

/**
 * Picker states, shared by the debug catalog and the instrumented tests.
 *
 * Debug sources only, because the execution contract forbids production placeholder data.
 */
object ProfilePickerFixtures {

    fun twoProfiles(): ProfilePickerUiState = ProfilePickerUiState(
        profiles = listOf(
            ChildProfile(id = MINH, nickname = "Minh", avatar = "M", summary = CONTINUING),
            ChildProfile(id = LAN, nickname = "Lan", avatar = "L", summary = "5 adventures")
        ),
        rememberedProfileId = MINH,
        loading = false
    )

    fun oneProfile(): ProfilePickerUiState = twoProfiles().let { base ->
        base.copy(profiles = base.profiles.take(1))
    }

    fun full(): ProfilePickerUiState = twoProfiles().let { base ->
        base.copy(
            profiles = base.profiles + listOf(
                ChildProfile(id = "an", nickname = "An", avatar = "A", summary = CONTINUING),
                ChildProfile(id = "bao", nickname = "Bao", avatar = "B", summary = "2 adventures")
            )
        )
    }

    fun empty(): ProfilePickerUiState =
        ProfilePickerUiState(profiles = emptyList(), rememberedProfileId = null, loading = false)

    fun loading(): ProfilePickerUiState = empty().copy(loading = true)

    /** The remembered child is remembered but cannot be opened. Focus must move on. */
    fun rememberedUnavailable(): ProfilePickerUiState = twoProfiles().let { base ->
        base.copy(
            profiles = base.profiles.map {
                if (it.id == MINH) it.copy(available = false) else it
            }
        )
    }

    /** A remembered id that no longer matches anyone. */
    fun staleRemembered(): ProfilePickerUiState =
        twoProfiles().copy(rememberedProfileId = "someone-else")

    /** Every approved picker state, in the order a reviewer would walk them. */
    fun reviewStates(): List<Pair<String, ProfilePickerUiState>> = listOf(
        "loading" to loading(),
        "no profiles" to empty(),
        "one profile" to oneProfile(),
        "two profiles, one remembered" to twoProfiles(),
        "four profiles, add unavailable" to full(),
        "remembered child unavailable" to rememberedUnavailable(),
        "remembered child no longer exists" to staleRemembered()
    )

    const val MINH = "minh"
    const val LAN = "lan"
    private const val CONTINUING = "Continue learning"
}
