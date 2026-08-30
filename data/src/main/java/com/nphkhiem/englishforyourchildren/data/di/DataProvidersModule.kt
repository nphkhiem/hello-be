package com.nphkhiem.englishforyourchildren.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.nphkhiem.englishforyourchildren.data.local.HelloBeDatabase
import com.nphkhiem.englishforyourchildren.data.profile.ChildProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The things that have to be built rather than bound: the database, the preferences file, and the
 * DAO that comes out of the database.
 *
 * The graph lives here rather than in `:app`. Constructing Room and DataStore needs their types on
 * the compile classpath, and putting that in `:app` would let anything there reach a database
 * directly, which is the layering rule this project holds. Storage assembles itself and `:app` only
 * installs it.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HelloBeDatabase =
        // Deliberately no fallbackToDestructiveMigration. A database whose shape has moved on must
        // reach the caregiver recovery, where an adult decides whether to reset it, rather than
        // quietly deleting a child's progress on the way to a home screen.
        Room.databaseBuilder(context, HelloBeDatabase::class.java, HelloBeDatabase.NAME).build()

    @Provides
    fun provideChildProfileDao(database: HelloBeDatabase): ChildProfileDao =
        database.childProfileDao()

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            // A settings file that will not parse is replaced with nothing, and every setting then
            // falls back to its accessible default: captions and Vietnamese help on. Losing a
            // caregiver's choices is bad; switching a child's support off silently is worse.
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE) }
        )

    private const val SETTINGS_FILE = "hello-be-settings"
}
