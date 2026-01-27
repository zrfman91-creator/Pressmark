@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.zak.pressmark.feature.library.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.local.entity.v2.WorkEntityV2
import com.zak.pressmark.data.prefs.LibraryGroupKey
import com.zak.pressmark.data.prefs.LibraryPreferences
import com.zak.pressmark.data.prefs.LibrarySortKey
import com.zak.pressmark.data.prefs.LibrarySortSpec
import com.zak.pressmark.data.prefs.SortDirection
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

data class LibraryItemUi(
    val workId: String,
    val title: String,
    val artistLine: String,
    val year: Int?,
    val artworkUri: String?,
)

sealed class LibraryListItem {
    data class Header(
        val id: String,
        val title: String,
        val count: Int,
        val isExpanded: Boolean,
        val level: Int = 0,
    ) : LibraryListItem()

    data class Row(
        val id: String,
        val item: LibraryItemUi,
        val level: Int = 0,
    ) : LibraryListItem()
}

data class LibraryUiState(
    val items: List<LibraryListItem> = emptyList(),
    val sortSpec: LibrarySortSpec = LibrarySortSpec(LibrarySortKey.RECENTLY_ADDED, SortDirection.DESC),
    val groupKey: LibraryGroupKey = LibraryGroupKey.NONE,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val workRepositoryV2: WorkRepositoryV2,
    private val libraryPreferences: LibraryPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    // Nested (artist-under-outer-group) collapse state is IN-MEMORY ONLY.
    // => Nested expansion/collapse does NOT persist across app restarts.
    private val _nestedArtistCollapsedIds = MutableStateFlow<Set<String>>(emptySet())

    private var lastNestedMode: LibraryGroupKey? = null
    private var lastWorksSnapshot: List<WorkEntityV2> = emptyList()

    init {
        val sortSpecFlow = libraryPreferences.sortSpecFlow
        val groupKeyFlow = libraryPreferences.groupKeyFlow
        val collapsedOuterFlow = libraryPreferences.collapsedGroupsFlow

        val worksFlow: Flow<List<WorkEntityV2>> =
            sortSpecFlow.flatMapLatest { spec ->
                workRepositoryV2.observeAllWorksSorted(spec)
            }

        // Keep a works snapshot and keep nested state synced (in-memory only).
        viewModelScope.launch {
            combine(groupKeyFlow, worksFlow) { groupKey, works ->
                lastWorksSnapshot = works
                syncNestedArtistCollapsedIdsIfNeeded(groupKey = groupKey, works = works)
            }.collect { /* no-op; side effects already applied */ }
        }

        // Build UI state.
        viewModelScope.launch {
            combine(
                sortSpecFlow,
                groupKeyFlow,
                collapsedOuterFlow,
                _nestedArtistCollapsedIds.asStateFlow(),
                worksFlow,
            ) { sortSpec, groupKey, collapsedOuterIds, collapsedNestedIds, works ->
                val items = buildLibraryItems(
                    works = works,
                    groupKey = groupKey,
                    collapsedOuterIds = collapsedOuterIds,
                    collapsedNestedIds = collapsedNestedIds,
                )

                LibraryUiState(
                    items = items,
                    sortSpec = sortSpec,
                    groupKey = groupKey,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateSort(sortSpec: LibrarySortSpec) {
        viewModelScope.launch { libraryPreferences.setSortSpec(sortSpec) }
    }

    fun updateGroup(groupKey: LibraryGroupKey) {
        viewModelScope.launch { libraryPreferences.setGroupKey(groupKey) }
    }

    /**
     * Toggle a group header.
     * - Outer headers (level 0) persist via preferences.
     * - Nested artist headers (level 1 in nested modes) are IN-MEMORY only.
     */
    fun toggleGroupExpanded(groupId: String, isExpanded: Boolean) {
        if (groupId.contains("|artist:")) {
            val current = _nestedArtistCollapsedIds.value
            _nestedArtistCollapsedIds.value =
                if (isExpanded) current + groupId else current - groupId
            return
        }

        viewModelScope.launch {
            // isExpanded is the CURRENT state.
            // expanded -> collapse => collapsed=true
            // collapsed -> expand => collapsed=false
            libraryPreferences.setGroupCollapsed(groupId, collapsed = isExpanded)
        }
    }

    /**
     * Expand/collapse all headers for the current grouping mode.
     * - Outer groups persist
     * - Nested artist groups are in-memory only (reset on app restart)
     */
    fun toggleAllSections(expand: Boolean) {
        val groupKey = _uiState.value.groupKey
        if (groupKey == LibraryGroupKey.NONE) return

        val works = lastWorksSnapshot
        val outerIds = computeOuterGroupIds(groupKey, works)

        viewModelScope.launch {
            // Expand => collapsed=false, Collapse => collapsed=true
            outerIds.forEach { id ->
                libraryPreferences.setGroupCollapsed(id, collapsed = !expand)
            }
        }

        if (isNestedMode(groupKey)) {
            val nestedIds = computeNestedArtistHeaderIds(groupKey, works)
            _nestedArtistCollapsedIds.value = if (expand) emptySet() else nestedIds
        } else {
            _nestedArtistCollapsedIds.value = emptySet()
        }
    }

    fun deleteWork(workId: String) {
        viewModelScope.launch { workRepositoryV2.deleteWork(workId) }
    }

    private fun buildLibraryItems(
        works: List<WorkEntityV2>,
        groupKey: LibraryGroupKey,
        collapsedOuterIds: Set<String>,
        collapsedNestedIds: Set<String>,
    ): List<LibraryListItem> {
        if (groupKey == LibraryGroupKey.NONE) {
            return works.map { work ->
                LibraryListItem.Row(
                    id = "row:${work.id}",
                    item = work.toUi(),
                    level = 0,
                )
            }
        }

        // Artist grouping = single-level (persistent collapse)
        if (groupKey == LibraryGroupKey.ARTIST) {
            val grouped = LinkedHashMap<GroupKey, MutableList<LibraryListItem.Row>>()

            works.forEach { work ->
                val item = work.toUi()
                val label = item.artistLine.takeIf { it.isNotBlank() } ?: "Unknown artist"
                addGroupedRow(
                    grouped = grouped,
                    key = GroupKey.artist(label),
                    item = item,
                    rowLevel = 1,
                )
            }

            val out = mutableListOf<LibraryListItem>()
            grouped.forEach { (gk, rows) ->
                val expanded = !collapsedOuterIds.contains(gk.id)
                out += LibraryListItem.Header(
                    id = gk.id,
                    title = gk.label,
                    count = rows.size,
                    isExpanded = expanded,
                    level = 0,
                )
                if (expanded) out += rows
            }
            return out
        }

        // Nested modes: outer = selected grouping, inner = artist (non-persistent collapse)
        val outerMap =
            LinkedHashMap<GroupKey, LinkedHashMap<GroupKey, MutableList<LibraryListItem.Row>>>()

        fun addToOuter(outerKey: GroupKey, item: LibraryItemUi) {
            val artistLabel = item.artistLine.takeIf { it.isNotBlank() } ?: "Unknown artist"
            val innerArtistKey = GroupKey.nestedArtist(parentId = outerKey.id, artistLabel = artistLabel)

            val innerMap = outerMap.getOrPut(outerKey) { LinkedHashMap() }
            val rows = innerMap.getOrPut(innerArtistKey) { mutableListOf() }

            rows += LibraryListItem.Row(
                id = "row:${item.workId}:${outerKey.id}:${innerArtistKey.id}",
                item = item,
                level = 2,
            )
        }

        works.forEach { work ->
            val item = work.toUi()
            when (groupKey) {
                LibraryGroupKey.YEAR -> {
                    val label = work.year?.toString() ?: "Unknown year"
                    addToOuter(GroupKey.year(label), item)
                }

                LibraryGroupKey.DECADE -> {
                    val label = work.year?.let { "${(it / 10) * 10}s" } ?: "Unknown year"
                    val idValue = work.year?.let { ((it / 10) * 10).toString() } ?: "unknown"
                    addToOuter(GroupKey.decade(label, idValue), item)
                }

                LibraryGroupKey.GENRE -> {
                    val genres = work.genres()
                    if (genres.isEmpty()) addToOuter(GroupKey.genre("Unknown genre"), item)
                    else genres.forEach { g -> addToOuter(GroupKey.genre(g), item) }
                }

                LibraryGroupKey.STYLE -> {
                    val styles = work.styles()
                    if (styles.isEmpty()) addToOuter(GroupKey.style("Unknown style"), item)
                    else styles.forEach { s -> addToOuter(GroupKey.style(s), item) }
                }

                LibraryGroupKey.NONE, LibraryGroupKey.ARTIST -> Unit
            }
        }

        val out = mutableListOf<LibraryListItem>()

        outerMap.forEach { (outerKey, innerMap) ->
            val outerExpanded = !collapsedOuterIds.contains(outerKey.id)
            val outerCount = innerMap.values.sumOf { it.size }

            out += LibraryListItem.Header(
                id = outerKey.id,
                title = outerKey.label,
                count = outerCount,
                isExpanded = outerExpanded,
                level = 0,
            )

            if (!outerExpanded) return@forEach

            innerMap.forEach { (artistKey, rows) ->
                val innerExpanded = !collapsedNestedIds.contains(artistKey.id)
                out += LibraryListItem.Header(
                    id = artistKey.id,
                    title = artistKey.label,
                    count = rows.size,
                    isExpanded = innerExpanded,
                    level = 1,
                )
                if (innerExpanded) out += rows
            }
        }

        return out
    }

    private fun addGroupedRow(
        grouped: LinkedHashMap<GroupKey, MutableList<LibraryListItem.Row>>,
        key: GroupKey,
        item: LibraryItemUi,
        rowLevel: Int,
    ) {
        val rows = grouped.getOrPut(key) { mutableListOf() }
        rows += LibraryListItem.Row(
            id = "row:${item.workId}:${key.id}",
            item = item,
            level = rowLevel,
        )
    }

    private fun syncNestedArtistCollapsedIdsIfNeeded(
        groupKey: LibraryGroupKey,
        works: List<WorkEntityV2>,
    ) {
        if (!isNestedMode(groupKey)) {
            lastNestedMode = null
            if (_nestedArtistCollapsedIds.value.isNotEmpty()) {
                _nestedArtistCollapsedIds.value = emptySet()
            }
            return
        }

        val allNestedIds = computeNestedArtistHeaderIds(groupKey, works)

        // Entering a nested mode: default ALL nested artists collapsed.
        if (lastNestedMode != groupKey) {
            lastNestedMode = groupKey
            _nestedArtistCollapsedIds.value = allNestedIds
            return
        }

        // Same mode: keep existing, add new ids collapsed by default, remove stale.
        val current = _nestedArtistCollapsedIds.value
        val next = (current intersect allNestedIds) + (allNestedIds - current)
        if (next != current) _nestedArtistCollapsedIds.value = next
    }

    private fun isNestedMode(groupKey: LibraryGroupKey): Boolean =
        groupKey != LibraryGroupKey.NONE && groupKey != LibraryGroupKey.ARTIST

    private fun computeOuterGroupIds(groupKey: LibraryGroupKey, works: List<WorkEntityV2>): List<String> {
        val ids = LinkedHashSet<String>()

        works.forEach { work ->
            val item = work.toUi()
            when (groupKey) {
                LibraryGroupKey.ARTIST -> {
                    val label = item.artistLine.takeIf { it.isNotBlank() } ?: "Unknown artist"
                    ids.add(GroupKey.artist(label).id)
                }

                LibraryGroupKey.YEAR -> {
                    val label = work.year?.toString() ?: "Unknown year"
                    ids.add(GroupKey.year(label).id)
                }

                LibraryGroupKey.DECADE -> {
                    val label = work.year?.let { "${(it / 10) * 10}s" } ?: "Unknown year"
                    val idValue = work.year?.let { ((it / 10) * 10).toString() } ?: "unknown"
                    ids.add(GroupKey.decade(label, idValue).id)
                }

                LibraryGroupKey.GENRE -> {
                    val genres = work.genres()
                    if (genres.isEmpty()) ids.add(GroupKey.genre("Unknown genre").id)
                    else genres.forEach { g -> ids.add(GroupKey.genre(g).id) }
                }

                LibraryGroupKey.STYLE -> {
                    val styles = work.styles()
                    if (styles.isEmpty()) ids.add(GroupKey.style("Unknown style").id)
                    else styles.forEach { s -> ids.add(GroupKey.style(s).id) }
                }

                LibraryGroupKey.NONE -> Unit
            }
        }

        return ids.toList()
    }

    private fun computeNestedArtistHeaderIds(groupKey: LibraryGroupKey, works: List<WorkEntityV2>): Set<String> {
        if (!isNestedMode(groupKey)) return emptySet()

        val ids = LinkedHashSet<String>()

        works.forEach { work ->
            val item = work.toUi()
            val artistLabel = item.artistLine.takeIf { it.isNotBlank() } ?: "Unknown artist"

            fun addOuter(outerKey: GroupKey) {
                ids.add(GroupKey.nestedArtist(parentId = outerKey.id, artistLabel = artistLabel).id)
            }

            when (groupKey) {
                LibraryGroupKey.YEAR -> {
                    val label = work.year?.toString() ?: "Unknown year"
                    addOuter(GroupKey.year(label))
                }

                LibraryGroupKey.DECADE -> {
                    val label = work.year?.let { "${(it / 10) * 10}s" } ?: "Unknown year"
                    val idValue = work.year?.let { ((it / 10) * 10).toString() } ?: "unknown"
                    addOuter(GroupKey.decade(label, idValue))
                }

                LibraryGroupKey.GENRE -> {
                    val genres = work.genres()
                    if (genres.isEmpty()) addOuter(GroupKey.genre("Unknown genre"))
                    else genres.forEach { g -> addOuter(GroupKey.genre(g)) }
                }

                LibraryGroupKey.STYLE -> {
                    val styles = work.styles()
                    if (styles.isEmpty()) addOuter(GroupKey.style("Unknown style"))
                    else styles.forEach { s -> addOuter(GroupKey.style(s)) }
                }

                LibraryGroupKey.ARTIST, LibraryGroupKey.NONE -> Unit
            }
        }

        return ids
    }

    private fun WorkEntityV2.toUi(): LibraryItemUi =
        LibraryItemUi(
            workId = id,
            title = title,
            artistLine = artistLine,
            year = year,
            artworkUri = primaryArtworkUri,
        )

    private fun WorkEntityV2.genres(): List<String> = parseJsonList(genresJson)
    private fun WorkEntityV2.styles(): List<String> = parseJsonList(stylesJson)

    private fun parseJsonList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i).trim()
                    if (v.isNotBlank()) add(v)
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private data class GroupKey(
        val id: String,
        val label: String,
    ) {
        companion object {
            fun artist(label: String) = GroupKey("group:artist:${normalizeKey(label)}", label)
            fun year(label: String) = GroupKey("group:year:${normalizeKey(label)}", label)
            fun decade(label: String, idValue: String) = GroupKey("group:decade:$idValue", label)
            fun genre(label: String) = GroupKey("group:genre:${normalizeKey(label)}", label)
            fun style(label: String) = GroupKey("group:style:${normalizeKey(label)}", label)

            fun nestedArtist(parentId: String, artistLabel: String) = GroupKey(
                id = "${parentId}|artist:${normalizeKey(artistLabel)}",
                label = artistLabel,
            )

            private fun normalizeKey(raw: String): String =
                raw.trim().lowercase().replace(Regex("\\s+"), " ")
        }
    }
}
