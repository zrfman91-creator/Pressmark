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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

internal sealed class GroupedData {
    data class None(val works: List<WorkEntityV2>) : GroupedData()
    data class Artist(val groups: List<ArtistGroup>) : GroupedData()
    data class Nested(val groups: List<NestedGroup>) : GroupedData()
}

internal data class ArtistGroup(
    val label: String,
    val works: List<WorkEntityV2>,
)

internal data class NestedGroup(
    val outerKey: GroupKey,
    val artists: List<ArtistGroup>,
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
    private var lastOuterGroupIds: List<String> = emptyList()
    private var lastNestedArtistIds: Set<String> = emptySet()

    init {
        val sortSpecFlow = libraryPreferences.sortSpecFlow
        val groupKeyFlow = libraryPreferences.groupKeyFlow
        val collapsedOuterFlow = libraryPreferences.collapsedGroupsFlow

        val groupedDataFlow = combine(sortSpecFlow, groupKeyFlow) { sortSpec, groupKey ->
            sortSpec to groupKey
        }.flatMapLatest { (sortSpec, groupKey) ->
            when (groupKey) {
                LibraryGroupKey.NONE -> workRepositoryV2.observeAllWorksSorted(sortSpec)
                    .map { works -> GroupedData.None(works) }
                LibraryGroupKey.ARTIST -> observeArtistGroups(sortSpec)
                    .map { groups -> GroupedData.Artist(groups) }
                LibraryGroupKey.YEAR -> observeYearGroups(sortSpec)
                    .map { groups -> GroupedData.Nested(groups) }
                LibraryGroupKey.DECADE -> observeDecadeGroups(sortSpec)
                    .map { groups -> GroupedData.Nested(groups) }
                LibraryGroupKey.GENRE -> observeGenreGroups(sortSpec)
                    .map { groups -> GroupedData.Nested(groups) }
                LibraryGroupKey.STYLE -> observeStyleGroups(sortSpec)
                    .map { groups -> GroupedData.Nested(groups) }
            }
        }

        // Keep group snapshots and keep nested state synced (in-memory only).
        viewModelScope.launch {
            combine(groupKeyFlow, groupedDataFlow) { groupKey, groupedData ->
                lastOuterGroupIds = extractOuterGroupIds(groupedData)
                lastNestedArtistIds = extractNestedArtistIds(groupedData)
                syncNestedArtistCollapsedIdsIfNeeded(groupKey = groupKey, nestedIds = lastNestedArtistIds)
            }.collect { /* no-op; side effects already applied */ }
        }

        // Build UI state.
        viewModelScope.launch {
            combine(
                sortSpecFlow,
                groupKeyFlow,
                collapsedOuterFlow,
                _nestedArtistCollapsedIds.asStateFlow(),
                groupedDataFlow,
            ) { sortSpec, groupKey, collapsedOuterIds, collapsedNestedIds, groupedData ->
                val items = buildLibraryItemsFromGroups(
                    groupedData = groupedData,
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

        viewModelScope.launch {
            // Expand => collapsed=false, Collapse => collapsed=true
            lastOuterGroupIds.forEach { id ->
                libraryPreferences.setGroupCollapsed(id, collapsed = !expand)
            }
        }

        if (isNestedMode(groupKey)) {
            _nestedArtistCollapsedIds.value = if (expand) emptySet() else lastNestedArtistIds
        } else {
            _nestedArtistCollapsedIds.value = emptySet()
        }
    }

    fun deleteWork(workId: String) {
        viewModelScope.launch { workRepositoryV2.deleteWork(workId) }
    }

    private fun syncNestedArtistCollapsedIdsIfNeeded(
        groupKey: LibraryGroupKey,
        nestedIds: Set<String>,
    ) {
        if (!isNestedMode(groupKey)) {
            lastNestedMode = null
            if (_nestedArtistCollapsedIds.value.isNotEmpty()) {
                _nestedArtistCollapsedIds.value = emptySet()
            }
            return
        }

        // Entering a nested mode: default ALL nested artists collapsed.
        if (lastNestedMode != groupKey) {
            lastNestedMode = groupKey
            _nestedArtistCollapsedIds.value = nestedIds
            return
        }

        // Same mode: keep existing, add new ids collapsed by default, remove stale.
        val current = _nestedArtistCollapsedIds.value
        val next = (current intersect nestedIds) + (nestedIds - current)
        if (next != current) _nestedArtistCollapsedIds.value = next
    }

    private fun observeArtistGroups(sortSpec: LibrarySortSpec) =
        workRepositoryV2.observeArtistHeadings().flatMapLatest { headings ->
            combineFlows(headings.map { artistLabel ->
                workRepositoryV2.observeWorksForArtist(artistLabel, sortSpec)
                    .map { works -> ArtistGroup(label = artistLabel, works = works) }
            })
        }

    private fun observeYearGroups(sortSpec: LibrarySortSpec) =
        workRepositoryV2.observeYearHeadings(sortSpec).flatMapLatest { years ->
            combineFlows(years.map { year ->
                val label = year?.toString() ?: "Unknown year"
                val outerKey = GroupKey.year(label)
                val artistsFlow = if (year == null) {
                    workRepositoryV2.observeArtistHeadingsForUnknownYear()
                } else {
                    workRepositoryV2.observeArtistHeadingsForYear(year)
                }
                artistsFlow.flatMapLatest { artists ->
                    combineFlows(artists.map { artistLabel ->
                        workRepositoryV2.observeWorksForYearAndArtist(year, artistLabel, sortSpec)
                            .map { works -> ArtistGroup(label = artistLabel, works = works) }
                    })
                }.map { artistGroups ->
                    NestedGroup(outerKey = outerKey, artists = artistGroups)
                }
            })
        }

    private fun observeDecadeGroups(sortSpec: LibrarySortSpec) =
        workRepositoryV2.observeDecadeHeadings(sortSpec).flatMapLatest { decades ->
            combineFlows(decades.map { decade ->
                val label = decade?.let { "${it}s" } ?: "Unknown year"
                val idValue = decade?.toString() ?: "unknown"
                val outerKey = GroupKey.decade(label, idValue)
                val artistsFlow = workRepositoryV2.observeArtistHeadingsForDecade(decade)
                artistsFlow.flatMapLatest { artists ->
                    combineFlows(artists.map { artistLabel ->
                        workRepositoryV2.observeWorksForDecadeAndArtist(decade, artistLabel, sortSpec)
                            .map { works -> ArtistGroup(label = artistLabel, works = works) }
                    })
                }.map { artistGroups ->
                    NestedGroup(outerKey = outerKey, artists = artistGroups)
                }
            })
        }

    private fun observeGenreGroups(sortSpec: LibrarySortSpec) =
        workRepositoryV2.observeGenreHeadings().flatMapLatest { headings ->
            combineFlows(headings.map { heading ->
                val outerKey = GroupKey.genre(heading.label)
                val isUnknown = heading.label.equals("Unknown genre", ignoreCase = true) &&
                    heading.normalized == "unknown genre"
                val artistsFlow = if (isUnknown) {
                    workRepositoryV2.observeArtistHeadingsForUnknownGenre()
                } else {
                    workRepositoryV2.observeArtistHeadingsForGenre(heading.normalized)
                }
                artistsFlow.flatMapLatest { artists ->
                    combineFlows(artists.map { artistLabel ->
                        workRepositoryV2.observeWorksForGenreAndArtist(
                            genreNormalized = if (isUnknown) null else heading.normalized,
                            artistLine = artistLabel,
                            sortSpec = sortSpec,
                        ).map { works -> ArtistGroup(label = artistLabel, works = works) }
                    })
                }.map { artistGroups ->
                    NestedGroup(outerKey = outerKey, artists = artistGroups)
                }
            })
        }

    private fun observeStyleGroups(sortSpec: LibrarySortSpec) =
        workRepositoryV2.observeStyleHeadings().flatMapLatest { headings ->
            combineFlows(headings.map { heading ->
                val outerKey = GroupKey.style(heading.label)
                val isUnknown = heading.label.equals("Unknown style", ignoreCase = true) &&
                    heading.normalized == "unknown style"
                val artistsFlow = if (isUnknown) {
                    workRepositoryV2.observeArtistHeadingsForUnknownStyle()
                } else {
                    workRepositoryV2.observeArtistHeadingsForStyle(heading.normalized)
                }
                artistsFlow.flatMapLatest { artists ->
                    combineFlows(artists.map { artistLabel ->
                        workRepositoryV2.observeWorksForStyleAndArtist(
                            styleNormalized = if (isUnknown) null else heading.normalized,
                            artistLine = artistLabel,
                            sortSpec = sortSpec,
                        ).map { works -> ArtistGroup(label = artistLabel, works = works) }
                    })
                }.map { artistGroups ->
                    NestedGroup(outerKey = outerKey, artists = artistGroups)
                }
            })
        }

    private fun extractOuterGroupIds(groupedData: GroupedData): List<String> = when (groupedData) {
        is GroupedData.None -> emptyList()
        is GroupedData.Artist -> groupedData.groups.map { GroupKey.artist(it.label).id }
        is GroupedData.Nested -> groupedData.groups.map { it.outerKey.id }
    }

    private fun extractNestedArtistIds(groupedData: GroupedData): Set<String> = when (groupedData) {
        is GroupedData.Nested -> groupedData.groups.flatMapTo(mutableSetOf()) { outer ->
            outer.artists.map { artist ->
                GroupKey.nestedArtist(parentId = outer.outerKey.id, artistLabel = artist.label).id
            }
        }
        else -> emptySet()
    }

    private fun <T> combineFlows(flows: List<kotlinx.coroutines.flow.Flow<T>>) =
        if (flows.isEmpty()) flowOf(emptyList()) else combine(flows) { it.toList() }
}

@VisibleForTesting
internal fun buildLibraryItemsFromGroups(
    groupedData: GroupedData,
    collapsedOuterIds: Set<String>,
    collapsedNestedIds: Set<String>,
): List<LibraryListItem> = when (groupedData) {
    is GroupedData.None -> groupedData.works.map { work ->
        LibraryListItem.Row(
            id = "row:${work.id}",
            item = work.toUi(),
            level = 0,
        )
    }

    is GroupedData.Artist -> {
        groupedData.groups.flatMap { group ->
            val key = GroupKey.artist(group.label)
            val expanded = !collapsedOuterIds.contains(key.id)
            val header = LibraryListItem.Header(
                id = key.id,
                title = key.label,
                count = group.works.size,
                isExpanded = expanded,
                level = 0,
            )
            val rows = if (expanded) {
                group.works.map { work ->
                    LibraryListItem.Row(
                        id = "row:${work.id}:${key.id}",
                        item = work.toUi(),
                        level = 1,
                    )
                }
            } else {
                emptyList()
            }
            listOf(header) + rows
        }
    }

    is GroupedData.Nested -> {
        groupedData.groups.flatMap { outer ->
            val outerExpanded = !collapsedOuterIds.contains(outer.outerKey.id)
            val outerCount = outer.artists.sumOf { it.works.size }
            val outerHeader = LibraryListItem.Header(
                id = outer.outerKey.id,
                title = outer.outerKey.label,
                count = outerCount,
                isExpanded = outerExpanded,
                level = 0,
            )
            if (!outerExpanded) return@flatMap listOf(outerHeader)

            val innerItems = outer.artists.flatMap { artistGroup ->
                val artistKey = GroupKey.nestedArtist(
                    parentId = outer.outerKey.id,
                    artistLabel = artistGroup.label,
                )
                val innerExpanded = !collapsedNestedIds.contains(artistKey.id)
                val innerHeader = LibraryListItem.Header(
                    id = artistKey.id,
                    title = artistKey.label,
                    count = artistGroup.works.size,
                    isExpanded = innerExpanded,
                    level = 1,
                )
                val rows = if (innerExpanded) {
                    artistGroup.works.map { work ->
                        LibraryListItem.Row(
                            id = "row:${work.id}:${outer.outerKey.id}:${artistKey.id}",
                            item = work.toUi(),
                            level = 2,
                        )
                    }
                } else {
                    emptyList()
                }
                listOf(innerHeader) + rows
            }
            listOf(outerHeader) + innerItems
        }
    }
}

private fun isNestedMode(groupKey: LibraryGroupKey): Boolean =
    groupKey != LibraryGroupKey.NONE && groupKey != LibraryGroupKey.ARTIST

private fun WorkEntityV2.toUi(): LibraryItemUi =
    LibraryItemUi(
        workId = id,
        title = title,
        artistLine = artistLine,
        year = year,
        artworkUri = primaryArtworkUri,
    )

internal fun normalizeForSort(raw: String, stripLeadingThe: Boolean = false): String {
    val trimmed = raw.trim().lowercase().replace(Regex("\\s+"), " ")
    if (!stripLeadingThe) return trimmed
    return trimmed.removePrefix("the ").trimStart()
}

internal data class GroupKey(
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
