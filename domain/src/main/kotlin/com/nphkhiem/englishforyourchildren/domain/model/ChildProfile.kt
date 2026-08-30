package com.nphkhiem.englishforyourchildren.domain.model

/**
 * How old a child is, to the nearest year the curriculum cares about.
 *
 * A band rather than a birth date. The product needs to know roughly how much language a child has,
 * and knowing more than that would be knowing something about a real three-year-old that this app
 * has no reason to hold.
 */
enum class AgeBand {
    THREE,
    FOUR,
    FIVE
}

/**
 * A child this television knows.
 *
 * Everything a caregiver typed and nothing more: a nickname to recognise, a band to pitch the
 * lessons at, and a picture to find on the picker. There is no birth date, no photograph and no
 * device identifier, and there is nowhere on this model to put one, which is how the privacy rule
 * is kept rather than remembered.
 *
 * The nickname is stored exactly as it was given. Tidying it belongs where the text is captured,
 * once, not in every model that happens to hold a name.
 */
data class ChildProfile(
    val id: ProfileId,
    val nickname: String,
    val ageBand: AgeBand,
    val avatarId: AvatarId
) {
    init {
        require(nickname.isNotBlank()) { "A profile needs a nickname" }
    }
}
