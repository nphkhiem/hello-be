package com.nphkhiem.englishforyourchildren.feature.learning

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
 * Whose home this is.
 *
 * Only the child's own name and picture so far. What they can do next needs progress, and progress
 * has no table until P2-T6, so the primary action stays [HomePrimary.StartFirstAdventure] and the
 * lesson it would start is added by P2-T9 when there is a lesson to start. Showing a Continue that
 * led nowhere would be the screen making a promise the app cannot keep.
 */
@HiltViewModel
class ChildHomeViewModel @Inject constructor(private val profiles: ProfileRepository) :
    ViewModel() {

    private val _state = MutableStateFlow(EMPTY)
    val state: StateFlow<ChildHomeUiState> = _state.asStateFlow()

    fun start(profileId: ProfileId) {
        viewModelScope.launch {
            profiles.observeProfiles().collect { result ->
                if (result !is DomainResult.Success) return@collect
                val child = result.value.firstOrNull { it.id == profileId } ?: return@collect
                _state.value = _state.value.copy(
                    profileName = child.nickname,
                    profileAvatar = child.avatarId.value
                )
            }
        }
    }

    private companion object {
        val EMPTY = ChildHomeUiState(
            profileName = "",
            profileAvatar = "",
            greeting = "Let us find words together",
            greetingHint = "Pip has a little adventure ready for you.",
            primary = HomePrimary.StartFirstAdventure,
            pendingSave = false
        )
    }
}
