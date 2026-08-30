package com.nphkhiem.englishforyourchildren.navigation

import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * What the entry resolver needs, taken from real storage.
 *
 * This is the moment the app stops saying storage cannot be read. It replaces
 * [UnavailableProfileGateway], whose whole job was to be honest about a build with no data layer.
 *
 * It reads once. The entry destination is decided at launch and does not change underneath a child
 * afterwards; a profile created later moves the app on through navigation rather than by rewriting
 * where it started.
 */
class RepositoryProfileGateway @Inject constructor(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository
) : ProfileGateway {

    override suspend fun snapshot(): ProfileSnapshot {
        val stored = profiles.observeProfiles().first()
        if (stored is DomainResult.Failure) {
            // Not an empty television. A caregiver whose children cannot be read must be told,
            // rather than invited to create one on top of them.
            return ProfileSnapshot(
                storageReadable = false,
                validProfileIds = emptyList(),
                rememberedProfileId = null
            )
        }

        val remembered = (settings.observeSettings().first() as? DomainResult.Success)
            ?.value
            ?.selectedProfileId

        return ProfileSnapshot(
            storageReadable = true,
            validProfileIds = (stored as DomainResult.Success).value.map { it.id },
            rememberedProfileId = remembered
        )
    }
}
