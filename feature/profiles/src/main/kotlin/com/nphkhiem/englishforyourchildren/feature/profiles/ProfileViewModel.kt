package com.nphkhiem.englishforyourchildren.feature.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.AgeBand
import com.nphkhiem.englishforyourchildren.domain.model.AvatarId
import com.nphkhiem.englishforyourchildren.domain.model.ChildProfile as StoredProfile
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CreateProfile
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the picker and the create screen are looking at, and what happens when someone presses.
 *
 * The screens stay exactly as they were built: they take a state and emit an action. This is the
 * thing ADR 0003 said would arrive with the engineering roadmap, and it is the first code in a
 * feature module that knows a repository exists. It knows the contract, in `:domain`, and not the
 * database behind it.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _picker = MutableStateFlow(
        ProfilePickerUiState(profiles = emptyList(), rememberedProfileId = null, loading = true)
    )
    val picker: StateFlow<ProfilePickerUiState> = _picker.asStateFlow()

    /**
     * Whether storage refused to answer.
     *
     * Kept apart from the picker's own state on purpose. An empty picker invites a caregiver to add
     * a child, and a television that cannot read the children it already has must not do that, so
     * the host routes this to the caregiver recovery instead of drawing an empty shelf.
     */
    private val _storageUnreadable = MutableStateFlow(false)
    val storageUnreadable: StateFlow<Boolean> = _storageUnreadable.asStateFlow()

    /**
     * The child just written, or null.
     *
     * The host needs to know which profile to open a home for, and it cannot work that out by
     * watching the picker: the write and the next emission are not the same moment. Saying it
     * outright is simpler than diffing two lists and hoping.
     */
    private val _lastCreated = MutableStateFlow<ProfileId?>(null)
    val lastCreated: StateFlow<ProfileId?> = _lastCreated.asStateFlow()

    private var nameIndex = 0
    private var avatarIndex = 0

    private val _create = MutableStateFlow(
        CreateProfileUiState(
            draft = ProfileDraft(nickname = NAMES.first(), avatarId = AVATARS.first(), age = null),
            ages = AGES,
            avatarChoices = AVATARS,
            choosingAvatar = false,
            capacityReached = false,
            saveFailed = false
        )
    )
    val create: StateFlow<CreateProfileUiState> = _create.asStateFlow()

    init {
        viewModelScope.launch {
            profiles.observeProfiles().collect { result ->
                when (result) {
                    is DomainResult.Success -> {
                        _storageUnreadable.value = false
                        _picker.value = _picker.value.copy(
                            profiles = result.value.map { it.toPickerProfile() },
                            loading = false
                        )
                    }

                    is DomainResult.Failure -> {
                        _storageUnreadable.value = true
                        _picker.value = _picker.value.copy(profiles = emptyList(), loading = false)
                    }
                }
            }
        }
        viewModelScope.launch {
            settings.observeSettings().collect { result ->
                if (result is DomainResult.Success) {
                    _picker.value = _picker.value.copy(
                        rememberedProfileId = result.value.selectedProfileId?.value
                    )
                }
            }
        }
    }

    suspend fun onPickerAction(action: ProfileAction) {
        when (action) {
            // Remembering which child was chosen is what lets the next launch open on them rather
            // than asking again. Where to go next is the host's decision, not this one's.
            is ProfileAction.ProfileChosen ->
                settings.updateSelectedProfile(ProfileId(action.profileId))

            else -> Unit
        }
    }

    suspend fun onCreateAction(action: CreateProfileAction) {
        // Any edit clears a stale failure first. Leaving one up would tell a caregiver something
        // that stopped being true the moment they changed the form.
        if (action !is CreateProfileAction.CreateRequested) {
            _create.value = _create.value.copy(saveFailed = false, capacityReached = false)
        }

        when (action) {
            is CreateProfileAction.CreateRequested -> save(action.draft)

            // A television has no keyboard worth typing a name on, so the name is chosen by
            // cycling suggestions. The screen renders a draft and does not invent one, which is
            // why the cycling lives here and is reviewable.
            is CreateProfileAction.ChangeNameRequested -> {
                nameIndex = (nameIndex + 1) % NAMES.size
                editDraft { it.copy(nickname = NAMES[nameIndex]) }
            }

            is CreateProfileAction.ChangePictureRequested -> {
                avatarIndex = (avatarIndex + 1) % AVATARS.size
                editDraft { it.copy(avatarId = AVATARS[avatarIndex]) }
            }

            is CreateProfileAction.AvatarChosen -> editDraft { it.copy(avatarId = action.avatarId) }

            is CreateProfileAction.AgeChosen -> editDraft { it.copy(age = action.age) }

            else -> Unit
        }
    }

    private fun editDraft(edit: (ProfileDraft) -> ProfileDraft) {
        _create.value = _create.value.copy(draft = edit(_create.value.draft))
    }

    private suspend fun save(draft: ProfileDraft) {
        // A child written without an age would have a band this model invented. Not chosen yet is a
        // real state, and the screen already knows how to ask again.
        val ageBand = draft.age?.toAgeBand() ?: return

        val created = profiles.create(
            CreateProfile(
                nickname = draft.nickname,
                ageBand = ageBand,
                avatarId = AvatarId(draft.avatarId)
            )
        )
        _create.value = when {
            created is DomainResult.Failure && created.error == DomainError.ProfileLimitReached ->
                _create.value.copy(capacityReached = true, saveFailed = false)

            created is DomainResult.Failure ->
                _create.value.copy(saveFailed = true, capacityReached = false)

            else -> {
                _lastCreated.value = (created as DomainResult.Success).value.id
                _create.value.copy(saveFailed = false, capacityReached = false)
            }
        }
    }

    private fun StoredProfile.toPickerProfile() = ChildProfile(
        id = id.value,
        nickname = nickname,
        avatar = avatarId.value,
        summary = "",
        available = true
    )

    private fun Int.toAgeBand(): AgeBand? = when (this) {
        3 -> AgeBand.THREE
        4 -> AgeBand.FOUR
        5 -> AgeBand.FIVE
        else -> null
    }

    private companion object {
        val AGES = listOf(3, 4, 5)
        val AVATARS = listOf("rabbit", "bear", "cat", "duck")

        /**
         * Names a caregiver cycles through rather than types.
         *
         * A television remote is a poor keyboard, and asking a parent to spell a name on a grid is
         * the kind of friction that stops a profile being made at all. These are common Vietnamese
         * given names; the caregiver area is where a name can be changed later.
         */
        val NAMES = listOf("Bé", "Minh", "Lan", "An", "Mai", "Nam")
    }
}
