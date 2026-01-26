package com.zak.pressmark.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val DATASTORE_NAME = "pressmark_prefs"

val Context.pressmarkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME,
)
