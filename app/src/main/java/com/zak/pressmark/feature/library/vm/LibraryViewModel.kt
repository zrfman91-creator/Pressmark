// FILE: app/src/main/java/com/zak/pressmark/feature/library/vm/LibraryViewModel.kt
package com.zak.pressmark.feature.library.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zak.pressmark.data.repository.v2.WorkRepositoryV2
import com.zak.pressmark.data.prefs.LibraryGroupKey
import com.zak.pressmark.data.prefs.LibraryPreferences
import com.zak.pressmark.data.prefs.LibrarySortKey
import com.zak.pressmark.data.prefs.LibrarySortSpec
import com.zak.pressmark.data.prefs.SortDirection
import com.zak.pressmark.data.util.Normalization
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    ) : LibraryListItem()

    data class Row(
        val id: String,
        val item: LibraryItemUi,
    ) : LibraryListItem()
}

data class LibraryUiState(
    val items: List<LibraryItemUi> = emptyList(),
    val listItems: List<LibraryListItem> = emptyList(),
    val sortSpec: LibrarySortSpec = LibrarySortSpec(LibrarySortKey.RECENTLY_ADDED, SortDirection.DESC),
    val groupKey: LibraryGroupKey = LibraryGroupKey.NONE,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val workRepositoryV2: WorkRepositoryV2,
    private val libraryPreferences: LibraryPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val sortSpecFlow = libraryPreferences.sortSpecFlow
        val groupKeyFlow = libraryPreferences.groupKeyFlow
        val collapsedFlow = libraryPreferences.collapsedGroupsFlow
        val worksFlow: Flow<List<com.zak.pressmark.data.local.entity.v2.WorkEntityV2>> =
            sortSpecFlow.flatMapLatest { spec ->
                workRepositoryV2.observeAllWorksSorted(spec)
            }

        viewModelScope.launch {
            combine(sortSpecFlow, groupKeyFlow, collapsedFlow, worksFlow) { sortSpec, groupKey, collapsedIds, works ->
                val listItems = buildLibraryItems(
                    works = works,
                    groupKey = groupKey,
                    collapsedGroupIds = collapsedIds,
                )
                val flatItems = works.map { it.toUi() }
                LibraryUiState(
                    items = flatItems,
                    listItems = listItems,
                    sortSpec = sortSpec,
                    groupKey = groupKey,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteWork(workId: String) {
        viewModelScope.launch {
            workRepositoryV2.deleteWork(workId)
        }
    }

    fun updateSort(spec: LibrarySortSpec) {
        viewModelScope.launch {
            libraryPreferences.setSortSpec(spec)
        }
    }

    fun updateGroup(groupKey: LibraryGroupKey) {
        viewModelScope.launch {
            libraryPreferences.setGroupKey(groupKey)
        }
    }

    fun toggleGroupExpanded(groupId: String, isExpanded: Boolean) {
        viewModelScope.launch {
            libraryPreferences.setGroupCollapsed(groupId, collapsed = !isExpanded)
        }
    }

    fun expandAll(groups: List<String>) {
        viewModelScope.launch {
            libraryPreferences.clearCollapsedGroupsForMode(groups)
        }
    }

    fun collapseAll(groups: List<String>) {
        viewModelScope.launch {
            libraryPreferences.setCollapsedGroupsForMode(groups)
        }
    }

    private fun buildLibraryItems(
        works: List<com.zak.pressmark.data.local.entity.v2.WorkEntityV2>,
        groupKey: LibraryGroupKey,
        collapsedGroupIds: Set<String>,
    ): List<LibraryListItem> {
        if (groupKey == LibraryGroupKey.NONE) {
            return works.map { work ->
                LibraryListItem.Row(
                    id = "row:${work.id}",
                    item = work.toUi(),
                )
            }
        }

        val grouped = LinkedHashMap<GroupKey, MutableList<LibraryListItem.Row>>()

        works.forEach { work ->
            val item = work.toUi()
            when (groupKey) {
                LibraryGroupKey.ARTIST -> {
                    val label = work.artistLine.takeIf { it.isNotBlank() } ?: "Unknown artist"
                    addGroupedRow(grouped, GroupKey.artist(label), item)
                }
                LibraryGroupKey.YEAR -> {
                    val label = work.year?.toString() ?: "Unknown year"
                    addGroupedRow(grouped, GroupKey.year(label), item)
                }
                LibraryGroupKey.DECADE -> {
                    val label = work.year?.let { "${(it / 10) * 10}s" } ?: "Unknown year"
                    val idValue = work.year?.let { ((it / 10) * 10).toString() } ?: "unknown"
                    addGroupedRow(grouped, GroupKey.decade(label, idValue), item)
                }
                LibraryGroupKey.GENRE -> {
                    val genres = work.genres()
                    if (genres.isEmpty()) {
                        addGroupedRow(grouped, GroupKey.genre("Unknown genre"), item)
                    } else {
                        genres.forEach { genre ->
                            addGroupedRow(grouped, GroupKey.genre(genre), item)
                        }
                    }
                }
                LibraryGroupKey.STYLE -> {
                    val styles = work.styles()
                    if (styles.isEmpty()) {
                        addGroupedRow(grouped, GroupKey.style("Unknown style"), item)
                    } else {
                        styles.forEach { style ->
                            addGroupedRow(grouped, GroupKey.style(style), item)
                        }
                    }
                }
                LibraryGroupKey.NONE -> Unit
            }
        }

        val items = mutableListOf<LibraryListItem>()
        grouped.forEach { (groupKeyItem, rows) ->
            val isExpanded = !collapsedGroupIds.contains(groupKeyItem.id)
            items.add(
                LibraryListItem.Header(
                    id = groupKeyItem.id,
                    title = groupKeyItem.label,
                    count = rows.size,
                    isExpanded = isExpanded,
                )
            )
            if (isExpanded) {
                items.addAll(rows)
            }
        }
        return items
    }

    private fun addGroupedRow(
        grouped: LinkedHashMap<GroupKey, MutableList<LibraryListItem.Row>>,
        key: GroupKey,
        item: LibraryItemUi,
    ) {
        val rows = grouped.getOrPut(key) { mutableListOf() }
        rows.add(
            LibraryListItem.Row(
                id = "row:${item.workId}:${key.id}",
                item = item,
            )
        )
    }

    private fun com.zak.pressmark.data.local.entity.v2.WorkEntityV2.toUi(): LibraryItemUi {
        return LibraryItemUi(
            workId = id,
            title = title,
            artistLine = artistLine,
            year = year,
            artworkUri = primaryArtworkUri,
        )
    }

    private fun com.zak.pressmark.data.local.entity.v2.WorkEntityV2.genres(): List<String> =
        parseJsonList(genresJson)

    private fun com.zak.pressmark.data.local.entity.v2.WorkEntityV2.styles(): List<String> =
        parseJsonList(stylesJson)

    private fun parseJsonList(raw: String): List<String> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val value = array.optString(i)?.trim().orEmpty()
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }

    private data class GroupKey(
        val id: String,
        val label: String,
    ) {
        companion object {
            fun artist(label: String) = GroupKey(
                id = "artist:${normalizeKey(label)}",
                label = label,
            )

            fun year(label: String) = GroupKey(
                id = "year:${normalizeKey(label)}",
                label = label,
            )

            fun decade(label: String, idValue: String) = GroupKey(
                id = "decade:${normalizeKey(idValue)}",
                label = label,
            )

            fun genre(label: String) = GroupKey(
                id = "genre:${normalizeKey(label)}",
                label = label,
            )

            fun style(label: String) = GroupKey(
                id = "style:${normalizeKey(label)}",
                label = label,
            )

            private fun normalizeKey(value: String): String {
                return Normalization.sortKey(value).replace(" ", "_").ifBlank { "unknown" }
            }
        }
    }
}
