package com.nphkhiem.englishforyourchildren.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * The names settings are stored under, and the file they live in.
 *
 * Keys are written once, here, because a typo in a key does not fail to compile: it silently reads
 * a setting nobody ever wrote and hands back a default, which looks exactly like a caregiver's
 * choice being forgotten.
 */
internal object SettingsKeys {
    const val FILE_NAME = "hello-be-settings"

    val SELECTED_PROFILE = stringPreferencesKey("selected_profile_id")
    val CAREGIVER_LOCALE = stringPreferencesKey("caregiver_locale_tag")
    val VIETNAMESE_HELP = booleanPreferencesKey("vietnamese_help_enabled")
    val CAPTIONS = booleanPreferencesKey("captions_enabled")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion_enabled")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast_enabled")
    val BACKGROUND_MUSIC = booleanPreferencesKey("background_music_enabled")
}
