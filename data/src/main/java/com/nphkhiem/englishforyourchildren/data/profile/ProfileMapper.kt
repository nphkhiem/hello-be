package com.nphkhiem.englishforyourchildren.data.profile

import com.nphkhiem.englishforyourchildren.domain.model.AgeBand
import com.nphkhiem.englishforyourchildren.domain.model.AvatarId
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId

/**
 * The border between a row and a child.
 *
 * Entities never leave `:data` and the domain never learns that a database exists, so everything
 * that crosses does it here.
 *
 * Mapping inwards is where a bad row stops. A blank id or an age band that is not one throws,
 * because the domain's own invariants say those values cannot exist, and a row that cannot become a
 * valid child is corrupt. Corruption reaching the caregiver recovery is the intended outcome; a
 * profile with no name appearing on the picker is not.
 */
fun ChildProfileEntity.toDomain(): ChildProfile = ChildProfile(
    id = ProfileId(id),
    nickname = nickname,
    ageBand = ageBand.toAgeBand(),
    avatarId = AvatarId(avatarId)
)

/**
 * Mapping outwards needs a time from the caller, because the model does not carry one and the row
 * needs it to keep its place in the picker's order.
 */
fun ChildProfile.toEntity(createdAt: Long): ChildProfileEntity = ChildProfileEntity(
    id = id.value,
    nickname = nickname,
    ageBand = ageBand.name,
    avatarId = avatarId.value,
    createdAt = createdAt
)

private fun String.toAgeBand(): AgeBand = AgeBand.entries.firstOrNull { it.name == this }
    ?: throw IllegalArgumentException("Stored age band '$this' is not one this app knows")
