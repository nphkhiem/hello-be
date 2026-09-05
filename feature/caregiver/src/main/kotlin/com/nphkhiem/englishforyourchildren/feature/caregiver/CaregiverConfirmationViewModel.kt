package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProgressRepository
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A destructive act, mid-flight.
 *
 * [done] is here rather than as a fourth [CaregiverConfirmationPhase] because finishing is not a
 * state the dialog draws: the host leaves when it happens. A phase for it would be a phase with no
 * appearance, and something would eventually have to render it.
 */
data class ConfirmationState(
    val kind: CaregiverConfirmationKind,
    val profileId: ProfileId?,
    val phase: CaregiverConfirmationPhase,
    val done: Boolean
)

/**
 * The two things a caregiver cannot take back.
 *
 * Deleting removes a child from this television. Resetting keeps the child and restarts what they
 * have learned. They run through one class because the shape is identical, and they are kept apart
 * by [CaregiverConfirmationKind] rather than by a boolean, so neither can quietly do the other's
 * work.
 *
 * A second confirm while the first is underway does nothing. On a television a button held a
 * moment too long sends two presses, and this is the one screen where doing the thing twice cannot
 * be undone.
 */
@HiltViewModel
class CaregiverConfirmationViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        ConfirmationState(
            kind = CaregiverConfirmationKind.RESET_PROGRESS,
            profileId = null,
            phase = CaregiverConfirmationPhase.READY,
            done = false
        )
    )

    val state: StateFlow<ConfirmationState> = _state.asStateFlow()

    fun start(kind: CaregiverConfirmationKind, profileId: ProfileId) {
        _state.value = ConfirmationState(
            kind = kind,
            profileId = profileId,
            phase = CaregiverConfirmationPhase.READY,
            done = false
        )
    }

    fun onConfirm() {
        if (_state.value.phase == CaregiverConfirmationPhase.WORKING) return
        if (_state.value.done) return
        run()
    }

    /** After a refusal, the same act again. Nothing was changed by the attempt that failed. */
    fun onRetry() {
        if (_state.value.phase == CaregiverConfirmationPhase.WORKING) return
        run()
    }

    private fun run() {
        val profileId = _state.value.profileId ?: return
        val kind = _state.value.kind

        viewModelScope.launch {
            _state.value = _state.value.copy(phase = CaregiverConfirmationPhase.WORKING)

            // Progress goes in both cases. What separates them is whether the child goes with it.
            val cleared = progress.deleteProfileProgress(profileId)
            if (cleared is DomainResult.Failure) {
                _state.value = _state.value.copy(phase = CaregiverConfirmationPhase.FAILED)
                return@launch
            }

            if (kind == CaregiverConfirmationKind.RESET_PROGRESS) {
                // The child stays, and stays selected. A reset that switched them out would be a
                // surprise dressed as a tidy-up.
                _state.value = _state.value.copy(
                    phase = CaregiverConfirmationPhase.READY,
                    done = true
                )
                return@launch
            }

            val removed = profiles.delete(profileId)
            if (removed is DomainResult.Failure) {
                _state.value = _state.value.copy(phase = CaregiverConfirmationPhase.FAILED)
                return@launch
            }

            // Nothing may still be selecting a child who is gone. This is the one place that can
            // be prevented rather than resolved afterwards, and the stale selection the brief
            // names is what happens when it is not.
            settings.updateSelectedProfile(null)

            _state.value = _state.value.copy(
                phase = CaregiverConfirmationPhase.READY,
                done = true
            )
        }
    }
}
