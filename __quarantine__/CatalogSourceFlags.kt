package com.zak.pressmark.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Phase 0 migration flag: switch catalog list/detail source.
 *
 * false = release-first (current)
 * true  = catalog-item (master-first) source
 */
class CatalogSourceFlags(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_USE_CATALOG_ITEM = booleanPreferencesKey("use_catalog_item_source")
    }

    val useCatalogItemSource: Flow<Boolean> =
        dataStore.data.map { it[KEY_USE_CATALOG_ITEM] ?: false }

    suspend fun setUseCatalogItemSource(enabled: Boolean) {
        dataStore.edit { it[KEY_USE_CATALOG_ITEM] = enabled }
    }
}

val Context.catalogSourceDataStore: DataStore<Preferences> by preferencesDataStore(name = "catalog_source_flags")
