package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The children on this television, and which one it is currently on.
 *
 * [ProfileManagementUiState] carries a finished detail line per profile, which is a piece of
 * writing, so this holds the profiles themselves and the host writes the lines. The same division
 * the gate and the overview make.
 */
data class ProfileManagementState(
    val profiles: List<ChildProfile>,
    val selectedId: ProfileId?,
    /**
     * How many adventures each child has finished, for the detail line beside their name.
     *
     * A child with none is absent rather than present with a zero. The overview refuses to draw a
     * row of zeroes for a child who has never practised, for the reason that a zero reads as a poor
     * result, and a subtitle is not a better place to say it.
     */
    val adventuresFinished: Map<ProfileId, Int>,
    val persistenceFailed: Boolean
)

/**
 * Adding, choosing between and acting on the children this television knows.
 *
 * The selection is stored rather than held here, because it outlives the screen: it is what the
 * television opens on. That also means it can name a child who has since been deleted, which is
 * why `selectedProfile` resolves it against the list rather than trusting it. A caregiver acting
 * on a stale selection would be pressing delete against somebody else's profile.
 *
 * Nothing destructive happens here. Reset and delete are asked for on this screen and confirmed on
 * another, which is the second confirmation the information architecture requires.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileManagementViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        ProfileManagementState(
            profiles = emptyList(),
            selectedId = null,
            adventuresFinished = emptyMap(),
            persistenceFailed = false
        )
    )

    val state: StateFlow<ProfileManagementState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(profiles.observeProfiles(), settings.observeSettings()) { people, preferences ->
                people to preferences
            }.collect { (people, preferences) ->
                if (people !is DomainResult.Success) {
                    // An empty list would read as a television with no children on it, which is a
                    // very different thing from one that cannot open its own storage.
                    _state.value = _state.value.copy(persistenceFailed = true)
                    return@collect
                }

                _state.value = _state.value.copy(
                    profiles = people.value,
                    selectedId = (preferences as? DomainResult.Success)?.value?.selectedProfileId,
                    persistenceFailed = preferences !is DomainResult.Success
                )
            }
        }

        viewModelScope.launch {
            profileIds()
                .flatMapLatest { adventuresFinished(it) }
                .collect { counts ->
                    _state.value = _state.value.copy(adventuresFinished = counts)
                }
        }
    }

    /**
     * Who this television knows, and nothing about them.
     *
     * Distinct, so that renaming a child does not tear down and rebuild four progress subscriptions
     * for a fact none of them depend on.
     */
    private fun profileIds(): Flow<List<ProfileId>> = profiles.observeProfiles()
        .map { people -> (people as? DomainResult.Success)?.value?.map { it.id }.orEmpty() }
        .distinctUntilChanged()

    /**
     * One count per child, from one subscription per child.
     *
     * Four flows at most, which is the cap this television holds, and the reason a fan-out is
     * affordable here where it would not be on a screen that lists many. A child whose progress
     * will not read is left out rather than counted as none: the detail line is a nicety, and a
     * storage problem is already being reported by the header.
     */
    private fun adventuresFinished(ids: List<ProfileId>): Flow<Map<ProfileId, Int>> =
        if (ids.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                ids.map { id ->
                    progress.observeProfileProgress(id).map { stored ->
                        id to (stored as? DomainResult.Success)?.value?.lessonsCompleted?.size
                    }
                }
            ) { counted ->
                counted.mapNotNull { (id, count) -> count?.let { id to it } }.toMap()
            }
        }

    /** Which child this television is on. Stored, because it outlives the screen. */
    fun onSelect(profileId: ProfileId) {
        viewModelScope.launch {
            val written = settings.updateSelectedProfile(profileId)
            _state.value = _state.value.copy(persistenceFailed = written is DomainResult.Failure)
        }
    }
}
