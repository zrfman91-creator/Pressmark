package com.zak.pressmark.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OnboardingPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val onboardingSeenFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PREF_ONBOARDING_SEEN] ?: false
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[PREF_ONBOARDING_SEEN] = seen
        }
    }

    private companion object {
        private val PREF_ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    }
}
