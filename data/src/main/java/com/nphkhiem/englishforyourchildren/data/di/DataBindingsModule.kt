package com.nphkhiem.englishforyourchildren.data.di

import com.nphkhiem.englishforyourchildren.data.SystemTimeProvider
import com.nphkhiem.englishforyourchildren.data.UuidProfileIdProvider
import com.nphkhiem.englishforyourchildren.data.curriculum.PackagedCurriculumRepository
import com.nphkhiem.englishforyourchildren.data.profile.RoomProfileRepository
import com.nphkhiem.englishforyourchildren.data.settings.DataStoreSettingsRepository
import com.nphkhiem.englishforyourchildren.domain.id.IdProvider
import com.nphkhiem.englishforyourchildren.domain.model.ProfileId
import com.nphkhiem.englishforyourchildren.domain.repository.CurriculumRepository
import com.nphkhiem.englishforyourchildren.domain.repository.ProfileRepository
import com.nphkhiem.englishforyourchildren.domain.repository.SettingsRepository
import com.nphkhiem.englishforyourchildren.domain.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Which implementation answers each contract. Everything above this line asks for the interface. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds
    @Singleton
    abstract fun bindProfileRepository(implementation: RoomProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        implementation: DataStoreSettingsRepository
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCurriculumRepository(
        implementation: PackagedCurriculumRepository
    ): CurriculumRepository

    @Binds
    abstract fun bindTimeProvider(implementation: SystemTimeProvider): TimeProvider

    @Binds
    abstract fun bindProfileIdProvider(implementation: UuidProfileIdProvider): IdProvider<ProfileId>
}
