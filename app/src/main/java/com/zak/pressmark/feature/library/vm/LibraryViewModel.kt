@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.zak.pressmark.feature.library.vm

import androidx.annotation.VisibleForTesting
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

    // Outer (top-level) collapse state is also IN-MEMORY ONLY.
    // => All headers start collapsed (outer + nested) each time you enter a grouping mode.
    private val _outerGroupCollapsedIds = MutableStateFlow<Set<String>>(emptySet())

    private var lastNestedMode: LibraryGroupKey? = null
    private var lastOuterMode: LibraryGroupKey? = null
    private var lastWorksSnapshot: List<WorkEntityV2> = emptyList()

    init {
        val sortSpecFlow = libraryPreferences.sortSpecFlow
        val groupKeyFlow = libraryPreferences.groupKeyFlow
        val worksFlow: Flow<List<WorkEntityV2>> =
            sortSpecFlow.flatMapLatest { spec ->
                workRepositoryV2.observeAllWorksSorted(spec)
            }

        // Keep a works snapshot and keep nested state synced (in-memory only).
        viewModelScope.launch {
            combine(groupKeyFlow, worksFlow) { groupKey, works ->
                lastWorksSnapshot = works
                syncOuterGroupCollapsedIdsIfNeeded(groupKey = groupKey, works = works)
                syncNestedArtistCollapsedIdsIfNeeded(groupKey = groupKey, works = works)
            }.collect { /* no-op; side effects already applied */ }
        }

        // Build UI state.
        viewModelScope.launch {
            combine(
                sortSpecFlow,
                groupKeyFlow,
                _outerGroupCollapsedIds.asStateFlow(),
                _nestedArtistCollapsedIds.asStateFlow(),
                worksFlow,
            ) { sortSpec, groupKey, collapsedOuterIds, collapsedNestedIds, works ->
                val items = buildLibraryItems(
                    works = works,
                    groupKey = groupKey,
                    sortSpec = sortSpec,
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
     * - Outer headers (level 0) are IN-MEMORY only (default collapsed).
     * - Nested artist headers (level 1 in nested modes) are IN-MEMORY only (default collapsed).
     */
    fun toggleGroupExpanded(groupId: String) {
        // Nested artist headers are encoded with "|artist:".
        val isNested = groupId.contains("|artist:")
        if (isNested) {
            val current = _nestedArtistCollapsedIds.value
            val isCollapsed = current.contains(groupId)
            _nestedArtistCollapsedIds.value = if (isCollapsed) current - groupId else current + groupId
            return
        }

        val current = _outerGroupCollapsedIds.value
        val isCollapsed = current.contains(groupId)
        _outerGroupCollapsedIds.value = if (isCollapsed) current - groupId else current + groupId
    }
    /**
     * Expand/collapse all headers for the current grouping mode.
     * - Outer groups are in-memory only (default collapsed)
     * - Nested artist groups are in-memory only (default collapsed)
     */
    fun toggleAllSections(expand: Boolean) {
        val groupKey = _uiState.value.groupKey
        if (groupKey == LibraryGroupKey.NONE) return

        val works = lastWorksSnapshot
        val outerIds = computeOuterGroupIds(groupKey, works).toSet()
        _outerGroupCollapsedIds.value = if (expand) emptySet() else outerIds

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


    private fun syncOuterGroupCollapsedIdsIfNeeded(
        groupKey: LibraryGroupKey,
        works: List<WorkEntityV2>,
    ) {
        if (groupKey == LibraryGroupKey.NONE) {
            lastOuterMode = null
            if (_outerGroupCollapsedIds.value.isNotEmpty()) {
                _outerGroupCollapsedIds.value = emptySet()
            }
            return
        }

        val allOuterIds = computeOuterGroupIds(groupKey, works).toSet()

        // Entering a grouping mode: default ALL outer headers collapsed.
        if (lastOuterMode != groupKey) {
            lastOuterMode = groupKey
            _outerGroupCollapsedIds.value = allOuterIds
            return
        }

        // Same mode: keep existing, add new ids collapsed by default, remove stale.
        val current = _outerGroupCollapsedIds.value
        val next = (current intersect allOuterIds) + (allOuterIds - current)
        if (next != current) _outerGroupCollapsedIds.value = next
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

    private fun computeOuterGroupIds(groupKey: LibraryGroupKey, works: List<WorkEntityV2>): List<String> {
        return computeOuterGroupIdsForWorks(groupKey, works)
    }

    private fun computeNestedArtistHeaderIds(groupKey: LibraryGroupKey, works: List<WorkEntityV2>): Set<String> {
        return computeNestedArtistHeaderIdsForWorks(groupKey, works)
    }

    private fun WorkEntityV2.genres(): List<String> = parseJsonList(genresJson)
    private fun WorkEntityV2.styles(): List<String> = parseJsonList(stylesJson)
}

@VisibleForTesting
internal fun buildLibraryItems(
    works: List<WorkEntityV2>,
    groupKey: LibraryGroupKey,
    sortSpec: LibrarySortSpec,
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

    val albumComparator = albumComparator(sortSpec)
    val headingComparator = headingComparator(groupKey, sortSpec)
    val artistComparator = artistComparator(sortSpec)

    if (groupKey == LibraryGroupKey.ARTIST) {
        val grouped = mutableMapOf<GroupKey, MutableList<WorkEntityV2>>()

        works.forEach { work ->
            val artistLabel = work.artistLabel()
            grouped.getOrPut(GroupKey.artist(artistLabel)) { mutableListOf() }.add(work)
        }

        val out = mutableListOf<LibraryListItem>()
        grouped.entries
            .sortedWith { left, right -> headingComparator.compare(left.key, right.key) }
            .forEach { (group, groupWorks) ->
                val expanded = !collapsedOuterIds.contains(group.id)
                val rows = groupWorks.sortedWith(albumComparator).map { work ->
                    LibraryListItem.Row(
                        id = "row:${work.id}:${group.id}",
                        item = work.toUi(),
                        level = 1,
                    )
                }
                out += LibraryListItem.Header(
                    id = group.id,
                    title = group.label,
                    count = rows.size,
                    isExpanded = expanded,
                    level = 0,
                )
                if (expanded) out += rows
            }

        return out
    }

    val outerMap = mutableMapOf<GroupKey, MutableMap<GroupKey, MutableList<WorkEntityV2>>>()

    fun addToOuter(outerKey: GroupKey, work: WorkEntityV2) {
        val artistLabel = work.artistLabel()
        val innerArtistKey = GroupKey.nestedArtist(parentId = outerKey.id, artistLabel = artistLabel)
        val innerMap = outerMap.getOrPut(outerKey) { mutableMapOf() }
        innerMap.getOrPut(innerArtistKey) { mutableListOf() }.add(work)
    }

    works.forEach { work ->
        when (groupKey) {
            LibraryGroupKey.YEAR -> addToOuter(GroupKey.year(work.yearLabel()), work)
            LibraryGroupKey.DECADE -> addToOuter(GroupKey.decade(work.decadeLabel(), work.decadeIdValue()), work)
            LibraryGroupKey.GENRE -> {
                val genres = work.genres()
                if (genres.isEmpty()) addToOuter(GroupKey.genre("Unknown genre"), work)
                else genres.forEach { genre -> addToOuter(GroupKey.genre(genre), work) }
            }

            LibraryGroupKey.STYLE -> {
                val styles = work.styles()
                if (styles.isEmpty()) addToOuter(GroupKey.style("Unknown style"), work)
                else styles.forEach { style -> addToOuter(GroupKey.style(style), work) }
            }

            LibraryGroupKey.NONE, LibraryGroupKey.ARTIST -> Unit
        }
    }

    val out = mutableListOf<LibraryListItem>()
    outerMap.entries
        .sortedWith { left, right -> headingComparator.compare(left.key, right.key) }
        .forEach { (outerKey, innerMap) ->
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

            innerMap.entries
                .sortedWith { left, right -> artistComparator.compare(left.key, right.key) }
                .forEach { (artistKey, rows) ->
                    val innerExpanded = !collapsedNestedIds.contains(artistKey.id)
                    out += LibraryListItem.Header(
                        id = artistKey.id,
                        title = artistKey.label,
                        count = rows.size,
                        isExpanded = innerExpanded,
                        level = 1,
                    )
                    if (innerExpanded) {
                        out += rows
                            .sortedWith(albumComparator)
                            .map { work ->
                                LibraryListItem.Row(
                                    id = "row:${work.id}:${outerKey.id}:${artistKey.id}",
                                    item = work.toUi(),
                                    level = 2,
                                )
                            }
                    }
                }
        }

    return out
}

private fun headingComparator(
    groupKey: LibraryGroupKey,
    sortSpec: LibrarySortSpec,
): Comparator<GroupKey> = when (groupKey) {
    LibraryGroupKey.GENRE, LibraryGroupKey.STYLE -> compareBy<GroupKey> { it.sortKey }.thenBy { it.label }
    LibraryGroupKey.ARTIST -> compareBy<GroupKey> { it.sortKey }.thenBy { it.label }
    LibraryGroupKey.YEAR, LibraryGroupKey.DECADE -> {
        val direction = if (sortSpec.key == LibrarySortKey.YEAR) sortSpec.direction else SortDirection.ASC
        Comparator { left, right ->
            val numeric = compareNullableInts(left.sortValue, right.sortValue, direction)
            if (numeric != 0) numeric else left.sortKey.compareTo(right.sortKey)
        }
    }

    LibraryGroupKey.NONE -> compareBy<GroupKey> { it.sortKey }
}

private fun artistComparator(sortSpec: LibrarySortSpec): Comparator<GroupKey> {
    val direction = if (sortSpec.key == LibrarySortKey.ARTIST) sortSpec.direction else SortDirection.ASC
    return Comparator { left, right ->
        val primary = if (direction == SortDirection.ASC) {
            left.sortKey.compareTo(right.sortKey)
        } else {
            right.sortKey.compareTo(left.sortKey)
        }
        if (primary != 0) primary else left.label.compareTo(right.label)
    }
}

private fun albumComparator(sortSpec: LibrarySortSpec): Comparator<WorkEntityV2> = when (sortSpec.key) {
    LibrarySortKey.ARTIST -> compareBy<WorkEntityV2> { normalizeForSort(it.title) }
    LibrarySortKey.TITLE -> {
        val base = compareBy<WorkEntityV2> { normalizeForSort(it.title) }
        if (sortSpec.direction == SortDirection.ASC) base else base.reversed()
    }

    LibrarySortKey.YEAR -> Comparator { left, right ->
        val primary = compareNullableInts(left.year, right.year, sortSpec.direction)
        if (primary != 0) primary else normalizeForSort(left.title).compareTo(normalizeForSort(right.title))
    }

    LibrarySortKey.RECENTLY_ADDED -> Comparator { left, right ->
        val primary = compareNullableLongs(left.createdAt, right.createdAt, sortSpec.direction)
        if (primary != 0) primary else normalizeForSort(left.title).compareTo(normalizeForSort(right.title))
    }
}

private fun compareNullableInts(left: Int?, right: Int?, direction: SortDirection): Int {
    if (left == null && right == null) return 0
    if (left == null) return if (direction == SortDirection.ASC) 1 else -1
    if (right == null) return if (direction == SortDirection.ASC) -1 else 1
    return if (direction == SortDirection.ASC) left.compareTo(right) else right.compareTo(left)
}

private fun compareNullableLongs(left: Long?, right: Long?, direction: SortDirection): Int {
    if (left == null && right == null) return 0
    if (left == null) return if (direction == SortDirection.ASC) 1 else -1
    if (right == null) return if (direction == SortDirection.ASC) -1 else 1
    return if (direction == SortDirection.ASC) left.compareTo(right) else right.compareTo(left)
}

private fun computeOuterGroupIdsForWorks(groupKey: LibraryGroupKey, works: List<WorkEntityV2>): List<String> {
    val ids = LinkedHashSet<String>()

    works.forEach { work ->
        when (groupKey) {
            LibraryGroupKey.ARTIST -> ids.add(GroupKey.artist(work.artistLabel()).id)
            LibraryGroupKey.YEAR -> ids.add(GroupKey.year(work.yearLabel()).id)
            LibraryGroupKey.DECADE -> ids.add(GroupKey.decade(work.decadeLabel(), work.decadeIdValue()).id)
            LibraryGroupKey.GENRE -> {
                val genres = work.genres()
                if (genres.isEmpty()) ids.add(GroupKey.genre("Unknown genre").id)
                else genres.forEach { genre -> ids.add(GroupKey.genre(genre).id) }
            }

            LibraryGroupKey.STYLE -> {
                val styles = work.styles()
                if (styles.isEmpty()) ids.add(GroupKey.style("Unknown style").id)
                else styles.forEach { style -> ids.add(GroupKey.style(style).id) }
            }

            LibraryGroupKey.NONE -> Unit
        }
    }

    return ids.toList()
}

private fun computeNestedArtistHeaderIdsForWorks(groupKey: LibraryGroupKey, works: List<WorkEntityV2>): Set<String> {
    if (!isNestedMode(groupKey)) return emptySet()

    val ids = LinkedHashSet<String>()

    works.forEach { work ->
        val artistLabel = work.artistLabel()

        fun addOuter(outerKey: GroupKey) {
            ids.add(GroupKey.nestedArtist(parentId = outerKey.id, artistLabel = artistLabel).id)
        }

        when (groupKey) {
            LibraryGroupKey.YEAR -> addOuter(GroupKey.year(work.yearLabel()))
            LibraryGroupKey.DECADE -> addOuter(GroupKey.decade(work.decadeLabel(), work.decadeIdValue()))
            LibraryGroupKey.GENRE -> {
                val genres = work.genres()
                if (genres.isEmpty()) addOuter(GroupKey.genre("Unknown genre"))
                else genres.forEach { genre -> addOuter(GroupKey.genre(genre)) }
            }

            LibraryGroupKey.STYLE -> {
                val styles = work.styles()
                if (styles.isEmpty()) addOuter(GroupKey.style("Unknown style"))
                else styles.forEach { style -> addOuter(GroupKey.style(style)) }
            }

            LibraryGroupKey.ARTIST, LibraryGroupKey.NONE -> Unit
        }
    }

    return ids
}

private fun isNestedMode(groupKey: LibraryGroupKey): Boolean =
    groupKey != LibraryGroupKey.NONE && groupKey != LibraryGroupKey.ARTIST

private fun WorkEntityV2.artistLabel(): String =
    artistLine.takeIf { it.isNotBlank() } ?: "Unknown artist"

private fun WorkEntityV2.yearLabel(): String =
    year?.toString() ?: "Unknown year"

private fun WorkEntityV2.decadeLabel(): String =
    year?.let { "${(it / 10) * 10}s" } ?: "Unknown year"

private fun WorkEntityV2.decadeIdValue(): String =
    year?.let { ((it / 10) * 10).toString() } ?: "unknown"

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

internal fun normalizeForSort(raw: String, stripLeadingThe: Boolean = false): String {
    val trimmed = raw.trim().lowercase().replace(Regex("\\s+"), " ")
    if (!stripLeadingThe) return trimmed
    return trimmed.removePrefix("the ").trimStart()
}

private data class GroupKey(
    val id: String,
    val label: String,
    val sortKey: String,
    val sortValue: Int? = null,
) {
    companion object {
        fun artist(label: String) = GroupKey(
            id = "group:artist:${normalizeKey(label)}",
            label = label,
            sortKey = normalizeForSort(label, stripLeadingThe = true),
        )

        fun year(label: String) = GroupKey(
            id = "group:year:${normalizeKey(label)}",
            label = label,
            sortKey = normalizeForSort(label),
            sortValue = label.toIntOrNull(),
        )

        fun decade(label: String, idValue: String) = GroupKey(
            id = "group:decade:$idValue",
            label = label,
            sortKey = normalizeForSort(label),
            sortValue = idValue.toIntOrNull(),
        )

        fun genre(label: String) = GroupKey(
            id = "group:genre:${normalizeKey(label)}",
            label = label,
            sortKey = normalizeForSort(label),
        )

        fun style(label: String) = GroupKey(
            id = "group:style:${normalizeKey(label)}",
            label = label,
            sortKey = normalizeForSort(label),
        )

        fun nestedArtist(parentId: String, artistLabel: String) = GroupKey(
            id = "${parentId}|artist:${normalizeKey(artistLabel)}",
            label = artistLabel,
            sortKey = normalizeForSort(artistLabel, stripLeadingThe = true),
        )

        private fun normalizeKey(raw: String): String =
            raw.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
