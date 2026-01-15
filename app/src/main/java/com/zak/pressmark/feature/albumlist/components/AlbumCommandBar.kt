// =======================================================
// file: app/src/main/java/com/zak/pressmark/feature/albumlist/components/AlbumList.kt
// =======================================================
package com.zak.pressmark.feature.albumlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.rotate
import com.zak.pressmark.feature.albumlist.model.AlbumGrouping
import com.zak.pressmark.feature.albumlist.model.decadeLabel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zak.pressmark.core.ui.theme.AppTypography
import com.zak.pressmark.data.local.model.AlbumWithArtistName
import java.util.Locale

private enum class AlbumSort { ARTIST_AZ, TITLE_AZ, YEAR_DESC }

private fun String.norm(): String = trim().lowercase(Locale.ROOT)

private val SortOptions = listOf(
    CommandOption(id = "artist", label = "Artist (A–Z)"),
    CommandOption(id = "title", label = "Title (A–Z)"),
    CommandOption(id = "year", label = "Year (desc)"),
)

private val DensityOptions = listOf(
    CommandOption(id = "spacious", label = "Spacious"),
    CommandOption(id = "standard", label = "Standard"),
    CommandOption(id = "compact", label = "Compact"),
    CommandOption(id = "text_only", label = "Text Only"),
)

private fun AlbumWithArtistName.artistDisplaySafe(): String =
    artistDisplayName?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown Artist"

private fun AlbumWithArtistName.artistSortKey(): String =
    artistSortName?.trim().takeUnless { it.isNullOrBlank() }
        ?: artistDisplayName?.trim().takeUnless { it.isNullOrBlank() }
        ?: ""

@Composable
fun AlbumList(
    contentPadding: PaddingValues,
    albums: List<AlbumWithArtistName>,
    onAlbumClick: (AlbumWithArtistName) -> Unit,
    onDelete: (AlbumWithArtistName) -> Unit,
    onFindCover: (AlbumWithArtistName) -> Unit,
    onEdit: (AlbumWithArtistName) -> Unit,
    modifier: Modifier = Modifier,
    selectedAlbumId: String? = null,
    selectionEnabled: Boolean = true,
    dividerInset: Dp = 8.dp,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(AlbumSort.ARTIST_AZ) }
    var density by rememberSaveable { mutableStateOf(AlbumRowDensity.STANDARD) }

    var groupingName by rememberSaveable { mutableStateOf(AlbumGrouping.NONE.name) }
    val grouping = remember(groupingName) {
        runCatching { AlbumGrouping.valueOf(groupingName) }.getOrDefault(AlbumGrouping.NONE)
    }
    var expandedGroups by rememberSaveable { mutableStateOf(setOf<String>()) }


    var selectedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    val selectionActive = selectionEnabled && selectedIds.isNotEmpty()

    val filteredSorted = remember(albums, query, sort) {
        val q = query.trim()
        val filtered = if (q.isBlank()) {
            albums
        } else {
            albums.filter { row ->
                row.album.title.contains(q, ignoreCase = true) ||
                        row.artistDisplaySafe().contains(q, ignoreCase = true)
            }
        }

        when (sort) {
            AlbumSort.ARTIST_AZ ->
                filtered.sortedWith(
                    compareBy<AlbumWithArtistName> { it.artistSortKey().norm() }
                        .thenBy { it.album.title.norm() }
                )

            AlbumSort.TITLE_AZ ->
                filtered.sortedWith(
                    compareBy<AlbumWithArtistName> { it.album.title.norm() }
                        .thenBy { it.artistSortKey().norm() }
                )

            AlbumSort.YEAR_DESC ->
                filtered.sortedWith(
                    compareByDescending<AlbumWithArtistName> { it.album.releaseYear ?: Int.MIN_VALUE }
                        .thenBy { it.artistSortKey().norm() }
                        .thenBy { it.album.title.norm() }
                )
        }
    }

    val countText = if (query.isBlank()) "${albums.size}" else "${filteredSorted.size} results"

    val artistCountText = run {
        val distinctIds = filteredSorted
            .map { it.album.artistId ?: Long.MIN_VALUE }
            .distinct()
        distinctIds.size.toString()
    }

    val yearCountText = run {
        val distinctYears = filteredSorted
            .mapNotNull { it.album.releaseYear }
            .distinct()
        distinctYears.size.toString()
    }

    val selectedSortId = when (sort) {
        AlbumSort.ARTIST_AZ -> "artist"
        AlbumSort.TITLE_AZ -> "title"
        AlbumSort.YEAR_DESC -> "year"
    }

    val selectedDensityId = when (density) {
        AlbumRowDensity.SPACIOUS -> "spacious"
        AlbumRowDensity.STANDARD -> "standard"
        AlbumRowDensity.COMPACT -> "compact"
        AlbumRowDensity.TEXT_ONLY -> "text_only"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(top = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AlbumCommandBar(
                query = query,
                onQueryChange = { query = it },
                countText = countText,
                sortText = artistCountText,
                densityText = yearCountText,
                sortOptions = SortOptions,
                selectedSortId = selectedSortId,
                onSortSelected = { id ->
                    sort = when (id) {
                        "artist" -> AlbumSort.ARTIST_AZ
                        "title" -> AlbumSort.TITLE_AZ
                        "year" -> AlbumSort.YEAR_DESC
                        else -> sort
                    }
                },
                densityOptions = DensityOptions,
                selectedDensityId = selectedDensityId,
                onDensitySelected = { id ->
                    density = when (id) {
                        "spacious" -> AlbumRowDensity.SPACIOUS
                        "standard" -> AlbumRowDensity.STANDARD
                        "compact" -> AlbumRowDensity.COMPACT
                        "text_only" -> AlbumRowDensity.TEXT_ONLY
                        else -> density
                    }
                },
                grouping = grouping,
                onGroupingChange = { newGrouping ->
                    groupingName = newGrouping.name
                    if (newGrouping == AlbumGrouping.NONE) expandedGroups = emptySet()
                },
                selectionActive = selectionActive,
                selectionBar = {
                    if (!selectionActive) return@AlbumCommandBar
                    SelectionBar(
                        selectedCount = selectedIds.size,
                        onClear = { selectedIds = emptySet() },
                        onSelectAll = { selectedIds = filteredSorted.map { it.album.id }.toSet() },
                        onDeleteSelected = {
                            val byId = albums.associateBy { it.album.id }
                            selectedIds.forEach { id -> byId[id]?.let(onDelete) }
                            selectedIds = emptySet()
                        },
                        onRefreshArtworkSelected = {
                            // Intentionally left as a hook (no-op like your original)
                            selectedIds = selectedIds
                        },
                    )
                },
            )

            HorizontalDivider(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 4.dp)
                    .padding(horizontal = dividerInset),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
            )

            if (filteredSorted.isEmpty()) {
                EmptyState(
                    query = query,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    when (grouping) {
                        AlbumGrouping.NONE -> {
                            itemsIndexed(
                                items = filteredSorted,
                                key = { _, row -> row.album.id },
                            ) { index, row ->
                                val album = row.album

                                val isSelected =
                                    (selectionEnabled && selectedIds.contains(album.id)) || (selectedAlbumId == album.id)

                                AlbumListRow(
                                    row = row,
                                    isSelected = isSelected,
                                    selectionActive = selectionActive,
                                    onClick = {
                                        if (selectionActive) selectedIds = toggleId(selectedIds, album.id)
                                        else onAlbumClick(row)
                                    },
                                    onLongPress = {
                                        if (!selectionEnabled) return@AlbumListRow
                                        selectedIds = toggleId(selectedIds, album.id)
                                    },
                                    onDelete = { onDelete(row) },
                                    onFindCover = { onFindCover(row) },
                                    showDivider = index != filteredSorted.lastIndex,
                                    rowDensity = density,
                                    onEdit = { onEdit(row) },
                                )
                            }
                        }

                        AlbumGrouping.ARTIST -> {
                            val groups =
                                filteredSorted.groupBy { row -> row.album.artistId?.toString() ?: "unknown" }

                            val sortedKeys = groups.keys.sortedWith { a, b ->
                                val ra = groups[a]?.firstOrNull()
                                val rb = groups[b]?.firstOrNull()

                                val ka = ra?.artistSortKey()?.norm().orEmpty()
                                val kb = rb?.artistSortKey()?.norm().orEmpty()

                                // Unknown at bottom
                                when {
                                    a == "unknown" && b != "unknown" -> 1
                                    a != "unknown" && b == "unknown" -> -1
                                    else -> ka.compareTo(kb)
                                }
                            }

                            sortedKeys.forEach { artistKey ->
                                val rows = groups[artistKey].orEmpty()
                                if (rows.isEmpty()) return@forEach

                                val headerKey = "artist:$artistKey"
                                val expanded = expandedGroups.contains(headerKey)

                                item(key = headerKey) {
                                    GroupHeaderRow(
                                        title = rows.first().artistDisplaySafe(),
                                        subtitle = "${rows.size} album${if (rows.size == 1) "" else "s"}",
                                        expanded = expanded,
                                        onToggle = { expandedGroups = toggleId(expandedGroups, headerKey) },
                                    )
                                }

                                item(key = "$headerKey:content") {
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically(),
                                    ) {
                                        Column(modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)) {
                                            rows.forEachIndexed { index, row ->
                                                val album = row.album
                                                val isSelected =
                                                    (selectionEnabled && selectedIds.contains(album.id)) || (selectedAlbumId == album.id)

                                                AlbumListRow(
                                                    row = row,
                                                    isSelected = isSelected,
                                                    selectionActive = selectionActive,
                                                    onClick = {
                                                        if (selectionActive) selectedIds = toggleId(selectedIds, album.id)
                                                        else onAlbumClick(row)
                                                    },
                                                    onLongPress = {
                                                        if (!selectionEnabled) return@AlbumListRow
                                                        selectedIds = toggleId(selectedIds, album.id)
                                                    },
                                                    onDelete = { onDelete(row) },
                                                    onFindCover = { onFindCover(row) },
                                                    showDivider = index != rows.lastIndex,
                                                    rowDensity = density,
                                                    onEdit = { onEdit(row) },
                                                )
                                            }
                                        }
                                    }
                                }

                                item(key = "$headerKey:spacer") { Spacer(Modifier.height(6.dp)) }
                            }
                        }

                        AlbumGrouping.DECADE -> {
                            fun decadeStart(year: Int?): Int? = year?.let { (it / 10) * 10 }

                            val groups = filteredSorted.groupBy { row -> decadeStart(row.album.releaseYear) }

                            val sortedKeys = groups.keys.sortedWith { a, b ->
                                // Unknown at bottom, otherwise newer decades first
                                when {
                                    a == null && b != null -> 1
                                    a != null && b == null -> -1
                                    a == null && b == null -> 0
                                    else -> (b ?: 0).compareTo(a ?: 0)
                                }
                            }

                            sortedKeys.forEach { decade ->
                                val rows = groups[decade].orEmpty()
                                if (rows.isEmpty()) return@forEach

                                val headerKey = "decade:${decade ?: "unknown"}"
                                val expanded = expandedGroups.contains(headerKey)

                                item(key = headerKey) {
                                    GroupHeaderRow(
                                        title = decadeLabel(decade),
                                        subtitle = "${rows.size} album${if (rows.size == 1) "" else "s"}",
                                        expanded = expanded,
                                        onToggle = { expandedGroups = toggleId(expandedGroups, headerKey) },
                                    )
                                }

                                item(key = "$headerKey:content") {
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically(),
                                    ) {
                                        Column(modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)) {
                                            val sortedRows = rows.sortedWith(
                                                compareByDescending<AlbumWithArtistName> { it.album.releaseYear ?: Int.MIN_VALUE }
                                                    .thenBy { it.artistSortKey().norm() }
                                                    .thenBy { it.album.title.norm() }
                                            )

                                            sortedRows.forEachIndexed { index, row ->
                                                val album = row.album
                                                val isSelected =
                                                    (selectionEnabled && selectedIds.contains(album.id)) || (selectedAlbumId == album.id)

                                                AlbumListRow(
                                                    row = row,
                                                    isSelected = isSelected,
                                                    selectionActive = selectionActive,
                                                    onClick = {
                                                        if (selectionActive) selectedIds = toggleId(selectedIds, album.id)
                                                        else onAlbumClick(row)
                                                    },
                                                    onLongPress = {
                                                        if (!selectionEnabled) return@AlbumListRow
                                                        selectedIds = toggleId(selectedIds, album.id)
                                                    },
                                                    onDelete = { onDelete(row) },
                                                    onFindCover = { onFindCover(row) },
                                                    showDivider = index != sortedRows.lastIndex,
                                                    rowDensity = density,
                                                    onEdit = { onEdit(row) },
                                                )
                                            }
                                        }
                                    }
                                }

                                item(key = "$headerKey:spacer") { Spacer(Modifier.height(6.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun GroupHeaderRow(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }
    }
}

@Composable
private fun SelectionBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onRefreshArtworkSelected: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onSelectAll) {
            Icon(Icons.Filled.SelectAll, contentDescription = "Select filtered")
        }
        IconButton(onClick = onRefreshArtworkSelected) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh artwork for selected")
        }
        IconButton(onClick = onDeleteSelected) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
        }
    }
}

@Composable
private fun EmptyState(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(top = 72.dp),
            text = if (query.isBlank()) "No albums" else "No results",
            style = AppTypography.titleLarge,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (query.isBlank()) "Add albums to build your library." else "Try a different search term.",
            style = AppTypography.bodyMedium,
        )
    }
}

private fun toggleId(current: Set<String>, id: String): Set<String> =
    if (current.contains(id)) current - id else current + id
