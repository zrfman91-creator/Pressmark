package com.zak.pressmark.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ingestDataStore: DataStore<Preferences> by preferencesDataStore(name = "ingest_settings")

enum class IngestMode {
    SPEED,
    ACCURACY,
}

class IngestSettingsRepository(
    private val context: Context,
) {
    private val dataStore = context.ingestDataStore

    fun observeMode(): Flow<IngestMode> {
        return dataStore.data.map { prefs ->
            val raw = prefs[PreferencesKeys.INGEST_MODE] ?: IngestMode.SPEED.name
            runCatching { IngestMode.valueOf(raw) }.getOrDefault(IngestMode.SPEED)
        }
    }

    suspend fun setMode(mode: IngestMode) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.INGEST_MODE] = mode.name
        }
    }

    private object PreferencesKeys {
        val INGEST_MODE = stringPreferencesKey("ingest_mode")
    }
}
