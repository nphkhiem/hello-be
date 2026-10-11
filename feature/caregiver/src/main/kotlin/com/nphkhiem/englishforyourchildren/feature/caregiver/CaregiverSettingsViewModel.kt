package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
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
 * The six settings as they stand, whether the last change held, and where new lessons stand.
 *
 * The settings themselves rather than finished rows, for the reason [AdultGateState] holds a sum
 * rather than a question: every title, every child-facing consequence and every option name is a
 * piece of writing, and writing belongs where there are string resources and a caregiver language.
 * The host builds the rows; this holds the truth they are drawn from.
 *
 * [unreadable] is separate from the settings because a television whose preferences will not open
 * still has to show something, and what it shows is a recovery panel rather than a screen full of
 * defaults presented as the caregiver's own choices.
 */
data class CaregiverSettingsState(
    val settings: AppSettings,
    val saveStatus: SettingsSaveStatus,
    val canRestoreDefaults: Boolean,
    val unreadable: Boolean,
    /** Where getting new lessons stands, or null when this build offers no way to get them. */
    val lessons: TaskState?
)

/**
 * What a caregiver changes, and the truth about whether it was kept.
 *
 * Every write goes to storage and the answer comes back before this claims anything. A screen that
 * showed a switch as on because it had been pressed would be lying to the one person who has to be
 * able to trust it: a caregiver who turned captions on for a child who cannot follow without them.
 */
@HiltViewModel
class CaregiverSettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val lessonUpdates: LessonUpdates
) : ViewModel() {
    private val _state = MutableStateFlow(
        CaregiverSettingsState(
            settings = AppSettings.DEFAULT,
            saveStatus = SettingsSaveStatus.Idle,
            canRestoreDefaults = false,
            unreadable = false,
            lessons = null
        )
    )

    val state: StateFlow<CaregiverSettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.observeSettings().collect { read ->
                when (read) {
                    is DomainResult.Success -> _state.value = _state.value.copy(
                        settings = read.value,
                        canRestoreDefaults = read.value.differsFromDefaults(),
                        unreadable = false
                    )

                    is DomainResult.Failure ->
                        _state.value =
                            _state.value.copy(unreadable = true)
                }
            }
        }

        if (lessonUpdates.offered) {
            viewModelScope.launch {
                lessonUpdates.state.collect { _state.value = _state.value.copy(lessons = it) }
            }
            // Opening Settings is a caregiver asking, behind the adult gate, which is the one place
            // a fetch is allowed to happen in the foreground. The row then says something true on
            // arrival rather than a state nobody has looked at.
            viewModelScope.launch { lessonUpdates.check() }
        }
    }

    /**
     * The task row was pressed: fetch what is waiting, or look again.
     *
     * Nothing while working. The row is unavailable then, and a press that slipped through must
     * not start a second check underneath a download.
     */
    fun onTaskRequested() {
        val lessons = _state.value.lessons ?: return
        viewModelScope.launch {
            when (lessons) {
                TaskState.Available -> lessonUpdates.download()
                TaskState.UpToDate, is TaskState.Failed -> lessonUpdates.check()
                is TaskState.Working -> Unit
            }
        }
    }

    /**
     * Turns one switch over.
     *
     * The caregiver language is not a switch and says so by doing nothing here. Three modes cannot
     * be turned on and off, and picking one on the caregiver's behalf because a press arrived would
     * change the language they read by accident. Getting new lessons is not a switch either; it
     * arrives through [onTaskRequested].
     */
    fun onToggle(id: SettingId) {
        val now = _state.value.settings
        when (id) {
            SettingId.VIETNAMESE_HELP -> write {
                it.updateVietnameseHelp(!now.vietnameseHelpEnabled)
            }

            SettingId.CAPTIONS -> write { it.updateCaptions(!now.captionsEnabled) }

            SettingId.REDUCED_MOTION -> write { it.updateReducedMotion(!now.reducedMotionEnabled) }

            SettingId.HIGH_CONTRAST -> write { it.updateHighContrast(!now.highContrastEnabled) }

            SettingId.BACKGROUND_MUSIC ->
                write { it.updateBackgroundMusic(!now.backgroundMusicEnabled) }

            SettingId.CAREGIVER_LANGUAGE, SettingId.CHECK_FOR_LESSONS -> Unit
        }
    }

    fun onLanguageChosen(language: CaregiverLanguage) {
        write { it.updateCaregiverLanguage(language) }
    }

    /**
     * Puts the six settings back, and leaves the seventh thing alone.
     *
     * `AppSettings` also holds which child the television is on, and that is not one of the six
     * this screen offers. A caregiver tidying up their captions would not expect to be asked who is
     * learning today, so restoring defaults never touches it.
     */
    fun onRestoreDefaults() {
        val defaults = AppSettings.DEFAULT
        viewModelScope.launch {
            _state.value = _state.value.copy(saveStatus = SettingsSaveStatus.Saving)
            val results = listOf(
                settings.updateCaregiverLanguage(defaults.caregiverLanguage),
                settings.updateVietnameseHelp(defaults.vietnameseHelpEnabled),
                settings.updateCaptions(defaults.captionsEnabled),
                settings.updateReducedMotion(defaults.reducedMotionEnabled),
                settings.updateHighContrast(defaults.highContrastEnabled),
                settings.updateBackgroundMusic(defaults.backgroundMusicEnabled)
            )
            // One refusal is a failed restore. Reporting success because five of six landed would
            // leave a caregiver believing a setting went back when it did not.
            _state.value = _state.value.copy(saveStatus = statusOf(results))
        }
    }

    private fun write(change: suspend (SettingsRepository) -> DomainResult<Unit>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saveStatus = SettingsSaveStatus.Saving)
            _state.value = _state.value.copy(saveStatus = statusOf(listOf(change(settings))))
        }
    }

    private fun statusOf(results: List<DomainResult<Unit>>): SettingsSaveStatus =
        if (results.any { it is DomainResult.Failure }) {
            SettingsSaveStatus.Failed
        } else {
            SettingsSaveStatus.Idle
        }
}

/**
 * Whether any of the six a caregiver can change differs from how it ships.
 *
 * Which child is selected is deliberately not one of them: it is not offered on this screen, and a
 * restore that switched the child out would be a surprise rather than a tidy-up.
 */
private fun AppSettings.differsFromDefaults(): Boolean {
    val defaults = AppSettings.DEFAULT
    return caregiverLanguage != defaults.caregiverLanguage ||
        vietnameseHelpEnabled != defaults.vietnameseHelpEnabled ||
        captionsEnabled != defaults.captionsEnabled ||
        reducedMotionEnabled != defaults.reducedMotionEnabled ||
        highContrastEnabled != defaults.highContrastEnabled ||
        backgroundMusicEnabled != defaults.backgroundMusicEnabled
}
