package com.nphkhiem.englishforyourchildren.journey

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.nphkhiem.englishforyourchildren.data.local.HelloBeDatabase

/**
 * Storage a journey can start from nothing.
 *
 * Plain functions rather than a Dagger module, on purpose. A module in this source set annotated
 * `@TestInstallIn` would replace storage for every instrumented test, including `HiltGraphTest`,
 * whose whole job is to build the real graph and catch the bindings that only clash at runtime.
 * Each journey nests its own module and names what it uninstalls instead.
 */
object TestStorage {
    /** In memory, so every test gets a television with no child and no history on it. */
    fun database(context: Context): HelloBeDatabase =
        Room.inMemoryDatabaseBuilder(context, HelloBeDatabase::class.java).build()

    /** Named per graph, because a real file on disk would outlive the test that wrote it. */
    fun settings(context: Context): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("journey-settings-${System.nanoTime()}") }
    )
}
