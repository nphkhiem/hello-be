package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
@HiltViewModel
class ProfileManagementViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        ProfileManagementState(profiles = emptyList(), selectedId = null, persistenceFailed = false)
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

                _state.value = ProfileManagementState(
                    profiles = people.value,
                    selectedId = (preferences as? DomainResult.Success)?.value?.selectedProfileId,
                    persistenceFailed = preferences !is DomainResult.Success
                )
            }
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
