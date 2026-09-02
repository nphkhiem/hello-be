package com.nphkhiem.englishforyourchildren.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.nphkhiem.englishforyourchildren.domain.model.AppSettings
import com.nphkhiem.englishforyourchildren.domain.model.CaregiverLanguage
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.result.DomainError
import com.nphkhiem.englishforyourchildren.domain.result.DomainResult
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * What a caregiver has chosen, kept in DataStore.
 *
 * Two failures and two answers. A store that cannot be read at all is reported, because the same
 * disk holds the child's progress and pretending otherwise would be the app lying about itself. A
 * store that is present but unparseable is replaced with the defaults by the corruption handler
 * this repository is built with, and those defaults are the accessible ones, so a reset lands on
 * the side that keeps a child's support switched on.
 */
class DataStoreSettingsRepository @Inject constructor(private val store: DataStore<Preferences>) :
    SettingsRepository {

    override fun observeSettings(): Flow<DomainResult<AppSettings>> = store.data
        .map<Preferences, DomainResult<AppSettings>> { DomainResult.Success(it.toSettings()) }
        .catch { cause ->
            if (cause is IOException) {
                emit(DomainResult.Failure(DomainError.PersistenceUnavailable))
            } else {
                throw cause
            }
        }

    override suspend fun updateSelectedProfile(profileId: ProfileId?): DomainResult<Unit> = write {
        if (profileId == null) {
            it.remove(SettingsKeys.SELECTED_PROFILE)
        } else {
            it[SettingsKeys.SELECTED_PROFILE] = profileId.value
        }
    }

    override suspend fun updateCaregiverLanguage(language: CaregiverLanguage): DomainResult<Unit> =
        write {
            it[SettingsKeys.CAREGIVER_LOCALE] = language.stored
        }

    override suspend fun updateVietnameseHelp(enabled: Boolean): DomainResult<Unit> = write {
        it[SettingsKeys.VIETNAMESE_HELP] = enabled
    }

    override suspend fun updateCaptions(enabled: Boolean): DomainResult<Unit> = write {
        it[SettingsKeys.CAPTIONS] = enabled
    }

    override suspend fun updateReducedMotion(enabled: Boolean): DomainResult<Unit> = write {
        it[SettingsKeys.REDUCED_MOTION] = enabled
    }

    override suspend fun updateHighContrast(enabled: Boolean): DomainResult<Unit> = write {
        it[SettingsKeys.HIGH_CONTRAST] = enabled
    }

    override suspend fun updateBackgroundMusic(enabled: Boolean): DomainResult<Unit> = write {
        it[SettingsKeys.BACKGROUND_MUSIC] = enabled
    }

    private suspend fun write(
        edit: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ): DomainResult<Unit> = runCatching {
        store.edit(edit)
        DomainResult.Success(Unit)
    }.getOrElse { DomainResult.Failure(DomainError.PersistenceUnavailable) }

    /**
     * Anything absent falls back to the default for that one setting rather than to a whole default
     * object, so a store written before a setting existed keeps every choice a caregiver did make.
     */
    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings.DEFAULT
        return AppSettings(
            selectedProfileId = this[SettingsKeys.SELECTED_PROFILE]?.let { ProfileId(it) },
            caregiverLanguage = CaregiverLanguage.from(this[SettingsKeys.CAREGIVER_LOCALE]),
            vietnameseHelpEnabled = this[SettingsKeys.VIETNAMESE_HELP]
                ?: defaults.vietnameseHelpEnabled,
            captionsEnabled = this[SettingsKeys.CAPTIONS] ?: defaults.captionsEnabled,
            reducedMotionEnabled = this[SettingsKeys.REDUCED_MOTION]
                ?: defaults.reducedMotionEnabled,
            highContrastEnabled = this[SettingsKeys.HIGH_CONTRAST] ?: defaults.highContrastEnabled,
            backgroundMusicEnabled = this[SettingsKeys.BACKGROUND_MUSIC]
                ?: defaults.backgroundMusicEnabled
        )
    }
}
