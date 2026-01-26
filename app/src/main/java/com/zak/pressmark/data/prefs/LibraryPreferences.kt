package com.zak.pressmark.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class LibrarySortKey {
    TITLE,
    ARTIST,
    RECENTLY_ADDED,
    YEAR,
}

enum class SortDirection {
    ASC,
    DESC,
}

enum class LibraryGroupKey {
    NONE,
    ARTIST,
    GENRE,
    STYLE,
    DECADE,
    YEAR,
}

data class LibrarySortSpec(
    val key: LibrarySortKey,
    val direction: SortDirection,
)

@Singleton
class LibraryPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val sortSpecFlow: Flow<LibrarySortSpec> = dataStore.data.map { prefs ->
        val key = prefs[PREF_SORT_KEY]?.let { runCatching { LibrarySortKey.valueOf(it) }.getOrNull() }
            ?: LibrarySortKey.RECENTLY_ADDED
        val direction = prefs[PREF_SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
            ?: SortDirection.DESC
        LibrarySortSpec(key, direction)
    }

    val groupKeyFlow: Flow<LibraryGroupKey> = dataStore.data.map { prefs ->
        prefs[PREF_GROUP_KEY]?.let { runCatching { LibraryGroupKey.valueOf(it) }.getOrNull() }
            ?: LibraryGroupKey.NONE
    }

    val collapsedGroupsFlow: Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[PREF_COLLAPSED_GROUPS].orEmpty()
        decodeList(raw).toSet()
    }

    suspend fun setSortSpec(sortSpec: LibrarySortSpec) {
        dataStore.edit { prefs ->
            prefs[PREF_SORT_KEY] = sortSpec.key.name
            prefs[PREF_SORT_DIRECTION] = sortSpec.direction.name
        }
    }

    suspend fun setGroupKey(groupKey: LibraryGroupKey) {
        dataStore.edit { prefs ->
            prefs[PREF_GROUP_KEY] = groupKey.name
        }
    }

    suspend fun setGroupCollapsed(groupId: String, collapsed: Boolean) {
        dataStore.edit { prefs ->
            val current = decodeList(prefs[PREF_COLLAPSED_GROUPS].orEmpty()).toMutableList()
            current.remove(groupId)
            if (collapsed) {
                current.add(0, groupId)
            }
            prefs[PREF_COLLAPSED_GROUPS] = encodeList(current.take(MAX_COLLAPSED))
        }
    }

    suspend fun setCollapsedGroupsForMode(groupIds: List<String>) {
        dataStore.edit { prefs ->
            val current = decodeList(prefs[PREF_COLLAPSED_GROUPS].orEmpty()).toMutableList()
            groupIds.forEach { current.remove(it) }
            current.addAll(0, groupIds)
            prefs[PREF_COLLAPSED_GROUPS] = encodeList(current.take(MAX_COLLAPSED))
        }
    }

    suspend fun clearCollapsedGroupsForMode(groupIds: List<String>) {
        dataStore.edit { prefs ->
            val current = decodeList(prefs[PREF_COLLAPSED_GROUPS].orEmpty()).toMutableList()
            groupIds.forEach { current.remove(it) }
            prefs[PREF_COLLAPSED_GROUPS] = encodeList(current.take(MAX_COLLAPSED))
        }
    }

    private fun decodeList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(DELIMITER).filter { it.isNotBlank() }
    }

    private fun encodeList(values: List<String>): String {
        return values.joinToString(DELIMITER)
    }

    private companion object {
        private val PREF_SORT_KEY = stringPreferencesKey("library_sort_key")
        private val PREF_SORT_DIRECTION = stringPreferencesKey("library_sort_direction")
        private val PREF_GROUP_KEY = stringPreferencesKey("library_group_key")
        private val PREF_COLLAPSED_GROUPS = stringPreferencesKey("library_collapsed_groups_lru")
        private const val DELIMITER = "\u001F"
        private const val MAX_COLLAPSED = 200
    }
}
