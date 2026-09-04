package com.nphkhiem.englishforyourchildren.feature.caregiver

import androidx.compose.runtime.Composable
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage

/**
 * The six settings, written out.
 *
 * Here rather than in a ViewModel because every word of it is a string resource read in whichever
 * language the caregiver is using, and a ViewModel that reached for a `Context` to get one would
 * be a ViewModel that could not be tested without a device.
 *
 * The order and grouping are the approved screen's, not a preference: the information architecture
 * allows exactly three groups and this is what goes in them.
 *
 * Public because the host assembles the screen's state, the same way it composes the gate's
 * question. This is the module's mapping surface rather than an accident of visibility.
 */
@Composable
fun settingRows(settings: AppSettings): List<SettingRow> = listOf(
    SettingRow(
        id = SettingId.VIETNAMESE_HELP,
        group = SettingGroup.LANGUAGE,
        title = caregiverText(R.string.settings_vietnamese_help_title),
        consequence = caregiverText(R.string.settings_vietnamese_help_consequence),
        value = SettingValue.Toggle(on = settings.vietnameseHelpEnabled)
    ),
    SettingRow(
        id = SettingId.CAREGIVER_LANGUAGE,
        group = SettingGroup.LANGUAGE,
        title = caregiverText(R.string.settings_caregiver_language_title),
        consequence = caregiverText(R.string.settings_caregiver_language_consequence),
        value = SettingValue.Choice(
            current = languageName(settings.caregiverLanguage),
            options = CaregiverLanguage.entries.map { languageName(it) }
        )
    ),
    SettingRow(
        id = SettingId.CAPTIONS,
        group = SettingGroup.ACCESSIBILITY,
        title = caregiverText(R.string.settings_captions_title),
        consequence = caregiverText(R.string.settings_captions_consequence),
        value = SettingValue.Toggle(on = settings.captionsEnabled)
    ),
    SettingRow(
        id = SettingId.REDUCED_MOTION,
        group = SettingGroup.ACCESSIBILITY,
        title = caregiverText(R.string.settings_reduced_motion_title),
        consequence = caregiverText(R.string.settings_reduced_motion_consequence),
        value = SettingValue.Toggle(on = settings.reducedMotionEnabled)
    ),
    SettingRow(
        id = SettingId.HIGH_CONTRAST,
        group = SettingGroup.ACCESSIBILITY,
        title = caregiverText(R.string.settings_high_contrast_title),
        consequence = caregiverText(R.string.settings_high_contrast_consequence),
        value = SettingValue.Toggle(on = settings.highContrastEnabled)
    ),
    SettingRow(
        id = SettingId.BACKGROUND_MUSIC,
        group = SettingGroup.AUDIO,
        title = caregiverText(R.string.settings_background_music_title),
        consequence = caregiverText(R.string.settings_background_music_consequence),
        value = SettingValue.Toggle(on = settings.backgroundMusicEnabled)
    )
)

/**
 * What a language is called on the screen.
 *
 * Read through [caregiverText] like everything else here, so a caregiver reading Vietnamese meets
 * "Tiếng Anh" rather than "English". An earlier version of this comment claimed the opposite while
 * the code already did this, and the native speaker on the project settled it: a language name is
 * a word in a sentence, not a label that stands outside translation.
 */
@Composable
fun languageName(language: CaregiverLanguage): String = when (language) {
    CaregiverLanguage.ENGLISH -> caregiverText(R.string.settings_language_english)
    CaregiverLanguage.VIETNAMESE -> caregiverText(R.string.settings_language_vietnamese)
    CaregiverLanguage.BOTH -> caregiverText(R.string.settings_language_both)
}

/** The mode a name stands for, or null when it names nothing this app offers. */
fun languageNamed(name: String, names: Map<CaregiverLanguage, String>): CaregiverLanguage? =
    names.entries.firstOrNull { it.value == name }?.key
