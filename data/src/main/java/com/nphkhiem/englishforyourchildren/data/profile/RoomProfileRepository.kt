package com.nphkhiem.englishforyourchildren.data.profile

import com.nphkhiem.englishforyourchildren.domain.id.IdProvider
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.MAX_CHILD_PROFILES
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CreateProfile
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import com.nphkhiem.englishforyourchildren.domain.time.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The children this television knows, kept in Room.
 *
 * The id and the instant come from providers rather than from here, so a test can say which of each
 * and a caregiver gets a random id and the real clock. That seam is what P1-T4 built.
 */
class RoomProfileRepository @Inject constructor(
    private val dao: ChildProfileDao,
    private val idProvider: IdProvider<ProfileId>,
    private val timeProvider: TimeProvider
) : ProfileRepository {

    override fun observeProfiles(): Flow<DomainResult<List<ChildProfile>>> = dao.observeAll()
        .map<List<ChildProfileEntity>, DomainResult<List<ChildProfile>>> { rows ->
            DomainResult.Success(rows.map { it.toDomain() })
        }
        .catch {
            // A database that will not answer is not an empty database. Emitting an empty list
            // here would send a caregiver to create a profile on a television that cannot read
            // the ones they already made.
            emit(DomainResult.Failure(DomainError.PersistenceUnavailable))
        }

    override suspend fun create(command: CreateProfile): DomainResult<ChildProfile> = attempt {
        if (dao.count() >= MAX_CHILD_PROFILES) {
            return DomainResult.Failure(DomainError.ProfileLimitReached)
        }
        val profile = ChildProfile(
            id = idProvider.next(),
            nickname = command.nickname,
            ageBand = command.ageBand,
            avatarId = command.avatarId
        )
        dao.insert(profile.toEntity(createdAt = timeProvider.now().value))
        DomainResult.Success(profile)
    }

    override suspend fun update(profile: ChildProfile): DomainResult<ChildProfile> = attempt {
        val existing = dao.findById(profile.id.value)
            ?: return DomainResult.Failure(DomainError.ProfileNotFound)

        // The stored instant is kept. It orders the picker, and editing a nickname must not move a
        // child's face to the end of the row.
        dao.update(profile.toEntity(createdAt = existing.createdAt))
        DomainResult.Success(profile)
    }

    override suspend fun delete(id: ProfileId): DomainResult<Unit> = attempt {
        // Deleting a profile that is not there is not an error: the caller asked for it to be gone
        // and it is gone.
        dao.delete(id.value)
        DomainResult.Success(Unit)
    }

    /** Turns anything the database throws into the one failure a caller can act on. */
    private inline fun <T> attempt(block: () -> DomainResult<T>): DomainResult<T> =
        runCatching(block).getOrElse { DomainResult.Failure(DomainError.PersistenceUnavailable) }
}
