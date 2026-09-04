package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Whose caregiver area this is.
 *
 * The rail says "For Minh", so the frame around every caregiver section needs one fact: the name of
 * the child the caregiver came in from. Nothing else here is the shell's business, which is why
 * this holds a name rather than a profile.
 *
 * An empty name is what a caregiver who arrived without a child selected sees, and the rail already
 * handles that: entering from the profile picker rather than from a child's home is a real path.
 */
@HiltViewModel
class CaregiverShellViewModel @Inject constructor(private val profiles: ProfileRepository) :
    ViewModel() {
    private val _profileName = MutableStateFlow("")

    val profileName: StateFlow<String> = _profileName.asStateFlow()

    fun start(profileId: ProfileId?) {
        if (profileId == null) {
            _profileName.value = ""
            return
        }

        viewModelScope.launch {
            profiles.observeProfiles().collect { read ->
                if (read is DomainResult.Success) {
                    _profileName.value =
                        read.value.firstOrNull { it.id == profileId }?.nickname.orEmpty()
                }
            }
        }
    }
}
