package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Which language the caregiver area is read in, for as long as somebody is in it.
 *
 * Separate from [CaregiverSettingsViewModel] because the gate needs it too, and the gate stands in
 * front of the settings. A caregiver whose television is set to Vietnamese should meet Vietnamese
 * at the door rather than after opening it.
 *
 * Preferences that will not open fall back to [CaregiverLanguage.BOTH], which is the fallback
 * `CaregiverLanguage.from` already chose and for the same reason: it is the only mode that cannot
 * strand somebody who reads just one of the two.
 */
@HiltViewModel
class CaregiverLanguageViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    private val _language = MutableStateFlow(CaregiverLanguage.BOTH)

    val language: StateFlow<CaregiverLanguage> = _language.asStateFlow()

    init {
        viewModelScope.launch {
            settings.observeSettings().collect { read ->
                if (read is DomainResult.Success) _language.value = read.value.caregiverLanguage
            }
        }
    }
}
