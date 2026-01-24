package com.zak.pressmark.data.repository.v1

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.devSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dev_settings")

class DevSettingsRepository(
    context: Context,
) {
    private val dataStore = context.devSettingsDataStore

    fun observeOcrDebugOverlayEnabled(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[PreferencesKeys.OCR_DEBUG_OVERLAY] ?: false
        }
    }

    fun observeOcrLogCandidatesEnabled(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[PreferencesKeys.OCR_LOG_CANDIDATES] ?: false
        }
    }

    suspend fun setOcrDebugOverlayEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.OCR_DEBUG_OVERLAY] = enabled
        }
    }

    suspend fun setOcrLogCandidatesEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.OCR_LOG_CANDIDATES] = enabled
        }
    }

    private object PreferencesKeys {
        val OCR_DEBUG_OVERLAY = booleanPreferencesKey("ocr_debug_overlay_enabled")
        val OCR_LOG_CANDIDATES = booleanPreferencesKey("ocr_log_candidates_enabled")
    }
}
