package com.zak.pressmark.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val autoReopenScannerFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PREF_AUTO_REOPEN] ?: false
    }

    suspend fun setAutoReopenScanner(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PREF_AUTO_REOPEN] = enabled
        }
    }

    private companion object {
        private val PREF_AUTO_REOPEN = booleanPreferencesKey("scanner_auto_reopen")
    }
}
