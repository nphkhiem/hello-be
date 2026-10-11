package com.nphkhiem.englishforyourchildren.data.di

import com.nphkhiem.englishforyourchildren.content.validation.CurriculumJsonParser
import com.nphkhiem.englishforyourchildren.content.validation.CurriculumValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * How a course bundle is read and judged.
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
}
