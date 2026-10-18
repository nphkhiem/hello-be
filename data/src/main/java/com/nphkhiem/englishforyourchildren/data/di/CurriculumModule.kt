package com.nphkhiem.englishforyourchildren.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumValidator
import com.nphkhiem.englishforyourchildren.data.curriculum.CurriculumLoader
import com.nphkhiem.englishforyourchildren.data.curriculum.PackagedCurriculumRepository
import com.nphkhiem.englishforyourchildren.data.pack.ContentPackStore
import com.nphkhiem.englishforyourchildren.data.pack.LayeredCurriculumRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * How a course bundle is read and judged, and where the live one comes from.
 *
 * Built here because `:content:validation` is plain JVM and carries no injection annotations: the
 * content pipeline uses the same two classes and has no graph to be part of. Both hold no state, so
 * a fresh one per use costs nothing.
 *
 * A module of its own rather than a corner of `DataProvidersModule`, because journeys uninstall
 * that one to put their own storage in its place. Reading content is not storage, and a journey
 * replacing the database must not lose the parser with it.
 */
@Module
@InstallIn(SingletonComponent::class)
object CurriculumModule {
    @Provides
    fun provideCurriculumParser(): CurriculumJsonParser = CurriculumJsonParser()

    @Provides
    fun provideCurriculumValidator(): CurriculumValidator = CurriculumValidator()

    // Here rather than with storage for the same reason: a journey that swaps the database still
    // reads lessons through these. It brings its own preferences, and finds no packs on disk.
    @Provides
    @Singleton
    fun provideContentPackStore(
        @ApplicationContext context: Context,
        preferences: DataStore<Preferences>
    ): ContentPackStore = ContentPackStore(File(context.filesDir, PACKS_DIRECTORY), preferences)

    // Provided rather than bound, because its seed is itself a curriculum repository: asking the
    // graph for the contract here would ask for this very class.
    @Provides
    @Singleton
    fun provideLayeredCurriculumRepository(
        seed: PackagedCurriculumRepository,
        packs: ContentPackStore,
        loader: CurriculumLoader
    ): LayeredCurriculumRepository = LayeredCurriculumRepository(seed, packs, loader)

    private const val PACKS_DIRECTORY = "packs"
}
