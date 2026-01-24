package com.zak.pressmark.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.catalogSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "catalog_settings")

enum class CatalogViewMode {
    LIST,
    GRID,
}

enum class CatalogDensity {
    COMPACT,
    SPACIOUS,
}

enum class CatalogSource {
    RELEASE,
    CATALOG_ITEM,
}

class CatalogSettingsRepository(
    context: Context,
) {
    private val dataStore = context.catalogSettingsDataStore

    fun observeViewMode(): Flow<CatalogViewMode> {
        return dataStore.data.map { prefs ->
            val raw = prefs[PreferencesKeys.VIEW_MODE] ?: CatalogViewMode.LIST.name
            runCatching { CatalogViewMode.valueOf(raw) }.getOrDefault(CatalogViewMode.LIST)
        }
    }

    fun observeDensity(): Flow<CatalogDensity> {
        return dataStore.data.map { prefs ->
            val raw = prefs[PreferencesKeys.DENSITY] ?: CatalogDensity.SPACIOUS.name
            runCatching { CatalogDensity.valueOf(raw) }.getOrDefault(CatalogDensity.SPACIOUS)
        }
    }

    fun observeCatalogSource(): Flow<CatalogSource> {
        return dataStore.data.map { prefs ->
            val raw = prefs[PreferencesKeys.SOURCE] ?: CatalogSource.RELEASE.name
            runCatching { CatalogSource.valueOf(raw) }.getOrDefault(CatalogSource.RELEASE)
        }
    }

    suspend fun setViewMode(mode: CatalogViewMode) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.VIEW_MODE] = mode.name
        }
    }

    suspend fun setDensity(density: CatalogDensity) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.DENSITY] = density.name
        }
    }

    suspend fun setCatalogSource(source: CatalogSource) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SOURCE] = source.name
        }
    }

    private object PreferencesKeys {
        val VIEW_MODE = stringPreferencesKey("catalog_view_mode")
        val DENSITY = stringPreferencesKey("catalog_density")
        val SOURCE = stringPreferencesKey("catalog_source")
    }
}
